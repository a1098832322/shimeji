package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 片段（{@code actions.d/*.xml} / {@code behaviors.d/*.xml}）的读写与配置清单扫描。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §4.3、§5.7、§6.1、DR-09 / DR-11。
 * <p>
 * 写文件严格遵循「规范化 → 沙箱校验 → 原子写 → 备份」；清单扫描只用于展示
 * （加载顺序、覆盖链），所有校验结论仍然只由 {@link ValidationService} 给出（P3）。
 */
public final class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final java.util.Set<String> PROTECTED = java.util.Set.of(
            "actions.xml", "behaviors.xml", "settings.properties", "Mascot.xsd");

    private final WorkspaceManager wm;
    private final ValidationService validation;

    public DocumentService(WorkspaceManager wm, ValidationService validation) {
        this.wm = wm;
        this.validation = validation;
    }

    // ------------------------------------------------------------------
    // 片段列表 / 读取 / 保存 / 删除
    // ------------------------------------------------------------------

    public JSONObject listFragments() throws IOException {
        JSONObject out = new JSONObject(true);
        out.put("actions", fragmentArray(wm.actionsDir(), "actions"));
        out.put("behaviors", fragmentArray(wm.behaviorsDir(), "behaviors"));
        return out;
    }

    private JSONArray fragmentArray(Path dir, String kind) throws IOException {
        JSONArray arr = new JSONArray();
        if (!Files.isDirectory(dir)) {
            return arr;
        }
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (Files.isRegularFile(p) && (name.endsWith(".xml") || name.endsWith(".xml.disabled"))) {
                    files.add(p);
                }
            }
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        for (Path p : files) {
            boolean disabled = p.getFileName().toString().endsWith(".disabled");
            JSONObject item = new JSONObject(true);
            item.put("file", p.getFileName().toString());
            item.put("kind", kind);
            item.put("order", orderOf(p.getFileName().toString()));
            item.put("generatedBy", wm.isGeneratedByStudio(p));
            item.put("disabled", disabled);
            item.put("size", Files.size(p));
            item.put("modified", Files.getLastModifiedTime(p).toMillis());
            item.put("entries", entriesOf(p));
            arr.add(item);
        }
        return arr;
    }

    private static int orderOf(String fileName) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{2})-").matcher(fileName);
        return m.find() ? Integer.parseInt(m.group(1)) : 999;
    }

    private JSONArray entriesOf(Path file) {
        JSONArray arr = new JSONArray();
        try {
            Document doc = Xml.parse(file);
            for (String tag : new String[]{"Action", "Behavior"}) {
                NodeList list = doc.getElementsByTagName(tag);
                for (int i = 0; i < list.getLength(); i++) {
                    Element el = (Element) list.item(i);
                    Node parent = el.getParentNode();
                    if (parent instanceof Element && tag.equals(Xml.localName((Element) parent))) {
                        continue;
                    }
                    String name = el.getAttribute("Name");
                    if (name != null && !name.isEmpty()) {
                        arr.add(name);
                    }
                }
            }
        } catch (Exception e) {
            arr.add("<解析失败: " + Xml.shortMessage(e) + ">");
        }
        return arr;
    }

    public JSONObject readFragment(String kind, String file) throws IOException {
        Path target = fragmentPath(kind, file);
        JSONObject out = new JSONObject(true);
        out.put("file", file);
        out.put("kind", kind);
        out.put("generatedBy", wm.isGeneratedByStudio(target));
        out.put("xml", Files.readString(target, StandardCharsets.UTF_8));
        JSONArray backups = new JSONArray();
        for (Path backup : wm.listBackups(target)) {
            JSONObject b = new JSONObject(true);
            b.put("name", backup.getFileName().toString());
            b.put("modified", Files.getLastModifiedTime(backup).toMillis());
            backups.add(b);
        }
        out.put("backups", backups);
        return out;
    }

    /**
     * 保存片段：规范化 → 校验（默认阻断 error）→ 原子写 → 旧文件备份。
     */
    public JSONObject saveFragment(String kind, String file, String xml, String skin, boolean force)
            throws IOException {
        Path target = fragmentPath(kind, file);
        if (Files.isRegularFile(target) && !wm.isGeneratedByStudio(target) && !force) {
            throw new StudioException(409, "该文件不是设计器生成的（无 generated-by 标记），需要显式确认后才能覆盖：" + file);
        }
        String normalized = Xml.normalizeFragment(xml, kind);
        ValidationService.Overlay overlay = new ValidationService.Overlay(kind, file, normalized);
        JSONObject result = validation.validate(skin, List.of(overlay));
        JSONObject summary = result.getJSONObject("summary");
        boolean hasErrors = summary != null && summary.getIntValue("errors") > 0;
        if (hasErrors) {
            JSONObject error = new JSONObject(true);
            error.put("ok", false);
            error.put("error", "校验未通过，文件未改动");
            error.put("diagnostics", result.getJSONArray("diagnostics"));
            error.put("summary", summary);
            throw new StudioValidationException(error);
        }

        Path backup = wm.writeAtomic(target, normalized.getBytes(StandardCharsets.UTF_8));
        log.info("Fragment saved: {} (backup={})", file, backup);

        JSONObject out = new JSONObject(true);
        out.put("file", file);
        out.put("kind", kind);
        out.put("bytes", normalized.getBytes(StandardCharsets.UTF_8).length);
        out.put("backup", backup == null ? null : backup.getFileName().toString());
        out.put("diagnostics", result.getJSONArray("diagnostics"));
        out.put("summary", summary);
        return out;
    }

    public JSONObject deleteFragment(String kind, String file, boolean force) throws IOException {
        Path target = fragmentPath(kind, file);
        if (Files.isRegularFile(target) && !wm.isGeneratedByStudio(target) && !force) {
            throw new StudioException(409, "非设计器生成的文件，需要 force=true 才能删除：" + file);
        }
        Files.deleteIfExists(target);
        log.info("Fragment deleted: {}", file);
        JSONObject out = new JSONObject(true);
        out.put("file", file);
        out.put("deleted", true);
        return out;
    }

    private Path fragmentPath(String kind, String file) {
        if (file == null || file.contains("/") || file.contains("\\") || !file.endsWith(".xml")) {
            throw new StudioException(400, "片段文件名不合法: " + file);
        }
        if (PROTECTED.contains(file)) {
            throw new StudioException(403, "不允许通过设计器修改基础配置文件: " + file);
        }
        Path dir;
        if ("actions".equals(kind)) {
            dir = wm.actionsDir();
        } else if ("behaviors".equals(kind)) {
            dir = wm.behaviorsDir();
        } else {
            throw new StudioException(400, "未知片段类型: " + kind);
        }
        Path target = dir.resolve(file).normalize();
        if (!target.startsWith(dir.normalize())) {
            throw new StudioException(400, "片段路径越界: " + file);
        }
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new StudioException(500, "无法创建目录: " + dir);
            }
        }
        return target;
    }

    /** 生成下一个可用文件名，例如 {@code 50-wave.xml}。 */
    public String suggestFileName(String kind, String slug, int order) throws IOException {
        Path dir = "actions".equals(kind) ? wm.actionsDir() : wm.behaviorsDir();
        String safeSlug = slug == null || slug.isBlank() ? "fragment" : slug.replaceAll("[^A-Za-z0-9._-]", "-");
        int nn = order > 0 ? order : ("actions".equals(kind) ? 50 : 50);
        String name = String.format("%02d-%s.xml", nn, safeSlug);
        int suffix = 2;
        while (Files.exists(dir.resolve(name))) {
            name = String.format("%02d-%s-%d.xml", nn, safeSlug, suffix++);
        }
        return name;
    }

    // ------------------------------------------------------------------
    // 配置清单（展示用）
    // ------------------------------------------------------------------

    /**
     * 合并清单：按引擎真实加载顺序扫描文件，产出每个名字的「生效定义 / 被覆盖定义」。
     * <p>
     * 顺序严格对应 {@code ConfigurationFiles.build}：全局 actions（主文件 + actions.d）
     * → 皮肤 actions（+ actions.d）→ 全局 behaviors → 皮肤 behaviors。
     */
    public JSONObject manifest(String skinId) throws IOException {
        String skin = skinId;
        if (skin == null || skin.trim().isEmpty()) {
            List<String> ids = wm.listSkinIds();
            skin = ids.isEmpty() ? null : ids.get(0);
        }

        List<Layer> layers = new ArrayList<>();
        for (Path f : loadOrder(wm.getConfDir(), wm.actionsDir(), "action")) {
            layers.add(Layer.of(f, "global", wm));
        }
        Path skinConf = skin == null ? null : wm.skinConfDir(skin);
        if (skinConf != null && Files.isRegularFile(skinConf.resolve("actions.xml"))) {
            for (Path f : loadOrder(skinConf, skinConf.resolve("actions.d"), "action")) {
                layers.add(Layer.of(f, "skin", wm));
            }
        }
        for (Path f : loadOrder(wm.getConfDir(), wm.behaviorsDir(), "behavior")) {
            layers.add(Layer.of(f, "global", wm));
        }
        if (skinConf != null && Files.isRegularFile(skinConf.resolve("behaviors.xml"))) {
            for (Path f : loadOrder(skinConf, skinConf.resolve("behaviors.d"), "behavior")) {
                layers.add(Layer.of(f, "skin", wm));
            }
        }

        Map<String, JSONObject> actions = new LinkedHashMap<>();
        Map<String, List<String>> actionChains = new LinkedHashMap<>();
        Map<String, JSONObject> behaviors = new LinkedHashMap<>();
        Map<String, List<String>> behaviorChains = new LinkedHashMap<>();
        int poses = 0;

        for (Layer layer : layers) {
            if (!Files.isRegularFile(layer.path)) {
                continue;
            }
            Document doc;
            try {
                doc = Xml.parse(layer.path);
            } catch (Exception e) {
                layer.error = "解析失败: " + Xml.shortMessage(e);
                continue;
            }
            for (String tag : new String[]{"Action", "Behavior"}) {
                NodeList list = doc.getElementsByTagName(tag);
                for (int i = 0; i < list.getLength(); i++) {
                    Element el = (Element) list.item(i);
                    Node parent = el.getParentNode();
                    if (parent instanceof Element && tag.equals(Xml.localName((Element) parent))) {
                        continue;
                    }
                    String name = el.getAttribute("Name");
                    if (name == null || name.isEmpty()) {
                        continue;
                    }
                    if ("Action".equals(tag)) {
                        JSONObject item = new JSONObject(true);
                        item.put("name", name);
                        item.put("type", el.getAttribute("Type"));
                        item.put("borderType", emptyToNull(el.getAttribute("BorderType")));
                        item.put("loop", emptyToNull(el.getAttribute("Loop")));
                        int poseCount = el.getElementsByTagName("Pose").getLength();
                        item.put("poses", poseCount);
                        item.put("source", layer.display);
                        item.put("layer", layer.role);
                        item.put("editable", layer.generatedBy);
                        item.put("fragment", layer.generatedBy ? layer.fileName : null);
                        poses += poseCount;
                        actionChains.computeIfAbsent(name, k -> new ArrayList<>()).add(layer.display);
                        actions.put(name, item);
                    } else {
                        JSONObject item = new JSONObject(true);
                        item.put("name", name);
                        String actionName = el.getAttribute("Action");
                        item.put("action", actionName == null || actionName.isEmpty() ? name : actionName);
                        item.put("frequency", parseInt(el.getAttribute("Frequency")));
                        item.put("hidden", Boolean.parseBoolean(el.getAttribute("Hidden")));
                        item.put("toggleable", Boolean.parseBoolean(el.getAttribute("Toggleable")));
                        item.put("condition", emptyToNull(el.getAttribute("Condition")));
                        item.put("source", layer.display);
                        item.put("layer", layer.role);
                        item.put("editable", layer.generatedBy);
                        item.put("fragment", layer.generatedBy ? layer.fileName : null);
                        item.put("next", nextOf(el));
                        behaviorChains.computeIfAbsent(name, k -> new ArrayList<>()).add(layer.display);
                        behaviors.put(name, item);
                    }
                }
            }
        }

        for (Map.Entry<String, JSONObject> e : actions.entrySet()) {
            List<String> chain = actionChains.get(e.getKey());
            JSONArray overridden = new JSONArray();
            for (int i = 0; i + 1 < chain.size(); i++) {
                overridden.add(chain.get(i));
            }
            e.getValue().put("overriddenBy", overridden);
            e.getValue().put("overrides", chain.size() > 1);
        }
        for (Map.Entry<String, JSONObject> e : behaviors.entrySet()) {
            List<String> chain = behaviorChains.get(e.getKey());
            JSONArray overridden = new JSONArray();
            for (int i = 0; i + 1 < chain.size(); i++) {
                overridden.add(chain.get(i));
            }
            e.getValue().put("overriddenBy", overridden);
            e.getValue().put("overrides", chain.size() > 1);
        }

        JSONObject out = new JSONObject(true);
        out.put("skin", skin);
        JSONArray layerArr = new JSONArray();
        for (Layer l : layers) {
            JSONObject o = new JSONObject(true);
            o.put("file", l.display);
            o.put("role", l.role);
            o.put("generatedBy", l.generatedBy);
            o.put("fragment", l.generatedBy ? l.fileName : null);
            o.put("error", l.error);
            layerArr.add(o);
        }
        out.put("layers", layerArr);
        out.put("actions", new ArrayList<>(actions.values()));
        out.put("behaviors", new ArrayList<>(behaviors.values()));
        JSONObject counts = new JSONObject(true);
        counts.put("actions", actions.size());
        counts.put("behaviors", behaviors.size());
        counts.put("poses", poses);
        out.put("counts", counts);
        return out;
    }

    private static JSONObject nextOf(Element behavior) {
        JSONObject next = new JSONObject(true);
        next.put("add", true);
        JSONArray refs = new JSONArray();
        NodeList lists = behavior.getElementsByTagName("NextBehaviorList");
        if (lists.getLength() == 0) {
            lists = behavior.getElementsByTagName("NextBehavior");
        }
        if (lists.getLength() > 0) {
            Element list = (Element) lists.item(0);
            String add = list.getAttribute("Add");
            next.put("add", add == null || add.isEmpty() || Boolean.parseBoolean(add));
            NodeList children = list.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n instanceof Element && "BehaviorReference".equals(Xml.localName((Element) n))) {
                    Element ref = (Element) n;
                    JSONObject o = new JSONObject(true);
                    o.put("name", ref.getAttribute("Name"));
                    o.put("frequency", parseInt(ref.getAttribute("Frequency")));
                    o.put("condition", emptyToNull(ref.getAttribute("Condition")));
                    o.put("action", emptyToNull(ref.getAttribute("Action")));
                    refs.add(o);
                }
            }
        }
        next.put("refs", refs);
        return next;
    }

    private static List<Path> loadOrder(Path mainDir, Path fragmentDir, String kind) throws IOException {
        List<Path> files = new ArrayList<>();
        Path main = mainDir.resolve("action".equals(kind) ? "actions.xml" : "behaviors.xml");
        if (Files.isRegularFile(main)) {
            files.add(main);
        }
        if (Files.isDirectory(fragmentDir)) {
            List<Path> fragments = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(fragmentDir, "*.xml")) {
                for (Path p : stream) {
                    fragments.add(p);
                }
            }
            fragments.sort(Comparator.comparing(p -> p.getFileName().toString()));
            files.addAll(fragments);
        }
        return files;
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    /** 加载顺序中的一个文件。 */
    private static final class Layer {
        final Path path;
        final String role;
        final String display;
        final boolean generatedBy;
        final String fileName;
        String error;

        private Layer(Path path, String role, String display, boolean generatedBy, String fileName) {
            this.path = path;
            this.role = role;
            this.display = display;
            this.generatedBy = generatedBy;
            this.fileName = fileName;
        }

        static Layer of(Path path, String role, WorkspaceManager wm) {
            boolean generated = wm.isGeneratedByStudio(path);
            String display = wm.relativeToEngine(path).toString();
            return new Layer(path, role, display, generated, path.getFileName().toString());
        }
    }

    /** 校验失败但需要把 diagnostics 一起返回给 UI 的异常。 */
    public static final class StudioValidationException extends RuntimeException {
        private final JSONObject payload;

        StudioValidationException(JSONObject payload) {
            super(payload.getString("error"));
            this.payload = payload;
        }

        public JSONObject getPayload() {
            return payload;
        }
    }
}
