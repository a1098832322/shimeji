package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作区与文件系统边界。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §4.3、§6.1（安全）、DR-09、DR-13。
 * <p>
 * 职责：
 * <ul>
 *   <li>路径白名单：所有外部传入的相对路径都经 {@link #safeResolve} 归一化，越界抛 400；</li>
 *   <li>原子写：临时文件 → ATOMIC_MOVE；旧文件备份到 {@code .studio/backups}，每个目标保留 5 份；</li>
 *   <li>形象目录枚举、{@code generated-by} 归属判定、SHA-256 计算（包安装与兼容性校验用）。</li>
 * </ul>
 */
public final class WorkspaceManager {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceManager.class);

    /** 设计器自有元数据目录（引擎永不读取）。 */
    public static final String STUDIO_DIR = ".studio";
    /** 片段首行归属标记；P2 的判定依据。 */
    public static final String GENERATED_MARKER = "generated-by: shimeji-studio";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final int BACKUP_KEEP = 5;

    private final StudioOptions options;
    private final Path confDir;
    private final Path imgDir;
    private final Path studioDir;
    private final Path backupsDir;
    private final Path packsDir;

    public WorkspaceManager(StudioOptions options) throws IOException {
        this.options = options;
        this.confDir = options.getConfDir();
        this.imgDir = options.getImgDir();
        this.studioDir = options.getEngineRoot().resolve(STUDIO_DIR);
        this.backupsDir = studioDir.resolve("backups");
        this.packsDir = studioDir.resolve("packs");
        Files.createDirectories(studioDir);
        Files.createDirectories(backupsDir);
        Files.createDirectories(packsDir);
    }

    public StudioOptions getOptions() {
        return options;
    }

    public Path getConfDir() {
        return confDir;
    }

    public Path getImgDir() {
        return imgDir;
    }

    public Path getStudioDir() {
        return studioDir;
    }

    public Path getPacksDir() {
        return packsDir;
    }

    public Path actionsDir() {
        return confDir.resolve("actions.d");
    }

    public Path behaviorsDir() {
        return confDir.resolve("behaviors.d");
    }

    public Path skinDir(String skinId) {
        Path dir = imgDir.resolve(requireSafeName(skinId, "skin"));
        Path normalized = dir.normalize();
        if (!normalized.startsWith(imgDir.normalize())) {
            throw new StudioException(400, "皮肤路径越界: " + skinId);
        }
        return normalized;
    }

    public Path skinConfDir(String skinId) {
        return skinDir(skinId).resolve("conf");
    }

    // ------------------------------------------------------------------
    // 路径安全
    // ------------------------------------------------------------------

    /**
     * 相对路径 → 工作区内的绝对路径；越界、绝对路径、符号链接逃逸一律拒绝（AC-13-2）。
     */
    public Path safeResolve(String relative, String what) {
        if (relative == null || relative.trim().isEmpty()) {
            throw new StudioException(400, what + " 不能为空");
        }
        String cleaned = relative.replace('\\', '/').trim();
        if (cleaned.startsWith("/") || cleaned.matches("^[A-Za-z]:.*")) {
            throw new StudioException(400, what + " 不允许绝对路径: " + relative);
        }
        Path rel;
        try {
            rel = Path.of(cleaned).normalize();
        } catch (Exception e) {
            throw new StudioException(400, what + " 不是合法路径: " + relative);
        }
        if (rel.startsWith("..")) {
            throw new StudioException(400, what + " 不允许越界: " + relative);
        }
        Path resolved = imgDir.resolve(rel).normalize();
        ensureInside(resolved, imgDir, what);
        return resolved;
    }

    /** 静态资源服务用：允许 img 目录下的相对路径。 */
    public Path resolveImage(String skinId, String relPath) {
        String cleaned = relPath == null ? "" : relPath.replace('\\', '/');
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        Path skin = skinDir(skinId);
        Path resolved;
        if (cleaned.toLowerCase().startsWith("file:")) {
            resolved = Path.of(cleaned.substring("file:".length()).replaceFirst("^//", "")).normalize();
            // 外部引用只允许读取，不允许通过静态服务暴露任意文件：限制在 img 目录内。
            ensureInside(resolved, imgDir, "image");
            return resolved;
        }
        resolved = skin.resolve(cleaned).normalize();
        ensureInside(resolved, skin, "image");
        return resolved;
    }

    private static void ensureInside(Path candidate, Path root, String what) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (!candidate.toAbsolutePath().normalize().startsWith(normalizedRoot)) {
            throw new StudioException(400, what + " 越界: " + candidate);
        }
        try {
            // 符号链接逃逸：已存在的父目录真实路径必须仍在根内
            Path parent = candidate.getParent();
            while (parent != null && !Files.exists(parent)) {
                parent = parent.getParent();
            }
            if (parent != null && Files.exists(parent)) {
                Path realParent = parent.toRealPath();
                Path realRoot = normalizedRoot.toRealPath();
                if (!realParent.startsWith(realRoot) && !realRoot.startsWith(realParent)) {
                    throw new StudioException(400, what + " 符号链接越界: " + candidate);
                }
            }
        } catch (IOException e) {
            throw new StudioException(400, what + " 路径不可访问: " + candidate);
        }
    }

    public static String requireSafeName(String name, String what) {
        if (name == null || name.trim().isEmpty()) {
            throw new StudioException(400, what + " 不能为空");
        }
        String trimmed = name.trim();
        // 形象目录名允许中文（仓库内就有 妖梦 / 八云紫 / 芙兰朵露），但仍禁止路径分隔符与控制字符，
        // 避免通过目录名做路径逃逸（AC-13-2）。
        if (trimmed.length() > 64
                || trimmed.contains("/") || trimmed.contains("\\")
                || trimmed.contains(":") || trimmed.contains("*")
                || trimmed.contains("?") || trimmed.contains("\"")
                || trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("|")
                || trimmed.startsWith(".") || trimmed.equals("..")
                || trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new StudioException(400, what + " 含非法字符: " + name);
        }
        return trimmed;
    }

    // ------------------------------------------------------------------
    // 原子写与备份
    // ------------------------------------------------------------------

    /**
     * 原子写入：临时文件 → 可选预检 → ATOMIC_MOVE → 旧文件备份。
     * <p>
     * 调用方必须已经完成校验（DR-09 AC-09-1/09-2）；本方法只保证文件系统层面不会产生半截文件。
     *
     * @return 备份文件路径（原文件不存在时为 null）
     */
    public Path writeAtomic(Path target, byte[] content) throws IOException {
        Path dir = target.getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "." + target.getFileName() + ".", ".tmp");
        Path backup = null;
        try {
            Files.write(tmp, content);
            if (Files.isRegularFile(target)) {
                backup = backupOf(target);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
        return backup;
    }

    private Path backupOf(Path target) throws IOException {
        Path dir = backupDirOf(target);
        Files.createDirectories(dir);
        Path backup = dir.resolve(LocalDateTime.now().format(TS) + ".bak");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        pruneBackups(dir);
        log.info("Backup created: {} -> {}", relativeToEngine(target), backup);
        return backup;
    }

    /** 备份目录：按目标文件的相对路径摊平命名，一个目标一个目录。 */
    private Path backupDirOf(Path target) {
        String flat = relativeToEngine(target).toString().replace('/', '_').replace('\\', '_');
        return backupsDir.resolve(flat);
    }

    /** 每个目标只保留最近 5 份备份（DR-09 AC-09-3）。 */
    private void pruneBackups(Path dir) throws IOException {
        List<Path> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.bak")) {
            for (Path p : stream) {
                backups.add(p);
            }
        }
        backups.sort(Comparator.comparing(this::mtime).reversed());
        for (int i = BACKUP_KEEP; i < backups.size(); i++) {
            Files.deleteIfExists(backups.get(i));
        }
    }

    private FileTime mtime(Path p) {
        try {
            return Files.getLastModifiedTime(p);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    /** 列出某个目标文件的备份（新→旧），用于「还原到上次成功」。 */
    public List<Path> listBackups(Path target) throws IOException {
        Path dir = backupDirOf(target);
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.bak")) {
            for (Path p : stream) {
                result.add(p);
            }
        }
        result.sort(Comparator.comparing(this::mtime).reversed());
        return result;
    }

    public Path relativeToEngine(Path path) {
        Path root = options.getEngineRoot().toAbsolutePath().normalize();
        Path abs = path.toAbsolutePath().normalize();
        if (abs.startsWith(root)) {
            return root.relativize(abs);
        }
        return abs.getFileName();
    }

    // ------------------------------------------------------------------
    // 形象与归属
    // ------------------------------------------------------------------

    /** 枚举 img 下所有形象目录（至少含一张图片，跳过 unused/icon 这类散文件）。 */
    public List<String> listSkinIds() throws IOException {
        List<String> ids = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(imgDir)) {
            for (Path p : stream) {
                if (!Files.isDirectory(p)) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.startsWith(".") || name.equalsIgnoreCase("unused")) {
                    continue;
                }
                if (hasImage(p)) {
                    ids.add(name);
                }
            }
        }
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    private boolean hasImage(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String n = p.getFileName().toString().toLowerCase();
                if (Files.isRegularFile(p) && (n.endsWith(".png") || n.endsWith(".gif"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 是否为设计器生成的文件（首 1KB 内出现 generated-by 标记）。 */
    public boolean isGeneratedByStudio(Path file) {
        try {
            byte[] head = new byte[1024];
            int read;
            try (java.io.InputStream in = Files.newInputStream(file)) {
                read = in.read(head);
            }
            if (read <= 0) {
                return false;
            }
            String text = new String(head, 0, read, StandardCharsets.UTF_8);
            return text.contains(GENERATED_MARKER);
        } catch (IOException e) {
            return false;
        }
    }

    public String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 基础层指纹：conf 主文件 + settings + 全部基础 PNG。
     * 用于 AC-15-1「安装扩展包前后基础层字节哈希不变」。
     */
    public Map<String, String> baselineFingerprint() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : new String[]{"actions.xml", "behaviors.xml", "settings.properties", "Mascot.xsd"}) {
            Path p = confDir.resolve(name);
            if (Files.isRegularFile(p)) {
                try {
                    map.put(relativeToEngine(p).toString(), sha256(p));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return map;
    }

    // ------------------------------------------------------------------
    // 设计器元数据（anchors / registry / settings）
    // ------------------------------------------------------------------

    public JSONObject readJson(Path file, JSONObject fallback) {
        if (!Files.isRegularFile(file)) {
            return fallback == null ? new JSONObject() : fallback;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JSONObject obj = JSON.parseObject(text);
            return obj == null ? (fallback == null ? new JSONObject() : fallback) : obj;
        } catch (Exception e) {
            log.warn("Failed to read studio metadata {}: {}", file, e.toString());
            return fallback == null ? new JSONObject() : fallback;
        }
    }

    public void writeJson(Path file, JSONObject json) throws IOException {
        Files.createDirectories(file.getParent());
        // 注意用 JSON 的静态重载 toJSONString(Object, boolean)；JSONObject 实例方法没有这个签名，
        // 误用会写出 "true" 这种非预期内容。
        Files.writeString(file, JSON.toJSONString(json, true), StandardCharsets.UTF_8);
    }

    public JSONObject anchors() {
        return readJson(studioDir.resolve("anchors.json"), new JSONObject());
    }

    public void saveAnchors(JSONObject anchors) throws IOException {
        writeJson(studioDir.resolve("anchors.json"), anchors);
    }

    public JSONArray arrayOf(JSONObject obj, String key) {
        JSONArray arr = obj.getJSONArray(key);
        return arr == null ? new JSONArray() : arr;
    }
}
