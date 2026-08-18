package com.ruott.beantap.exception;

public class BeanTapRunTimeException extends RuntimeException {

    public BeanTapRunTimeException(String message) {
        super(message);
    }

    public BeanTapRunTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
