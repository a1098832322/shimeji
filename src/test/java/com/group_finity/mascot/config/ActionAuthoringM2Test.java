package com.group_finity.mascot.config;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.ConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * M2 表达力需求的契约与单元测试：FR-05 帧参数表达式化 / FR-06 图片路径解绑 /
 * FR-09 XSD 对齐 / FR-10 皮肤级增量覆盖。
 * <p>
 * 需求文档：docs/action-authoring-extensibility-requirements.md
 * <p>
 * 与 {@link ConfigurationFilesTest} 一样放在 config 同包，以便访问包私有的
 * {@code Configuration.getActionBuilders()}。所有用例 headless 安全：
 * 帧动作测试用临时生成的小 PNG，不依赖真实皮肤资源。
 */
public class ActionAuthoringM2Test {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static String mascot(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot>" + body + "</Mascot>";
    }

    private static File write(File dir, String name, String content) throws Exception {
        File f = new File(dir, name);
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Document parse(File f) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
    }

    private static ResourceBundle schema() {
        return ResourceBundle.getBundle("conf.schema", Locale.US);
    }

    private Path png() throws Exception {
        File f = tmp.newFile("pose-" + System.nanoTime() + ".png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", f);
        return f.toPath().toAbsolutePath();
    }

    /** 用真实 AnimationBuilder 构造一个含单帧的 Animation（不是镜像逻辑）。 */
    private Animation animation(String poseAttributes, Map<String, String> params) throws Exception {
        Document doc = parse("<Animation><Pose " + poseAttributes + "/></Animation>");
        AnimationBuilder builder = new AnimationBuilder(
                schema(), new Entry((Element) doc.getDocumentElement()), "Shimeji", "MyAction", params);
        return builder.buildAnimation();
    }

