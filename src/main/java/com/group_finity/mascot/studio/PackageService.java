package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 扩展包与皮肤包。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §4.4、§5.6、DR-12 / DR-15。
 * <p>
 * <b>落法 A（引擎零改动）</b>：扩展包在磁盘上是「片段 + 可选 PNG + manifest.json」，
 * 安装时把片段摊平进引擎能发现的 {@code conf/actions.d/}、{@code conf/behaviors.d/}；
 * manifest 只给设计器读，引擎永不读（不违反 P1）。
 * <p>
 * 安装前必做：L0–L4 校验（含基础名/保留名冲突，硬拒绝）、基础层指纹前后比对（AC-15-1）、
 * 按 manifest 清单记录写入文件（卸载时只删自己的，AC-15-4）。
 */
public final class PackageService {

    private static final Logger log = LoggerFactory.getLogger(PackageService.class);

    /** 扩展包片段统一占 50–89 号段，避免与基础/手写片段抢覆盖顺序。 */
    private static final int PACK_ORDER = 60;

    private final WorkspaceManager wm;
    private final ValidationService validation;

    public PackageService(WorkspaceManager wm, ValidationService validation) {
        this.wm = wm;
        this.validation = validation;
    }

    // ------------------------------------------------------------------
    // 注册表
    // ------------------------------------------------------------------

    private Path registryFile() {
        return wm.getStudioDir().resolve("packs").resolve("registry.json");
    }

    private JSONArray packs() {
        JSONObject registry = wm.readJson(registryFile(), new JSONObject());
        JSONArray arr = registry.getJSONArray("packs");
        return arr == null ? new JSONArray() : arr;
    }

    private void savePacks(JSONArray arr) throws IOException {
        JSONObject registry = new JSONObject(true);
        registry.put("packs", arr);
        wm.writeJson(registryFile(), registry);
    }

