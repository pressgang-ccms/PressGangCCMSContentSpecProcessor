package org.jboss.pressgang.ccms.contentspec.processor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.entities.ProcessRelationship;
import org.jboss.pressgang.ccms.contentspec.entities.Relationship;
import org.jboss.pressgang.ccms.contentspec.entities.TargetRelationship;
import org.jboss.pressgang.ccms.contentspec.entities.TopicRelationship;
import org.jboss.pressgang.ccms.contentspec.enums.RelationshipType;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.CSRelatedNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.UpdateableCollectionWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeTopicRelationshipsTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer topicId;
    @Arbitrary Integer topicRevision;
    @Arbitrary String title;
    @Arbitrary Integer secondId;
    @Arbitrary Integer secondTopicId;
    @Arbitrary Integer secondTopicRevision;
    @Arbitrary String secondTitle;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphaString;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock SpecTopic specTopic;
    @Mock SpecTopic relatedSpecTopic;
    @Mock Level relatedLevel;
    @Mock CSNodeWrapper nodeEntity;
    @Mock CSNodeWrapper nodeEntity2;
    @Mock CSNodeWrapper relatedNodeEntity;
    @Mock CSNodeWrapper relatedNodeEntity2;
    @Mock CSRelatedNodeWrapper relatedNode;
    @Mock CSRelatedNodeWrapper relatedNode2;
    @Mock CSNodeWrapper parent;

    UpdateableCollectionWrapperMock<CSNodeWrapper> updatedChildrenNodes;
    UpdateableCollectionWrapperMock<CSRelatedNodeWrapper> relatedToNodes;
    UpdateableCollectionWrapperMock<CSRelatedNodeWrapper> relatedToNodes2;
    UpdateableCollectionWrapperMock<CSRelatedNodeWrapper> newRelatedToNodes;
    UpdateableCollectionWrapperMock<CSRelatedNodeWrapper> newRelatedToNodes2;
    Map<SpecNode, CSNodeWrapper> nodeMap;

    @Before
    public void setUpCollections() {
        updatedChildrenNodes = new UpdateableCollectionWrapperMock<CSNodeWrapper>();
        relatedToNodes = new UpdateableCollectionWrapperMock<CSRelatedNodeWrapper>();
        newRelatedToNodes = new UpdateableCollectionWrapperMock<CSRelatedNodeWrapper>();
        newRelatedToNodes2 = new UpdateableCollectionWrapperMock<CSRelatedNodeWrapper>();
        nodeMap = new HashMap<SpecNode, CSNodeWrapper>();

        when(contentSpecNodeProvider.newCSRelatedNodeCollection()).thenReturn(newRelatedToNodes, newRelatedToNodes2);

        // and the spec topic node has a parent
        when(nodeEntity.getParent()).thenReturn(parent);
        when(parent.getChildren()).thenReturn(updatedChildrenNodes);
    }

    @Test
    public void shouldIgnoreTopicWhenNoRelationships() {
        // Given a spec topic that has no relationships
        given(specTopic.getRelationships()).willReturn(new LinkedList<Relationship>());
        // and a matching node with no relationships
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        given(nodeEntity.getId()).willReturn(id);
        given(nodeEntity.getTitle()).willReturn(title);
        // and the matching nodes are in the map
        nodeMap.put(specTopic, nodeEntity);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated node collection should be empty
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the topic still has no relationships
        assertThat(specTopic.getRelationships().size(), is(0));
        // and the topic entity relationships is empty
        assertThat(relatedToNodes.size(), is(0));
        assertThat(newRelatedToNodes.size(), is(0));
        // and the topic didn't have the relationships set
        verify(relatedNodeEntity, never()).setRelatedToNodes(any(UpdateableCollectionWrapper.class));
    }

    @Test
    public void shouldAddNewTopicRelationship() {
        // Given a new relationship to another topic
        final Relationship relationship = new TopicRelationship(specTopic, relatedSpecTopic, RelationshipType.REFER_TO);
        // and a spec topic to hold the relationship
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node that hasn't got any relationships
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        nodeMap.put(specTopic, nodeEntity);
        // and the related spec topic exists
        nodeMap.put(relatedSpecTopic, relatedNodeEntity);
        // and creating the new related node works
        given(contentSpecNodeProvider.newCSRelatedNode(eq(relatedNodeEntity))).willReturn(relatedNode);


        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the related node was added to the collection
        assertThat(relatedToNodes.size(), is(1));
        assertThat(relatedToNodes.getAddItems().size(), is(1));
        assertSame(relatedToNodes.getItems().get(0), relatedNode);
        // and the related node has the type and sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        verify(relatedNode, times(1)).setRelationshipType(CommonConstants.CS_RELATIONSHIP_REFER_TO);
    }

    @Test
    public void shouldAddMultipleNewTopicRelationships() {
        final LinkedList<Relationship> relationships = new LinkedList<Relationship>();
        // Given two new relationships to other topics
        final Relationship relationship = new TopicRelationship(specTopic, relatedSpecTopic, RelationshipType.REFER_TO);
        final Relationship relationship2 = new TargetRelationship(specTopic, relatedLevel, RelationshipType.REFER_TO);
        relationships.add(relationship);
        relationships.add(relationship2);
        // and a spec topic to hold the relationship
        given(specTopic.getRelationships()).willReturn(relationships);
        // and matching nodes that haven't got any relationships
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        nodeMap.put(specTopic, nodeEntity);
        // and the related spec topic exists
        nodeMap.put(relatedSpecTopic, relatedNodeEntity);
        // and the related level topic exists
        nodeMap.put(relatedLevel, relatedNodeEntity2);
        // and creating the new related topic works
        given(contentSpecNodeProvider.newCSRelatedNode(eq(relatedNodeEntity))).willReturn(relatedNode);
        // and creating the new related level works
        given(contentSpecNodeProvider.newCSRelatedNode(eq(relatedNodeEntity2))).willReturn(relatedNode2);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertTrue(updatedChildrenNodes.getUpdateItems().contains(nodeEntity));
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the related topic & level node were added to the collection
        assertThat(relatedToNodes.size(), is(2));
        assertThat(relatedToNodes.getAddItems().size(), is(2));
        assertTrue(relatedToNodes.getAddItems().contains(relatedNode));
        assertTrue(relatedToNodes.getAddItems().contains(relatedNode2));
        // and the related topic node has the type and sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        verify(relatedNode, times(1)).setRelationshipType(CommonConstants.CS_RELATIONSHIP_REFER_TO);
        // and the related level node has the type and sort set
        verify(relatedNode2, times(1)).setRelationshipSort(2);
        verify(relatedNode2, times(1)).setRelationshipType(CommonConstants.CS_RELATIONSHIP_REFER_TO);
    }

    @Test
    public void shouldAddNewTargetTopicRelationship() {
        // Given a new relationship to another topic
        final Relationship relationship = new TargetRelationship(specTopic, relatedSpecTopic, RelationshipType.PREREQUISITE);
        // and a spec topic to hold the relationship
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node that hasn't got any relationships
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        nodeMap.put(specTopic, nodeEntity);
        // and the related spec topic exists
        nodeMap.put(relatedSpecTopic, relatedNodeEntity);
        // and creating the new related node works
        given(contentSpecNodeProvider.newCSRelatedNode(eq(relatedNodeEntity))).willReturn(relatedNode);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the related node was added to the collection
        assertThat(relatedToNodes.size(), is(1));
        assertThat(relatedToNodes.getAddItems().size(), is(1));
        assertSame(relatedToNodes.getItems().get(0), relatedNode);
        // and the related node has the type and sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        verify(relatedNode, times(1)).setRelationshipType(CommonConstants.CS_RELATIONSHIP_PREREQUISITE);
    }

    @Test
    public void shouldAddNewTargetLevelRelationship() {
        // Given a new relationship to another topic
        final Relationship relationship = new TargetRelationship(specTopic, relatedLevel, RelationshipType.LINKLIST);
        // and a spec topic to hold the relationship
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node that hasn't got any relationships
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        nodeMap.put(specTopic, nodeEntity);
        // and the related level exists
        nodeMap.put(relatedLevel, relatedNodeEntity);
        // and creating the new related node works
        given(contentSpecNodeProvider.newCSRelatedNode(eq(relatedNodeEntity))).willReturn(relatedNode);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the related node was added to the collection
        assertThat(relatedToNodes.size(), is(1));
        assertThat(relatedToNodes.getAddItems().size(), is(1));
        assertSame(relatedToNodes.getItems().get(0), relatedNode);
        // and the related node has the type and sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        verify(relatedNode, times(1)).setRelationshipType(CommonConstants.CS_RELATIONSHIP_LINK_LIST);
    }

    @Test
    public void shouldIgnoreNewProcessTopicRelationship() {
        // Given a new relationship to another topic
        final Relationship relationship = new ProcessRelationship(specTopic, relatedSpecTopic, RelationshipType.PREREQUISITE);
        // and a spec topic to hold the relationship
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node that hasn't got any relationships
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        nodeMap.put(specTopic, nodeEntity);
        // and creating the new related node works
        given(contentSpecNodeProvider.newCSRelatedNode(eq(relatedNodeEntity))).willReturn(relatedNode);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated node collection should be empty
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the topic entity relationships is empty
        assertThat(relatedToNodes.size(), is(0));
        assertThat(newRelatedToNodes.size(), is(0));
        // and the topic didn't have the relationships set
        verify(relatedNodeEntity, never()).setRelatedToNodes(any(UpdateableCollectionWrapper.class));
    }

    @Test
    public void shouldIgnoreTopicWhenRelationshipsMatchExistingRelationships() {
        // Given a spec topic that has a relationship that already exists
        final Relationship relationship = new TopicRelationship(specTopic, relatedSpecTopic, RelationshipType.LINKLIST);
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        // and the node has a matching relationship
        relatedToNodes.addItem(relatedNode);
        given(relatedNode.getRelationshipSort()).willReturn(1);
        given(relatedNode.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_LINK_LIST);
        // and the related node exists in the map
        nodeMap.put(relatedSpecTopic, relatedNodeEntity);
        // and the matching nodes are in the map
        nodeMap.put(specTopic, nodeEntity);
        // and the spec topics have the db id's set
        given(specTopic.getDBId()).willReturn(topicId);
        given(nodeEntity.getEntityId()).willReturn(topicId);
        given(relatedSpecTopic.getDBId()).willReturn(secondTopicId);
        given(relatedNodeEntity.getEntityId()).willReturn(secondTopicId);
        given(relatedNode.getEntityId()).willReturn(secondTopicId);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated node collection should be empty
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the topic entity relationships is still the same
        assertThat(relatedToNodes.size(), is(1));
        assertThat(relatedToNodes.getUnchangedItems().size(), is(1));
        // and the topic didn't have the relationships set
        verify(relatedNodeEntity, never()).setRelatedToNodes(any(UpdateableCollectionWrapper.class));
    }

    @Test
    public void shouldMergeTopicRelationshipsWithExistingRelationship() {
        // Given a spec topic that has a relationship that already exists
        final Relationship relationship = new TopicRelationship(specTopic, relatedSpecTopic, RelationshipType.LINKLIST);
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        // and the node has a matching relationship that has a different sort
        relatedToNodes.addItem(relatedNode);
        given(relatedNode.getRelationshipSort()).willReturn(0);
        given(relatedNode.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_LINK_LIST);
        // and the related node exists in the map
        nodeMap.put(relatedSpecTopic, relatedNodeEntity);
        // and the matching nodes are in the map
        nodeMap.put(specTopic, nodeEntity);
        // and the spec topics have the db id's set
        given(specTopic.getDBId()).willReturn(topicId);
        given(nodeEntity.getEntityId()).willReturn(topicId);
        given(relatedSpecTopic.getDBId()).willReturn(secondTopicId);
        given(relatedNodeEntity.getEntityId()).willReturn(secondTopicId);
        given(relatedNode.getEntityId()).willReturn(secondTopicId);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the updated related node was added to the collection
        assertThat(relatedToNodes.size(), is(1));
        assertThat(relatedToNodes.getUpdateItems().size(), is(1));
        assertSame(relatedToNodes.getItems().get(0), relatedNode);
        // and the related node has the sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        // and the related node still has the same relationship type
        verify(relatedNode, never()).setRelationshipType(anyInt());
    }

    @Test
    public void shouldMergeTopicRelationshipsWithMultipleExistingRelationships() {
        // Given a spec topic that has a relationship that already exists
        final Relationship relationship = new TopicRelationship(specTopic, relatedSpecTopic, RelationshipType.LINKLIST);
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        // and the relatedToNodes collection has some related nodes
        relatedToNodes.addItem(relatedNode2);
        relatedToNodes.addItem(relatedNode);
        // and the node has a matching relationship that has a different sort
        given(relatedNode.getRelationshipSort()).willReturn(0);
        given(relatedNode.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_LINK_LIST);
        // and the other relationship has some data
        given(relatedNode2.getRelationshipSort()).willReturn(3);
        given(relatedNode2.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_PREREQUISITE);
        // and the related node exists in the map
        nodeMap.put(relatedSpecTopic, relatedNodeEntity);
        // and the matching nodes are in the map
        nodeMap.put(specTopic, nodeEntity);
        // and the spec topics have the db id's set
        given(specTopic.getDBId()).willReturn(topicId);
        given(nodeEntity.getEntityId()).willReturn(topicId);
        given(relatedSpecTopic.getDBId()).willReturn(secondTopicId);
        given(relatedNodeEntity.getEntityId()).willReturn(secondTopicId);
        given(relatedNode.getEntityId()).willReturn(secondTopicId);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the updated related node was added to the collection
        assertThat(relatedToNodes.size(), is(2));
        assertThat(relatedToNodes.getUpdateItems().size(), is(1));
        assertSame(relatedToNodes.getUpdateItems().get(0), relatedNode);
        // and the other relationship should be set for removal
        assertThat(relatedToNodes.getRemoveItems().size(), is(1));
        assertSame(relatedToNodes.getRemoveItems().get(0), relatedNode2);
        // and the related node has the sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        // and the related node still has the same relationship type
        verify(relatedNode, never()).setRelationshipType(anyInt());
    }

    @Test
    public void shouldMergeTargetTopicRelationshipsWithExistingRelationship() {
        // Given a spec topic that has a relationship that already exists
        final Relationship relationship = new TargetRelationship(specTopic, relatedSpecTopic, RelationshipType.LINKLIST);
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        // and the node has a matching relationship that has a different sort
        relatedToNodes.addItem(relatedNode);
        given(relatedNode.getRelationshipSort()).willReturn(0);
        given(relatedNode.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_LINK_LIST);
        // and the related node exists in the map
        nodeMap.put(relatedSpecTopic, relatedNodeEntity);
        // and the matching nodes are in the map
        nodeMap.put(specTopic, nodeEntity);
        // and the spec topics have the db id's set
        given(specTopic.getTargetId()).willReturn("T" + topicId);
        given(nodeEntity.getTargetId()).willReturn("T" + topicId);
        given(relatedSpecTopic.getTargetId()).willReturn("T" + secondTopicId);
        given(relatedNodeEntity.getTargetId()).willReturn("T" + secondTopicId);
        given(relatedNode.getTargetId()).willReturn("T" + secondTopicId);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the updated related node was added to the collection
        assertThat(relatedToNodes.size(), is(1));
        assertThat(relatedToNodes.getUpdateItems().size(), is(1));
        assertSame(relatedToNodes.getItems().get(0), relatedNode);
        // and the related node has the sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        // and the related node still has the same relationship type
        verify(relatedNode, never()).setRelationshipType(anyInt());
    }

    @Test
    public void shouldMergeTargetLevelRelationshipsWithExistingRelationship() {
        // Given a spec topic that has a relationship that already exists
        final Relationship relationship = new TargetRelationship(specTopic, relatedLevel, RelationshipType.LINKLIST);
        given(specTopic.getRelationships()).willReturn(Arrays.asList(relationship));
        // and a matching node
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        // and the node has a matching relationship that has a different sort
        relatedToNodes.addItem(relatedNode);
        given(relatedNode.getRelationshipSort()).willReturn(0);
        given(relatedNode.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_LINK_LIST);
        // and the related node exists in the map
        nodeMap.put(relatedLevel, relatedNodeEntity);
        // and the matching nodes are in the map
        nodeMap.put(specTopic, nodeEntity);
        // and the spec topics have the db id's set
        given(specTopic.getTargetId()).willReturn("T" + topicId);
        given(nodeEntity.getTargetId()).willReturn("T" + topicId);
        given(relatedLevel.getTargetId()).willReturn("T" + secondTopicId);
        given(relatedNodeEntity.getTargetId()).willReturn("T" + secondTopicId);
        given(relatedNode.getTargetId()).willReturn("T" + secondTopicId);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the updated related node was added to the collection
        assertThat(relatedToNodes.size(), is(1));
        assertThat(relatedToNodes.getUpdateItems().size(), is(1));
        assertSame(relatedToNodes.getItems().get(0), relatedNode);
        // and the related node has the sort set
        verify(relatedNode, times(1)).setRelationshipSort(1);
        // and the related node still has the same relationship type
        verify(relatedNode, never()).setRelationshipType(anyInt());
    }

    @Test
    public void shouldRemoveAllExistingRelationshipsWhenSpecTopicHasNoRelationships() {
        // Given a spec topic that has no relationships
        given(specTopic.getRelationships()).willReturn(new LinkedList<Relationship>());
        // and a matching node
        given(nodeEntity.getRelatedToNodes()).willReturn(relatedToNodes);
        // and the relatedToNodes collection has some related nodes
        relatedToNodes.addItem(relatedNode);
        relatedToNodes.addItem(relatedNode2);
        // and the relationships have some data
        given(relatedNode.getRelationshipSort()).willReturn(1);
        given(relatedNode.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_LINK_LIST);
        given(relatedNode2.getRelationshipSort()).willReturn(2);
        given(relatedNode2.getRelationshipType()).willReturn(CommonConstants.CS_RELATIONSHIP_PREREQUISITE);
        // and the matching nodes are in the map
        nodeMap.put(specTopic, nodeEntity);

        // When merging the topic relationships
        try {
            processor.mergeTopicRelationships(nodeMap, providerFactory);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes should hold the nodeEntity
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), nodeEntity);
        // and the node had the relationships set
        verify(nodeEntity, times(1)).setRelatedToNodes(relatedToNodes);
        // and the existing relationships should have been set for removal
        assertThat(relatedToNodes.size(), is(2));
        assertThat(relatedToNodes.getRemoveItems().size(), is(2));
        assertTrue(relatedToNodes.getRemoveItems().contains(relatedNode));
        assertTrue(relatedToNodes.getRemoveItems().contains(relatedNode2));
    }
}