package com.group_finity.mascot.glados;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.group_finity.mascot.Manager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * /status 响应体结构测试。
 * <p>
 * 设计文档：docs/glados-ai-integration-design.md §4
 * <p>
 * 覆盖：mascots 数组字段存在（GLaDOS 靠它定位角色、画回复气泡）。
 * 空列表场景可无窗口环境单测；非空场景（真实角色坐标）依赖真机联调，
 * 因为 Mascot 构造需要 NativeFactory 创建透明窗口，headless 下不可用。
 */
public class GladosStatusBodyTest {

    @Test
    public void testStatusBodyHasMascotsArray() {
        Manager manager = new Manager();
        GladosBridge bridge = new GladosBridge(GladosOptions.parse(new String[]{"--glados-managed", "--glados-port", "0"}), manager);

        JSONObject body = bridge.buildStatusBody();

        assertNotNull("mascots 字段必须存在", body.getJSONArray("mascots"));
        assertEquals("无角色时 mascots 应为空数组", 0, body.getJSONArray("mascots").size());
    }

    @Test
    public void testStatusBodyHasCoreFields() {
        Manager manager = new Manager();
        GladosBridge bridge = new GladosBridge(GladosOptions.parse(new String[]{"--glados-managed", "--glados-port", "0"}), manager);

        JSONObject body = bridge.buildStatusBody();

        assertTrue(body.containsKey("role"));
        assertTrue(body.containsKey("count"));
        assertTrue(body.containsKey("paused"));
        assertTrue(body.containsKey("behavior"));
        assertTrue(body.containsKey("managed"));
        assertTrue(body.containsKey("version"));
    }
}