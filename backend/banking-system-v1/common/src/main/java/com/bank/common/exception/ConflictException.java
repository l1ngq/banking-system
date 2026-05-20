package com.bank.common.exception;

public class ConflictException extends BaseException {

    public static final int HTTP_CODE = 409;
    public static final int CODE = 4090;

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getHttpCode() {
        return HTTP_CODE;
    }

    @Override
    public int getCode() {
        return CODE;
    }
}
