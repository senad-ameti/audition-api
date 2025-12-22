package com.audition.common.exception;

import lombok.Getter;

/**
 * Custom runtime exception for system-level errors in the application.
 */
@Getter
public class SystemException extends RuntimeException {

    private static final long serialVersionUID = -5876728854007114881L;

    public static final String DEFAULT_TITLE = "API Error Occurred";
    private Integer statusCode;
    private String title;
    private String detail;

    /**
     * Default constructor.
     */
    public SystemException() {
        super();
    }

    /**
     * Constructor with message.
     *
     * @param message the error message
     */
    public SystemException(final String message) {
        super(message);
        this.title = DEFAULT_TITLE;
    }

    /**
     * Constructor with message and error code.
     *
     * @param message   the error message
     * @param errorCode the error code
     */
    public SystemException(final String message, final Integer errorCode) {
        super(message);
        this.title = DEFAULT_TITLE;
        this.statusCode = errorCode;
    }

    /**
     * Constructor with message and exception.
     *
     * @param message   the error message
     * @param exception the cause exception
     */
    public SystemException(final String message, final Throwable exception) {
        super(message, exception);
        this.title = DEFAULT_TITLE;
    }

    /**
     * Constructor with detail, title, and error code.
     *
     * @param detail    the error detail
     * @param title     the error title
     * @param errorCode the error code
     */
    public SystemException(final String detail, final String title, final Integer errorCode) {
        super(detail);
        this.statusCode = errorCode;
        this.title = title;
        this.detail = detail;
    }

    /**
     * Constructor with detail, title, and exception.
     *
     * @param detail    the error detail
     * @param title     the error title
     * @param exception the cause exception
     */
    public SystemException(final String detail, final String title, final Throwable exception) {
        super(detail, exception);
        this.title = title;
        this.statusCode = 500;
        this.detail = detail;
    }

    /**
     * Constructor with detail, error code, and exception.
     *
     * @param detail    the error detail
     * @param errorCode the error code
     * @param exception the cause exception
     */
    public SystemException(final String detail, final Integer errorCode, final Throwable exception) {
        super(detail, exception);
        this.statusCode = errorCode;
        this.title = DEFAULT_TITLE;
        this.detail = detail;
    }

    /**
     * Constructor with detail, title, error code, and exception.
     *
     * @param detail    the error detail
     * @param title     the error title
     * @param errorCode the error code
     * @param exception the cause exception
     */
    public SystemException(final String detail, final String title, final Integer errorCode,
        final Throwable exception) {
        super(detail, exception);
        this.statusCode = errorCode;
        this.title = title;
        this.detail = detail;
    }
}
