package com.group_finity.mascot.glados;

import com.alibaba.fastjson.JSONObject;
import com.group_finity.mascot.Manager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * FR-03 reload 命令入口的 headless 测试。
 * <p>
 * 需求文档：docs/action-authoring-extensibility-requirements.md AC-03-1 / AC-03-5。
 * 真正的「配置替换 + 重建角色」需要 GUI，无法在 headless 下验证（见 §8.4 真机验收）；
 * 这里覆盖的是：命令可达、失败时返回可读 error JSON、且不会误触发窗口逻辑。
 */
public class GladosReloadCommandTest {

    @Test
    public void reloadWithoutLoadedImageSetsReturnsReadableError() {
        Manager manager = new Manager();
        GladosBridge bridge = new GladosBridge(
                GladosOptions.parse(new String[]{"--glados-managed", "--glados-port", "0"}), manager);

        // 测试进程的 Main 单例没有跑过 run()，imageSets 为空，reload 必须安全返回错误
        String response = bridge.reload();
        JSONObject body = JSONObject.parseObject(response);

        assertNotNull(body);
        assertTrue("无已加载角色时应返回明确错误", body.containsKey("error"));
        assertFalse(body.getBooleanValue("ok"));
        assertTrue(body.getString("error").contains("no image set loaded"));
    }
}
