package com.group_finity.mascot.glados;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 托管模式下把桌宠的用户交互上报给 GLaDOS。
 * <p>
 * 设计文档：docs/glados-managed-constraints-design.md
 * <p>
 * 为什么用文件而不是 HTTP 回调：
 * <ul>
 *   <li>GLaDOS 桌面主进程不监听 HTTP 端口，没有可回调的地址；</li>
 *   <li>桥本身是 HTTP <b>服务端</b>，方向相反；</li>
 *   <li>文件写入是原子 rename，GLaDOS 侧用已有的 2 秒轮询顺带读取，
 *       不新增网络依赖、不需要端口协商、进程崩溃也不会留下半截数据。</li>
 * </ul>
 * 文件位置：{@code <--glados-home>/glados-events/<序号>-<type>}。
 * 用递增序号的独立文件而不是单文件追加，避免读写竞争与"读到半行"。
 */
public final class GladosEventReporter {

    private static final Logger log = LoggerFactory.getLogger(GladosEventReporter.class);

    /** 事件目录名，相对 --glados-home。 */
    public static final String EVENT_DIR = "glados-events";

    /** 左键点击桌宠：GLaDOS 侧应打开快捷对话。 */
    public static final String EVENT_CLICK = "click";

    /** 角色移动：GLaDOS 侧应更新气泡位置。payload 为坐标 JSON。 */
    public static final String EVENT_MOVE = "move";

    /** move 事件最小上报间隔（毫秒）。走路时每帧位置都在变，全量上报会写爆文件。 */
    private static final long MOVE_MIN_INTERVAL_MS = 200;

    /** move 事件坐标变化阈值（像素）。亚像素抖动不上报。 */
    private static final int MOVE_MIN_DELTA = 2;

    private static volatile String homeDir;
    private static volatile String lastClickedId;
    private static final java.util.Map<String, long[]> moves = new java.util.HashMap<>();
    private static long sequence;
    private static volatile long lastMoveAt;
    private static volatile int lastMoveX;
    private static volatile int lastMoveY;
    private static volatile int lastMoveW;
    private static volatile int lastMoveH;

    private GladosEventReporter() {
    }

    /** 由 Main 在受管模式启动时调用。 */
    public static void configure(String gladosHome) {
        homeDir = gladosHome;
    }

    /** 测试用：清除已配置的目录，模拟"未收到 --glados-home"。 */
    static void resetForTest() {
        homeDir = null;
    }

    public static boolean isEnabled() {
        return GladosOptions.isManagedProcess() && homeDir != null && !homeDir.trim().isEmpty();
    }

    /**
     * 上报一次用户交互。失败只记日志，不影响桌宠主流程。
     *
     * @param type 事件类型，见本类常量
     */
    public static void report(String type) {
        report(type, null);
    }

    /**
     * 上报一次带 payload 的事件。
     *
     * @param type    事件类型
     * @param payload 可选 JSON 文本；为 null 时文件内容只写类型名
     */
    public static void report(String type, String payload) {
        String content = (payload == null || payload.isEmpty()) ? type : type + "\n" + payload;
        reportContent(type, content);
    }

    private static void reportContent(String type, String content) {
        if (!isEnabled() || type == null) return;
        try {
            Path dir = Paths.get(homeDir, EVENT_DIR);
            Files.createDirectories(dir);
            // 文件名带序号，保证同一毫秒内的多次点击也不互相覆盖
            String name;
            synchronized (GladosEventReporter.class) {
                sequence += 1;
                name = String.format("%013d-%s", System.currentTimeMillis() * 1000 + (sequence % 1000), type);
            }
            Path tmp = dir.resolve(name + ".tmp");
            Path target = dir.resolve(name);
            Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Failed to report GLaDOS event {}: {}", type, e.getMessage());
        }
    }

    /**
     * 上报角色移动（带节流）。
     * <p>
     * 走路时每帧位置都在变，全量上报会写爆事件目录。这里做两级过滤：
     * <ul>
     *   <li>时间：距上次上报不足 {@value #MOVE_MIN_INTERVAL_MS}ms 丢弃；</li>
     *   <li>位移：坐标变化小于 {@value #MOVE_MIN_DELTA}px 丢弃（亚像素抖动）。</li>
     * </ul>
     *
     * @param x      角色窗口左上角 x
     * @param y      角色窗口左上角 y
     * @param width  角色窗口宽
     * @param height 角色窗口高
     */
    public static void reportMascotMove(int x, int y, int width, int height) {
        if (!isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (GladosEventReporter.class) {
            if (now - lastMoveAt < MOVE_MIN_INTERVAL_MS) {
                return;
            }
            if (Math.abs(x - lastMoveX) < MOVE_MIN_DELTA && Math.abs(y - lastMoveY) < MOVE_MIN_DELTA) {
                return;
            }
            lastMoveAt = now;
            lastMoveX = x;
            lastMoveY = y;
            lastMoveW = width;
            lastMoveH = height;
        }
        String payload = "{\"x\":" + x + ",\"y\":" + y + ",\"width\":" + width + ",\"height\":" + height + "}";
        report(EVENT_MOVE, payload);
    }

    public static void reportClick(com.group_finity.mascot.Mascot mascot) {
        lastClickedId = mascot.getInstanceId();
        reportContent(EVENT_CLICK, "{\"type\":\"click\",\"id\":\"" + lastClickedId + "\",\"ts\":" + System.currentTimeMillis() + "}");
    }

    public static void reportMascotMove(com.group_finity.mascot.Mascot mascot, int x, int y, int width, int height) {
        if (!isEnabled() || mascot.getManager() == null) return;
        com.group_finity.mascot.Manager manager = mascot.getManager();
        com.group_finity.mascot.Mascot primary = manager.getPrimaryMascot();
        String id = mascot.getInstanceId();
        if (manager.getMascotsForBridge().size() > 3 && mascot != primary && !id.equals(lastClickedId)) return;
        long now = System.currentTimeMillis();
        synchronized (GladosEventReporter.class) {
            long[] previous = moves.get(id);
            if (previous != null && (now - previous[0] < MOVE_MIN_INTERVAL_MS ||
                (Math.abs(x - previous[1]) < MOVE_MIN_DELTA && Math.abs(y - previous[2]) < MOVE_MIN_DELTA && width == previous[3] && height == previous[4]))) return;
            moves.put(id, new long[]{now, x, y, width, height});
        }
        reportContent(EVENT_MOVE, "{\"type\":\"move\",\"id\":\"" + id + "\",\"x\":" + x + ",\"y\":" + y + ",\"width\":" + width + ",\"height\":" + height + "}");
    }

    /** 测试用：重置节流状态（静态量会在用例间串味）。 */
    static void resetMoveThrottleForTest() {
        synchronized (GladosEventReporter.class) {
            lastMoveAt = 0;
            lastMoveX = Integer.MIN_VALUE;
            lastMoveY = Integer.MIN_VALUE;
            lastMoveW = 0;
            lastMoveH = 0;
        }
    }
}