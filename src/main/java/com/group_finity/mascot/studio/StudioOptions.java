package com.group_finity.mascot.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 设计器启动参数与引擎目录布局解析。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §3.3、§5.7。
 * <p>
 * 引擎的图片路径规则（{@code AnimationBuilder.resolveImagePath}）是
 * 「相对当前工作目录」的：dev 模式读 {@code ./src/main/resources/img}，发行模式读 {@code ./img}。
 * 因此设计器进程必须与预览进程使用同一个工作目录，本类负责把这件事显式化并打印到 UI 顶部。
 */
public final class StudioOptions {

    private final Path workspace;
    private final Path engineRoot;
    private final Path confDir;
    private final Path imgDir;
    private final boolean devLayout;
    private final int port;
    private final boolean openBrowser;
    private final String engineClasspath;

    private StudioOptions(Path workspace, Path engineRoot, Path confDir, Path imgDir, boolean devLayout,
                          int port, boolean openBrowser, String engineClasspath) {
        this.workspace = workspace;
        this.engineRoot = engineRoot;
        this.confDir = confDir;
        this.imgDir = imgDir;
        this.devLayout = devLayout;
        this.port = port;
        this.openBrowser = openBrowser;
        this.engineClasspath = engineClasspath;
    }

    /**
     * 解析参数。未知参数忽略，保持与引擎一致的宽松策略。
     * <pre>
     * --workspace &lt;dir&gt;     工作区（默认当前目录），必须包含 conf 或 src/main/resources/conf
     * --engine-root &lt;dir&gt;   引擎根目录（默认等于 workspace）
     * --port &lt;n&gt;            监听端口，0 表示随机
     * --engine-jar &lt;path&gt;   预览进程的 classpath（默认沿用当前进程 classpath）
     * --no-browser          不自动打开浏览器
     * </pre>
     */
    public static StudioOptions parse(String[] args) {
        Path workspace = Paths.get("").toAbsolutePath().normalize();
        Path engineRoot = null;
        int port = 0;
        boolean openBrowser = true;
        String classpath = null;

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg == null) {
                    continue;
                }
                switch (arg) {
                    case "--workspace":
                        if (i + 1 < args.length) {
                            workspace = Paths.get(args[++i]).toAbsolutePath().normalize();
                        }
                        break;
                    case "--engine-root":
                        if (i + 1 < args.length) {
                            engineRoot = Paths.get(args[++i]).toAbsolutePath().normalize();
                        }
                        break;
                    case "--port":
                        if (i + 1 < args.length) {
                            try {
                                port = Integer.parseInt(args[++i]);
                            } catch (NumberFormatException e) {
                                port = 0;
                            }
                        }
                        break;
                    case "--engine-jar":
                        if (i + 1 < args.length) {
                            classpath = args[++i];
                        }
                        break;
                    case "--no-browser":
                        openBrowser = false;
                        break;
                    default:
                        break;
                }
            }
        }

        if (engineRoot == null) {
            engineRoot = workspace;
        }

        boolean dev = Files.isRegularFile(engineRoot.resolve("src/main/resources/conf/actions.xml"));
        Path conf = dev ? engineRoot.resolve("src/main/resources/conf") : engineRoot.resolve("conf");
        Path img = dev ? engineRoot.resolve("src/main/resources/img") : engineRoot.resolve("img");

        if (classpath == null || classpath.trim().isEmpty()) {
            classpath = System.getProperty("java.class.path");
        }
        return new StudioOptions(workspace, engineRoot, conf, img, dev, port, openBrowser, classpath);
    }

    /** 工作区是否可用于保存（目录布局不完整时 UI 必须禁用保存）。 */
    public String validateLayout() {
        if (!Files.isDirectory(engineRoot)) {
            return "引擎根目录不存在: " + engineRoot;
        }
        if (!Files.isRegularFile(confDir.resolve("actions.xml"))) {
            return "找不到 " + confDir.resolve("actions.xml") + "（请用 --workspace/--engine-root 指定引擎根目录）";
        }
        if (!Files.isRegularFile(confDir.resolve("behaviors.xml"))) {
            return "找不到 " + confDir.resolve("behaviors.xml");
        }
        if (!Files.isDirectory(imgDir)) {
            return "找不到形象目录: " + imgDir;
        }
        return null;
    }

    public Path getWorkspace() {
        return workspace;
    }

    public Path getEngineRoot() {
        return engineRoot;
    }

    public Path getConfDir() {
        return confDir;
    }

    public Path getImgDir() {
        return imgDir;
    }

    public boolean isDevLayout() {
        return devLayout;
    }

    public int getPort() {
        return port;
    }

    public boolean isOpenBrowser() {
        return openBrowser;
    }

    public String getEngineClasspath() {
        return engineClasspath;
    }
}
