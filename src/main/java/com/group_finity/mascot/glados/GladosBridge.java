package com.group_finity.mascot.glados;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.group_finity.mascot.Main;
import com.group_finity.mascot.Manager;
import com.group_finity.mascot.Mascot;
import java.awt.Rectangle;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.wishes.fix.OriginEngineFix;
import com.wishes.utils.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GLaDOS 外部控制桥（HTTP 127.0.0.1）。
 * <p>
 * 设计文档：docs/glados-ai-integration-design.md §4
 * <p>
 * 职责：
 * <ul>
 *   <li>监听 127.0.0.1 随机端口（{@code --glados-port 0}）；</li>
 *   <li>{@code GET /status} 查询状态；</li>
 *   <li>{@code POST /control} 执行白名单命令（setRole/setBehavior/pause/resume/
 *       createMascot/remainOne/disposeAll/reload/quit）；</li>
 *   <li>把实际监听端口写入 {@code --glados-home/glados-bridge.port}。</li>
 * </ul>
 * 桥使用独立线程池，不触碰 Swing EDT。
 */
public final class GladosBridge {

    private static final Logger log = LoggerFactory.getLogger(GladosBridge.class);

    /** 端口文件相对 --glados-home 的文件名。 */
    public static final String PORT_FILE_NAME = "glados-bridge.port";

    /** 内置默认 AI 行为名（与 glados-behaviors.xml 一致）。 */
    public static final String BEHAVIOR_IDLE = "AIIdle";

    private final GladosOptions options;
    private final Manager manager;
    private HttpServer server;
    private ExecutorService executor;

    public GladosBridge(GladosOptions options, Manager manager) {
        this.options = options;
        this.manager = manager;
    }

    /**
     * 启动桥。绑定失败时记录错误但不中断主程序。
     */
    public void start() {
        try {
            InetSocketAddress addr = new InetSocketAddress("127.0.0.1", options.getPort());
            server = HttpServer.create(addr, 0);
            server.createContext("/status", this::handleStatus);
            server.createContext("/control", this::handleControl);
            executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "glados-bridge");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);
            server.start();

