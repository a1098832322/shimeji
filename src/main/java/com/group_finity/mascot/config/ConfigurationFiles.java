package com.group_finity.mascot.config;

import com.group_finity.mascot.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 动作/行为配置文件的加载入口。
 * <p>
 * 需求文档：docs/action-authoring-extensibility-requirements.md FR-01 / FR-02 / FR-08
 * <p>
 * 职责：把「主文件 + 动作包目录下按字典序排列的片段」依次加载进同一个
 * {@link Configuration}，最后统一 {@link Configuration#validate()}。
 * 拆成独立类是为了与 {@code Main} 的 Swing/窗口逻辑解耦，便于 headless 单测，
 * 也便于显式重载（FR-03）复用完全相同的构建路径。
 * <p>
 * <b>片段文件格式</b>：与主文件一致，根节点必须是 {@code <Mascot>}，
 * 内部可只包含一个 {@code <ActionList>} 或 {@code <BehaviorList>}。
 * 不要求包含全部动作，因为 {@code Configuration.load} 是累加语义。
 */
public final class ConfigurationFiles {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationFiles.class);

    /** 动作包目录名（相对主文件所在目录）。 */
    public static final String ACTIONS_D_DIR = "actions.d";
    /** 行为包目录名（相对主文件所在目录）。 */
    public static final String BEHAVIORS_D_DIR = "behaviors.d";

    private ConfigurationFiles() {
    }

    /** 一次构建的结果：配置本体 + 实际读取的文件列表（按加载顺序）。 */
    public static final class Result {
        private final Configuration configuration;
        private final List<String> sources;
        private final List<String> bornMascots;

        private Result(Configuration configuration, List<String> sources, Set<String> bornMascots) {
            this.configuration = configuration;
            this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
            this.bornMascots = Collections.unmodifiableList(new ArrayList<>(bornMascots));
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public List<String> getSources() {
            return sources;
        }

        /** 所有动作文件（含动作包片段）里声明的 BornMascot，按出现顺序去重。 */
        public List<String> getBornMascots() {
            return bornMascots;
        }
    }

    /**
     * 构建一份完整配置。
     *
     * @param actionsFile   主动作文件（必须存在）
     * @param behaviorsFile 主行为文件（必须存在）
     * @param imageSet      角色目录，用于解析 Pose 的图片路径
     */
    public static Result build(final String actionsFile, final String behaviorsFile, final String imageSet)
            throws IOException, SAXException, ParserConfigurationException, ConfigurationException {
        final Configuration configuration = new Configuration();
        final List<String> sources = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        final Set<String> bornMascots = new LinkedHashSet<>();

        loadInto(configuration, new File(actionsFile), imageSet, sources, problems, bornMascots, true, true);
        for (File fragment : listFragmentFiles(new File(actionsFile).getParentFile(), ACTIONS_D_DIR)) {
            loadInto(configuration, fragment, imageSet, sources, problems, bornMascots, false, true);
        }

        loadInto(configuration, new File(behaviorsFile), imageSet, sources, problems, bornMascots, true, false);
        for (File fragment : listFragmentFiles(new File(behaviorsFile).getParentFile(), BEHAVIORS_D_DIR)) {
            loadInto(configuration, fragment, imageSet, sources, problems, bornMascots, false, false);
        }

        if (!problems.isEmpty()) {
            log.warn("Configuration diagnostics ({} issue(s)):\n  {}", problems.size(), String.join("\n  ", problems));
        }

        // 所有片段加载完毕后再统一校验，跨文件引用才能被正确解析（FR-01 AC-01-4）
        configuration.validate();
        log.info("Configuration built for imageSet '{}' from {} file(s): {}", imageSet, sources.size(), sources);
        return new Result(configuration, sources, bornMascots);
    }

    /**
     * 列出动作包目录下所有 {@code .xml} 片段，按文件名字典序排序（FR-01 AC-01-1）。
     * 目录不存在时返回空列表（FR-01 AC-01-3）。
     */
    public static List<File> listFragmentFiles(final File parent, final String dirName) {
        if (parent == null) {
            return Collections.emptyList();
        }
        final File dir = new File(parent, dirName);
        if (!dir.isDirectory()) {
            return Collections.emptyList();
        }
        final File[] files = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".xml") && !name.startsWith("."));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return Collections.unmodifiableList(Arrays.asList(files));
    }

    /**
     * 检测 XML 中「后继行为列表」节点名与 schema 映射是否一致。
     * <p>
     * 已核实缺陷 D-1：{@code conf/*.xml} 使用 {@code <NextBehavior>}，
     * 而 {@code schema*.properties} 映射为 {@code NextBehaviorList}，
     * {@code Entry.selectChildren} 又是精确匹配，导致这些节点不被解析。
     * 本方法<b>只诊断、不修数据、不改变运行时语义</b>，
     * 以免在 Q5 决策前擅自改变自主行为分布（NFR-01）。
     *
     * @return 不一致描述列表；为空表示一致
     */
    public static List<String> detectNodeNameMismatch(final Document doc, final ResourceBundle schema) {
        final List<String> problems = new ArrayList<>();
        if (doc == null || schema == null) {
            return problems;
        }
        final String expected;
        try {
            expected = schema.getString("NextBehaviourList");
        } catch (Exception e) {
            return problems;
        }
        final Set<String> found = new LinkedHashSet<>();
        final NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            final String name = all.item(i).getNodeName();
            if (name != null && name.contains("NextBehav") && !name.equals(expected)) {
                found.add(name);
            }
        }
        for (String name : found) {
            problems.add("node <" + name + "> does not match schema NextBehaviourList=<" + expected
                    + ">; these nodes will be ignored by BehaviorBuilder");
        }
        return problems;
    }

    /**
     * 预检所有 Pose 引用的图片文件是否存在，汇总全部缺失项（FR-08 AC-08-3）。
     * 真正的加载失败仍由 {@code ImagePairLoader} 抛出；这里只负责一次性报告全貌。
     */
    public static void collectMissingImages(final Document doc, final String imageSet,
                                           final String source, final List<String> problems) {
        if (doc == null) {
            return;
        }
        final NodeList poses = doc.getElementsByTagName("Pose");
        for (int i = 0; i < poses.getLength(); i++) {
            final Element pose = (Element) poses.item(i);
            checkImage(pose, "Image", imageSet, source, problems);
            checkImage(pose, "ImageRight", imageSet, source, problems);
        }
    }

    private static void checkImage(final Element pose, final String attribute, final String imageSet,
                                   final String source, final List<String> problems) {
        final String value = pose.getAttribute(attribute);
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        final Path path = AnimationBuilder.resolveImagePath(imageSet, value);
        if (!Files.isRegularFile(path)) {
            final String actionName = findAncestorName(pose, "Action");
            problems.add(source + ": " + (actionName == null ? "<Pose>" : "Action[" + actionName + "]")
                    + " Pose " + attribute + "=\"" + value + "\" -> missing file " + path);
        }
    }

    private static String findAncestorName(final Element element, final String tag) {
        org.w3c.dom.Node node = element.getParentNode();
        while (node != null) {
            if (node instanceof Element && tag.equals(node.getNodeName())) {
                return ((Element) node).getAttribute("Name");
            }
            node = node.getParentNode();
        }
        return null;
    }

    private static void loadInto(final Configuration configuration, final File file, final String imageSet,
                                 final List<String> sources, final List<String> problems,
                                 final Set<String> bornMascots, final boolean required,
                                 final boolean isActionSource)
            throws IOException, SAXException, ParserConfigurationException, ConfigurationException {
        if (!file.isFile()) {
            if (required) {
                throw new IOException("Configuration file not found: " + file.getAbsolutePath());
            }
            return;
        }
        final String source = file.getAbsolutePath();
        final Document doc;
        try (InputStream in = new FileInputStream(file)) {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        } catch (SAXException e) {
            throw new SAXException("Failed to parse " + source + ": " + e.getMessage(), e);
        }

        // FR-08 AC-08-1：把来源文件前缀进错误信息，设计者能直接定位到具体文件
        try {
            configuration.load(new Entry(doc.getDocumentElement()), imageSet, source);
        } catch (ConfigurationException e) {
            throw new ConfigurationException(source + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IOException(source + ": " + e.getMessage(), e);
        }
        sources.add(source);

        // 诊断在 load 之后执行，此时 configuration.getSchema() 已由 load 初始化
        for (String mismatch : detectNodeNameMismatch(doc, configuration.getSchema())) {
            problems.add(source + ": " + mismatch);
        }
        collectMissingImages(doc, imageSet, source, problems);
        if (isActionSource) {
            final NodeList actionNodes = doc.getElementsByTagName("Action");
            for (int i = 0; i < actionNodes.getLength(); i++) {
                final String born = ((Element) actionNodes.item(i)).getAttribute("BornMascot");
                if (born != null && !born.trim().isEmpty()) {
                    bornMascots.add(born.trim());
                }
            }
        }
        log.info("Loaded configuration fragment: {}", source);
    }
}
