package com.group_finity.mascot.config;

import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.glados.GladosCommand;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.image.MascotImage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 动作包 / 同名覆盖 / 显式重载 / 设计时校验的契约与单元测试。
 * <p>
 * 需求文档：docs/action-authoring-extensibility-requirements.md FR-01/02/03/04/08、TC 矩阵
 * <p>
 * 刻意放在 {@code com.group_finity.mascot.config} 包内，以便访问包私有的
 * {@code Configuration.getActionBuilders()}，断言同名覆盖后<b>真正生效的类型</b>，
 * 而不是只测一层镜像逻辑。全部用例 headless 安全：自建临时 XML 不含真实 PNG。
 */
public class ConfigurationFilesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static String mascot(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot>" + body + "</Mascot>";
    }

    private static File write(File dir, String name, String content) throws IOException {
        File f = new File(dir, name);
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** 造一份最小可用的主配置：Base 动作 + 同名 Base 行为。 */
    private File[] minimalConfig(String actionsBody, String behaviorsBody) throws IOException {
        File root = tmp.newFolder();
        File actions = write(root, "actions.xml", mascot("<ActionList>" + actionsBody + "</ActionList>"));
        File behaviors = write(root, "behaviors.xml", mascot("<BehaviorList>" + behaviorsBody + "</BehaviorList>"));
        return new File[]{root, actions, behaviors};
    }

    // ------------------------------------------------------------------
    // FR-01 动作包
    // ------------------------------------------------------------------

    @Test
    public void actionPackageLoadsInLexicographicOrderAndResolvesCrossFileReferences() throws Exception {
        File[] c = minimalConfig(
                "<Action Name=\"Base\" Type=\"Stay\"/>",
                "<Behavior Name=\"Base\" Frequency=\"0\" Hidden=\"true\"/>");
        File d = new File(c[0], ConfigurationFiles.ACTIONS_D_DIR);
        assertTrue(d.mkdirs());
        write(d, "20-second.xml", mascot("<ActionList><Action Name=\"Second\" Type=\"Sequence\">"
                + "<ActionReference Name=\"Base\"/></Action></ActionList>"));
        write(d, "10-first.xml", mascot("<ActionList><Action Name=\"First\" Type=\"Sequence\">"
                + "<ActionReference Name=\"Base\"/></Action></ActionList>"));

        ConfigurationFiles.Result r = ConfigurationFiles.build(
                c[1].getAbsolutePath(), c[2].getAbsolutePath(), "Shimeji");

        List<String> sources = r.getSources();
        assertEquals("主文件 + 2 个片段 + 行为文件", 4, sources.size());
        assertTrue("字典序：10-first 必须先于 20-second", sources.get(1).endsWith("10-first.xml"));
        assertTrue(sources.get(2).endsWith("20-second.xml"));
        assertTrue(sources.get(3).endsWith("behaviors.xml"));
        // 跨文件引用在统一 validate() 中通过
        assertNotNull(r.getConfiguration().getActionBuilders().get("First"));
        assertNotNull(r.getConfiguration().getActionBuilders().get("Second"));
    }

    @Test
    public void missingPackageDirectoryFallsBackToSingleFile() throws Exception {
        File[] c = minimalConfig(
                "<Action Name=\"Base\" Type=\"Stay\"/>",
                "<Behavior Name=\"Base\" Frequency=\"0\" Hidden=\"true\"/>");

        ConfigurationFiles.Result r = ConfigurationFiles.build(
                c[1].getAbsolutePath(), c[2].getAbsolutePath(), "Shimeji");
        assertEquals(2, r.getSources().size());
        assertEquals(0, ConfigurationFiles.listFragmentFiles(c[0], ConfigurationFiles.ACTIONS_D_DIR).size());
    }

    @Test
    public void bornMascotIsCollectedFromPackageFragments() throws Exception {
        File[] c = minimalConfig(
                "<Action Name=\"Base\" Type=\"Stay\"/>",
                "<Behavior Name=\"Base\" Frequency=\"0\" Hidden=\"true\"/>");
        File d = new File(c[0], ConfigurationFiles.ACTIONS_D_DIR);
        assertTrue(d.mkdirs());
        write(d, "10-born.xml", mascot("<ActionList>"
                + "<Action Name=\"Spawner\" Type=\"Stay\" BornMascot=\"AnotherShimeji\"/>"
                + "</ActionList>"));

        ConfigurationFiles.Result r = ConfigurationFiles.build(
                c[1].getAbsolutePath(), c[2].getAbsolutePath(), "Shimeji");
        assertTrue(r.getBornMascots().contains("AnotherShimeji"));
    }

    // ------------------------------------------------------------------
    // FR-02 同名覆盖
    // ------------------------------------------------------------------

    @Test
    public void crossFileSameNameActionIsOverriddenByLaterFile() throws Exception {
        File[] c = minimalConfig(
                "<Action Name=\"Base\" Type=\"Stay\"/>",
                "<Behavior Name=\"Base\" Frequency=\"0\" Hidden=\"true\"/>");
        File d = new File(c[0], ConfigurationFiles.ACTIONS_D_DIR);
        assertTrue(d.mkdirs());
        write(d, "10-override.xml", mascot("<ActionList><Action Name=\"Base\" Type=\"Move\"/></ActionList>"));

        ConfigurationFiles.Result r = ConfigurationFiles.build(
                c[1].getAbsolutePath(), c[2].getAbsolutePath(), "Shimeji");

        assertEquals("后写覆盖必须真正改变生效的类型", "Move",
                r.getConfiguration().getActionBuilders().get("Base").getType());
        assertTrue("来源应指向覆盖文件", r.getConfiguration().getActionSource("Base").endsWith("10-override.xml"));
    }

    @Test
    public void duplicateWithinSameFileStillFailsAndNamesTheSource() throws Exception {
        File[] c = minimalConfig(
                "<Action Name=\"Dup\" Type=\"Stay\"/><Action Name=\"Dup\" Type=\"Move\"/>",
                "<Behavior Name=\"Dup\" Frequency=\"0\" Hidden=\"true\"/>");
        try {
            ConfigurationFiles.build(c[1].getAbsolutePath(), c[2].getAbsolutePath(), "Shimeji");
            fail("单文件内同名声明必须抛异常");
        } catch (ConfigurationException e) {
            assertTrue("错误信息应含来源文件: " + e.getMessage(), e.getMessage().contains("actions.xml"));
            assertTrue("错误信息应含动作名: " + e.getMessage(), e.getMessage().contains("Dup"));
        }
    }

    // ------------------------------------------------------------------
    // FR-08 设计时校验 / D-1 诊断
    // ------------------------------------------------------------------

    private static final String[] BEHAVIOR_CONFIGS = {
            "behaviors.xml", "CalmBehavior.xml", "MischievousBehavior.xml", "ProfessionalBehavior.xml"
    };

    private static Document parseReal(String name) throws Exception {
        File real = new File("src/main/resources/conf/" + name);
        assertTrue("契约用例必须读真实配置文件: " + name, real.isFile());
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(real);
    }

    /** D-1 修复（Q5 方案 C，TC-10 绿灯版）：仓库内 4 份行为配置必须全部使用规范名。 */
    @Test
    public void allRepositoryBehaviorFilesUseCanonicalNextBehaviorList() throws Exception {
        ResourceBundle schema = ResourceBundle.getBundle("conf.schema", Locale.US);
        assertEquals("schema 实际映射", "NextBehaviorList", schema.getString("NextBehaviourList"));

        for (String name : BEHAVIOR_CONFIGS) {
            Document doc = parseReal(name);
            List<String> problems = ConfigurationFiles.detectNodeNameMismatch(doc, schema);
            assertTrue(name + " 不应再有节点名不一致: " + problems, problems.isEmpty());
            assertEquals(name + " 应含 7 处后继行为列表", 7,
                    doc.getElementsByTagName("NextBehaviorList").getLength());
        }
    }

    /** 诊断方法本身仍有效：旧写法能被检出，并说明仍被兼容、仅建议改名。 */
    @Test
    public void legacyNextBehaviorAliasIsReportedByDiagnostic() throws Exception {
        Document doc = parse(mascot("<BehaviorList><Behavior Name=\"B\" Frequency=\"1\">"
                + "<NextBehavior Add=\"false\"><BehaviorReference Name=\"B\" Frequency=\"1\"/>"
                + "</NextBehavior></Behavior></BehaviorList>"));

        List<String> problems = ConfigurationFiles.detectNodeNameMismatch(
                doc, ResourceBundle.getBundle("conf.schema", Locale.US));

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("NextBehavior"));
        assertTrue("诊断应说明旧写法仍被兼容: " + problems.get(0),
                problems.get(0).contains("legacy alias"));
    }

    /** 规范名与兼容别名都必须真正解析出后继行为，杜绝再次静默失效。 */
    @Test
    public void nextBehaviorListsAreParsedForBothCanonicalAndLegacyNames() throws Exception {
        for (String tag : new String[]{"NextBehaviorList", "NextBehavior"}) {
            File[] c = minimalConfig(
                    "<Action Name=\"Base\" Type=\"Stay\"/><Action Name=\"Next\" Type=\"Stay\"/>",
                    "<Behavior Name=\"Base\" Frequency=\"1\" Hidden=\"true\"/>");
            Files.write(c[2].toPath(), mascot("<BehaviorList>"
                    + "<Behavior Name=\"Base\" Frequency=\"1\" Hidden=\"true\">"
                    + "<" + tag + " Add=\"false\"><BehaviorReference Name=\"Next\" Frequency=\"1\"/>"
                    + "</" + tag + "></Behavior></BehaviorList>").getBytes(StandardCharsets.UTF_8));

            ConfigurationFiles.Result r = ConfigurationFiles.build(
                    c[1].getAbsolutePath(), c[2].getAbsolutePath(), "Shimeji");
            BehaviorBuilder b = r.getConfiguration().getBehaviorBuilder("Base");

            assertNotNull("<" + tag + "> 应产出 BehaviorBuilder", b);
            assertEquals("<" + tag + "> 的后继列表应被解析", 1, b.getNextBehaviorBuilders().size());
            assertEquals("Next", b.getNextBehaviorBuilders().get(0).getName());
            assertFalse("<" + tag + "> 的 Add=false 应被识别", b.isNextAdditive());
        }
    }

    /** R4 防卡死：Add=false 会清空全局候选，其后继列表必须非空。 */
    @Test
    public void nonAdditiveNextListsAreNeverEmpty() throws Exception {
        for (String name : BEHAVIOR_CONFIGS) {
            NodeList lists = parseReal(name).getElementsByTagName("NextBehaviorList");
            for (int i = 0; i < lists.getLength(); i++) {
                Element list = (Element) lists.item(i);
                if ("false".equalsIgnoreCase(list.getAttribute("Add"))) {
                    assertTrue(name + " 第 " + (i + 1) + " 个 Add=false 后继列表不能为空，"
                                    + "否则 buildNextBehavior 会因 totalFrequency=0 把角色传送回屏幕顶端",
                            list.getElementsByTagName("BehaviorReference").getLength() > 0);
                }
            }
        }
    }

    /** NFR-02：托管 AI 行为表 5 处规范名与禁增殖覆盖不得被本次改名波及。 */
    @Test
    public void gladosBehaviorTableStillUsesCanonicalList() throws Exception {
        File f = new File("src/main/resources/glados-behaviors.xml");
        assertTrue(f.isFile());
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);

        assertEquals(5, doc.getElementsByTagName("NextBehaviorList").getLength());
        assertTrue(ConfigurationFiles.detectNodeNameMismatch(
                doc, ResourceBundle.getBundle("conf.schema", Locale.US)).isEmpty());
    }

    @Test
    public void missingImagesAreReportedAllAtOnce() throws Exception {
        Document doc = parse(mascot("<ActionList><Action Name=\"A\" Type=\"Stay\"><Animation>"
                + "<Pose Image=\"/definitely-missing-1.png\" ImageAnchor=\"1,1\" Velocity=\"0,0\" Duration=\"1\"/>"
                + "<Pose Image=\"/definitely-missing-2.png\" ImageAnchor=\"1,1\" Velocity=\"0,0\" Duration=\"1\"/>"
                + "</Animation></Action></ActionList>"));
        List<String> problems = new ArrayList<>();

        ConfigurationFiles.collectMissingImages(doc, "Shimeji", "unit-test.xml", problems);

        assertEquals("应汇总全部缺失项而非遇第一个就中断", 2, problems.size());
        assertTrue(problems.get(0).contains("unit-test.xml"));
        assertTrue(problems.get(0).contains("Action[A]"));
    }

    @Test
    public void resolveImagePathSupportsRelativeSubdirectory() {
        Path p = AnimationBuilder.resolveImagePath("Shimeji", "/sub/foo.png");
        String normalized = p.toString().replace('\\', '/');
        assertTrue("相对子目录应被 Paths.resolve 保留: " + normalized,
                normalized.contains("Shimeji/sub/foo.png"));
    }

    // ------------------------------------------------------------------
    // FR-03 / FR-04 重载命令与缓存
    // ------------------------------------------------------------------

    @Test
    public void reloadCommandIsWhitelisted() {
        assertEquals(GladosCommand.RELOAD, GladosCommand.fromWireName("reload"));
        assertNull("近似命令仍须被拒绝", GladosCommand.fromWireName("reloadAll"));
        assertNull(GladosCommand.fromWireName(null));
    }

    @Test
    public void imageCacheClearIsScopedAndCanBeRestoredFromSnapshot() {
        ImagePairs.clear();
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        MascotImage mi = new MascotImage(img, new Point(0, 0));
        ImagePair pair = new ImagePair(mi, mi);
        ImagePairs.load("./img/Shimeji/shime1.png", pair);
        ImagePairs.load("./img/Miku/shime1.png", pair);

        Map<String, ImagePair> snapshot = ImagePairs.snapshot();
        assertEquals(1, ImagePairs.clearForImageSet("/any/path/img/Shimeji"));
        assertFalse(ImagePairs.contains("./img/Shimeji/shime1.png"));
        assertTrue("只清目标角色，其他角色不受影响", ImagePairs.contains("./img/Miku/shime1.png"));

        ImagePairs.restore(snapshot);
        assertTrue("失败回滚应精确恢复缓存", ImagePairs.contains("./img/Shimeji/shime1.png"));
        assertTrue(ImagePairs.contains("./img/Miku/shime1.png"));
        ImagePairs.clear();
    }

    // ------------------------------------------------------------------
    // 契约：实现不得污染既有配置
    // ------------------------------------------------------------------

    @Test
    public void repositoryConfigFilesRemainUntouched() throws Exception {
        assertFalse("示例资产不得写回 conf/（FR-12 AC-12-4）",
                new File("src/main/resources/conf/actions.d").exists());

        File glados = new File("src/main/resources/glados-behaviors.xml");
        assertTrue(glados.isFile());
        String g = read(glados);
        assertTrue("托管禁增殖覆盖不得回归（AC-02-4 / NFR-02）",
                g.contains("Name=\"SplitIntoTwo\" Frequency=\"0\""));
        assertTrue(g.contains("Name=\"PullUpShimeji\" Frequency=\"0\""));

        File settings = new File("src/main/resources/conf/settings.properties");
        assertTrue("重载不得回写 settings（AC-03-4）", !read(settings).contains("Breeding="));
    }
}