            int actualPort = server.getAddress().getPort();
            log.info("GLaDOS bridge listening on 127.0.0.1:{}", actualPort);
            writePortFile(actualPort);
        } catch (IOException e) {
            log.error("Failed to start GLaDOS bridge: {}", e.toString(), e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     * 测试辅助：暴露端口文件写入逻辑（不启动 HTTP 服务）。
     */
    void writePortFileForTest(int port) {
        writePortFile(port);
    }

    public int getPort() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    private void writePortFile(int port) {
        String home = options.getHome();
        if (home == null || home.trim().isEmpty()) {
            log.warn("--glados-home not set, skip writing port file");
            return;
        }
        try {
            File dir = new File(home);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                log.warn("Cannot create glados home dir: {}", home);
                return;
            }
            File portFile = new File(dir, PORT_FILE_NAME);
            Files.write(portFile.toPath(), String.valueOf(port).getBytes(StandardCharsets.UTF_8));
            log.info("GLaDOS bridge port file written: {} -> {}", portFile.getAbsolutePath(), port);
        } catch (IOException e) {
            log.warn("Failed to write GLaDOS bridge port file: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------
    // HTTP handlers
    // ------------------------------------------------------------------

    private void handleStatus(HttpExchange exchange) throws IOException {
        respond(exchange, 200, buildStatusBody().toJSONString());
    }

    /**
     * 组装 /status 响应体。
     * <p>
     * 拆出来便于单测：不依赖 HTTP 层。mascots 数组给 GLaDOS 上报
     * 每个角色的屏幕坐标，用于在角色头上画回复气泡。
     */
    JSONObject buildStatusBody() {
        JSONObject body = new JSONObject();
        Mascot primary = manager.getPrimaryMascot();
        body.put("role", primary == null ? null : shortRole(primary.getImageSet()));
        body.put("count", manager.getMascotsForBridge().size());
        body.put("instances", manager.getMascotsForBridge().size());
        body.put("maxInstances", maxInstances());
        body.put("primaryId", primary == null ? null : primary.getInstanceId());
        body.put("paused", manager.isPaused());
        body.put("behavior", currentBehavior());
        body.put("managed", true);
        body.put("mischief", com.group_finity.mascot.mischief.MischiefMode.status());
        body.put("version", com.wishes.constant.Constant.CURRENT_VERSION);
        JSONArray mascots = new JSONArray();
        for (Mascot mascot : manager.getMascotsForBridge()) {
            JSONObject entry = new JSONObject();
            entry.put("id", mascot.getInstanceId());
            entry.put("role", shortRole(mascot.getImageSet()));
            entry.put("origin", mascot == primary ? "primary" : ("primary".equals(mascot.getOrigin()) ? "clone" : mascot.getOrigin()));
            entry.put("behavior", behaviorName(mascot));
            entry.put("paused", mascot.isPaused());
            Rectangle bounds = mascot.getBounds();
            entry.put("x", bounds.x);
            entry.put("y", bounds.y);
            entry.put("width", bounds.width);
            entry.put("height", bounds.height);
            mascots.add(entry);
        }
        body.put("mascots", mascots);
        return body;
    }

    private void handleControl(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, errorJson("method not allowed"));
            return;
        }
        String raw = readBody(exchange);
        JSONObject json;
        try {
            json = JSON.parseObject(raw);
        } catch (Exception e) {
            respond(exchange, 400, errorJson("invalid json body"));
            return;
        }
        if (json == null) {
            respond(exchange, 400, errorJson("empty json body"));
            return;
        }

        String cmdName = json.getString("cmd");
        GladosCommand cmd = GladosCommand.fromWireName(cmdName);
        if (cmd == null) {
            respond(exchange, 400, errorJson("unknown cmd: " + cmdName));
            return;
        }

        Map<String, String> params = new HashMap<>();
        for (String key : json.keySet()) {
            Object v = json.get(key);
            params.put(key, v == null ? null : String.valueOf(v));
        }

        String validationError = cmd.validate(params);
        if (validationError != null) {
            respond(exchange, 400, errorJson("invalid params: " + validationError));
            return;
        }

        try {
            String result = execute(cmd, params);
            respond(exchange, 200, result);
        } catch (IllegalArgumentException e) {
            respond(exchange, 400, errorJson(e.getMessage()));
        } catch (Exception e) {
            log.error("GLaDOS control command {} failed", cmdName, e);
            respond(exchange, 500, errorJson("internal error: " + e.getMessage()));
        }
    }

    /**
     * 执行命令，返回响应 JSON 字符串。
     */
    private synchronized String execute(GladosCommand cmd, Map<String, String> params) {
        switch (cmd) {
            case SET_ROLE:
                return setRole(params.get("role"));
            case SET_BEHAVIOR:
                return setBehavior(params.get("behavior"), params.get("role"), params.get("target"));
            case SET_MISCHIEF:
                String enabled = params.get("enabled");
                String cursor = params.getOrDefault("pushCursor", "false");
                if (!("true".equals(enabled) || "false".equals(enabled)) || !("true".equals(cursor) || "false".equals(cursor)))
                    throw new IllegalArgumentException("enabled and pushCursor must be boolean");
                com.group_finity.mascot.mischief.MischiefMode.set(Main.getInstance().getProperties(), Boolean.parseBoolean(enabled), Boolean.parseBoolean(cursor));
                return okJson();
            case MISCHIEF_RESTORE:
                com.group_finity.mascot.mischief.MischiefMode.set(Main.getInstance().getProperties(), false, false);
                return okJson();
            case CLONE:
                return cloneMascots(params);
            case DISPOSE:
                for (Mascot m : select(params.get("target"), false)) m.dispose();
                return okJson();
            case SET_PRIMARY:
                if (!manager.setPrimary(params.get("target"))) throw new IllegalArgumentException("unknown target: " + params.get("target"));
                return okJson();
            case PAUSE:
                if (!manager.isPaused()) {
                    manager.togglePauseAll();
                }
                return okJson();
            case RESUME:
                if (manager.isPaused()) {
                    manager.togglePauseAll();
                }
                return okJson();
            case CREATE_MASCOT:
                return createMascot(params.get("role"));
            case REMAIN_ONE:
                return remainOne(params.get("role"));
            case DISPOSE_ALL:
                manager.disposeAll();
                return okJson();
            case RELOAD:
                return reload();
            case QUIT:
                // 异步优雅退出：先响应，再 disposeAll + exit
                new Thread(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {
                        // ignore
                    }
                    Main.getInstance().exit();
                }, "glados-quit").start();
                return okJson();
            default:
                return errorJson("unknown cmd");
        }
    }

    // ------------------------------------------------------------------
    // Command implementations
    // ------------------------------------------------------------------

    private String setRole(String role) {
        String full = resolveRole(role);
        if (full == null) {
            return errorJson("unknown role: " + role);
        }
        ensureConfiguration(full);
        manager.disposeAll();
        Main.getInstance().createMascot(full);
        return okJson();
    }

    private String setBehavior(String behavior, String role, String target) {
        String full = role == null ? null : resolveRole(role);
        if (role != null && full == null) return errorJson("unknown role: " + role);
        int applied = 0;
        java.util.List<Mascot> selected = select(target, true);
        for (Mascot mascot : selected) {
            if (full != null && !full.equals(mascot.getImageSet())) continue;
            Configuration config = Main.getInstance().getConfiguration(mascot.getImageSet());
            if (config == null) continue;
            try {
                mascot.setBehavior(BEHAVIOR_IDLE.equals(behavior)
                    ? config.buildNextBehavior(null, mascot) : config.buildBehavior(behavior, mascot));
                applied++;
            } catch (Exception e) { log.warn("Failed to set behavior {}: {}", behavior, e.getMessage()); }
        }
        return applied > 0 || ("followers".equals(target) && selected.isEmpty()) ? okJson() : errorJson("unknown behavior: " + behavior);
    }

    private int maxInstances() {
        try { return Math.max(1, Integer.parseInt(Main.getInstance().getProperties().getProperty("Glados.MaxInstances", "8"))); }
        catch (Exception e) { return 8; }
    }

    private java.util.List<Mascot> select(String target, boolean allowGroups) {
        java.util.List<Mascot> live = manager.getMascotsForBridge();
        Mascot primary = manager.getPrimaryMascot();
        if (target == null || "all".equals(target)) {
            if (!allowGroups) throw new IllegalArgumentException("unknown target: " + target);
            return live;
        }
        java.util.List<Mascot> result = new java.util.ArrayList<>();
        if ("followers".equals(target)) {
            for (Mascot m : live) if (m != primary) result.add(m);
            return result;
        }
        if ("primary".equals(target)) { if (primary != null) result.add(primary); }
        else if ("nearest".equals(target) && allowGroups) {
            java.awt.PointerInfo pointer = java.awt.MouseInfo.getPointerInfo();
            if (pointer != null) live.stream().min(java.util.Comparator.comparingDouble(m -> {
                Rectangle b = m.getBounds(); return pointer.getLocation().distanceSq(b.getCenterX(), b.getCenterY());
            })).ifPresent(result::add);
        } else for (Mascot m : live) if (m.getInstanceId().equals(target)) result.add(m);
        if (result.isEmpty()) throw new IllegalArgumentException("unknown target: " + target);
        return result;
    }

    private String cloneMascots(Map<String, String> params) {
        int count;
        try { count = Integer.parseInt(params.getOrDefault("count", "1")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("invalid count"); }
        if (count < 1) throw new IllegalArgumentException("invalid count");
        if (count > maxInstances() - manager.getMascotsForBridge().size()) return errorJson("instance limit reached (" + maxInstances() + ")");
        String anchorId = params.get("anchorId");
        if (anchorId != null && !anchorId.matches("mascot-[0-9]+")) throw new IllegalArgumentException("unknown anchorId: " + anchorId);
        Mascot anchor = select(anchorId == null ? "primary" : anchorId, false).get(0);
        String role = params.get("role");
        String full = role == null ? anchor.getImageSet() : resolveRole(role);
        if (full == null) return errorJson("unknown role: " + role);
        ensureConfiguration(full);
        JSONArray ids = new JSONArray();
        java.util.List<Mascot> made = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            java.awt.Point point = new java.awt.Point(anchor.getAnchor());
            point.translate(30 * (i + 1), 0);
            Mascot m = Main.getInstance().createMascotInstance(full, point, "clone");
            if (m == null) { for (Mascot created : made) created.dispose(); return errorJson("failed to create clone"); }
            made.add(m); ids.add(m.getInstanceId());
        }
        JSONObject response = new JSONObject(); response.put("ok", true); response.put("created", ids);
        return response.toJSONString();
    }

    private String createMascot(String role) {
        String full = resolveRole(role);
        if (full == null) {
            return errorJson("unknown role: " + role);
        }
        ensureConfiguration(full);
        Main.getInstance().createMascot(full);
        return okJson();
    }

    private String remainOne(String role) {
        String full = resolveRole(role);
        if (full == null) {
            return errorJson("unknown role: " + role);
        }
        manager.remainOneOnly(full);
        return okJson();
    }

    /**
     * FR-03：显式重载动作/行为配置。
     * 失败时 {@code Main.reloadConfigurations()} 已回滚图片缓存并保留旧配置，
     * 这里把原因以 error JSON 返回给调用方。
     */
    // 包私有：便于 headless 单测直接验证命令入口，不启动 HTTP 服务
    String reload() {
        String error = Main.getInstance().reloadConfigurations();
        return error == null ? okJson() : errorJson(error);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * 确保目标角色配置已加载；未加载则先加载（含 AI 行为合并）。
     */
    private void ensureConfiguration(String full) {
        if (Main.getInstance().getConfiguration(full) != null) {
            return;
        }
        if (Main.getInstance().loadConfiguration(full)) {
            Configuration config = Main.getInstance().getConfiguration(full);
            if (config != null) {
                GladosBehaviors.loadInto(config, options.getHome());
            }
        }
    }

    /**
     * 把角色短名解析为完整 imageSet 路径（与 Main.imageSets 一致）。
     * 支持传完整路径原样返回；找不到返回 null。
     */
    private String resolveRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return null;
        }
        String trimmed = role.trim();
        // 已含路径分隔符则视为完整路径
        if (trimmed.contains("/") || trimmed.contains("\\")) {
            return FormatUtils.formatImagePath(trimmed);
        }
        String base = OriginEngineFix.getInstance().getBASE_IMG_PATH();
        String full = FormatUtils.formatImagePath(base + trimmed);
        File f = new File(full);
        return f.isDirectory() ? full : null;
    }

    /**
     * 完整 imageSet 路径 → 短名（用于 /status 的 role 字段）。
     */
    private String shortRole(String full) {
        if (full == null) {
            return null;
        }
        String s = FormatUtils.formatImagePath(full);
        int idx = s.lastIndexOf('/');
        return idx >= 0 ? s.substring(idx + 1) : s;
    }

    /**
     * 当前行为名：取第一个角色的 behavior 名称（若有）。
     */
    private String currentBehavior() { return behaviorName(manager.getPrimaryMascot()); }

    private String behaviorName(Mascot mascot) {
        Behavior b = mascot == null ? null : mascot.getBehavior();
        if (b == null) return null;
        String value = b.toString();
        return value != null && value.startsWith("Behavior(") && value.endsWith(")")
            ? value.substring(9, value.length() - 1) : value;
    }

    private static String okJson() {
        return "{\"ok\":true}";
    }

    private static String errorJson(String error) {
        JSONObject o = new JSONObject();
        o.put("ok", false);
        o.put("error", error);
        return o.toJSONString();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}