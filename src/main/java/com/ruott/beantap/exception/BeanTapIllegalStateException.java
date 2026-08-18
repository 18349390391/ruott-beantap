package com.ruott.beantap.exception;

public class BeanTapIllegalStateException extends RuntimeException {

    public BeanTapIllegalStateException(String message) {
        super(message);
    }

    public BeanTapIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
