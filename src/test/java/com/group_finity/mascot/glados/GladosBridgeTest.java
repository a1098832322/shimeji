package com.group_finity.mascot.glados;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * GLaDOS 桥单元测试。
 * <p>
 * 设计文档：docs/glados-ai-integration-design.md §9.1
 * <p>
 * 覆盖：启动参数解析、命令白名单与参数校验、端口文件写入。
 * 不依赖 Swing/原生窗口，可在无显示环境运行。
 */
public class GladosBridgeTest {

    // ------------------------------------------------------------------
    // --glados-managed 解析
    // ------------------------------------------------------------------

    @Test
    public void testParseManagedFlag() {
        GladosOptions opts = GladosOptions.parse(new String[]{"--glados-managed"});
        assertTrue(opts.isManaged());
    }

    @Test
    public void testParseNoArgsNotManaged() {
        GladosOptions opts = GladosOptions.parse(new String[]{});
        assertFalse(opts.isManaged());
    }

    @Test
    public void testParseNullArgsNotManaged() {
        GladosOptions opts = GladosOptions.parse(null);
        assertFalse(opts.isManaged());
    }

    // ------------------------------------------------------------------
    // --glados-port 解析
    // ------------------------------------------------------------------

    @Test
    public void testParsePortZero() {
        GladosOptions opts = GladosOptions.parse(new String[]{"--glados-managed", "--glados-port", "0"});
        assertEquals(0, opts.getPort());
    }

    @Test
    public void testParsePortFixed() {
        GladosOptions opts = GladosOptions.parse(new String[]{"--glados-port", "8765"});
        assertEquals(8765, opts.getPort());
    }

    @Test
    public void testParsePortInvalidFallsBackToZero() {
        GladosOptions opts = GladosOptions.parse(new String[]{"--glados-port", "abc"});
        assertEquals(0, opts.getPort());
    }

    @Test
    public void testParsePortMissingValue() {
        GladosOptions opts = GladosOptions.parse(new String[]{"--glados-port"});
        assertEquals(0, opts.getPort());
    }

    // ------------------------------------------------------------------
    // --glados-home 解析
    // ------------------------------------------------------------------

    @Test
    public void testParseHome() {
        GladosOptions opts = GladosOptions.parse(new String[]{"--glados-home", "/tmp/glados"});
        assertEquals("/tmp/glados", opts.getHome());
    }

    @Test
    public void testParseFullCommandLine() {
        GladosOptions opts = GladosOptions.parse(new String[]{
                "--glados-managed", "--glados-port", "0", "--glados-home", "/tmp/glados"});
        assertTrue(opts.isManaged());
        assertEquals(0, opts.getPort());
        assertEquals("/tmp/glados", opts.getHome());
    }

    @Test
    public void testUnknownArgsIgnored() {
        GladosOptions opts = GladosOptions.parse(new String[]{"--unknown", "--glados-managed"});
        assertTrue(opts.isManaged());
    }

    // ------------------------------------------------------------------
    // 命令白名单
    // ------------------------------------------------------------------

    @Test
    public void testKnownCommandsResolved() {
        assertNotNull(GladosCommand.fromWireName("setRole"));
        assertNotNull(GladosCommand.fromWireName("setBehavior"));
        assertNotNull(GladosCommand.fromWireName("pause"));
        assertNotNull(GladosCommand.fromWireName("resume"));
        assertNotNull(GladosCommand.fromWireName("createMascot"));
        assertNotNull(GladosCommand.fromWireName("remainOne"));
        assertNotNull(GladosCommand.fromWireName("disposeAll"));
        assertNotNull(GladosCommand.fromWireName("quit"));
    }

    @Test
    public void testUnknownCommandRejected() {
        assertNull(GladosCommand.fromWireName("hack"));
        assertNull(GladosCommand.fromWireName(""));
        assertNull(GladosCommand.fromWireName(null));
    }

    // ------------------------------------------------------------------
    // 参数校验
    // ------------------------------------------------------------------

    @Test
    public void testSetRoleRequiresRole() {
        Map<String, String> params = new HashMap<>();
        assertNotNull(GladosCommand.SET_ROLE.validate(params));

        params.put("role", "魔理沙");
        assertNull(GladosCommand.SET_ROLE.validate(params));
    }

    @Test
    public void testSetBehaviorRequiresBehavior() {
        Map<String, String> params = new HashMap<>();
        assertNotNull(GladosCommand.SET_BEHAVIOR.validate(params));

        params.put("behavior", "AIWorking");
        assertNull(GladosCommand.SET_BEHAVIOR.validate(params));
    }

    @Test
    public void testCreateMascotRequiresRole() {
        Map<String, String> params = new HashMap<>();
        assertNotNull(GladosCommand.CREATE_MASCOT.validate(params));

        params.put("role", "灵梦");
        assertNull(GladosCommand.CREATE_MASCOT.validate(params));
    }

    @Test
    public void testRemainOneRequiresRole() {
        Map<String, String> params = new HashMap<>();
        assertNotNull(GladosCommand.REMAIN_ONE.validate(params));

        params.put("role", "灵梦");
        assertNull(GladosCommand.REMAIN_ONE.validate(params));
    }

    @Test
    public void testNoParamCommandsValidateOk() {
        assertNull(GladosCommand.PAUSE.validate(new HashMap<>()));
        assertNull(GladosCommand.RESUME.validate(new HashMap<>()));
        assertNull(GladosCommand.DISPOSE_ALL.validate(new HashMap<>()));
        assertNull(GladosCommand.QUIT.validate(new HashMap<>()));
    }

    // ------------------------------------------------------------------
    // 端口文件写入
    // ------------------------------------------------------------------

    @Test
    public void testWritePortFile() throws IOException {
        File dir = Files.createTempDirectory("glados-test").toFile();
        try {
            GladosOptions opts = new GladosOptions(true, 0, dir.getAbsolutePath());
            GladosBridge bridge = new GladosBridge(opts, null);
            bridge.writePortFileForTest(43210);
            File portFile = new File(dir, GladosBridge.PORT_FILE_NAME);
            assertTrue(portFile.isFile());
            String content = new String(Files.readAllBytes(portFile.toPath()), StandardCharsets.UTF_8);
            assertEquals("43210", content);
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    public void testWritePortFileCreatesMissingDir() throws IOException {
        File base = Files.createTempDirectory("glados-test").toFile();
        File nested = new File(base, "a/b/c");
        try {
            GladosOptions opts = new GladosOptions(true, 0, nested.getAbsolutePath());
            GladosBridge bridge = new GladosBridge(opts, null);
            bridge.writePortFileForTest(1234);
            File portFile = new File(nested, GladosBridge.PORT_FILE_NAME);
            assertTrue(portFile.isFile());
        } finally {
            deleteRecursively(base);
        }
    }

    @Test
    public void testWritePortFileNoHomeSkips() {
        GladosOptions opts = new GladosOptions(true, 0, null);
        GladosBridge bridge = new GladosBridge(opts, null);
        // 不应抛异常
        bridge.writePortFileForTest(9999);
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursively(c);
            }
        }
        f.delete();
    }
}