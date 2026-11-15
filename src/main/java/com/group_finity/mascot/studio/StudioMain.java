package com.group_finity.mascot.studio;

import com.group_finity.mascot.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Shimeji Studio 独立入口（与引擎同 jar、不同 Main-Class）。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §3.1 形态 B、§3.3 进程模型。
 * <pre>
 * java -Ddev=true -cp shimeji.jar com.group_finity.mascot.studio.StudioMain \
 *      --workspace &lt;引擎根目录&gt; [--port 0] [--no-browser]
 * </pre>
 * 默认只监听 {@code 127.0.0.1}；端口写入 {@code <workspace>/.studio/studio.port}。
 */
public final class StudioMain {

    private static final Logger log = LoggerFactory.getLogger(StudioMain.class);

    private StudioMain() {
    }

    public static void main(String[] args) {
        StudioOptions options = StudioOptions.parse(args);

        // 引擎的图片路径规则依赖 -Ddev，且 Constant.isDevEnvironment 是静态常量。
        // 必须在任何引擎类初始化之前对齐，否则会「设计器写 src/main/resources/conf，
        // 预览读 conf/」这类路径错位（R-D1）。
        if (options.isDevLayout() && !Boolean.parseBoolean(System.getProperty("dev", "false"))) {
            System.setProperty("dev", "true");
            log.info("Studio: dev layout detected, forcing -Ddev=true to match engine path rules");
        }

        // 语言包与引擎基础路径初始化（不碰 Swing，不影响正常运行路径）
        try {
            Main.getInstance().initForHeadlessTooling();
        } catch (Throwable t) {
            log.warn("Studio: headless engine init failed: {}", t.toString());
        }

        int exit = 1;
        PreviewOrchestrator preview = null;
        StudioServer server = null;
        try {
            WorkspaceManager wm = new WorkspaceManager(options);
            ValidationService validation = new ValidationService(wm);
            DocumentService documents = new DocumentService(wm, validation);
            AssetService assets = new AssetService(wm);
            preview = new PreviewOrchestrator(wm, options);
            PackageService packages = new PackageService(wm, validation);

            String layoutError = options.validateLayout();
            if (layoutError != null) {
                log.warn("Studio layout check: {}", layoutError);
                System.err.println("[Shimeji Studio] 目录布局不完整：" + layoutError);
            }

            server = new StudioServer(options, wm, documents, assets, validation, preview, packages);
            int port = server.start();

            Path portFile = wm.getStudioDir().resolve("studio.port");
            Files.writeString(portFile, String.valueOf(port), StandardCharsets.UTF_8);

            String url = "http://127.0.0.1:" + port + "/";
            System.out.println();
            System.out.println("  Shimeji Studio 已启动");
            System.out.println("  ────────────────────────────────────────────");
            System.out.println("  地址      : " + url);
            System.out.println("  引擎根目录: " + options.getEngineRoot());
            System.out.println("  生效 conf : " + options.getConfDir());
            System.out.println("  生效 img  : " + options.getImgDir());
            System.out.println("  模式      : " + (options.isDevLayout() ? "dev (src/main/resources)" : "prod (发行目录)"));
            if (layoutError != null) {
                System.out.println("  注意      : " + layoutError);
            }
            System.out.println("  ────────────────────────────────────────────");
            System.out.println("  Ctrl+C 退出（会一并关闭预览进程）");
            System.out.println();

            if (options.isOpenBrowser()) {
                openBrowser(url);
            }

            final PreviewOrchestrator previewRef = preview;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (previewRef != null) {
                        previewRef.stop();
                    }
                } catch (Exception ignored) {
                    // best effort
                }
            }, "studio-shutdown"));

            exit = 0;
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[Shimeji Studio] 启动失败：" + Xml.shortMessage(e));
            log.error("Studio failed to start", e);
        } finally {
            if (server != null) {
                server.stop();
            }
            if (preview != null) {
                preview.stop();
            }
        }
        System.exit(exit);
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            log.debug("Desktop browse unavailable: {}", e.toString());
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (IOException e) {
            log.info("Please open {} manually", url);
        }
    }
}
