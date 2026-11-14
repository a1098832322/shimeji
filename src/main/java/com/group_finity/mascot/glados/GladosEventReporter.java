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

    private static volatile String homeDir;
    private static long sequence;

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
        if (!isEnabled() || type == null) {
            return;
        }
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
            Files.write(tmp, type.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Failed to report GLaDOS event {}: {}", type, e.getMessage());
        }
    }
}