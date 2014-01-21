package org.jboss.pressgang.ccms.contentspec.processor.structures;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;

public class ParserResults {
    final boolean parsedSuccessfully;
    final ContentSpec contentSpec;

    public ParserResults(final boolean parsedSuccessfully, final ContentSpec contentSpec) {
        this.parsedSuccessfully = parsedSuccessfully;
        this.contentSpec = contentSpec;
    }

    public boolean parsedSuccessfully() {
        return parsedSuccessfully;
    }

    public ContentSpec getContentSpec() {
        return contentSpec;
    }
}
