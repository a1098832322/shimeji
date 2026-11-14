package com.group_finity.mascot.glados;

/**
 * GLaDOS 启动参数解析。
 * <p>
 * 设计文档：docs/glados-ai-integration-design.md §4.1
 *
 * <pre>
 * java -jar shimeji.jar --glados-managed --glados-port 0 --glados-home &lt;dir&gt;
 * </pre>
 */
public class GladosOptions {

    private final boolean managed;
    private final int port;
    private final String home;

    /**
     * 当前进程是否处于 GLaDOS 托管模式。
     * <p>
     * 供深层调用栈（如 {@code Configuration.isBehaviorEnabled}）判断用：那里拿不到
     * {@code GladosOptions} 实例，需要静态可达。默认 false，任何未经过 {@link #parse}
     * 的路径（用户直接运行、单元测试）都保持原版行为。
     */
    private static volatile boolean managedProcess = false;

    public GladosOptions(boolean managed, int port, String home) {
        this.managed = managed;
        this.port = port;
        this.home = home;
    }

    /** 当前进程是否为 GLaDOS 托管。 */
    public static boolean isManagedProcess() {
        return managedProcess;
    }

    /** 测试用：显式设置托管状态（null 表示恢复默认 false）。 */
    static void setManagedProcessForTest(Boolean managed) {
        managedProcess = managed != null && managed;
    }

    /**
     * 解析命令行参数。未知参数忽略（保持原版兼容）。
     */
    public static GladosOptions parse(String[] args) {
        boolean managed = false;
        int port = 0;
        String home = null;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg == null) {
                    continue;
                }
                switch (arg) {
                    case "--glados-managed":
                        managed = true;
                        break;
                    case "--glados-port":
                        if (i + 1 < args.length) {
                            try {
                                port = Integer.parseInt(args[++i]);
                            } catch (NumberFormatException e) {
                                port = 0;
                            }
                        }
                        break;
                    case "--glados-home":
                        if (i + 1 < args.length) {
                            home = args[++i];
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return new GladosOptions(managed, port, home);
    }

    /**
     * 解析并把托管状态同步到静态标记。
     * <p>
     * {@link Main#main} 应使用本方法而非 {@link #parse}，否则深层调用栈
     * （{@code Configuration.isBehaviorEnabled}）读不到托管状态。
     */
    public static GladosOptions parseAndMark(String[] args) {
        GladosOptions options = parse(args);
        managedProcess = options.isManaged();
        return options;
    }

    public boolean isManaged() {
        return managed;
    }

    public int getPort() {
        return port;
    }

    public String getHome() {
        return home;
    }
}