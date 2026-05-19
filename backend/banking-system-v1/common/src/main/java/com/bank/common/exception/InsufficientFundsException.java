package com.bank.common.exception;

public class InsufficientFundsException extends BaseException {

    public static final int HTTP_CODE = 422;
    public static final int CODE = 4220;

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
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
