package com.group_finity.mascot.studio;

/**
 * 设计器面向 UI 的可读错误：携带 HTTP 状态码，由 {@link StudioServer} 统一转成
 * {@code {"ok":false,"error":"..."}}，不打印堆栈到 UI（SD-NFR-6）。
 */
public class StudioException extends RuntimeException {

    private final int status;

    public StudioException(int status, String message) {
        super(message);
        this.status = status;
    }

    public StudioException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
