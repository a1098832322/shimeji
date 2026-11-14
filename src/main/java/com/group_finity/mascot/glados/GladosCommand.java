package com.group_finity.mascot.glados;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GLaDOS 控制命令枚举 + 参数校验。
 * <p>
 * 设计文档：docs/glados-ai-integration-design.md §4.2 / §4.4
 * <p>
 * 命令白名单：只允许本枚举中的命令，未知命令由 {@link #fromWireName(String)} 返回 null，
 * 由桥层拒绝。
 */
public enum GladosCommand {

    SET_ROLE("setRole", "role"),
    SET_BEHAVIOR("setBehavior", "behavior"),
    PAUSE("pause"),
    RESUME("resume"),
    CREATE_MASCOT("createMascot", "role"),
    REMAIN_ONE("remainOne", "role"),
    DISPOSE_ALL("disposeAll"),
    QUIT("quit");

    private final String wireName;
    private final Set<String> requiredParams;

    GladosCommand(String wireName, String... requiredParams) {
        this.wireName = wireName;
        this.requiredParams = requiredParams.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(Arrays.asList(requiredParams)));
    }

    public String getWireName() {
        return wireName;
    }

    public Set<String> getRequiredParams() {
        return requiredParams;
    }

    /**
     * 按线上命令名查找枚举；未知命令返回 null。
     */
    public static GladosCommand fromWireName(String name) {
        if (name == null) {
            return null;
        }
        for (GladosCommand cmd : values()) {
            if (cmd.wireName.equals(name)) {
                return cmd;
            }
        }
        return null;
    }

    /**
     * 校验必需参数。通过返回 null，否则返回错误描述。
     */
    public String validate(Map<String, String> params) {
        if (params == null) {
            return "missing params";
        }
        for (String p : requiredParams) {
            String v = params.get(p);
            if (v == null || v.trim().isEmpty()) {
                return "missing required param: " + p;
            }
        }
        return null;
    }
}