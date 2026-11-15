package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 设计器本地 HTTP 服务（仅 127.0.0.1）。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §6.1（接口契约）、DR-13（安全）。
 * <p>
 * 统一响应包装：成功 {@code {"ok":true,"data":...}}，失败 {@code {"ok":false,"error":"..."}}；
 * 校验未通过时额外带 {@code diagnostics[]}（HTTP 422）。
 */
public final class StudioServer {

    private static final Logger log = LoggerFactory.getLogger(StudioServer.class);

    private final StudioOptions options;
    private final WorkspaceManager wm;
    private final DocumentService documents;
    private final AssetService assets;
    private final ValidationService validation;
    private final PreviewOrchestrator preview;
    private final PackageService packages;

    private HttpServer server;

    public StudioServer(StudioOptions options, WorkspaceManager wm, DocumentService documents,
                        AssetService assets, ValidationService validation,
                        PreviewOrchestrator preview, PackageService packages) {
        this.options = options;
        this.wm = wm;
        this.documents = documents;
        this.assets = assets;
        this.validation = validation;
        this.preview = preview;
        this.packages = packages;
    }

    public int start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", options.getPort()), 0);
        server.createContext("/", this::dispatch);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "studio-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        int port = server.getAddress().getPort();
        log.info("Shimeji Studio listening on http://127.0.0.1:{}", port);
        return port;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    // ------------------------------------------------------------------
    // 路由
    // ------------------------------------------------------------------

