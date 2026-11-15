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

    @Test
    public void d1MismatchIsDetectableInRealBehaviorsXml() throws Exception {
        File real = new File("src/main/resources/conf/behaviors.xml");
        assertTrue("契约用例必须读真实配置文件", real.isFile());
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(real);
        ResourceBundle schema = ResourceBundle.getBundle("conf.schema", Locale.US);

        List<String> problems = ConfigurationFiles.detectNodeNameMismatch(doc, schema);

        assertFalse("D-1：<NextBehavior> 与 schema 的 NextBehaviorList 不一致应被检出", problems.isEmpty());
        assertTrue(problems.get(0).contains("NextBehavior"));
        assertEquals("schema 实际映射", "NextBehaviorList", schema.getString("NextBehaviourList"));
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
