/*
  Copyright 2011-2014 Red Hat, Inc

  This file is part of PressGang CCMS.

  PressGang CCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PressGang CCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with PressGang CCMS.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.jboss.pressgang.ccms.contentspec.processor.structures;

public class ProcessingOptions {

    private boolean validateOnly = false;
    private boolean ignoreChecksum = false;
    private boolean allowEmptyLevels = false;
    private boolean allowNewTopics = true;
    private boolean strictTitles = false;
    private boolean strictBugLinks = false;
    private boolean validateBugLinks = true;
    private boolean doBugLinkLastValidateCheck = true;
    private boolean translation = false;
    private String translationLocale = null;
    private Integer maxRevision = null;
    private boolean printChangeWarnings = true;
    private boolean validate = true;

    public boolean isValidateOnly() {
        return validateOnly;
    }

    public void setValidateOnly(boolean validateOnly) {
        this.validateOnly = validateOnly;
    }

    public boolean isIgnoreChecksum() {
        return ignoreChecksum;
    }

    public void setIgnoreChecksum(boolean ignoreChecksum) {
        this.ignoreChecksum = ignoreChecksum;
    }

    public boolean isAllowEmptyLevels() {
        return allowEmptyLevels;
    }

    public void setAllowEmptyLevels(boolean allowEmptyLevels) {
        this.allowEmptyLevels = allowEmptyLevels;
    }

    public boolean isAllowNewTopics() {
        return allowNewTopics;
    }

    public void setAllowNewTopics(boolean allowNewTopics) {
        this.allowNewTopics = allowNewTopics;
    }

    public boolean isStrictTitles() {
        return strictTitles;
    }

    public void setStrictTitles(boolean strictTitles) {
        this.strictTitles = strictTitles;
    }

    public boolean isTranslation() {
        return translation;
    }

    public void setTranslation(boolean translation) {
        this.translation = translation;
    }

    public boolean isStrictBugLinks() {
        return strictBugLinks;
    }

    public void setStrictBugLinks(boolean strictBugLinks) {
        this.strictBugLinks = strictBugLinks;
    }

    public boolean isValidateBugLinks() {
        return validateBugLinks;
    }

    public void setValidateBugLinks(boolean validateBugLinks) {
        this.validateBugLinks = validateBugLinks;
    }

    public Integer getMaxRevision() {
        return maxRevision;
    }

    public void setMaxRevision(Integer maxRevision) {
        this.maxRevision = maxRevision;
    }

    public boolean isDoBugLinkLastValidateCheck() {
        return doBugLinkLastValidateCheck;
    }

    public void setDoBugLinkLastValidateCheck(boolean doBugLinkLastValidateCheck) {
        this.doBugLinkLastValidateCheck = doBugLinkLastValidateCheck;
    }

    public String getTranslationLocale() {
        return translationLocale;
    }

    public void setTranslationLocale(String translationLocale) {
        this.translationLocale = translationLocale;
    }

    public boolean isPrintChangeWarnings() {
        return printChangeWarnings;
    }

    public void setPrintChangeWarnings(boolean printChangeWarnings) {
        this.printChangeWarnings = printChangeWarnings;
    }

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }
}