    private static Map<String, String> params(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private String attrs(Path image, String anchor, String velocity, String duration) {
        return "Image=\"file:" + image + "\" ImageAnchor=\"" + anchor + "\" Velocity=\""
                + velocity + "\" Duration=\"" + duration + "\"";
    }

    // ------------------------------------------------------------------
    // FR-05 帧参数表达式化
    // ------------------------------------------------------------------

    @Test
    public void poseParametersAcceptExpressionsFromActionContext() throws Exception {
        Animation a = animation(attrs(png(), "${1+1},${2+1}", "${vx},${vy}", "${d}"),
                params("vx", "4", "vy", "-5", "d", "250"));

        assertEquals(250, a.getDuration());
        Pose p = a.getPoseAt(0);
        assertEquals("Velocity 两个分量应分别求值并参与 scaling 换算", 4, p.getDx());
        assertEquals(-5, p.getDy());
    }

    @Test
    public void negatedActionParameterExpressionEvaluates() throws Exception {
        // examples/with-expression/40-expression.xml 使用了 ${-stepX}
        Animation a = animation(attrs(png(), "1,1", "${-stepX},0", "200"),
                params("stepX", "6"));
        assertEquals(-6, a.getPoseAt(0).getDx());
    }

    @Test
    public void mathRandomDurationExpressionIsEvaluated() throws Exception {
        Animation a = animation(attrs(png(), "1,1", "0,0", "${500+Math.random()*1000}"),
                Collections.<String, String>emptyMap());

        assertTrue("Duration 应落在 500~1500: " + a.getDuration(),
                a.getDuration() >= 500 && a.getDuration() < 1500);
    }

    @Test
    public void plainIntegerPoseParametersKeepLegacyBehaviour() throws Exception {
        Animation a = animation(attrs(png(), "64,128", "0,0", "300"),
                Collections.<String, String>emptyMap());

        assertEquals(300, a.getDuration());
        assertEquals(0, a.getPoseAt(0).getDx());
        assertEquals(0, a.getPoseAt(0).getDy());
    }

    @Test
    public void invalidPoseExpressionNamesActionAndPose() throws Exception {
        try {
            animation(attrs(png(), "1,1", "0,0", "notANumber"),
                    Collections.<String, String>emptyMap());
            fail("非数值 Duration 必须在加载阶段报错");
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            assertTrue("错误信息应定位到 Action: " + msg, msg.contains("Action[MyAction]"));
            assertTrue("错误信息应定位到 Pose 序号: " + msg, msg.contains("Pose[1]"));
            assertTrue("错误信息应含字段名: " + msg, msg.contains("Duration"));
        }
    }

    @Test
    public void zeroDurationIsRejectedToAvoidDivisionByZero() throws Exception {
        try {
            animation(attrs(png(), "1,1", "0,0", "0"), Collections.<String, String>emptyMap());
            fail("Duration=0 会让 Animation.getPoseAt() 除零，必须拒绝");
        } catch (ConfigurationException e) {
            assertTrue(e.getMessage().contains("Duration must be >= 1"));
        }
    }

    @Test
    public void malformedPairIsRejectedWithFieldName() throws Exception {
        try {
            animation("Image=\"file:" + png() + "\" ImageAnchor=\"1\" Velocity=\"0,0\" Duration=\"10\"",
                    Collections.<String, String>emptyMap());
            fail("逗号分隔的分量不足必须报错");
        } catch (Exception e) {
            assertTrue(String.valueOf(e.getMessage()).contains("ImageAnchor"));
        }
    }

    // ------------------------------------------------------------------
    // FR-06 图片路径解绑
    // ------------------------------------------------------------------

    @Test
    public void absoluteImagePathIsUsedDirectly() throws Exception {
        Path p = png();
        assertEquals(p, AnimationBuilder.resolveImagePath("Shimeji", "file:" + p));
        assertEquals("file:/// 写法同样支持", p,
                AnimationBuilder.resolveImagePath("Shimeji", "file://" + p));
    }

    @Test
    public void leadingSlashStaysRelativeToImageSet() {
        String resolved = AnimationBuilder.resolveImagePath("Shimeji", "/shime1.png")
                .toString().replace('\\', '/');
        assertTrue("历史写法 /shimeN.png 必须仍按相对路径解析: " + resolved,
                resolved.contains("Shimeji/shime1.png"));
    }

    @Test
    public void relativeSubdirectoryStillResolvesUnderImageSet() {
        String normalized = AnimationBuilder.resolveImagePath("Shimeji", "/sub/foo.png")
                .toString().replace('\\', '/');
        assertTrue(normalized, normalized.contains("Shimeji/sub/foo.png"));
    }

    @Test
    public void missingImageErrorContainsFullPathAndPoseLocation() throws Exception {
        String missing = tmp.getRoot().toPath().resolve("definitely-missing.png").toString();
        try {
            animation("Image=\"file:" + missing + "\" ImageAnchor=\"1,1\" Velocity=\"0,0\" Duration=\"10\"",
                    Collections.<String, String>emptyMap());
            fail("缺图必须报错");
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            assertTrue("错误信息应含完整路径: " + msg, msg.contains(missing));
            assertTrue("错误信息应定位 Action: " + msg, msg.contains("Action[MyAction]"));
            assertTrue("错误信息应定位 Pose: " + msg, msg.contains("Pose[1]"));
        }
    }

    @Test
    public void allRealPoseImagesResolveConsistently() throws Exception {
        Document doc = parse(new File("src/main/resources/conf/actions.xml"));
        NodeList poses = doc.getElementsByTagName("Pose");
        assertEquals("真实配置基线", 132, poses.getLength());

        Set<String> images = new HashSet<>();
        for (int i = 0; i < poses.getLength(); i++) {
            Element pose = (Element) poses.item(i);
            String image = pose.getAttribute("Image");
            if (image.isEmpty()) {
                continue;
            }
            images.add(image);
            String resolved = AnimationBuilder.resolveImagePath("Shimeji", image)
                    .toString().replace('\\', '/');
            assertTrue("解析路径应落在 imageSet 目录下: " + resolved, resolved.contains("Shimeji/"));
            assertTrue("文件名不得被改写: " + resolved,
                    resolved.endsWith(image.replaceFirst("^/+", "")));
        }
        assertEquals("真实配置基线", 46, images.size());
    }

    // ------------------------------------------------------------------
    // FR-09 XSD 与运行时对齐
    // ------------------------------------------------------------------

    @Test
    public void xsdActionTypeEnumerationMatchesRuntime() throws Exception {
        File xsd = new File("src/main/resources/conf/Mascot.xsd");
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd);

        Document doc = parse(xsd);
        NodeList attributes = doc.getElementsByTagName("attribute");
        Set<String> values = new HashSet<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attr = (Element) attributes.item(i);
            if (!"Type".equals(attr.getAttribute("name"))) {
                continue;
            }
            NodeList enums = attr.getElementsByTagName("enumeration");
            for (int j = 0; j < enums.getLength(); j++) {
                values.add(((Element) enums.item(j)).getAttribute("value"));
            }
        }
        assertEquals(new HashSet<>(Arrays.asList(
                "Embedded", "Move", "Stay", "Animate", "Sequence", "Select")), values);
        assertTrue("过时的 Pause/Fixed/Composite 不应残留",
                Collections.disjoint(values, Arrays.asList("Pause", "Fixed", "Composite")));
    }

    @Test
    public void xsdPoseAttributesAllowExpressions() throws Exception {
        Document doc = parse(new File("src/main/resources/conf/Mascot.xsd"));
        NodeList complexTypes = doc.getElementsByTagName("complexType");
        Element poseType = null;
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element ct = (Element) complexTypes.item(i);
            if ("Pose".equals(ct.getAttribute("name"))) {
                poseType = ct;
            }
        }
        assertNotNull(poseType);
        NodeList attributes = poseType.getElementsByTagName("attribute");
        Map<String, String> types = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            types.put(((Element) attributes.item(i)).getAttribute("name"),
                    ((Element) attributes.item(i)).getAttribute("type"));
        }
        assertEquals("tns:Parameter", types.get("Duration"));
        assertEquals("tns:NumericPairOrExpression", types.get("Velocity"));
        assertEquals("tns:NumericPairOrExpression", types.get("ImageAnchor"));
    }

    // ------------------------------------------------------------------
    // FR-10 皮肤级增量覆盖
    // ------------------------------------------------------------------

    private File[] skinFixture() throws Exception {
        File root = tmp.newFolder();
        File global = new File(root, "global");
        File skin = new File(root, "skin");
        assertTrue(global.mkdirs());
        assertTrue(skin.mkdirs());
        File ga = write(global, "actions.xml", mascot("<ActionList>"
                + "<Action Name=\"Base\" Type=\"Stay\"/>"
                + "<Action Name=\"Keep\" Type=\"Stay\"/></ActionList>"));
        File gb = write(global, "behaviors.xml", mascot("<BehaviorList>"
                + "<Behavior Name=\"Base\" Frequency=\"0\" Hidden=\"true\"/>"
                + "<Behavior Name=\"Keep\" Frequency=\"0\" Hidden=\"true\"/></BehaviorList>"));
        File sa = write(skin, "actions.xml", mascot("<ActionList>"
                + "<Action Name=\"Base\" Type=\"Move\"/></ActionList>"));
        File sb = write(skin, "behaviors.xml", mascot("<BehaviorList>"
                + "<Behavior Name=\"Base\" Frequency=\"7\" Hidden=\"true\"/></BehaviorList>"));
        return new File[]{ga, gb, sa, sb, skin};
    }

    @Test
    public void skinIncrementInheritsGlobalAndOverridesByName() throws Exception {
        File[] f = skinFixture();
        ConfigurationFiles.Result r = ConfigurationFiles.build(
                f[2].getAbsolutePath(), f[3].getAbsolutePath(), "Shimeji",
                f[0].getAbsolutePath(), f[1].getAbsolutePath());

        assertEquals("皮肤同名 Action 应覆盖全局", "Move",
                r.getConfiguration().getActionBuilders().get("Base").getType());
        assertNotNull("皮肤未声明的 Action 应从全局继承（AC-10-1）",
                r.getConfiguration().getActionBuilders().get("Keep"));
        assertEquals("皮肤同名 Behavior 应覆盖全局", 7,
                r.getConfiguration().getBehaviorBuilder("Base").getFrequency());
        assertNotNull("皮肤未声明的 Behavior 应从全局继承",
                r.getConfiguration().getBehaviorBuilder("Keep"));

        assertEquals("全局 2 个 + 皮肤 2 个主文件", 4, r.getSources().size());
        assertTrue("全局必须先生效", r.getSources().get(0).contains("global"));
        assertTrue("皮肤必须后生效", r.getSources().get(1).contains("skin"));
    }

    @Test
    public void identicalBaseAndSkinFilesAreLoadedOnlyOnce() throws Exception {
        File[] f = skinFixture();
        ConfigurationFiles.Result r = ConfigurationFiles.build(
                f[0].getAbsolutePath(), f[1].getAbsolutePath(), "Shimeji",
                f[0].getAbsolutePath(), f[1].getAbsolutePath());
        assertEquals("同一文件不得重复加载（否则同名 Action 会被误判为单文件内重复）",
                2, r.getSources().size());
    }

    @Test
    public void skinActionPackageFragmentsAreLoadedAfterGlobal() throws Exception {
        File[] f = skinFixture();
        File d = new File(f[4], ConfigurationFiles.ACTIONS_D_DIR);
        assertTrue(d.mkdirs());
        write(d, "10-extra.xml", mascot("<ActionList>"
                + "<Action Name=\"Extra\" Type=\"Stay\"/></ActionList>"));

        ConfigurationFiles.Result r = ConfigurationFiles.build(
                f[2].getAbsolutePath(), f[3].getAbsolutePath(), "Shimeji",
                f[0].getAbsolutePath(), f[1].getAbsolutePath());

        assertNotNull("皮肤自己的 actions.d 片段也应生效",
                r.getConfiguration().getActionBuilders().get("Extra"));
        assertNotNull("全局动作仍应存在", r.getConfiguration().getActionBuilders().get("Keep"));
    }
}