    private void dispatch(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI().getPath();
        try {
            if (rawPath.startsWith("/api/")) {
                handleApi(exchange, rawPath);
            } else {
                serveStatic(exchange, rawPath);
            }
        } catch (DocumentService.StudioValidationException e) {
            respondJson(exchange, 422, e.getPayload());
        } catch (StudioException e) {
            respondError(exchange, e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Studio request failed: {} {}", exchange.getRequestMethod(), rawPath, e);
            respondError(exchange, 500, "服务内部错误：" + Xml.shortMessage(e));
        } finally {
            exchange.close();
        }
    }

    private void handleApi(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        JSONObject body = readJsonBody(exchange);

        if ("/api/health".equals(path)) {
            JSONObject data = new JSONObject(true);
            data.put("ok", true);
            data.put("version", com.wishes.constant.Constant.CURRENT_VERSION);
            ok(exchange, data);
            return;
        }

        if ("/api/workspace".equals(path)) {
            JSONObject data = new JSONObject(true);
            data.put("engineRoot", options.getEngineRoot().toString());
            data.put("confDir", options.getConfDir().toString());
            data.put("imgDir", options.getImgDir().toString());
            data.put("relativeConf", wm.relativeToEngine(options.getConfDir()).toString());
            data.put("relativeImg", wm.relativeToEngine(options.getImgDir()).toString());
            data.put("devMode", options.isDevLayout());
            data.put("layout", options.isDevLayout() ? "dev" : "prod");
            data.put("layoutError", options.validateLayout());
            data.put("version", com.wishes.constant.Constant.CURRENT_VERSION);
            data.put("preview", preview.snapshot());
            data.put("studioHome", wm.relativeToEngine(wm.getStudioDir()).toString());
            ok(exchange, data);
            return;
        }

        if ("/api/baseline".equals(path)) {
            JSONObject data = new JSONObject(true);
            data.put("files", wm.baselineFingerprint());
            data.put("note", "基础层指纹（SHA-256）：安装/卸载扩展包前后应完全一致（AC-15-1）。");
            ok(exchange, data);
            return;
        }

        if ("/api/schema".equals(path)) {
            ok(exchange, schema());
            return;
        }

        // ---- 皮肤与素材 ----
        if ("/api/skins".equals(path) && "GET".equals(method)) {
            ok(exchange, assets.listSkins());
            return;
        }
        if ("/api/skins".equals(path) && "POST".equals(method)) {
            JSONObject data = assets.createSkin(body.getString("id"), body);
            ok(exchange, data);
            return;
        }
        if (path.matches("^/api/skins/[^/]+/assets$") && "GET".equals(method)) {
            String skin = decode(segments(path).get(2));
            ok(exchange, assets.listAssets(skin));
            return;
        }
        if ("/api/assets/import".equals(path) && "POST".equals(method)) {
            ok(exchange, assets.importAssets(body.getString("skin"), body));
            return;
        }
        if ("/api/assets/anchor".equals(path) && "POST".equals(method)) {
            ok(exchange, assets.saveAnchor(body.getString("skin"), body.getString("asset"),
                    body.getIntValue("x"), body.getIntValue("y")));
            return;
        }
        if ("/api/assets/raw".equals(path)) {
            Path file = wm.resolveImage(query.get("skin"), query.get("path"));
            if (!Files.isRegularFile(file)) {
                respondError(exchange, 404, "图片不存在");
                return;
            }
            respondBinary(exchange, Files.readAllBytes(file), contentType(file.getFileName().toString()), null);
            return;
        }
        if ("/api/assets/thumb".equals(path)) {
            int w = query.get("w") == null ? 64 : Integer.parseInt(query.get("w"));
            byte[] bytes = assets.thumbnail(query.get("skin"), query.get("path"), w);
            respondBinary(exchange, bytes, "image/png", null);
            return;
        }
        if ("/api/info".equals(path) && "GET".equals(method)) {
            ok(exchange, assets.readInfo(query.get("skin")));
            return;
        }
        if ("/api/info".equals(path) && "PUT".equals(method)) {
            boolean force = body.getBooleanValue("force");
            ok(exchange, assets.writeInfo(body.getString("skin"), body, force));
            return;
        }

        // ---- 清单 / 片段 ----
        if ("/api/manifest".equals(path)) {
            ok(exchange, documents.manifest(query.get("skin")));
            return;
        }
        if ("/api/fragments".equals(path) && "GET".equals(method)) {
            ok(exchange, documents.listFragments());
            return;
        }
        if ("/api/fragments/suggest".equals(path)) {
            JSONObject data = new JSONObject(true);
            data.put("file", documents.suggestFileName(query.getOrDefault("kind", "actions"),
                    query.getOrDefault("slug", "fragment"),
                    query.get("order") == null ? 0 : Integer.parseInt(query.get("order"))));
            ok(exchange, data);
            return;
        }
        List<String> seg = segments(path);
        if (seg.size() == 4 && "api".equals(seg.get(0)) && "fragments".equals(seg.get(1))) {
            String kind = decode(seg.get(2));
            String file = decode(seg.get(3));
            if ("GET".equals(method)) {
                ok(exchange, documents.readFragment(kind, file));
                return;
            }
            if ("PUT".equals(method)) {
                ok(exchange, documents.saveFragment(kind, file, body.getString("xml"),
                        body.getString("skin"), body.getBooleanValue("force")));
                return;
            }
            if ("DELETE".equals(method)) {
                ok(exchange, documents.deleteFragment(kind, file, query.containsKey("force")
                        && "true".equalsIgnoreCase(query.get("force"))));
                return;
            }
        }

        // ---- 校验 / 试算 ----
        if ("/api/validate".equals(path) && "POST".equals(method)) {
            ok(exchange, validation.validate(body.getString("skin"), overlaysOf(body)));
            return;
        }
        if ("/api/simulate".equals(path) && "POST".equals(method)) {
            Map<String, String> params = new HashMap<>();
            JSONObject raw = body.getJSONObject("params");
            if (raw != null) {
                for (String key : raw.keySet()) {
                    params.put(key, raw.getString(key));
                }
            }
            ok(exchange, validation.simulate(body.getString("expression"), params));
            return;
        }

        // ---- 预览 ----
        if ("/api/preview/start".equals(path) && "POST".equals(method)) {
            ok(exchange, preview.start());
            return;
        }
        if ("/api/preview/stop".equals(path) && "POST".equals(method)) {
            ok(exchange, preview.stop());
            return;
        }
        if ("/api/preview/status".equals(path)) {
            ok(exchange, preview.status());
            return;
        }
        if ("/api/preview/run".equals(path) && "POST".equals(method)) {
            ok(exchange, preview.run(body.getString("behavior"), body.getString("skin")));
            return;
        }
        if ("/api/preview/command".equals(path) && "POST".equals(method)) {
            String cmd = body.getString("cmd");
            Map<String, String> extra = new HashMap<>();
            for (String key : body.keySet()) {
                if (!"cmd".equals(key)) {
                    extra.put(key, body.getString(key));
                }
            }
            ok(exchange, preview.command(cmd, extra));
            return;
        }

        // ---- 扩展包 ----
        if ("/api/packs".equals(path) && "GET".equals(method)) {
            ok(exchange, packages.listPacks());
            return;
        }
        if ("/api/packs/install".equals(path) && "POST".equals(method)) {
            JSONObject data = packages.installPack(base64(body.getString("zipBase64")));
            reloadIfRunning();
            ok(exchange, data);
            return;
        }
        if (path.matches("^/api/packs/[^/]+$") && "DELETE".equals(method)) {
            String id = decode(segments(path).get(2));
            JSONObject data = packages.uninstallPack(id);
            reloadIfRunning();
            ok(exchange, data);
            return;
        }
        if (path.matches("^/api/packs/[^/]+/toggle$") && "POST".equals(method)) {
            String id = decode(segments(path).get(2));
            JSONObject data = packages.togglePack(id, body.getBooleanValue("enabled"));
            reloadIfRunning();
            ok(exchange, data);
            return;
        }
        if ("/api/packs/export".equals(path) && "POST".equals(method)) {
            byte[] zip = packages.exportPack(body.getString("id"));
            respondBinary(exchange, zip, "application/zip", "pack-" + body.getString("id") + ".shimeji-pack.zip");
            return;
        }

        // ---- 导出 / 导入 ----
        if ("/api/export".equals(path) && "POST".equals(method)) {
            byte[] zip = packages.exportSkin(body.getString("skin"), body.getBooleanValue("includeStudioFragments"));
            respondBinary(exchange, zip, "application/zip", "skin-" + body.getString("skin") + ".shimeji-skin.zip");
            return;
        }
        if ("/api/import/analyze".equals(path) && "POST".equals(method)) {
            ok(exchange, packages.analyzeImport(base64(body.getString("zipBase64"))));
            return;
        }
        if ("/api/import/skin".equals(path) && "POST".equals(method)) {
            JSONObject data = packages.importSkin(base64(body.getString("zipBase64")),
                    body.getBooleanValue("overwriteGenerated"));
            reloadIfRunning();
            ok(exchange, data);
            return;
        }

        respondError(exchange, 404, "未知接口: " + path);
    }

    private List<ValidationService.Overlay> overlaysOf(JSONObject body) {
        List<ValidationService.Overlay> overlays = new ArrayList<>();
        JSONArray arr = body.getJSONArray("fragments");
        if (arr == null) {
            return overlays;
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            overlays.add(new ValidationService.Overlay(o.getString("kind"), o.getString("file"), o.getString("xml")));
        }
        return overlays;
    }

    private void reloadIfRunning() {
        JSONObject snapshot = preview.snapshot();
        if (PreviewOrchestrator.STATE_RUNNING.equals(snapshot.getString("state"))) {
            try {
                preview.command("reload", Map.of());
            } catch (Exception e) {
                log.warn("Auto reload after pack operation failed: {}", e.toString());
            }
        }
    }

    // ------------------------------------------------------------------
    // 静态资源
    // ------------------------------------------------------------------

    private void serveStatic(HttpExchange exchange, String rawPath) throws IOException {
        String path = rawPath == null || rawPath.isEmpty() || "/".equals(rawPath) ? "/index.html" : rawPath;
        if (path.contains("..")) {
            respondError(exchange, 400, "非法路径");
            return;
        }
        String resource = "/studio/web" + path;
        try (InputStream in = StudioServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                respondText(exchange, 404, "未找到资源: " + path);
                return;
            }
            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            respondBinary(exchange, bytes, contentType(path), null);
        }
    }

