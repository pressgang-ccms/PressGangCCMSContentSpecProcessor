package org.jboss.pressgang.ccms.contentspec.processor.structures;

public class SnapshotOptions {
    private boolean addRevisions = false;
    private boolean updateRevisions = false;
    private Integer revision = null;
    private boolean translation = false;
    private String translationLocale = null;

    public boolean isAddRevisions() {
        return addRevisions;
    }

    public void setAddRevisions(boolean addRevisions) {
        this.addRevisions = addRevisions;
    }

    public boolean isUpdateRevisions() {
        return updateRevisions;
    }

    public void setUpdateRevisions(boolean updateRevisions) {
        this.updateRevisions = updateRevisions;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(final Integer revision) {
        this.revision = revision;
    }

    public boolean isTranslation() {
        return translation;
    }

    public void setTranslation(boolean translation) {
        this.translation = translation;
    }

    public String getTranslationLocale() {
        return translationLocale;
    }

    public void setTranslationLocale(String translationLocale) {
        this.translationLocale = translationLocale;
    }
}
