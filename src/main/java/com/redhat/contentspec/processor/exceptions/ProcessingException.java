package com.redhat.contentspec.processor.exceptions;

public class ProcessingException extends Exception {
    private static final long serialVersionUID = 5545431739360420849L;

    public ProcessingException(final String message) {
        super(message);
    }
}
