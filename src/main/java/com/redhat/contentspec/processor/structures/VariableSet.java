package com.redhat.contentspec.processor.structures;

public class VariableSet {
    Integer startPos = null;
    Integer endPos = null;
    String contents = null;

    public Integer getStartPos() {
        return startPos;
    }

    public void setStartPos(final Integer startPos) {
        this.startPos = startPos;
    }

    public Integer getEndPos() {
        return endPos;
    }

    public void setEndPos(final Integer endPos) {
        this.endPos = endPos;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(final String contents) {
        this.contents = contents;
    }
}