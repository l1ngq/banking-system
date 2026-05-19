package com.bank.common.exception;

public class CurrencyServiceUnavailableException extends BaseException {

    public static final int HTTP_CODE = 503;
    public static final int CODE = 5030;

    public CurrencyServiceUnavailableException(String message) {
        super(message);
    }

    public CurrencyServiceUnavailableException(String message, Throwable cause) {
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
