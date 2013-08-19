package org.jboss.pressgang.ccms.wrapper.mocks;

import java.util.Date;
import java.util.List;

import org.jboss.pressgang.ccms.wrapper.PropertyTagInTopicWrapper;
import org.jboss.pressgang.ccms.wrapper.TagWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicSourceURLWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.TranslatedTopicWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.UpdateableCollectionWrapper;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;

public class TopicWrapperMock implements TopicWrapper {
    private String description;
    private Date created;
    private Date lastModified;
    private Integer xmlDoctype;
    private Integer id;
    private Integer revision;
    private String title;
    private String xml;
    private String locale;
    private CollectionWrapper<TagWrapper> tags;
    private UpdateableCollectionWrapper<TopicSourceURLWrapper> sourceUrls;
    private CollectionWrapper<TopicWrapper> incomingTopics;
    private CollectionWrapper<TopicWrapper> outgoingTopics;
    private CollectionWrapper<TranslatedTopicWrapper> translatedTopics;
    private UpdateableCollectionWrapper<PropertyTagInTopicWrapper> properties;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public Integer getXmlDoctype() {
        return xmlDoctype;
    }

    @Override
    public void setXmlDoctype(Integer doctypeId) {
        this.xmlDoctype = doctypeId;
    }

    @Override
    public CollectionWrapper<TranslatedTopicWrapper> getTranslatedTopics() {
        return translatedTopics;
    }

    @Override
    public String getEditorURL() {
        // TODO
        return null;
    }

    @Override
    public Integer getTopicId() {
        return getId();
    }

    @Override
    public Integer getTopicRevision() {
        return getRevision();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getXml() {
        return xml;
    }

    @Override
    public void setXml(String xml) {
        this.xml = xml;
    }

    @Override
    public String getLocale() {
        return locale;
    }

    @Override
    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public CollectionWrapper<TagWrapper> getTags() {
        return tags;
    }

    @Override
    public void setTags(CollectionWrapper<TagWrapper> tags) {
        this.tags = tags;
    }

    @Override
    public CollectionWrapper<TopicWrapper> getOutgoingRelationships() {
        return outgoingTopics;
    }

    @Override
    public void setOutgoingRelationships(CollectionWrapper<TopicWrapper> outgoingTopics) {
        this.outgoingTopics = outgoingTopics;
    }

    @Override
    public CollectionWrapper<TopicWrapper> getIncomingRelationships() {
        return incomingTopics;
    }

    @Override
    public void setIncomingRelationships(CollectionWrapper<TopicWrapper> incomingTopics) {
        this.incomingTopics = incomingTopics;
    }

    @Override
    public UpdateableCollectionWrapper<PropertyTagInTopicWrapper> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(UpdateableCollectionWrapper<PropertyTagInTopicWrapper> properties) {
        this.properties = properties;
    }

    @Override
    public UpdateableCollectionWrapper<TopicSourceURLWrapper> getSourceURLs() {
        return sourceUrls;
    }

    @Override
    public void setSourceURLs(UpdateableCollectionWrapper<TopicSourceURLWrapper> sourceURLs) {
        this.sourceUrls = sourceURLs;
    }

    @Override
    public List<TagWrapper> getTagsInCategories(List<Integer> categoryIds) {
        // TODO
        return null;
    }

    @Override
    public boolean hasTag(int tagId) {
        // TODO
        return false;
    }

    @Override
    public PropertyTagInTopicWrapper getProperty(int propertyId) {
        // TODO
        return null;
    }

    @Override
    public List<PropertyTagInTopicWrapper> getProperties(int propertyId) {
        // TODO
        return null;
    }

    @Override
    public String getBugzillaBuildId() {
        // TODO
        return null;
    }

    @Override
    public String getEditorURL(ZanataDetails zanataDetails) {
        // TODO
        return null;
    }

    @Override
    public String getPressGangURL() {
        // TODO
        return null;
    }

    @Override
    public String getInternalURL() {
        // TODO
        return null;
    }

    @Override
    public String getErrorXRefId() {
        // TODO
        return null;
    }

    @Override
    public String getXRefId() {
        // TODO
        return null;
    }

    @Override
    public String getXRefPropertyOrId(int propertyId) {
        // TODO
        return null;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getRevision() {
        return revision;
    }

    @Override
    public CollectionWrapper<TopicWrapper> getRevisions() {
        // TODO
        return null;
    }

    @Override
    public Object unwrap() {
        // TODO
        return null;
    }

    @Override
    public TopicWrapper clone(boolean deepCopy) {
        // TODO
        return null;
    }

    @Override
    public boolean isRevisionEntity() {
        // TODO
        return false;
    }
}
