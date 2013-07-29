package org.jboss.pressgang.ccms.contentspec.processor.exceptions;

import org.jboss.pressgang.ccms.contentspec.exceptions.ParsingException;

public class InvalidKeyValueException extends ParsingException {
    private static final long serialVersionUID = -5977447731911162387L;

    public InvalidKeyValueException() {
    }

    public InvalidKeyValueException(String message) {
        super(message);
    }
}
