package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 皮肤与素材。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §5.1、DR-02 / DR-03 / DR-04。
 * <p>
 * 只做「管理、切图、锚点、镜像、编排」：不提供逐像素绘图，不缩放图片（引擎按原始像素渲染）。
 * 锚点是设计器的编辑期默认值，最终仍然写进 {@code <Pose ImageAnchor="x,y">}，XML 才是事实来源。
 */
public final class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    private static final long MAX_IMPORT_BYTES = 12L * 1024 * 1024;

    private final WorkspaceManager wm;
    private final Map<String, int[]> dimensionCache = new ConcurrentHashMap<>();

    public AssetService(WorkspaceManager wm) {
        this.wm = wm;
    }

    // ------------------------------------------------------------------
    // 皮肤
    // ------------------------------------------------------------------

    public JSONObject listSkins() throws IOException {
        JSONArray arr = new JSONArray();
        for (String id : wm.listSkinIds()) {
            arr.add(skinInfo(id));
        }
        JSONObject out = new JSONObject(true);
        out.put("skins", arr);
        return out;
    }

    public JSONObject skinInfo(String id) throws IOException {
        Path dir = wm.skinDir(id);
        JSONObject info = new JSONObject(true);
        info.put("id", id);
        info.put("path", wm.relativeToEngine(dir).toString());
        int count = 0;
        String firstPng = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (Files.isRegularFile(p) && name.toLowerCase().endsWith(".png")) {
                    count++;
                    if (firstPng == null) {
                        firstPng = name;
                    }
                }
            }
        }
        JSONObject meta = readInfo(id);
        String preview = meta.getString("previewImage");
        if (preview == null || preview.isEmpty()) {
            preview = Files.isRegularFile(dir.resolve("shime1.png")) ? "shime1.png" : firstPng;
        }
        info.put("assetCount", count);
        info.put("previewImage", preview);
        info.put("previewUrl", preview == null ? null : "/api/assets/raw?skin=" + url(id) + "&path=" + url(preview));
        info.put("displayName", meta.getString("displayName"));
        info.put("info", meta);
        info.put("hasIncrement", Files.isRegularFile(wm.skinConfDir(id).resolve("actions.xml")));
        info.put("hasDescriptor", Files.isRegularFile(wm.skinConfDir(id).resolve("info.xml")));
        return info;
    }

    public JSONObject createSkin(String id, JSONObject info) throws IOException {
        String skinId = WorkspaceManager.requireSafeName(id, "皮肤 id");
        Path dir = wm.skinDir(skinId);
        if (Files.isDirectory(dir)) {
            throw new StudioException(409, "皮肤已存在: " + skinId);
        }
        Files.createDirectories(dir);
        log.info("Skin created: {}", skinId);
        JSONObject out = new JSONObject(true);
        out.put("id", skinId);
        if (info != null && !info.isEmpty()) {
            writeInfo(skinId, info, true);
            out.put("info", readInfo(skinId));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 素材
    // ------------------------------------------------------------------

    public JSONObject listAssets(String skinId) throws IOException {
        Path dir = wm.skinDir(skinId);
        if (!Files.isDirectory(dir)) {
            throw new StudioException(404, "皮肤不存在: " + skinId);
        }
        JSONArray arr = new JSONArray();
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString().toLowerCase();
                if (Files.isRegularFile(p) && (name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".bmp"))) {
                    files.add(p);
                }
            }
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        for (Path p : files) {
            String name = p.getFileName().toString();
            JSONObject item = new JSONObject(true);
            item.put("name", name);
            item.put("path", name);
            item.put("size", Files.size(p));
            int[] dim = dimension(p);
            item.put("width", dim[0]);
            item.put("height", dim[1]);
            item.put("url", "/api/assets/raw?skin=" + url(skinId) + "&path=" + url(name));
            item.put("thumbUrl", "/api/assets/thumb?skin=" + url(skinId) + "&path=" + url(name) + "&w=64");
            JSONObject anchor = anchorOf(skinId, name);
            item.put("anchor", anchor);
            item.put("anchorSource", anchor.getString("source"));
            item.put("missing", false);
            arr.add(item);
        }
        JSONObject out = new JSONObject(true);
        out.put("skin", skinId);
        out.put("assets", arr);
        return out;
    }

    private int[] dimension(Path file) {
        String key = file.toAbsolutePath() + ":" + mtime(file);
        int[] cached = dimensionCache.get(key);
        if (cached != null) {
            return cached;
        }
        int[] dim = new int[]{0, 0};
        try (java.io.InputStream in = Files.newInputStream(file)) {
            BufferedImage img = ImageIO.read(in);
            if (img != null) {
                dim = new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Exception e) {
            log.warn("Failed to read image dimension {}: {}", file, e.toString());
        }
        dimensionCache.put(key, dim);
        return dim;
    }

    private static long mtime(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 导入 PNG（支持一次多张，可选精灵图按网格切分）。
     * <p>
     * 切图规则：给 {@code grid{rows,cols}} 或 {@code cell{w,h}}，按行列裁成 N 张，
     * 命名为 {@code prefix + 序号 + .png}；不缩放、不改变像素（引擎按原始像素渲染）。
     */
    public JSONObject importAssets(String skinId, JSONObject body) throws IOException {
        Path dir = wm.skinDir(skinId);
        if (!Files.isDirectory(dir)) {
            throw new StudioException(404, "皮肤不存在: " + skinId);
        }
        JSONArray files = body.getJSONArray("files");
        if (files == null || files.isEmpty()) {
            throw new StudioException(400, "没有收到任何图片");
        }
        String prefix = body.getString("prefix");
        JSONObject grid = body.getJSONObject("grid");
        JSONObject cell = body.getJSONObject("cell");
        JSONArray written = new JSONArray();
        JSONArray warnings = new JSONArray();

        for (int i = 0; i < files.size(); i++) {
            JSONObject f = files.getJSONObject(i);
            String name = f.getString("name");
            String base64 = f.getString("base64");
            if (base64 == null || base64.isEmpty()) {
                continue;
            }
            if (base64.startsWith("data:")) {
                int comma = base64.indexOf(',');
                base64 = comma >= 0 ? base64.substring(comma + 1) : base64;
            }
            byte[] bytes;
            try {
                bytes = java.util.Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                throw new StudioException(400, "图片 base64 解码失败: " + name);
            }
            if (bytes.length > MAX_IMPORT_BYTES) {
                throw new StudioException(413, "单张图片超过 12MB: " + name);
            }
            BufferedImage image;
            try {
                image = ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new StudioException(400, "不是可识别的图片: " + name);
            }
            if (image == null) {
                throw new StudioException(400, "不是可识别的图片: " + name);
            }

            if (grid != null && grid.getIntValue("rows") > 0 && grid.getIntValue("cols") > 0) {
                int rows = grid.getIntValue("rows");
                int cols = grid.getIntValue("cols");
                int cw = image.getWidth() / cols;
                int ch = image.getHeight() / rows;
                String baseName = prefix != null && !prefix.isEmpty() ? prefix : stripExtension(name);
                int seq = 1;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        BufferedImage part = image.getSubimage(c * cw, r * ch, cw, ch);
                        String outName = baseName + seq++ + ".png";
                        writeImage(dir.resolve(outName), part);
                        written.add(outName);
                    }
                }
                warnings.add(name + " 已按 " + rows + "×" + cols + " 切成 " + (rows * cols) + " 张（"
                        + cw + "×" + ch + "）");
            } else if (cell != null && cell.getIntValue("w") > 0 && cell.getIntValue("h") > 0) {
                int cw = cell.getIntValue("w");
                int ch = cell.getIntValue("h");
                if (image.getWidth() % cw != 0 || image.getHeight() % ch != 0) {
                    warnings.add(name + " 的尺寸 " + image.getWidth() + "×" + image.getHeight()
                            + " 不是格子 " + cw + "×" + ch + " 的整数倍，边缘会被裁掉");
                }
                int cols = image.getWidth() / cw;
                int rows = image.getHeight() / ch;
                String baseName = prefix != null && !prefix.isEmpty() ? prefix : stripExtension(name);
                int seq = 1;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        BufferedImage part = image.getSubimage(c * cw, r * ch, cw, ch);
                        String outName = baseName + seq++ + ".png";
                        writeImage(dir.resolve(outName), part);
                        written.add(outName);
                    }
                }
            } else {
                String outName = prefix != null && !prefix.isEmpty()
                        ? prefix + (files.size() > 1 ? String.valueOf(i + 1) : "") + ".png"
                        : name.replaceAll("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]", "_");
                Path target = dir.resolve(outName).normalize();
                if (!target.startsWith(dir.normalize())) {
                    throw new StudioException(400, "文件名越界: " + outName);
                }
                writeImage(target, image);
                written.add(outName);
                if (image.getWidth() != 128 || image.getHeight() != 128) {
                    warnings.add(outName + " 为 " + image.getWidth() + "×" + image.getHeight()
                            + "，非 128×128；锚点需人工确认（引擎按原始像素渲染）");
                }
            }
        }
        JSONObject out = new JSONObject(true);
        out.put("written", written);
        out.put("warnings", warnings);
        return out;
    }

    /**
     * 写 PNG：先编码到内存，再走 {@link WorkspaceManager#writeAtomic}。
     * 这样覆盖既有素材时自动留备份，且不会出现写一半的损坏图片（SD-NFR-8）。
     */
    private void writeImage(Path target, BufferedImage image) throws IOException {
        BufferedImage argb = image;
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            argb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = argb.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(argb, "png", out);
        wm.writeAtomic(target, out.toByteArray());
    }

    private static String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(0, idx) : name;
    }

    /** 缩略图：按需生成并缓存到 {@code .studio/thumbs}（SD-NFR-5）。 */
    public byte[] thumbnail(String skinId, String relPath, int width) throws IOException {
        int w = Math.max(16, Math.min(512, width));
        Path source = wm.resolveImage(skinId, relPath);
        if (!Files.isRegularFile(source)) {
            throw new StudioException(404, "图片不存在: " + relPath);
        }
        Path cacheDir = wm.getStudioDir().resolve("thumbs");
        Files.createDirectories(cacheDir);
        String key = wm.sha256(source) + "-" + w + ".png";
        Path cached = cacheDir.resolve(key);
        if (Files.isRegularFile(cached)) {
            return Files.readAllBytes(cached);
        }
        BufferedImage src = ImageIO.read(source.toFile());
        if (src == null) {
            throw new StudioException(415, "无法解析图片: " + relPath);
        }
        int h = Math.max(1, (int) Math.round(src.getHeight() * (w / (double) src.getWidth())));
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", out);
        byte[] bytes = out.toByteArray();
        Files.write(cached, bytes);
        return bytes;
    }

    // ------------------------------------------------------------------
    // 锚点
    // ------------------------------------------------------------------

    /** 锚点优先级：设计器预设 > 默认 (宽/2, 高)。 */
    public JSONObject anchorOf(String skinId, String assetName) {
        JSONObject anchors = wm.anchors();
        JSONObject entry = anchors.getJSONObject(skinId + "/" + assetName);
        JSONObject out = new JSONObject(true);
        if (entry != null) {
            out.put("x", entry.getIntValue("x"));
            out.put("y", entry.getIntValue("y"));
            out.put("source", "preset");
            return out;
        }
        int[] dim = dimension(wm.skinDir(skinId).resolve(assetName));
        out.put("x", dim[0] > 0 ? dim[0] / 2 : 64);
        out.put("y", dim[1] > 0 ? dim[1] : 128);
        out.put("source", "default");
        return out;
    }

    public JSONObject saveAnchor(String skinId, String assetName, int x, int y) throws IOException {
        if (x < 0 || y < 0) {
            throw new StudioException(400, "锚点不能为负值");
        }
        Path asset = wm.skinDir(skinId).resolve(assetName).normalize();
        if (!Files.isRegularFile(asset)) {
            throw new StudioException(404, "图片不存在: " + assetName);
        }
        JSONObject anchors = wm.anchors();
        JSONObject entry = new JSONObject(true);
        entry.put("x", x);
        entry.put("y", y);
        anchors.put(skinId + "/" + assetName, entry);
        wm.saveAnchors(anchors);
        JSONObject out = new JSONObject(true);
        out.put("skin", skinId);
        out.put("asset", assetName);
        out.put("anchor", entry);
        return out;
    }

    // ------------------------------------------------------------------
    // info.xml
    // ------------------------------------------------------------------

    public JSONObject readInfo(String skinId) {
        JSONObject out = new JSONObject(true);
        Path info = wm.skinConfDir(skinId).resolve("info.xml");
        if (!Files.isRegularFile(info)) {
            out.put("exists", false);
            return out;
        }
        out.put("exists", true);
        out.put("generatedBy", wm.isGeneratedByStudio(info));
        try {
            Document doc = Xml.parse(info);
            out.put("raw", Files.readString(info, StandardCharsets.UTF_8));
            NodeList list = doc.getElementsByTagName("Information");
            if (list.getLength() > 0) {
                NodeList fields = list.item(0).getChildNodes();
                for (int i = 0; i < fields.getLength(); i++) {
                    Node n = fields.item(i);
                    if (!(n instanceof Element)) {
                        continue;
                    }
                    Element el = (Element) n;
                    String tag = Xml.localName(el);
                    switch (tag) {
                        case "Name":
                            out.put("displayName", el.getTextContent().trim());
                            break;
                        case "PreviewImage":
                            out.put("previewImage", el.getTextContent().trim());
                            break;
                        case "SplashImage":
                            out.put("splashImage", el.getTextContent().trim());
                            break;
                        case "Artist":
                        case "Scripter":
                        case "Commissioner":
                        case "Support":
                            out.put(tag.toLowerCase(), el.getAttribute("Name"));
                            out.put(tag.toLowerCase() + "Url", el.getAttribute("URL"));
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            out.put("error", "解析 info.xml 失败：" + Xml.shortMessage(e));
        }
        return out;
    }

    /**
     * 写 info.xml。非设计器生成的文件默认拒绝覆盖，需 {@code force}（P2）。
     */
    public JSONObject writeInfo(String skinId, JSONObject body, boolean force) throws IOException {
        Path dir = wm.skinConfDir(skinId);
        Path target = dir.resolve("info.xml");
        if (Files.isRegularFile(target) && !wm.isGeneratedByStudio(target) && !force) {
            throw new StudioException(409, "info.xml 不是设计器生成的，需确认后才能覆盖");
        }
        String raw = body.getString("raw");
        String content;
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                Document doc = Xml.parse(raw.getBytes(StandardCharsets.UTF_8));
                if (!"Mascot".equals(Xml.localName(doc.getDocumentElement()))) {
                    throw new StudioException(422, "info.xml 根节点必须是 <Mascot>");
                }
                content = Xml.serialize(doc);
            } catch (StudioException e) {
                throw e;
            } catch (Exception e) {
                throw new StudioException(422, "info.xml 无法解析：" + Xml.shortMessage(e));
            }
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            sb.append("<!-- generated-by: shimeji-studio v1.0; do not hand-edit -->\n");
            sb.append("<Mascot>\n  <Information>\n");
            appendText(sb, "Name", body.getString("displayName"));
            appendText(sb, "PreviewImage", body.getString("previewImage"));
            appendText(sb, "SplashImage", body.getString("splashImage"));
            appendPerson(sb, "Artist", body.getString("artist"), body.getString("artistUrl"));
            appendPerson(sb, "Scripter", body.getString("scripter"), body.getString("scripterUrl"));
            appendPerson(sb, "Commissioner", body.getString("commissioner"), body.getString("commissionerUrl"));
            appendPerson(sb, "Support", body.getString("support"), body.getString("supportUrl"));
            sb.append("  </Information>\n</Mascot>\n");
            content = sb.toString();
        }
        Path backup = wm.writeAtomic(target, content.getBytes(StandardCharsets.UTF_8));
        JSONObject out = readInfo(skinId);
        out.put("backup", backup == null ? null : backup.getFileName().toString());
        return out;
    }

    private static void appendText(StringBuilder sb, String tag, String value) {
        if (value != null && !value.trim().isEmpty()) {
            sb.append("    <").append(tag).append('>').append(escape(value.trim())).append("</").append(tag).append(">\n");
        }
    }

    private static void appendPerson(StringBuilder sb, String tag, String name, String url) {
        if (name != null && !name.trim().isEmpty()) {
            sb.append("    <").append(tag).append(" Name=\"").append(escape(name.trim())).append('"');
            if (url != null && !url.trim().isEmpty()) {
                sb.append(" URL=\"").append(escape(url.trim())).append('"');
            }
            sb.append(" />\n");
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String url(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }
}