    private static String contentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (lower.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
            return "text/javascript; charset=utf-8";
        }
        if (lower.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }

    // ------------------------------------------------------------------
    // 元数据
    // ------------------------------------------------------------------

    private static String describeType(String type) {
        if ("Sequence".equals(type)) {
            return "按顺序播放子动作（ActionReference）";
        }
        if ("Select".equals(type)) {
            return "条件选择一个子动作";
        }
        if ("Animate".equals(type)) {
            return "原地播帧（Animation/Pose）";
        }
        if ("Stay".equals(type)) {
            return "原地停留，带帧与位移参数";
        }
        if ("Move".equals(type)) {
            return "按 Velocity 位移播放";
        }
        return "Java 内置动作，只读";
    }

    private JSONObject schema() {
        JSONObject data = new JSONObject(true);
        JSONArray types = new JSONArray();
        for (String type : new String[]{"Sequence", "Select", "Animate", "Stay", "Move", "Embedded"}) {
            JSONObject t = new JSONObject(true);
            t.put("value", type);
            t.put("readOnly", "Embedded".equals(type));
            t.put("description", describeType(type));
            types.add(t);
        }
        data.put("actionTypes", types);
        data.put("borderTypes", new JSONArray(List.of("Floor", "Wall", "Ceiling")));
        data.put("poseFields", new JSONArray(List.of("Image", "ImageRight", "ImageAnchor", "Velocity", "Duration", "Sound", "Volume")));
        data.put("reservedNames", new JSONArray(List.of("AI*", "SplitIntoTwo", "PullUpShimeji")));
        JSONObject defaults = new JSONObject(true);
        defaults.put("anchorX", 64);
        defaults.put("anchorY", 128);
        defaults.put("duration", 250);
        defaults.put("canvas", 128);
        defaults.put("behaviorFrequency", 0);
        data.put("defaults", defaults);
        data.put("expressionSamples", new JSONArray(List.of(
                "${500+Math.random()*1000}",
                "${200+Math.random()*200}",
                "${-stepX},0",
                "#{100+mascot.anchor.x*0}")));
        data.put("notes", new JSONArray(List.of(
                "${} 在配置加载期求值一次（缓存），#{} 每帧重算",
                "帧参数在加载期求值：可用 Action 属性，不可用 mascot 等运行时变量",
                "Duration 必须 >= 1，否则 Animation.getPoseAt() 会除零")));
        return data;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static List<String> segments(String path) {
        List<String> out = new ArrayList<>();
        for (String s : path.split("/")) {
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            map.put(decode(pair.substring(0, idx)), decode(pair.substring(idx + 1)));
        }
        return map;
    }

    private static JSONObject readJsonBody(HttpExchange exchange) throws IOException {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength == null || "0".equals(contentLength)) {
            return new JSONObject();
        }
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return new JSONObject();
        }
        try {
            JSONObject obj = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
            return obj == null ? new JSONObject() : obj;
        } catch (Exception e) {
            throw new StudioException(400, "请求体不是合法 JSON");
        }
    }

    private static byte[] base64(String data) {
        if (data == null || data.isEmpty()) {
            throw new StudioException(400, "缺少文件内容");
        }
        String clean = data;
        int comma = clean.indexOf(',');
        if (clean.startsWith("data:") && comma > 0) {
            clean = clean.substring(comma + 1);
        }
        try {
            return java.util.Base64.getDecoder().decode(clean);
        } catch (IllegalArgumentException e) {
            throw new StudioException(400, "base64 解码失败");
        }
    }

    private static void ok(HttpExchange exchange, Object data) throws IOException {
        JSONObject body = new JSONObject(true);
        body.put("ok", true);
        body.put("data", data);
        respondJson(exchange, 200, body);
    }

    private static void respondError(HttpExchange exchange, int status, String message) throws IOException {
        JSONObject body = new JSONObject(true);
        body.put("ok", false);
        body.put("error", message);
        respondJson(exchange, status, body);
    }

    private static void respondJson(HttpExchange exchange, int status, Object body) throws IOException {
        // fastjson 默认开启循环引用/重复引用检测，会把同一对象第二次出现序列化成
        // {"$ref":"$.data.preview.status"}，浏览器端拿到的是引用而不是数据；这里显式关闭。
        byte[] bytes = JSON.toJSONString(body, SerializerFeature.DisableCircularReferenceDetect)
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void respondText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void respondBinary(HttpExchange exchange, byte[] bytes, String contentType, String fileName)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (fileName != null) {
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