    public JSONObject listPacks() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < packs().size(); i++) {
            JSONObject pack = packs().getJSONObject(i);
            JSONObject view = new JSONObject(true);
            view.put("id", pack.getString("id"));
            view.put("name", pack.getString("name"));
            view.put("version", pack.getString("version"));
            view.put("author", pack.getString("author"));
            view.put("installedAt", pack.getString("installedAt"));
            view.put("targetSkin", pack.getString("targetSkin"));
            JSONArray files = pack.getJSONArray("files");
            int present = 0;
            int enabled = 0;
            JSONArray fileViews = new JSONArray();
            if (files != null) {
                for (int j = 0; j < files.size(); j++) {
                    JSONObject f = files.getJSONObject(j);
                    String rel = f.getString("path");
                    boolean disabled = rel.endsWith(".disabled");
                    Path p = wm.getOptions().getEngineRoot().resolve(rel);
                    if (Files.isRegularFile(p)) {
                        present++;
                    }
                    if (!disabled && Files.isRegularFile(p)) {
                        enabled++;
                    }
                    JSONObject fv = new JSONObject(true);
                    fv.put("path", rel);
                    fv.put("present", Files.isRegularFile(p));
                    fv.put("disabled", disabled);
                    fileViews.add(fv);
                }
            }
            view.put("fileCount", files == null ? 0 : files.size());
            view.put("presentCount", present);
            view.put("enabled", present > 0 && enabled > 0);
            view.put("files", fileViews);
            arr.add(view);
        }
        JSONObject out = new JSONObject(true);
        out.put("packs", arr);
        return out;
    }

    // ------------------------------------------------------------------
    // 安装 / 停用 / 卸载
    // ------------------------------------------------------------------

    /**
     * 安装扩展包 zip。
     * <p>
     * zip 结构：
     * <pre>
     * manifest.json                          {id,name,version,author,targetSkin}
     * actions.d/50-my-pack-wave.xml
     * behaviors.d/50-my-pack-wave.xml
     * img/<skin>/xxx.png                     （可选，安装进目标皮肤）
     * </pre>
     */
    public JSONObject installPack(byte[] zipBytes) {
        Path staging = null;
        Map<String, String> fingerprintBefore = wm.baselineFingerprint();
        try {
            staging = Files.createTempDirectory("shimeji-studio-pack-");
            extract(zipBytes, staging);
            JSONObject manifest = readManifest(staging);
            String id = WorkspaceManager.requireSafeName(manifest.getString("id"), "包 id");
            String targetSkin = manifest.getString("targetSkin");
            if (targetSkin != null && !targetSkin.isBlank()) {
                targetSkin = WorkspaceManager.requireSafeName(targetSkin, "目标皮肤");
            } else {
                targetSkin = null;
            }

            List<PlannedFile> planned = plan(staging, targetSkin);
            if (planned.isEmpty()) {
                throw new StudioException(422, "包内没有可安装的片段或图片");
            }

            // 冲突：已存在且不是设计器生成的片段
            JSONArray conflicts = new JSONArray();
            List<ValidationService.Overlay> overlays = new ArrayList<>();
            for (PlannedFile f : planned) {
                Path target = wm.getOptions().getEngineRoot().resolve(f.relativePath);
                if (f.overlay != null) {
                    overlays.add(f.overlay);
                }
                if (Files.isRegularFile(target) && !wm.isGeneratedByStudio(target) && !f.allowOverwrite) {
                    conflicts.add(f.relativePath);
                }
            }
            if (!conflicts.isEmpty()) {
                throw new StudioException(409, "以下文件已存在且不是设计器生成的，拒绝安装：" + conflicts.toJSONString());
            }

            // 目标皮肤需要启用标记（R-D12）：必须在 dry-run 之前准备好，
            // 否则引擎不会把 img/<skin>/conf/actions.xml 注册为增量层，皮肤级片段校验不到。
            List<PlannedFile> extras = new ArrayList<>();
            Path createdMarker = null;
            if (targetSkin != null) {
                Files.createDirectories(wm.skinConfDir(targetSkin));
                Path marker = wm.skinConfDir(targetSkin).resolve("actions.xml");
                if (!Files.isRegularFile(marker)) {
                    PlannedFile pf = new PlannedFile();
                    pf.relativePath = wm.relativeToEngine(marker).toString();
                    pf.content = ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                            + "<!-- generated-by: shimeji-studio v1.0; do not hand-edit -->\n"
                            + "<Mascot>\n  <ActionList />\n</Mascot>\n").getBytes(StandardCharsets.UTF_8);
                    pf.created = true;
                    extras.add(pf);
                    createdMarker = marker;
                }
            }

            // L0–L4：基础名 / 保留名冲突硬拒绝（AC-15-2）
            JSONObject validationResult;
            try {
                validationResult = validation.validate(targetSkin, overlays);
            } catch (RuntimeException e) {
                if (createdMarker != null) {
                    Files.deleteIfExists(createdMarker);
                }
                throw e;
            }
            JSONObject summary = validationResult.getJSONObject("summary");
            if (summary != null && summary.getIntValue("errors") > 0) {
                if (createdMarker != null) {
                    Files.deleteIfExists(createdMarker);
                }
                JSONObject payload = new JSONObject(true);
                payload.put("ok", false);
                payload.put("error", "包校验未通过，未写入任何文件");
                payload.put("diagnostics", validationResult.getJSONArray("diagnostics"));
                payload.put("summary", summary);
                throw new DocumentService.StudioValidationException(payload);
            }

            JSONArray written = new JSONArray();
            List<PlannedFile> all = new ArrayList<>(planned);
            all.addAll(extras);
            try {
                for (PlannedFile f : all) {
                    Path target = wm.getOptions().getEngineRoot().resolve(f.relativePath);
                    wm.writeAtomic(target, f.content);
                    written.add(f.relativePath);
                }
            } catch (Exception e) {
                // 回滚：删除本次已写入的文件
                for (int i = 0; i < written.size(); i++) {
                    try {
                        Files.deleteIfExists(wm.getOptions().getEngineRoot().resolve(written.getString(i)));
                    } catch (IOException ignored) {
                        // best effort
                    }
                }
                throw e;
            }

            Map<String, String> fingerprintAfter = wm.baselineFingerprint();
            if (!fingerprintBefore.equals(fingerprintAfter)) {
                throw new StudioException(500, "安装后基础层文件发生变化，已中止（AC-15-1）");
            }

            // 注册表
            JSONArray registry = packs();
            JSONObject entry = new JSONObject(true);
            entry.put("id", id);
            entry.put("name", manifest.getString("name") == null ? id : manifest.getString("name"));
            entry.put("version", manifest.getString("version"));
            entry.put("author", manifest.getString("author"));
            entry.put("targetSkin", targetSkin);
            entry.put("installedAt", java.time.LocalDateTime.now().toString());
            entry.put("schemaVersion", manifest.getString("schemaVersion"));
            JSONArray fileArr = new JSONArray();
            for (int i = 0; i < written.size(); i++) {
                JSONObject f = new JSONObject(true);
                f.put("path", written.getString(i));
                fileArr.add(f);
            }
            entry.put("files", fileArr);
            for (int i = registry.size() - 1; i >= 0; i--) {
                if (id.equals(registry.getJSONObject(i).getString("id"))) {
                    registry.remove(i);
                }
            }
            registry.add(entry);
            savePacks(registry);

            // 保留包体，便于导出/查看
            Path packDir = wm.getPacksDir().resolve(id);
            deleteRecursively(packDir);
            Files.createDirectories(packDir);
            copyTree(staging, packDir);

            JSONObject out = new JSONObject(true);
            out.put("id", id);
            out.put("files", written);
            out.put("warnings", validationResult.getJSONArray("diagnostics"));
            out.put("needReload", true);
            return out;
        } catch (DocumentService.StudioValidationException e) {
            throw e;
        } catch (StudioException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pack install failed", e);
            throw new StudioException(500, "安装失败：" + Xml.shortMessage(e), e);
        } finally {
            deleteRecursively(staging);
        }
    }

    public JSONObject togglePack(String id, boolean enable) {
        try {
            JSONArray registry = packs();
            JSONObject pack = findPack(registry, id);
            JSONArray files = pack.getJSONArray("files");
            JSONArray changed = new JSONArray();
            for (int i = 0; i < files.size(); i++) {
                String rel = files.getJSONObject(i).getString("path");
                Path p = wm.getOptions().getEngineRoot().resolve(rel);
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                if (enable && rel.endsWith(".xml.disabled")) {
                    Path target = wm.getOptions().getEngineRoot().resolve(rel.substring(0, rel.length() - ".disabled".length()));
                    Files.move(p, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    files.getJSONObject(i).put("path", wm.relativeToEngine(target).toString());
                    changed.add(wm.relativeToEngine(target).toString());
                } else if (!enable && rel.endsWith(".xml")) {
                    Path target = wm.getOptions().getEngineRoot().resolve(rel + ".disabled");
                    Files.move(p, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    files.getJSONObject(i).put("path", wm.relativeToEngine(target).toString());
                    changed.add(wm.relativeToEngine(target).toString());
                }
            }
            savePacks(registry);
            JSONObject out = new JSONObject(true);
            out.put("id", id);
            out.put("enabled", enable);
            out.put("changed", changed);
            out.put("needReload", true);
            return out;
        } catch (StudioException e) {
            throw e;
        } catch (Exception e) {
            throw new StudioException(500, "切换启用状态失败：" + Xml.shortMessage(e), e);
        }
    }

    public JSONObject uninstallPack(String id) {
        try {
            JSONArray registry = packs();
            JSONObject pack = findPack(registry, id);
            JSONArray files = pack.getJSONArray("files");
            JSONArray removed = new JSONArray();
            if (files != null) {
                for (int i = 0; i < files.size(); i++) {
                    String rel = files.getJSONObject(i).getString("path");
                    Path p = wm.getOptions().getEngineRoot().resolve(rel);
                    if (Files.deleteIfExists(p)) {
                        removed.add(rel);
                    }
                    // 同步删除禁用态
                    Path disabled = wm.getOptions().getEngineRoot().resolve(rel + ".disabled");
                    if (Files.deleteIfExists(disabled)) {
                        removed.add(wm.relativeToEngine(disabled).toString());
                    }
                }
            }
            for (int i = registry.size() - 1; i >= 0; i--) {
                if (id.equals(registry.getJSONObject(i).getString("id"))) {
                    registry.remove(i);
                }
            }
            savePacks(registry);
            deleteRecursively(wm.getPacksDir().resolve(id));
            JSONObject out = new JSONObject(true);
            out.put("id", id);
            out.put("removed", removed);
            out.put("needReload", true);
            return out;
        } catch (StudioException e) {
            throw e;
        } catch (Exception e) {
            throw new StudioException(500, "卸载失败：" + Xml.shortMessage(e), e);
        }
    }

    private JSONObject findPack(JSONArray registry, String id) {
        for (int i = 0; i < registry.size(); i++) {
            if (id.equals(registry.getJSONObject(i).getString("id"))) {
                return registry.getJSONObject(i);
            }
        }
        throw new StudioException(404, "扩展包未安装: " + id);
    }

    // ------------------------------------------------------------------
    // 导出
    // ------------------------------------------------------------------

    /** 导出皮肤包：{@code img/<skin>/**} + 分发用 manifest（引擎永不读）。 */
    public byte[] exportSkin(String skinId, boolean includeStudioFragments) {
        Path dir = wm.skinDir(skinId);
        if (!Files.isDirectory(dir)) {
            throw new StudioException(404, "皮肤不存在: " + skinId);
        }
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            List<Path> files = new ArrayList<>();
            Files.walk(dir).filter(Files::isRegularFile).forEach(files::add);
            files.sort(Comparator.comparing(Path::toString));
            for (Path f : files) {
                String rel = "img/" + skinId + "/" + dir.relativize(f).toString().replace('\\', '/');
                entries.put(rel, Files.readAllBytes(f));
            }
            if (includeStudioFragments) {
                for (Path f : listFragments(wm.actionsDir())) {
                    if (wm.isGeneratedByStudio(f)) {
                        entries.put("shared/actions.d/" + f.getFileName(), Files.readAllBytes(f));
                    }
                }
                for (Path f : listFragments(wm.behaviorsDir())) {
                    if (wm.isGeneratedByStudio(f)) {
                        entries.put("shared/behaviors.d/" + f.getFileName(), Files.readAllBytes(f));
                    }
                }
            }
            JSONObject manifest = new JSONObject(true);
            manifest.put("schemaVersion", 1);
            manifest.put("kind", "shimeji-skin");
            manifest.put("skin", skinId);
            manifest.put("engineBaseline", com.wishes.constant.Constant.CURRENT_VERSION);
            manifest.put("createdAt", java.time.LocalDateTime.now().toString());
            JSONArray fileList = new JSONArray();
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                JSONObject f = new JSONObject(true);
                f.put("path", e.getKey());
                f.put("bytes", e.getValue().length);
                f.put("sha256", sha256(e.getValue()));
                fileList.add(f);
            }
            manifest.put("files", fileList);
            entries.put("manifest.json", manifest.toJSONString(true).getBytes(StandardCharsets.UTF_8));
            return zip(entries);
        } catch (IOException e) {
            throw new StudioException(500, "导出失败：" + Xml.shortMessage(e), e);
        }
    }

    /** 导出一个已安装扩展包（片段 + 可选 PNG + manifest）。 */
    public byte[] exportPack(String id) {
        Path packDir = wm.getPacksDir().resolve(id);
        if (!Files.isDirectory(packDir)) {
            throw new StudioException(404, "找不到包体缓存: " + id);
        }
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            List<Path> files = new ArrayList<>();
            Files.walk(packDir).filter(Files::isRegularFile).forEach(files::add);
            files.sort(Comparator.comparing(Path::toString));
            for (Path f : files) {
                entries.put(packDir.relativize(f).toString().replace('\\', '/'), Files.readAllBytes(f));
            }
            return zip(entries);
        } catch (IOException e) {
            throw new StudioException(500, "导出失败：" + Xml.shortMessage(e), e);
        }
    }

    // ------------------------------------------------------------------
    // 导入（两阶段：先分析冲突，再写入）
    // ------------------------------------------------------------------

    public JSONObject analyzeImport(byte[] zipBytes) {
        Path staging = null;
        try {
            staging = Files.createTempDirectory("shimeji-studio-import-");
            extract(zipBytes, staging);
            JSONArray entries = new JSONArray();
            JSONArray conflicts = new JSONArray();
            List<Path> files = new ArrayList<>();
            Files.walk(staging).filter(Files::isRegularFile).forEach(files::add);
            files.sort(Comparator.comparing(Path::toString));
            for (Path f : files) {
                String rel = staging.relativize(f).toString().replace('\\', '/');
                if ("manifest.json".equals(rel)) {
                    continue;
                }
                String target = rel;
                if (rel.startsWith("shared/")) {
                    target = "conf/" + rel.substring("shared/".length());
                }
                JSONObject item = new JSONObject(true);
                item.put("source", rel);
                item.put("target", target);
                item.put("bytes", Files.size(f));
                item.put("exists", Files.exists(wm.getOptions().getEngineRoot().resolve(target)));
                if (item.getBooleanValue("exists")) {
                    Path existing = wm.getOptions().getEngineRoot().resolve(target);
                    item.put("overwritable", wm.isGeneratedByStudio(existing));
                    conflicts.add(target);
                }
                entries.add(item);
            }
            JSONObject manifest = null;
            Path mf = staging.resolve("manifest.json");
            if (Files.isRegularFile(mf)) {
                manifest = JSON.parseObject(Files.readString(mf, StandardCharsets.UTF_8));
            }
            JSONObject out = new JSONObject(true);
            out.put("entries", entries);
            out.put("conflicts", conflicts);
            out.put("manifest", manifest);
            return out;
        } catch (Exception e) {
            throw new StudioException(400, "无法解析导入包：" + Xml.shortMessage(e));
        } finally {
            deleteRecursively(staging);
        }
    }

    /**
     * 写入导入包。已有同名文件若不是设计器生成的文件，默认跳过并在结果中列出（§5.6 导入规则）。
     */
    public JSONObject importSkin(byte[] zipBytes, boolean overwriteGenerated) {
        Path staging = null;
        try {
            staging = Files.createTempDirectory("shimeji-studio-import-");
            extract(zipBytes, staging);
            JSONArray written = new JSONArray();
            JSONArray skipped = new JSONArray();
            List<Path> files = new ArrayList<>();
            Files.walk(staging).filter(Files::isRegularFile).forEach(files::add);
            files.sort(Comparator.comparing(Path::toString));
            for (Path f : files) {
                String rel = staging.relativize(f).toString().replace('\\', '/');
                if ("manifest.json".equals(rel)) {
                    continue;
                }
                String targetRel = rel.startsWith("shared/") ? "conf/" + rel.substring("shared/".length()) : rel;
                Path target = wm.getOptions().getEngineRoot().resolve(targetRel).normalize();
                if (!target.startsWith(wm.getOptions().getEngineRoot().normalize())) {
                    throw new StudioException(400, "包内路径越界: " + rel);
                }
                if (Files.isRegularFile(target)) {
                    boolean studioOwned = wm.isGeneratedByStudio(target);
                    if (!studioOwned || !overwriteGenerated) {
                        skipped.add(targetRel + (studioOwned ? "（已存在，选择覆盖才写入）" : "（非设计器文件，默认不覆盖）"));
                        continue;
                    }
                }
                wm.writeAtomic(target, Files.readAllBytes(f));
                written.add(targetRel);
            }
            JSONObject out = new JSONObject(true);
            out.put("written", written);
            out.put("skipped", skipped);
            out.put("needReload", true);
            return out;
        } catch (StudioException e) {
            throw e;
        } catch (Exception e) {
            throw new StudioException(500, "导入失败：" + Xml.shortMessage(e));
        } finally {
            deleteRecursively(staging);
        }
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private static final class PlannedFile {
        String relativePath;
        byte[] content;
        ValidationService.Overlay overlay;
        boolean allowOverwrite;
        boolean created;
    }

    private List<PlannedFile> plan(Path staging, String targetSkin) throws IOException {
        List<PlannedFile> planned = new ArrayList<>();
        Path actionsDir = staging.resolve("actions.d");
        Path behaviorsDir = staging.resolve("behaviors.d");
        Path imgDir = staging.resolve("img");
        Path skinActionsDir = staging.resolve("skin-actions.d");
        Path skinBehaviorsDir = staging.resolve("skin-behaviors.d");

        planned.addAll(planXml(actionsDir, "actions", wm.actionsDir(), null));
        planned.addAll(planXml(behaviorsDir, "behaviors", wm.behaviorsDir(), null));
        if (targetSkin != null) {
            planned.addAll(planXml(skinActionsDir, "actions",
                    wm.skinConfDir(targetSkin).resolve("actions.d"), "skin-actions"));
            planned.addAll(planXml(skinBehaviorsDir, "behaviors",
                    wm.skinConfDir(targetSkin).resolve("behaviors.d"), "skin-behaviors"));
        }
        if (Files.isDirectory(imgDir) && targetSkin != null) {
            for (Path skin : Files.newDirectoryStream(imgDir)) {
                if (!Files.isDirectory(skin) || !skin.getFileName().toString().equals(targetSkin)) {
                    continue;
                }
                List<Path> imgs = new ArrayList<>();
                Files.walk(skin).filter(Files::isRegularFile).forEach(imgs::add);
                for (Path img : imgs) {
                    PlannedFile pf = new PlannedFile();
                    String rel = skin.relativize(img).toString().replace('\\', '/');
                    pf.relativePath = wm.relativeToEngine(wm.skinDir(targetSkin).resolve(rel)).toString();
                    pf.content = Files.readAllBytes(img);
                    planned.add(pf);
                }
            }
        }
        return planned;
    }

    private List<PlannedFile> planXml(Path sourceDir, String kind, Path targetDir, String skinKind) throws IOException {
        List<PlannedFile> list = new ArrayList<>();
        if (!Files.isDirectory(sourceDir)) {
            return list;
        }
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, "*.xml")) {
            for (Path p : stream) {
                files.add(p);
            }
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        for (Path p : files) {
            String name = p.getFileName().toString();
            if (!name.matches("^\\d{2}-.*\\.xml$")) {
                name = String.format("%02d-%s", PACK_ORDER, name);
            }
            PlannedFile pf = new PlannedFile();
            pf.relativePath = wm.relativeToEngine(targetDir.resolve(name)).toString();
            String xml = Files.readString(p, StandardCharsets.UTF_8);
            pf.content = xml.getBytes(StandardCharsets.UTF_8);
            pf.overlay = new ValidationService.Overlay(skinKind == null ? kind : skinKind, name, xml);
            pf.allowOverwrite = true;
            list.add(pf);
        }
        return list;
    }

    private JSONObject readManifest(Path staging) throws IOException {
        Path mf = staging.resolve("manifest.json");
        if (!Files.isRegularFile(mf)) {
            throw new StudioException(422, "包内缺少 manifest.json（至少需要 id 字段）");
        }
        JSONObject manifest;
        try {
            manifest = JSON.parseObject(Files.readString(mf, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new StudioException(422, "manifest.json 不是合法 JSON：" + Xml.shortMessage(e));
        }
        if (manifest == null || manifest.getString("id") == null) {
            throw new StudioException(422, "manifest.json 必须包含 id");
        }
        return manifest;
    }

    private static void extract(byte[] zipBytes, Path target) throws IOException {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new StudioException(400, "包内容为空");
        }
        if (zipBytes.length > 60 * 1024 * 1024) {
            throw new StudioException(413, "包超过 60MB");
        }
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("__MACOSX") || name.endsWith("/")) {
                    continue;
                }
                Path out = target.resolve(name).normalize();
                if (!out.startsWith(target.normalize())) {
                    throw new StudioException(400, "包内路径越界: " + name);
                }
                Files.createDirectories(out.getParent());
                try (java.io.OutputStream os = Files.newOutputStream(out)) {
                    copy(zip, os);
                }
            }
        }
    }

    private static void copy(InputStream in, java.io.OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                zip.putNextEntry(entry);
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static List<Path> listFragments(Path dir) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
            for (Path p : stream) {
                result.add(p);
            }
        } catch (IOException ignored) {
            // ignore
        }
        result.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return result;
    }

    private static String sha256(byte[] bytes) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walk(from).filter(Files::isRegularFile).forEach(files::add);
        for (Path f : files) {
            Path target = to.resolve(from.relativize(f).toString());
            Files.createDirectories(target.getParent());
            Files.copy(f, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }
}
