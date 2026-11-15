package com.group_finity.mascot.studio;

import com.alibaba.fastjson.JSONObject;

/**
 * 一条设计期诊断。
 * <p>
 * 设计文档 §5.4：结果面板每条必须含「严重级别 / 文件 / 元素定位 / 修复建议」，
 * 点击可跳回对应工作台。{@code layer} 记录它来自 L0–L4 哪一层，便于 UI 分组。
 */
public final class Diagnostic {

    public static final String ERROR = "error";
    public static final String WARNING = "warning";
    public static final String INFO = "info";

    private final String severity;
    private final String layer;
    private final String file;
    private final String action;
    private final String pose;
    private final String field;
    private final String message;
    private final String hint;

    public Diagnostic(String severity, String layer, String file, String action, String pose,
                      String field, String message, String hint) {
        this.severity = severity;
        this.layer = layer;
        this.file = file;
        this.action = action;
        this.pose = pose;
        this.field = field;
        this.message = message;
        this.hint = hint;
    }

    public static Diagnostic error(String layer, String file, String message) {
        return new Diagnostic(ERROR, layer, file, null, null, null, message, null);
    }

    public static Diagnostic error(String layer, String file, String message, String hint) {
        return new Diagnostic(ERROR, layer, file, null, null, null, message, hint);
    }

    public static Diagnostic warning(String layer, String file, String message) {
        return new Diagnostic(WARNING, layer, file, null, null, null, message, null);
    }

    public static Diagnostic warning(String layer, String file, String message, String hint) {
        return new Diagnostic(WARNING, layer, file, null, null, null, message, hint);
    }

    public static Diagnostic info(String layer, String file, String message) {
        return new Diagnostic(INFO, layer, file, null, null, null, message, null);
    }

    public boolean isError() {
        return ERROR.equals(severity);
    }

    public String getSeverity() {
        return severity;
    }

    public String getFile() {
        return file;
    }

    public String getMessage() {
        return message;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject(true);
        o.put("severity", severity);
        o.put("layer", layer);
        o.put("file", file);
        if (action != null) {
            o.put("action", action);
        }
        if (pose != null) {
            o.put("pose", pose);
        }
        if (field != null) {
            o.put("field", field);
        }
        o.put("message", message);
        if (hint != null) {
            o.put("hint", hint);
        }
        return o;
    }

    @Override
    public String toString() {
        return "[" + severity + "/" + layer + "] " + (file == null ? "" : file + ": ") + message;
    }
}
