package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 校验中心 L0–L4 的行为契约。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §5.4、DR-08、DR-15。
 * <p>
 * 用最小引擎夹具（1 个动作 + 1 个行为 + 1 张真 PNG）跑真实 dry-run，
 * 断言各层能分别拦下：缺图、非法引用、保留名、与基础同名、Frequency&gt;0 提示。
 */
public class StudioValidationTest {

    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder();

    private static WorkspaceManager wm;
    private static ValidationService validation;

    private static final String SKIN = "Shimeji";

    private static final String ACTIONS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Mascot><ActionList>"
            + "<Action Name=\"Stand\" Type=\"Stay\" BorderType=\"Floor\"><Animation>"
            + "<Pose Image=\"/shime1.png\" ImageAnchor=\"64,128\" Velocity=\"0,0\" Duration=\"250\"/>"
            + "</Animation></Action>"
            + "</ActionList></Mascot>";

    private static final String BEHAVIORS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Mascot><BehaviorList>"
            + "<Behavior Name=\"Stand\" Frequency=\"10\"/>"
            + "</BehaviorList></Mascot>";

    @BeforeClass
    public static void setUpFixture() throws IOException {
        File root = tmp.newFolder("engine");
        File conf = new File(root, "conf");
        assertTrue(conf.mkdirs());
        Files.writeString(conf.toPath().resolve("actions.xml"), ACTIONS_XML, StandardCharsets.UTF_8);
        Files.writeString(conf.toPath().resolve("behaviors.xml"), BEHAVIORS_XML, StandardCharsets.UTF_8);

        Path xsd = Path.of("src/main/resources/conf/Mascot.xsd");
        assertTrue("需要真实 Mascot.xsd 才能跑 L1", Files.isRegularFile(xsd));
        Files.copy(xsd, conf.toPath().resolve("Mascot.xsd"));

        File imgDir = new File(root, "img" + File.separator + SKIN);
        assertTrue(imgDir.mkdirs());
        BufferedImage png = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        assertTrue(ImageIO.write(png, "png", new File(imgDir, "shime1.png")));

        StudioOptions options = StudioOptions.parse(
                new String[]{"--workspace", root.getAbsolutePath(), "--no-browser"});
        assertEquals("夹具必须是 prod 布局", null, options.validateLayout());
        wm = new WorkspaceManager(options);
        validation = new ValidationService(wm);
    }

    private static String actionsFragment(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><ActionList>" + body + "</ActionList></Mascot>";
    }

    private static String behaviorsFragment(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><BehaviorList>" + body + "</BehaviorList></Mascot>";
    }

    private static String stayAction(String name, String image, int duration) {
        return "<Action Name=\"" + name + "\" Type=\"Stay\" BorderType=\"Floor\"><Animation>"
                + "<Pose Image=\"" + image + "\" ImageAnchor=\"64,128\" Velocity=\"0,0\" "
                + "Duration=\"" + duration + "\"/></Animation></Action>";
    }

    private static JSONObject validate(List<ValidationService.Overlay> overlays) {
        JSONObject result = validation.validate(SKIN, overlays);
        assertNotNull(result);
        assertNotNull(result.getJSONObject("summary"));
        return result;
    }

    private static List<ValidationService.Overlay> one(String kind, String file, String xml) {
        List<ValidationService.Overlay> list = new ArrayList<>();
        list.add(new ValidationService.Overlay(kind, file, xml));
        return list;
    }

    private static boolean hasMessage(JSONArray diagnostics, String needle) {
        for (int i = 0; i < diagnostics.size(); i++) {
            JSONObject d = diagnostics.getJSONObject(i);
            String blob = d.getString("message") + "|" + d.getString("hint");
            if (blob.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void baselineWorkspacePassesWithNoDiagnostics() {
        JSONObject result = validate(new ArrayList<>());
        assertEquals("基线夹具本身必须干净：" + result.getJSONArray("diagnostics"),
                0, result.getJSONObject("summary").getIntValue("errors"));
    }

    @Test
    public void missingImageIsReportedByL3() {
        JSONObject result = validate(one("actions", "50-bad-image.xml",
                actionsFragment(stayAction("BrokenImg", "/no_such_sprite.png", 250))));

        JSONArray diagnostics = result.getJSONArray("diagnostics");
        assertTrue("缺图必须报错", result.getJSONObject("summary").getIntValue("errors") > 0);
        assertTrue("错误信息应指出缺失的图片：" + diagnostics, hasMessage(diagnostics, "no_such_sprite"));
    }

    @Test
    public void danglingActionReferenceIsRejectedByEngineDryRun() {
        JSONObject result = validate(one("actions", "50-dangling.xml",
                actionsFragment("<Action Name=\"Wave\" Type=\"Sequence\" Loop=\"true\">"
                        + "<ActionReference Name=\"NotDefined\"/></Action>")));

        JSONArray diagnostics = result.getJSONArray("diagnostics");
        assertTrue("引用不存在的动作必须报错",
                result.getJSONObject("summary").getIntValue("errors") > 0);
        assertTrue("错误信息应提到找不到引用：" + diagnostics,
                hasMessage(diagnostics, "NotDefined") || hasMessage(diagnostics, "无法找到"));
    }

    @Test
    public void reservedNameIsRejectedByL4() {
        JSONObject result = validate(one("behaviors", "50-reserved.xml",
                behaviorsFragment("<Behavior Name=\"SplitIntoTwo\" Frequency=\"0\" Hidden=\"true\"/>")));

        JSONArray diagnostics = result.getJSONArray("diagnostics");
        assertTrue("保留名必须报错", result.getJSONObject("summary").getIntValue("errors") > 0);
        assertTrue("错误信息应说明是保留名：" + diagnostics, hasMessage(diagnostics, "保留名"));
    }

    @Test
    public void sameNameAsBaseDefinitionIsRejectedByL4() {
        JSONObject result = validate(one("actions", "50-shadow.xml",
                actionsFragment(stayAction("Stand", "/shime1.png", 300))));

        JSONArray diagnostics = result.getJSONArray("diagnostics");
        assertTrue("覆盖基础动作必须报错", result.getJSONObject("summary").getIntValue("errors") > 0);
        assertTrue("错误信息应说明与基础同名：" + diagnostics, hasMessage(diagnostics, "同名"));
    }

    @Test
    public void positiveFrequencyOnlyWarns() {
        List<ValidationService.Overlay> overlays = new ArrayList<>();
        overlays.add(new ValidationService.Overlay("actions", "50-freq-actions.xml",
                actionsFragment(stayAction("MyFreq", "/shime1.png", 250))));
        overlays.add(new ValidationService.Overlay("behaviors", "50-freq-behaviors.xml",
                behaviorsFragment("<Behavior Name=\"MyFreq\" Frequency=\"10\" Hidden=\"true\"/>")));

        JSONObject result = validate(overlays);
        JSONObject summary = result.getJSONObject("summary");
        JSONArray diagnostics = result.getJSONArray("diagnostics");
        assertEquals("Frequency>0 不应阻断保存：" + diagnostics, 0, summary.getIntValue("errors"));
        assertTrue("应给出权重提示：" + diagnostics, hasMessage(diagnostics, "Frequency"));
    }

    @Test
    public void fileSchemeImageWarnsAsUndistributable() {
        JSONObject result = validate(one("actions", "50-file-scheme.xml",
                actionsFragment(stayAction("FileImg", "file:///tmp/a.png", 250))));

        JSONArray diagnostics = result.getJSONArray("diagnostics");
        assertTrue("file: 引用应给出不可分发提示：" + diagnostics, hasMessage(diagnostics, "不可打包分发"));
    }
}
