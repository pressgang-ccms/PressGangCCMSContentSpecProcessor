/*
  Copyright 2011-2014 Red Hat

  This file is part of PresGang CCMS.

  PresGang CCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PresGang CCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with PresGang CCMS.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.jboss.pressgang.ccms.contentspec.processor.structures;

public class SnapshotOptions {
    private boolean addRevisions = true;
    private boolean addFixedUrls = true;
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

    public boolean isAddFixedUrls() {
        return addFixedUrls;
    }

    public void setAddFixedUrls(boolean addFixedUrls) {
        this.addFixedUrls = addFixedUrls;
    }
}
