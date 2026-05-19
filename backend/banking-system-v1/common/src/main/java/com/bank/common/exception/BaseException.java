package com.bank.common.exception;

public abstract class BaseException extends RuntimeException {

    public static final int HTTP_CODE = 500;
    public static final int CODE = 5000;

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getHttpCode() {
        return HTTP_CODE;
    }

    public int getCode() {
        return CODE;
    }
}
