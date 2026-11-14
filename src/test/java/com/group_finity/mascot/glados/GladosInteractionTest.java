package com.group_finity.mascot.glados;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 托管模式下的交互约束。
 * <p>
 * 设计文档：docs/glados-managed-constraints-design.md
 * <ul>
 *   <li>右键菜单在托管下不弹出——原版菜单能直接 createMascot 生成多个角色，
 *       绕过了 Breeding 与行为表约束；</li>
 *   <li>左键点击上报给 GLaDOS，由宿主唤起桌宠对话。</li>
 * </ul>
 */
public class GladosInteractionTest {

    private Path home;

    @After
    public void cleanUp() throws Exception {
        GladosOptions.setManagedProcessForTest(null);
        // homeDir 是静态的，必须在用例之间清除，否则会串味
        GladosEventReporter.resetForTest();
        if (home != null) {
            deleteRecursively(home.toFile());
            home = null;
        }
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    // ------------------------------------------------------------------
    // 点击事件上报
    // ------------------------------------------------------------------

    @Test
    public void clickEventIsWrittenToHomeDirectory() throws Exception {
        home = Files.createTempDirectory("glados-home-");
        GladosOptions.setManagedProcessForTest(true);
        GladosEventReporter.configure(home.toString());

        GladosEventReporter.report(GladosEventReporter.EVENT_CLICK);

        File dir = new File(home.toFile(), GladosEventReporter.EVENT_DIR);
        assertTrue("事件目录应被创建", dir.isDirectory());
        String[] files = dir.list();
        assertTrue("应写入一个事件文件", files != null && files.length == 1);
        assertTrue("文件名应包含 click 类型", files[0].endsWith("-click"));
    }

    @Test
    public void multipleClicksProduceDistinctFiles() throws Exception {
        home = Files.createTempDirectory("glados-home-");
        GladosOptions.setManagedProcessForTest(true);
        GladosEventReporter.configure(home.toString());

        GladosEventReporter.report(GladosEventReporter.EVENT_CLICK);
        GladosEventReporter.report(GladosEventReporter.EVENT_CLICK);
        GladosEventReporter.report(GladosEventReporter.EVENT_CLICK);

        File dir = new File(home.toFile(), GladosEventReporter.EVENT_DIR);
        String[] files = dir.list();
        assertEquals("三次点击应产生三个文件，不能互相覆盖", 3, files == null ? 0 : files.length);
    }

    @Test
    public void noEventWrittenWhenNotManaged() throws Exception {
        home = Files.createTempDirectory("glados-home-");
        GladosOptions.setManagedProcessForTest(null);
        GladosEventReporter.configure(home.toString());

        GladosEventReporter.report(GladosEventReporter.EVENT_CLICK);

        File dir = new File(home.toFile(), GladosEventReporter.EVENT_DIR);
        assertFalse("非托管（用户直跑）不应产生事件文件", dir.isDirectory());
    }

    @Test
    public void noEventWrittenWhenHomeNotConfigured() {
        GladosOptions.setManagedProcessForTest(true);
        // 不调用 configure，模拟未设置 --glados-home
        GladosEventReporter.resetForTest();
        assertFalse(GladosEventReporter.isEnabled());
        // 不应抛异常
        GladosEventReporter.report(GladosEventReporter.EVENT_CLICK);
    }

    @Test
    public void reporterDisabledWithoutManagedFlag() throws Exception {
        home = Files.createTempDirectory("glados-home-");
        GladosOptions.setManagedProcessForTest(null);
        GladosEventReporter.configure(home.toString());
        assertFalse(GladosEventReporter.isEnabled());
    }

    @Test
    public void reporterEnabledWhenManagedAndConfigured() throws Exception {
        home = Files.createTempDirectory("glados-home-");
        GladosOptions.setManagedProcessForTest(true);
        GladosEventReporter.configure(home.toString());
        assertTrue(GladosEventReporter.isEnabled());
    }

    @Test
    public void nullTypeIsIgnored() throws Exception {
        home = Files.createTempDirectory("glados-home-");
        GladosOptions.setManagedProcessForTest(true);
        GladosEventReporter.configure(home.toString());

        GladosEventReporter.report(null);

        File dir = new File(home.toFile(), GladosEventReporter.EVENT_DIR);
        assertFalse("null 类型不应写文件", dir.isDirectory());
    }

    @Test
    public void noTempFileLeftBehind() throws Exception {
        home = Files.createTempDirectory("glados-home-");
        GladosOptions.setManagedProcessForTest(true);
        GladosEventReporter.configure(home.toString());

        GladosEventReporter.report(GladosEventReporter.EVENT_CLICK);

        File dir = new File(home.toFile(), GladosEventReporter.EVENT_DIR);
        String[] files = dir.list();
        assertTrue(files != null);
        for (String name : files) {
            assertFalse("写入完成后不应残留 .tmp 文件: " + name, name.endsWith(".tmp"));
        }
    }

    // ------------------------------------------------------------------
    // 托管标记与交互约束的关系
    // ------------------------------------------------------------------

    @Test
    public void managedFlagGatesRightClickSuppression() {
        // 右键菜单的抑制条件就是 isManagedProcess()：托管抑制，非托管恢复原版
        GladosOptions.setManagedProcessForTest(true);
        assertTrue("托管下应抑制右键菜单", GladosOptions.isManagedProcess());

        GladosOptions.setManagedProcessForTest(null);
        assertFalse("非托管下保留原版右键菜单", GladosOptions.isManagedProcess());
    }
}