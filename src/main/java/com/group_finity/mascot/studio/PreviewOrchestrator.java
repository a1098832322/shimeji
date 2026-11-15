package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.group_finity.mascot.glados.GladosBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预览编排：spawn → 端口发现 → reload → setRole → setBehavior → /status（DR-10）。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §3.3、§5.5、§6.4。
 * <p>
 * 预览进程是<b>黑盒</b>：只走引擎已有的 {@code /control} 与 {@code /status}，
 * 不读它的内存、不注入代码、不传任意文件路径（AC-13-3）。
 * <p>
 * 失败语义直接继承引擎：{@code reload} 失败时旧配置与角色仍然可用，
 * 因此 UI 可以安全地显示「上一次可用配置仍在运行」并提供还原按钮。
 */
public final class PreviewOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PreviewOrchestrator.class);

    public static final String STATE_STOPPED = "stopped";
    public static final String STATE_STARTING = "starting";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_OFFLINE = "offline";

    /** 托管预览的最小行为表：只保留 AIIdle，尽量不向设计态注入 AI 行为（§2.4）。 */
    private static final String MINIMAL_BEHAVIORS = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<!-- generated-by: shimeji-studio; 托管预览专用最小行为表 -->\n"
            + "<Mascot>\n"
            + "  <BehaviorList>\n"
            + "    <Behavior Name=\"AIIdle\" Frequency=\"0\" Hidden=\"true\" Action=\"Sit\" />\n"
            + "  </BehaviorList>\n"
            + "</Mascot>\n";

    private final WorkspaceManager wm;
    private final StudioOptions options;
    private final HttpClient http;

    private Process process;
    private int port = -1;
    private String state = STATE_STOPPED;
    private JSONObject lastStatus = new JSONObject();
    private String lastError;

    public PreviewOrchestrator(WorkspaceManager wm, StudioOptions options) {
        this.wm = wm;
        this.options = options;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    public synchronized JSONObject start() {
        if (STATE_RUNNING.equals(state) && process != null && process.isAlive()) {
            return snapshot();
        }
        if (process != null && process.isAlive()) {
            stopQuietly();
        }
        Path home = wm.getStudioDir().resolve("home");
        Path logFile = wm.getStudioDir().resolve("logs").resolve("preview.log");
        try {
            Files.createDirectories(home);
            Files.createDirectories(logFile.getParent());
            Path behaviors = home.resolve("glados-behaviors.xml");
            if (!Files.isRegularFile(behaviors)) {
                Files.writeString(behaviors, MINIMAL_BEHAVIORS, StandardCharsets.UTF_8);
            }
            Path portFile = home.resolve(GladosBridge.PORT_FILE_NAME);
            Files.deleteIfExists(portFile);

            List<String> command = new ArrayList<>();
            command.add(javaBinary());
            if (options.isDevLayout()) {
                command.add("-Ddev=true");
            }
            command.add("-cp");
            command.add(options.getEngineClasspath());
            command.add("com.group_finity.mascot.Main");
            command.add("--glados-managed");
            command.add("--glados-port");
            command.add("0");
            command.add("--glados-home");
            command.add(home.toAbsolutePath().toString());

            state = STATE_STARTING;
            lastError = null;
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(options.getEngineRoot().toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            log.info("Starting preview process: {}", String.join(" ", command));
            process = pb.start();

            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                if (!process.isAlive()) {
                    lastError = "预览进程已退出（exit=" + process.exitValue() + "），详见 " + logFile.getFileName();
                    state = STATE_STOPPED;
                    return snapshot();
                }
                if (Files.isRegularFile(portFile)) {
                    String text = Files.readString(portFile, StandardCharsets.UTF_8).trim();
                    if (!text.isEmpty()) {
                        port = Integer.parseInt(text);
                        JSONObject status = fetchStatus();
                        if (status != null) {
                            lastStatus = status;
                            state = STATE_RUNNING;
                            log.info("Preview bridge discovered on 127.0.0.1:{}", port);
                            return snapshot();
                        }
                    }
                }
                Thread.sleep(200);
            }
            lastError = "等待预览端口超时（20s）：请查看 " + logFile;
            state = STATE_OFFLINE;
            return snapshot();
        } catch (Exception e) {
            lastError = "启动预览失败：" + Xml.shortMessage(e);
            state = STATE_STOPPED;
            log.error("Failed to start preview", e);
            return snapshot();
        }
    }

    public synchronized JSONObject stop() {
        if (process != null && process.isAlive()) {
            try {
                post("/control", Map.of("cmd", "quit"));
                long deadline = System.currentTimeMillis() + 4000;
                while (System.currentTimeMillis() < deadline && process.isAlive()) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                log.warn("Preview quit command failed: {}", e.toString());
            }
            if (process.isAlive()) {
                log.warn("Preview process did not exit in time, destroying forcibly");
                process.destroyForcibly();
            }
        }
        process = null;
        port = -1;
        state = STATE_STOPPED;
        lastStatus = new JSONObject();
        try {
            Files.deleteIfExists(wm.getStudioDir().resolve("home").resolve(GladosBridge.PORT_FILE_NAME));
        } catch (IOException ignored) {
            // best effort
        }
        return snapshot();
    }

    private void stopQuietly() {
        try {
            stop();
        } catch (Exception ignored) {
            // best effort
        }
    }

    // ------------------------------------------------------------------
    // 运行编排（保存与校验由调用方先完成）
    // ------------------------------------------------------------------

    /**
     * 完整回路：确保进程 → reload → setRole（如需要）→ setBehavior → /status。
     * 每步结果都返回给 UI，失败即停在当步（§5.5 六步编排）。
     */
    public synchronized JSONObject run(String behavior, String skin) {
        JSONArray steps = new JSONArray();
        JSONObject snapshot = start();
        steps.add(step("launch", STATE_RUNNING.equals(state), snapshot.getString("error"), null));
        if (!STATE_RUNNING.equals(state)) {
            return runResult(steps, false, null);
        }

        JSONObject reload = post("/control", Map.of("cmd", "reload"));
        boolean reloadOk = reload != null && reload.getBooleanValue("ok");
        steps.add(step("reload", reloadOk, reload == null ? "无响应" : reload.getString("error"),
                reloadOk ? null : "上一次可用配置仍在运行（引擎已回滚），角色不会消失"));
        if (!reloadOk) {
            return runResult(steps, false, lastStatus);
        }

        if (skin != null && !skin.isBlank()) {
            String current = lastStatus.getString("role");
            if (!skin.equals(current)) {
                JSONObject setRole = post("/control", Map.of("cmd", "setRole", "role", skin));
                boolean ok = setRole != null && setRole.getBooleanValue("ok");
                steps.add(step("setRole", ok, setRole == null ? "无响应" : setRole.getString("error"),
                        ok ? "角色已按新皮肤重建" : null));
                if (!ok) {
                    return runResult(steps, false, lastStatus);
                }
                // 旧角色要被下一次调度 tick 回收，期间 /status 可能仍报旧皮肤；
                // 等到新皮肤真正接管再做后续步骤，避免 UI 显示陈旧角色名。
                awaitRole(skin, 3000L);
            } else {
                steps.add(step("setRole", true, null, "当前已是该皮肤，跳过重建"));
            }
        }

        if (behavior != null && !behavior.isBlank()) {
            // reload/setRole 都会重建角色，新角色从屏幕上方落下；需接地类动作（Stand/Sit 等）
            // 在离地状态下会立刻抛 LostGround 并退回 Fall，导致「设置成功了但看不到」。
            boolean landed = awaitLanded(12000L);
            // setRole 之后引擎刚 add() 的新角色要等下一次 tick 才进调度队列，
            // 旧引擎 jar 上 setBehavior 会在这一瞬误报 unknown behavior；此处做短重试兜底。
            JSONObject setBehavior = null;
            for (int attempt = 0; attempt < 4; attempt++) {
                setBehavior = post("/control", Map.of("cmd", "setBehavior", "behavior", behavior));
                if (setBehavior != null && setBehavior.getBooleanValue("ok")) {
                    break;
                }
                if (setBehavior == null || !String.valueOf(setBehavior.getString("error")).contains("unknown behavior")) {
                    break;
                }
                sleepQuietly(250L);
            }
            boolean ok = setBehavior != null && setBehavior.getBooleanValue("ok");
            steps.add(step("setBehavior", ok, setBehavior == null ? "无响应" : setBehavior.getString("error"),
                    ok ? (landed ? null : "角色仍在空中，接地类动作可能立即退回 Fall")
                            : "常见原因：行为未注册（片段没放进 actions.d/behaviors.d），或 Frequency=0 但名字拼错"));
            if (!ok) {
                return runResult(steps, false, lastStatus);
            }
        }

        JSONObject status = fetchStatus();
        String behaviorNow = null;
        if (status != null) {
            lastStatus = status;
            behaviorNow = status.getString("behavior");
        }
        String note = null;
        if (status != null && behavior != null && !behavior.isBlank() && !behavior.equals(behaviorNow)) {
            note = "切到 " + behavior + " 后已被自主行为树接管（当前 " + behaviorNow
                    + "）：单次行为播完会沿 NextBehaviorList 继续，属正常调度";
        }
        steps.add(step("status", status != null, status == null ? "无法读取 /status" : null, note));
        return runResult(steps, status != null, status);
    }

    /** 轮询 /status 直到角色名切换到目标皮肤（旧角色回收存在一两个 tick 的延迟）。 */
    private void awaitRole(String skin, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            JSONObject s = fetchStatus();
            if (s != null) {
                lastStatus = s;
                if (skin.equals(s.getString("role"))) {
                    return;
                }
            }
            sleepQuietly(120L);
        }
    }

    /**
     * 等角色接地：重建后的角色从屏幕上方落下，接地类动作（Stand/Sit…）离地时会立刻
     * 抛 LostGround 退回 Fall，所以「先落地再 setBehavior」才是可复现的验收姿势。
     * 判定方式：不再处于 Fall，或相邻两次采样的 y 不再变化（已落到底部）。
     */
    private boolean awaitLanded(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        Integer lastY = null;
        while (System.currentTimeMillis() < deadline) {
            JSONObject s = fetchStatus();
            if (s != null) {
                lastStatus = s;
                String behavior = s.getString("behavior");
                int y = Integer.MIN_VALUE;
                int width = 0;
                JSONArray mascots = s.getJSONArray("mascots");
                if (mascots != null && !mascots.isEmpty()) {
                    JSONObject first = mascots.getJSONObject(0);
                    y = first.getIntValue("y");
                    width = first.getIntValue("width");
                }
                boolean notFalling = behavior != null && !"Fall".equals(behavior);
                boolean stable = width > 0 && lastY != null && lastY == y;
                if (notFalling || stable) {
                    sleepQuietly(120L);
                    return true;
                }
                lastY = width > 0 ? y : null;
            }
            sleepQuietly(180L);
        }
        return false;
    }

    private JSONObject runResult(JSONArray steps, boolean ok, JSONObject status) {
        JSONObject out = new JSONObject(true);
        out.put("ok", ok);
        out.put("steps", steps);
        out.put("preview", snapshot());
        out.put("status", status == null ? lastStatus : status);
        return out;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static JSONObject step(String name, boolean ok, String error, String note) {
        JSONObject o = new JSONObject(true);
        o.put("step", name);
        o.put("ok", ok);
        if (error != null) {
            o.put("error", error);
        }
        if (note != null) {
            o.put("note", note);
        }
        return o;
    }

    // ------------------------------------------------------------------
    // 状态与命令
    // ------------------------------------------------------------------

    public synchronized JSONObject snapshot() {
        JSONObject out = new JSONObject(true);
        out.put("state", state);
        out.put("port", port > 0 ? port : null);
        out.put("pid", process == null ? null : process.pid());
        out.put("error", lastError);
        out.put("status", lastStatus);
        out.put("home", wm.relativeToEngine(wm.getStudioDir().resolve("home")).toString());
        return out;
    }

    public synchronized JSONObject status() {
        if (process != null && !process.isAlive() && STATE_RUNNING.equals(state)) {
            state = STATE_OFFLINE;
            lastError = "预览进程已退出";
        }
        JSONObject status = fetchStatus();
        if (status != null) {
            lastStatus = status;
            state = STATE_RUNNING;
        } else if (STATE_RUNNING.equals(state)) {
            state = STATE_OFFLINE;
            lastError = "预览进程端口失联（进入离线编辑模式）";
        }
        JSONObject out = snapshot();
        out.put("status", lastStatus);
        return out;
    }

    public synchronized JSONObject command(String cmd, Map<String, String> extra) {
        if (port <= 0) {
            throw new StudioException(409, "预览进程未启动");
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("cmd", cmd);
        if (extra != null) {
            body.putAll(extra);
        }
        JSONObject response = post("/control", body);
        if (response == null) {
            throw new StudioException(502, "预览进程无响应");
        }
        return response;
    }

    private JSONObject fetchStatus() {
        if (port <= 0) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/status"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            return JSON.parseObject(response.body());
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject post(String path, Map<String, String> body) {
        if (port <= 0) {
            return null;
        }
        try {
            String json = JSON.toJSONString(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return JSON.parseObject(response.body());
        } catch (Exception e) {
            log.warn("Bridge {} failed: {}", path, e.toString());
            return null;
        }
    }

    private static String javaBinary() {
        Path bin = Path.of(System.getProperty("java.home"), "bin", "java");
        return Files.isExecutable(bin) ? bin.toString() : "java";
    }
}
