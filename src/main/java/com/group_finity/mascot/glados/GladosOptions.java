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

    public GladosOptions(boolean managed, int port, String home) {
        this.managed = managed;
        this.port = port;
        this.home = home;
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