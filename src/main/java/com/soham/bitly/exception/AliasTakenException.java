package com.soham.bitly.exception;

public class AliasTakenException extends RuntimeException {

    public AliasTakenException(String message) {
        super(message);
    }
}
