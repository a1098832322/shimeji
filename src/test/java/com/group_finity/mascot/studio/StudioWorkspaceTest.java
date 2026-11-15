package com.group_finity.mascot.studio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 设计器工作区层测试：路径白名单、原子写与备份上限、基础层指纹、片段规范化与 XSD 校验。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md DR-09（先校验后落盘）、DR-13（原子写 + 备份）、
 * DR-14（路径白名单）、§4.2（片段归属标记）。
 */
public class StudioWorkspaceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** 造一个最小可用的 prod 布局：conf/{actions.xml,behaviors.xml} + img/&lt;skin&gt;/。 */
    private WorkspaceManager workspace() throws IOException {
        File root = tmp.newFolder("engine");
        File conf = new File(root, "conf");
        assertTrue(conf.mkdirs());
        Files.writeString(conf.toPath().resolve("actions.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><ActionList/></Mascot>", StandardCharsets.UTF_8);
        Files.writeString(conf.toPath().resolve("behaviors.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><BehaviorList/></Mascot>", StandardCharsets.UTF_8);
        File img = new File(root, "img" + File.separator + "Shimeji");
        assertTrue(img.mkdirs());
        StudioOptions options = StudioOptions.parse(
                new String[]{"--workspace", root.getAbsolutePath(), "--no-browser"});
        return new WorkspaceManager(options);
    }

    @Test
    public void requireSafeNameAllowsCjkButRejectsPathSeparators() {
        assertEquals("八云紫", WorkspaceManager.requireSafeName("八云紫", "皮肤"));
        assertEquals("miku-2.0", WorkspaceManager.requireSafeName("miku-2.0", "皮肤"));

        for (String bad : new String[]{"../etc", "a/b", "a\\b", "", "   ", "x\u0000y"}) {
            try {
                WorkspaceManager.requireSafeName(bad, "皮肤");
                fail("应当拒绝非法名称: " + bad);
            } catch (StudioException expected) {
                assertEquals(400, expected.getStatus());
            }
        }
    }

    @Test
    public void safeResolveRejectsTraversal() throws IOException {
        WorkspaceManager wm = workspace();
        assertNotNull(wm.safeResolve("conf/actions.xml", "文件"));
        for (String bad : new String[]{"../outside.txt", "conf/../../x", "/etc/passwd"}) {
            try {
                wm.safeResolve(bad, "文件");
                fail("应当拒绝越界路径: " + bad);
            } catch (StudioException expected) {
                assertEquals(400, expected.getStatus());
            }
        }
    }

    @Test
    public void writeAtomicKeepsFiveBackupsAndLeavesNoTempFile() throws IOException {
        WorkspaceManager wm = workspace();
        Path target = wm.getConfDir().resolve("actions.d").resolve("50-test.xml");

        for (int i = 0; i < 8; i++) {
            wm.writeAtomic(target, ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Mascot><ActionList/><!--" + i + "--></Mascot>").getBytes(StandardCharsets.UTF_8));
        }

        assertTrue("最后一次写入必须落盘", Files.readString(target).contains("<!--7-->"));
        List<Path> backups = wm.listBackups(target);
        assertEquals("备份最多保留 5 份", 5, backups.size());

        try (var stream = Files.list(target.getParent())) {
            assertFalse("不得残留临时文件",
                    stream.anyMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    public void writeAtomicKeepsMarkerAndDetectsOwnership() throws IOException {
        WorkspaceManager wm = workspace();
        Path target = wm.getConfDir().resolve("actions.d").resolve("50-mark.xml");
        // 真实链路：DocumentService 先经过 Xml.normalizeFragment（补归属标记）再 writeAtomic
        String xml = Xml.normalizeFragment("<Mascot><ActionList/></Mascot>", "actions");
        wm.writeAtomic(target, xml.getBytes(StandardCharsets.UTF_8));
        assertTrue("设计器写的片段必须能被识别", wm.isGeneratedByStudio(target));

        Path handwritten = wm.getConfDir().resolve("actions.d").resolve("60-hand.xml");
        Files.writeString(handwritten,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><ActionList/></Mascot>",
                StandardCharsets.UTF_8);
        assertFalse("手写片段不应被当作设计器产物", wm.isGeneratedByStudio(handwritten));
    }

    @Test
    public void baselineFingerprintCoversBaseLayerOnly() throws IOException {
        WorkspaceManager wm = workspace();
        Map<String, String> before = wm.baselineFingerprint();
        assertEquals(2, before.size());
        assertEquals("同一状态必须得到同一指纹", before, wm.baselineFingerprint());

        wm.writeAtomic(wm.getConfDir().resolve("actions.d").resolve("50-x.xml"),
                "<Mascot><ActionList/></Mascot>".getBytes(StandardCharsets.UTF_8));
        assertEquals("写片段不得影响基础层指纹", before, wm.baselineFingerprint());

        Files.writeString(wm.getConfDir().resolve("actions.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><ActionList/></Mascot><!--x-->",
                StandardCharsets.UTF_8);
        assertFalse("基础文件变了指纹必须变化", before.equals(wm.baselineFingerprint()));
    }

    @Test
    public void normalizeFragmentAddsMarkerAndStripsBomAndStandalone() {
        String withBom = "\uFEFF<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Mascot>\n<ActionList>\n"
                + "<Action Name=\"A\" Type=\"Stay\"/>\n</ActionList>\n</Mascot>";

        String normalized = Xml.normalizeFragment(withBom, "actions");

        assertFalse("序列化结果不得带 BOM", normalized.startsWith("\uFEFF"));
        assertFalse("不得带 standalone 声明", normalized.contains("standalone"));
        assertTrue("必须补上归属标记", normalized.contains("generated-by: shimeji-studio"));
        assertTrue(normalized.contains("Name=\"A\""));
        assertFalse("规范化后不得出现连续空行", normalized.contains("\n\n\n"));
    }

    @Test
    public void normalizeFragmentRejectsWrongStructure() {
        String[][] bad = {
                {"<NotMascot><ActionList/></NotMascot>", "根节点"},
                {"<Mascot></Mascot>", "ActionList"},
                {"<Mascot><ActionList/><BehaviorList/></Mascot>", "只能包含"},
                // 动作片段里混入行为列表
                {"<Mascot><BehaviorList/></Mascot>", "只能包含"},
        };
        for (String[] c : bad) {
            try {
                Xml.normalizeFragment(c[0], "actions");
                fail("应当拒绝：" + c[0]);
            } catch (StudioException expected) {
                assertEquals(422, expected.getStatus());
                assertTrue("错误信息应说明原因：" + expected.getMessage(),
                        expected.getMessage().contains(c[1]));
            }
        }
    }

    @Test
    public void validateXsdAcceptsRealFragmentAndRejectsUnknownAttribute() throws IOException {
        Path xsd = Path.of("src/main/resources/conf/Mascot.xsd");
        assertTrue("需要真实 Mascot.xsd 才能跑 L1", Files.isRegularFile(xsd));

        byte[] good = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><ActionList>"
                + "<Action Name=\"A\" Type=\"Stay\"><Animation>"
                + "<Pose Image=\"/shime1.png\" ImageAnchor=\"64,128\" Velocity=\"0,0\" Duration=\"250\"/>"
                + "</Animation></Action></ActionList></Mascot>").getBytes(StandardCharsets.UTF_8);
        List<String> errors = Xml.validateXsd(good, xsd);
        assertTrue("合法片段不应报错：" + errors, errors.isEmpty());

        byte[] bad = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Mascot><ActionList>"
                + "<Action Name=\"A\" Type=\"Stay\"><Animation>"
                + "<Pose Image=\"/shime1.png\" NoSuchAttr=\"1\"/>"
                + "</Animation></Action></ActionList></Mascot>").getBytes(StandardCharsets.UTF_8);
        assertFalse("非法属性必须被 XSD 拦下", Xml.validateXsd(bad, xsd).isEmpty());
    }
}
