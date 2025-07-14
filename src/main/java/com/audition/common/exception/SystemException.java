package com.audition.common.exception;

import java.io.Serial;
import lombok.Getter;

@Getter
public class SystemException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -5876728854007114881L;

    public static final String DEFAULT_TITLE = "API Error Occurred";
    private final Integer statusCode;
    private final String title;
    private final String detail;

    public SystemException(final String detail, final Integer errorCode, final Throwable exception) {
        this(detail, DEFAULT_TITLE, errorCode, exception);
    }

    public SystemException(final String detail, final String title, final Integer errorCode,
        final Throwable exception) {
        super(detail, exception);
        this.statusCode = errorCode;
        this.title = title;
        this.detail = detail;
    }
}
