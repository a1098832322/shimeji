package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.ConfigurationFiles;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校验中心：L0–L4。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §5.4、DR-08、DR-15。
 * <p>
 * <b>P3 原则</b>：全部「通过 / 不通过」结论来自引擎同一份规则 ——
 * {@link ConfigurationFiles#build} 做 dry-run、{@link ConfigurationFiles#collectMissingImages}
 * 做缺图预检、{@link ConfigurationFiles#detectNodeNameMismatch} 做节点名诊断；
 * 设计器只额外补两步引擎运行时不做的检查：XSD 静态校验（L1）与基础名冲突检查（L4）。
 * <p>
 * dry-run 的实现方式：把真实 {@code conf/} 复制到临时沙箱，把待保存片段写进沙箱的
 * {@code actions.d/}，再对沙箱调用引擎构建。真实文件在「校验通过」之前一个字节都不动（DR-09）。
 */
public final class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private static final Set<String> RESERVED_NAMES = Set.of("SplitIntoTwo", "PullUpShimeji");
    private static final int MAX_DIAGNOSTICS = 200;

    private final WorkspaceManager wm;

    public ValidationService(WorkspaceManager wm) {
        this.wm = wm;
    }

    /** 待校验的片段覆盖（尚未落盘）。 */
    public static final class Overlay {
        public final String kind;
        public final String file;
        public final String xml;

        public Overlay(String kind, String file, String xml) {
            this.kind = kind;
            this.file = file;
            this.xml = xml;
        }
    }

    /**
     * 执行 L0–L4。
     *
     * @param skinId   当前皮肤（决定图片解析根，即 {@code img/<skin>/}）
     * @param overlays 待写入 / 待校验的片段（可为空，此时等价于「校验当前工作区」）
     */
    public JSONObject validate(String skinId, List<Overlay> overlays) {
        // dry-run 会走引擎建配置，引擎报错文案取自 languageBundle；
        // 设计器/单元测试都是无 GUI 环境，先做一次幂等的 headless 初始化。
        try {
            com.group_finity.mascot.Main.getInstance().initForHeadlessTooling();
        } catch (Throwable t) {
            log.warn("headless 初始化失败（继续尝试校验）：{}", t.toString());
        }
        final List<Diagnostic> diagnostics = new ArrayList<>();
        final String imageSet = resolveImageSet(skinId);
        Path sandboxRoot = null;
        try {
            sandboxRoot = Files.createTempDirectory("shimeji-studio-validate-");
            Sandbox sandbox = new Sandbox(sandboxRoot, imageSet);
            sandbox.prepare();

            // ---- 基线：不含待校验片段，用于「基础名冲突」判定（L4） ----
            ConfigurationFiles.Result baseline = null;
            try {
                baseline = sandbox.build();
            } catch (Exception e) {
                diagnostics.add(Diagnostic.error("L2", displayName(sandbox.actionsFile(sandbox.global)),
                        "基线配置本身无法加载：" + Xml.shortMessage(e),
                        "先修复现有配置再校验新片段；设计器不会修改基础层文件"));
            }
            Map<String, String> baseActionSources = sourcesOf(baseline, true);
            Map<String, String> baseBehaviorSources = sourcesOf(baseline, false);

            // ---- L0 / L1：结构 + XSD ----
            List<Overlay> valid = new ArrayList<>();
            for (Overlay overlay : overlays) {
                Overlay normalized = checkStructure(overlay, diagnostics);
                if (normalized != null) {
                    valid.add(normalized);
                }
            }

            // ---- 写入沙箱（内存态，不碰真实文件） ----
            for (Overlay overlay : valid) {
                sandbox.place(overlay);
            }

            // ---- L2：引擎 dry-run ----
            ConfigurationFiles.Result effective = null;
            try {
                effective = sandbox.build();
            } catch (Exception e) {
                String msg = Xml.shortMessage(e);
                diagnostics.add(new Diagnostic(Diagnostic.ERROR, "L2", guessFile(msg),
                        extractAction(msg), extractPose(msg), null,
                        "引擎加载失败：" + msg,
                        "检查缺图 / 非法引用 / 非法表达式 / Duration<1 等常见原因"));
            }

            // ---- L3：缺图与节点名诊断（逐文件，给出全貌而非首个错误） ----
            scanDiagnostics(sandbox, diagnostics);

            // ---- L4：命名、保留名与兼容护栏 ----
            checkConflicts(sandbox, valid, baseActionSources, baseBehaviorSources, diagnostics);

            JSONObject result = new JSONObject(true);
            result.put("summary", summarize(diagnostics));
            result.put("diagnostics", toJson(diagnostics));
            result.put("imageSet", imageSet);
            result.put("baseline", counts(baseline));
            result.put("effective", counts(effective));
            result.put("sources", effective == null ? new JSONArray() : sourcesJson(sandbox, effective.getSources()));
            return result;
        } catch (StudioException e) {
            throw e;
        } catch (Exception e) {
            log.error("Validation failed", e);
            throw new StudioException(500, "校验执行失败：" + Xml.shortMessage(e), e);
        } finally {
            deleteQuietly(sandboxRoot);
        }
    }

    // ------------------------------------------------------------------
    // L0 结构
    // ------------------------------------------------------------------

    private Overlay checkStructure(Overlay overlay, List<Diagnostic> diagnostics) {
        String file = overlay.file;
        if (file == null || file.contains("/") || file.contains("\\") || !file.endsWith(".xml")) {
            diagnostics.add(Diagnostic.error("L0", file, "片段文件名不合法（只允许 *.xml，不含路径分隔符）"));
            return null;
        }
        if (!"actions".equals(overlay.kind) && !"behaviors".equals(overlay.kind)
                && !"skin-actions".equals(overlay.kind) && !"skin-behaviors".equals(overlay.kind)) {
            diagnostics.add(Diagnostic.error("L0", file, "未知片段类型：" + overlay.kind));
            return null;
        }
        String normalized;
        try {
            normalized = Xml.normalizeFragment(overlay.xml, overlay.kind.endsWith("actions") ? "actions" : "behaviors");
        } catch (StudioException e) {
            diagnostics.add(Diagnostic.error("L0", file, e.getMessage()));
            return null;
        }

        // L1：XSD（引擎运行时不校验，设计器补上）
        List<String> xsdErrors = Xml.validateXsd(normalized.getBytes(StandardCharsets.UTF_8),
                wm.getConfDir().resolve("Mascot.xsd"));
        for (String err : xsdErrors) {
            diagnostics.add(Diagnostic.error("L1", file, "不符合 Mascot.xsd：" + err,
                    "按提示补全必需属性（Pose 需要 Image / ImageAnchor / Velocity / Duration）"));
        }

        // 命名与排序提示
        if (!file.matches("^[0-9]{2}-[A-Za-z0-9._-]+\\.xml$")) {
            diagnostics.add(Diagnostic.warning("L4", file,
                    "文件名建议使用 NN-slug.xml（如 50-my-pack-wave.xml）：片段按文件名字典序加载，号码段决定覆盖顺序"));
        }
        if (!Xml.hasMarker(normalized)) {
            diagnostics.add(Diagnostic.info("L0", file, "已自动补充 generated-by 归属标记"));
        }
        return new Overlay(overlay.kind, file, normalized);
    }

    // ------------------------------------------------------------------
    // L3 诊断
    // ------------------------------------------------------------------

    private void scanDiagnostics(Sandbox sandbox, List<Diagnostic> diagnostics) {
        Set<String> seen = new LinkedHashSet<>();
        for (Path file : sandbox.xmlFiles()) {
            Document doc;
            try {
                doc = Xml.parse(file);
            } catch (Exception e) {
                diagnostics.add(Diagnostic.error("L0", displayName(file), "XML 无法解析：" + Xml.shortMessage(e)));
                continue;
            }
            List<String> problems = new ArrayList<>();
            collectMissingImages(doc, sandbox.imageSet, displayName(file), problems);
            for (String problem : problems) {
                if (seen.add(problem) && diagnostics.size() < MAX_DIAGNOSTICS) {
                    diagnostics.add(new Diagnostic(Diagnostic.ERROR, "L3", displayName(file),
                            extractAction(problem), extractPose(problem), "Image", problem,
                            "把缺失图片导入 img/" + sandbox.imageSet + "/，或改成本皮肤已有的图片"));
                }
            }
            for (String mismatch : ConfigurationFiles.detectNodeNameMismatch(doc, schemaOf(doc))) {
                String message = displayName(file) + ": " + mismatch;
                if (seen.add(message) && diagnostics.size() < MAX_DIAGNOSTICS) {
                    diagnostics.add(Diagnostic.warning("L3", displayName(file), message,
                            "把 <NextBehavior> 改名为 <NextBehaviorList>（旧名仍被兼容，但建议统一）"));
                }
            }
        }
    }

    private static java.util.ResourceBundle schemaOf(Document doc) {
        try {
            return java.util.ResourceBundle.getBundle("conf.schema", java.util.Locale.US);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 缺图检测：引擎的 {@code collectMissingImages} 按「当前工作目录 + img/&lt;skin&gt;/」解析，
     * 在设计器里改成按 {@code engineRoot} 解析 —— 两者在正常运行时等价（预览进程 cwd 就是
     * engineRoot），但这样即使设计器以别的 cwd 启动也不会误报缺图。
     */
    private void collectMissingImages(Document doc, String imageSet, String source, List<String> problems) {
        NodeList poses = doc.getElementsByTagName("Pose");
        for (int i = 0; i < poses.getLength(); i++) {
            if (!(poses.item(i) instanceof Element)) {
                continue;
            }
            Element pose = (Element) poses.item(i);
            for (String attr : new String[]{"Image", "ImageRight"}) {
                String value = pose.getAttribute(attr);
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }
                Path resolved;
                try {
                    resolved = wm.resolveImage(imageSet, value.trim());
                } catch (StudioException e) {
                    problems.add(source + ": Pose " + attr + "=\"" + value + "\" -> " + e.getMessage());
                    continue;
                }
                if (!Files.isRegularFile(resolved)) {
                    String actionName = ancestorName(pose);
                    String shown = value.trim();
                    if (shown.startsWith("file:")) {
                        // 沙箱里被改写成绝对路径的引用，报错时还原成「/皮肤/图片」形式
                        try {
                            shown = "/" + wm.getImgDir().toAbsolutePath().normalize()
                                    .relativize(resolved.toAbsolutePath().normalize()).toString().replace('\\', '/');
                        } catch (IllegalArgumentException ignored) {
                            // 不在 img 目录下就按原样显示
                        }
                    }
                    problems.add(source + ": " + (actionName == null ? "<Pose>" : "Action[" + actionName + "]")
                            + " Pose " + attr + " \"" + shown + "\" -> missing file "
                            + wm.relativeToEngine(resolved));
                }
            }
        }
    }

    private static String ancestorName(Element element) {
        org.w3c.dom.Node node = element.getParentNode();
        while (node != null) {
            if (node instanceof Element && "Action".equals(Xml.localName((Element) node))) {
                String name = ((Element) node).getAttribute("Name");
                return name == null || name.isEmpty() ? null : name;
            }
            node = node.getParentNode();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // L4 冲突与兼容护栏
    // ------------------------------------------------------------------

    private void checkConflicts(Sandbox sandbox, List<Overlay> overlays,
                                Map<String, String> baseActionSources,
                                Map<String, String> baseBehaviorSources,
                                List<Diagnostic> diagnostics) {
        for (Overlay overlay : overlays) {
            Document doc;
            try {
                doc = Xml.parse(overlay.xml.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                continue;
            }
            String target = displayName(sandbox.resolve(overlay));
            boolean actions = overlay.kind.endsWith("actions");
            String tag = actions ? "Action" : "Behavior";
            NodeList list = doc.getElementsByTagName(tag);
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }
                Element el = (Element) node;
                if (el.getParentNode() != null && el.getParentNode() instanceof Element
                        && tag.equals(Xml.localName((Element) el.getParentNode()))) {
                    continue; // 嵌套 Action 不是顶层定义
                }
                String name = el.getAttribute("Name");
                if (name == null || name.trim().isEmpty()) {
                    diagnostics.add(Diagnostic.error("L4", target, "<" + tag + "> 缺少 Name 属性"));
                    continue;
                }
                name = name.trim();

                if (isReserved(name)) {
                    diagnostics.add(new Diagnostic(Diagnostic.ERROR, "L4", target, name, null, "Name",
                            "使用了保留名 " + name,
                            "AI* / SplitIntoTwo / PullUpShimeji 由托管层占用，扩展包不得重定义"));
                    continue;
                }

                Map<String, String> base = actions ? baseActionSources : baseBehaviorSources;
                if (base.containsKey(name)) {
                    String source = base.get(name);
                    Path real = sandbox.toRealPath(source);
                    boolean sameFile = real != null && real.equals(sandbox.toRealPath(sandbox.resolve(overlay).toString()));
                    if (!sameFile) {
                        boolean ownedByStudio = real != null && Files.isRegularFile(real) && wm.isGeneratedByStudio(real);
                        String sourceDisplay = real == null ? source : displayName(real);
                        if (ownedByStudio) {
                            diagnostics.add(new Diagnostic(Diagnostic.WARNING, "L4", target, name, null, "Name",
                                    "同名定义已存在于设计器片段 " + sourceDisplay + "，本文件会覆盖它",
                                    "FR-02 后写覆盖：确认这是有意为之，或改名避免歧义"));
                        } else {
                            diagnostics.add(new Diagnostic(Diagnostic.ERROR, "L4", target, name, null, "Name",
                                    "与基础/既有" + (actions ? "动作" : "行为") + "同名（" + sourceDisplay + "）",
                                    "FR-02 是后写覆盖且只 warn：同名会静默替换基础定义。请加包前缀改名（如 MyPack." + name + "）"));
                        }
                    }
                }

                if (!actions) {
                    String frequency = el.getAttribute("Frequency");
                    try {
                        if (frequency != null && !frequency.isEmpty() && Integer.parseInt(frequency) > 0) {
                            diagnostics.add(new Diagnostic(Diagnostic.WARNING, "L4", target, name, null, "Frequency",
                                    "Frequency=" + frequency + " 会进入自主行为树，改变原生行为权重分布",
                                    "预览用行为建议 Frequency=0 Hidden=true"));
                        }
                    } catch (NumberFormatException ignored) {
                        // XSD 已给出 int 错误
                    }
                }
            }

            // DR-12：file: 绝对路径不可分发
            NodeList poses = doc.getElementsByTagName("Pose");
            for (int i = 0; i < poses.getLength(); i++) {
                Element pose = (Element) poses.item(i);
                String image = pose.getAttribute("Image");
                if (image != null && image.startsWith("file:")) {
                    diagnostics.add(new Diagnostic(Diagnostic.WARNING, "L4", target, null, "Pose " + (i + 1), "Image",
                            "引用了外部绝对路径 " + image,
                            "file: 引用不可打包分发，导出时会被阻止；建议把图片导入皮肤目录"));
                }
            }
        }
    }

    private static boolean isReserved(String name) {
        return name.startsWith("AI") || RESERVED_NAMES.contains(name);
    }

    // ------------------------------------------------------------------
    // 表达式试算（GD-3：加载期求值，交给引擎 Variable.parse）
    // ------------------------------------------------------------------

    /**
     * 用引擎自己的 {@link Variable#parse} 试算表达式，绝不 eval 前端字符串（R-D7）。
     * 明确不接受 {@code mascot} 等运行时变量：加载期没有角色实例。
     */
    public JSONObject simulate(String expression, Map<String, String> params) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new StudioException(400, "表达式不能为空");
        }
        VariableMap context = new VariableMap();
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getKey() == null || e.getKey().trim().isEmpty()) {
                    continue;
                }
                if ("mascot".equals(e.getKey().trim())) {
                    throw new StudioException(400, "mascot 是运行时变量，加载期不可用（GD-3）");
                }
                try {
                    context.put(e.getKey().trim(), Variable.parse(e.getValue() == null ? "" : e.getValue()));
                } catch (Exception ex) {
                    throw new StudioException(400, "模拟参数 " + e.getKey() + "=\"" + e.getValue() + "\" 无法解析：" + Xml.shortMessage(ex));
                }
            }
        }
        try {
            Object value = Variable.parse(expression.trim()).get(context);
            JSONObject out = new JSONObject(true);
            out.put("ok", true);
            out.put("expression", expression);
            out.put("value", value == null ? null : String.valueOf(value));
            out.put("type", value == null ? "null" : value.getClass().getSimpleName());
            if (value instanceof Number) {
                int intValue = ((Number) value).intValue();
                out.put("intValue", intValue);
                if ("Duration".equalsIgnoreCase(String.valueOf(params == null ? "" : params.get("_field"))) && intValue < 1) {
                    out.put("warning", "Duration 必须 >= 1");
                }
            }
            return out;
        } catch (Exception e) {
            throw new StudioException(422, "表达式求值失败：" + Xml.shortMessage(e));
        }
    }

    // ------------------------------------------------------------------
    // 沙箱
    // ------------------------------------------------------------------

    private final class Sandbox {
        final Path root;
        final Path global;
        final Path skin;
        final String imageSet;
        final boolean hasSkinLayer;

        Sandbox(Path root, String imageSet) {
            this.root = root;
            this.global = root.resolve("global");
            this.skin = root.resolve("skin");
            this.imageSet = imageSet;
            this.hasSkinLayer = Files.isRegularFile(wm.skinConfDir(imageSet).resolve("actions.xml"));
        }

        void prepare() throws IOException {
            Files.createDirectories(global.resolve("actions.d"));
            Files.createDirectories(global.resolve("behaviors.d"));
            copyIfExists(wm.getConfDir().resolve("actions.xml"), global.resolve("actions.xml"));
            copyIfExists(wm.getConfDir().resolve("behaviors.xml"), global.resolve("behaviors.xml"));
            copyDirXml(wm.getConfDir().resolve("actions.d"), global.resolve("actions.d"));
            copyDirXml(wm.getConfDir().resolve("behaviors.d"), global.resolve("behaviors.d"));
            if (hasSkinLayer) {
                Files.createDirectories(skin.resolve("actions.d"));
                Files.createDirectories(skin.resolve("behaviors.d"));
                Path conf = wm.skinConfDir(imageSet);
                copyIfExists(conf.resolve("actions.xml"), skin.resolve("actions.xml"));
                copyIfExists(conf.resolve("behaviors.xml"), skin.resolve("behaviors.xml"));
                copyDirXml(conf.resolve("actions.d"), skin.resolve("actions.d"));
                copyDirXml(conf.resolve("behaviors.d"), skin.resolve("behaviors.d"));
            }
        }

        ConfigurationFiles.Result build() throws Exception {
            absolutizeImages();
            String actionsFile;
            String behaviorsFile;
            String baseActions = null;
            String baseBehaviors = null;
            if (hasSkinLayer) {
                actionsFile = skin.resolve("actions.xml").toString();
                boolean skinBehaviors = Files.isRegularFile(skin.resolve("behaviors.xml"));
                behaviorsFile = (skinBehaviors ? skin.resolve("behaviors.xml") : global.resolve("behaviors.xml")).toString();
                baseActions = global.resolve("actions.xml").toString();
                baseBehaviors = skinBehaviors ? global.resolve("behaviors.xml").toString() : null;
            } else {
                actionsFile = global.resolve("actions.xml").toString();
                behaviorsFile = global.resolve("behaviors.xml").toString();
            }
            return ConfigurationFiles.build(actionsFile, behaviorsFile, imageSetPath(),
                    baseActions, baseBehaviors);
        }

        /**
         * dry-run 用的 imageSet 取「皮肤图片目录的绝对路径」：引擎的
         * {@code AnimationBuilder.resolveImagePath} 是「imageSet + 相对路径」拼接，
         * 传绝对路径后缺图判定不再依赖进程 cwd（Main 加载真实配置时也是绝对路径）。
         */
        String imageSetPath() {
            return wm.skinDir(imageSet).toAbsolutePath().toString();
        }

        /**
         * 把沙箱里的 {@code Pose/@Image}、{@code @ImageRight} 改写成 {@code file:} 绝对路径。
         * <p>
         * 引擎的 {@link com.group_finity.mascot.config.AnimationBuilder#resolveImagePath} 始终按
         * 「进程 cwd + ./img/&lt;skin&gt;」解析，设计器若从别的 cwd 启动就会误报缺图。
         * 这里只在<b>临时沙箱</b>内改写（真实文件一个字节不动），解析结果与 cwd 无关；
         * 生产环境 cwd 就是 engineRoot，两者指向同一文件，校验结论完全一致。
         */
        void absolutizeImages() {
            for (Path file : xmlFiles()) {
                try {
                    Document doc = Xml.parse(file);
                    NodeList poses = doc.getElementsByTagName("Pose");
                    boolean changed = false;
                    for (int i = 0; i < poses.getLength(); i++) {
                        if (!(poses.item(i) instanceof Element)) {
                            continue;
                        }
                        Element pose = (Element) poses.item(i);
                        for (String attr : new String[]{"Image", "ImageRight"}) {
                            String value = pose.getAttribute(attr);
                            if (value == null || value.trim().isEmpty() || value.startsWith("file:")) {
                                continue;
                            }
                            try {
                                Path abs = wm.resolveImage(imageSet, value.trim()).toAbsolutePath().normalize();
                                pose.setAttribute(attr, "file:" + abs);
                                changed = true;
                            } catch (StudioException ignored) {
                                // 越界/非法引用交给后续 L3 报错，这里不阻断
                            }
                        }
                    }
                    if (changed) {
                        Files.writeString(file, Xml.serialize(doc), StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    log.debug("absolutize images skipped for {}: {}", file, e.toString());
                }
            }
        }

        Path resolve(Overlay overlay) {
            switch (overlay.kind) {
                case "actions":
                    return global.resolve("actions.d").resolve(overlay.file);
                case "behaviors":
                    return global.resolve("behaviors.d").resolve(overlay.file);
                case "skin-actions":
                    return skin.resolve("actions.d").resolve(overlay.file);
                case "skin-behaviors":
                    return skin.resolve("behaviors.d").resolve(overlay.file);
                default:
                    throw new StudioException(400, "未知片段类型: " + overlay.kind);
            }
        }

        void place(Overlay overlay) throws IOException {
            Path target = resolve(overlay);
            Files.createDirectories(target.getParent());
            Files.writeString(target, overlay.xml, StandardCharsets.UTF_8);
        }

        List<Path> xmlFiles() {
            List<Path> files = new ArrayList<>();
            for (String dirName : new String[]{"actions.d", "behaviors.d"}) {
                files.addAll(listXml(global.resolve(dirName)));
                if (hasSkinLayer) {
                    files.addAll(listXml(skin.resolve(dirName)));
                }
            }
            for (Path main : new Path[]{global.resolve("actions.xml"), global.resolve("behaviors.xml"),
                    skin.resolve("actions.xml"), skin.resolve("behaviors.xml")}) {
                if (Files.isRegularFile(main)) {
                    files.add(main);
                }
            }
            return files;
        }

        Path actionsFile(Path base) {
            return base.resolve("actions.xml");
        }

        /** 沙箱文件 → 真实文件（用于判断「同名定义是不是设计器自己的」）。 */
        Path toRealPath(String sandboxPath) {
            if (sandboxPath == null) {
                return null;
            }
            try {
                Path p = Path.of(sandboxPath).toAbsolutePath().normalize();
                if (p.startsWith(global.toAbsolutePath())) {
                    return wm.getConfDir().resolve(global.toAbsolutePath().relativize(p).toString());
                }
                if (p.startsWith(skin.toAbsolutePath())) {
                    return wm.skinConfDir(imageSet).resolve(skin.toAbsolutePath().relativize(p).toString());
                }
            } catch (Exception ignored) {
                // 非沙箱路径（理论上不会出现）
            }
            return null;
        }
    }

    private static void copyIfExists(Path from, Path to) throws IOException {
        if (Files.isRegularFile(from)) {
            Files.createDirectories(to.getParent());
            Files.copy(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyDirXml(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(from, "*.xml")) {
            for (Path p : stream) {
                Files.copy(p, to.resolve(p.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static List<Path> listXml(Path dir) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
            for (Path p : stream) {
                result.add(p);
            }
        } catch (IOException ignored) {
            // 空目录/权限问题按无文件处理
        }
        result.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return result;
    }

    private static void deleteQuietly(Path root) {
        if (root == null) {
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

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private String resolveImageSet(String skinId) {
        if (skinId != null && !skinId.trim().isEmpty()) {
            Path dir = wm.skinDir(skinId.trim());
            if (!Files.isDirectory(dir)) {
                throw new StudioException(400, "皮肤不存在: " + skinId);
            }
            return skinId.trim();
        }
        try {
            List<String> ids = wm.listSkinIds();
            if (!ids.isEmpty()) {
                return ids.get(0);
            }
        } catch (IOException ignored) {
            // 落到默认值
        }
        return "Shimeji";
    }

    private static Map<String, String> sourcesOf(ConfigurationFiles.Result result, boolean actions) {
        Map<String, String> map = new LinkedHashMap<>();
        if (result == null) {
            return map;
        }
        Configuration conf = result.getConfiguration();
        if (actions) {
            for (String name : conf.getActionNames()) {
                map.put(name, conf.getActionSource(name));
            }
        } else {
            for (String name : conf.getBehaviorNames()) {
                map.put(name, conf.getBehaviorSource(name));
            }
        }
        return map;
    }

    private static JSONObject counts(ConfigurationFiles.Result result) {
        JSONObject o = new JSONObject(true);
        if (result == null) {
            o.put("actions", 0);
            o.put("behaviors", 0);
            o.put("loaded", false);
            return o;
        }
        o.put("actions", result.getConfiguration().getActionNames().size());
        o.put("behaviors", result.getConfiguration().getBehaviorNames().size());
        o.put("loaded", true);
        return o;
    }

    private JSONArray sourcesJson(Sandbox sandbox, List<String> sources) {
        JSONArray arr = new JSONArray();
        for (String s : sources) {
            Path real = sandbox.toRealPath(s);
            arr.add(real == null ? shortPath(s) : displayName(real));
        }
        return arr;
    }

    private static JSONObject summarize(List<Diagnostic> diagnostics) {
        int errors = 0;
        int warnings = 0;
        int infos = 0;
        for (Diagnostic d : diagnostics) {
            switch (d.getSeverity()) {
                case Diagnostic.ERROR:
                    errors++;
                    break;
                case Diagnostic.WARNING:
                    warnings++;
                    break;
                default:
                    infos++;
                    break;
            }
        }
        JSONObject o = new JSONObject(true);
        o.put("errors", errors);
        o.put("warnings", warnings);
        o.put("infos", infos);
        o.put("ok", errors == 0);
        return o;
    }

    private static JSONArray toJson(List<Diagnostic> diagnostics) {
        JSONArray arr = new JSONArray();
        for (Diagnostic d : diagnostics) {
            arr.add(d.toJson());
        }
        return arr;
    }

    /** 沙箱绝对路径 → 引擎相对路径，UI 展示更短也更稳定。 */
    private String displayName(Path file) {
        if (file == null) {
            return null;
        }
        Path root = wm.getOptions().getEngineRoot().toAbsolutePath().normalize();
        Path abs = file.toAbsolutePath().normalize();
        if (abs.startsWith(root)) {
            return root.relativize(abs).toString();
        }
        // 沙箱路径：尽量还原成引擎视角
        String s = abs.toString().replace('\\', '/');
        int idx = s.indexOf("/global/");
        if (idx >= 0) {
            return "conf/" + s.substring(idx + "/global/".length());
        }
        idx = s.indexOf("/skin/");
        if (idx >= 0) {
            return "img/<skin>/conf/" + s.substring(idx + "/skin/".length());
        }
        return file.getFileName() == null ? s : file.getFileName().toString();
    }

    private static String shortPath(String path) {
        if (path == null) {
            return null;
        }
        String s = path.replace('\\', '/');
        int idx = s.indexOf("/global/");
        if (idx >= 0) {
            return "conf/" + s.substring(idx + "/global/".length());
        }
        idx = s.indexOf("/skin/");
        if (idx >= 0) {
            return "img/<skin>/conf/" + s.substring(idx + "/skin/".length());
        }
        return Path.of(s).getFileName().toString();
    }

    private static String extractAction(String message) {
        if (message == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Action\\[([^\\]]+)\\]").matcher(message);
        return m.find() ? m.group(1) : null;
    }

    private static String extractPose(String message) {
        if (message == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Pose\\[(\\d+)\\]").matcher(message);
        return m.find() ? "Pose " + m.group(1) : null;
    }

    private static String guessFile(String message) {
        if (message == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([A-Za-z0-9_./\\\\-]+\\.xml)").matcher(message);
        return m.find() ? m.group(1) : null;
    }
}
