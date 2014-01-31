package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeChildrenTopicTest extends ContentSpecProcessorTest {
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
    @Mock CSNodeWrapper newCSNode;
    @Mock CSNodeWrapper foundCSNode;

    List<CSNodeWrapper> childrenNodes;
    UpdateableCollectionWrapperMock<CSNodeWrapper> updatedChildrenNodes;
    Map<SpecNode, CSNodeWrapper> nodeMap;

    @Before
    public void setUpCollections() {
        childrenNodes = new LinkedList<CSNodeWrapper>();
        updatedChildrenNodes = new UpdateableCollectionWrapperMock<CSNodeWrapper>();
        nodeMap = new HashMap<SpecNode, CSNodeWrapper>();

        when(contentSpecNodeProvider.newCSNode()).thenReturn(newCSNode);
        when(contentSpecNodeProvider.newCSNodeCollection()).thenReturn(new UpdateableCollectionWrapperMock<CSNodeWrapper>());

        // and the found topic is already assigned to the content spec
        when(foundCSNode.getContentSpec()).thenReturn(contentSpecWrapper);

        // Make sure the return type is always a topic
        when(foundCSNode.getNodeType()).thenReturn(0);
    }

    @Test
    public void shouldCreateNewTopicNode() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec topic that doesn't exist
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(1));
        // and the basic details are correct
        verifyBaseNewTopic(newCSNode);
        // and the topic revision wasn't set
        verify(newCSNode, never()).setEntityRevision(anyInt());
    }

    @Test
    public void shouldCreateNewTopicNodeWithRevision() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec topic that doesn't exist
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, topicRevision)));
        childNodes.add(specTopic);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(1));
        // and the basic details are correct
        verifyBaseNewTopic(newCSNode);
        // and the topic revision was set
        verify(newCSNode, times(1)).setEntityRevision(topicRevision);
    }

    @Test
    public void shouldCreateNewTopicNodeWithTarget() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec topic that doesn't exist
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.targetId, "T-" + randomAlphaString)));
        childNodes.add(specTopic);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(1));
        // and the basic details are correct
        verifyBaseNewTopic(newCSNode);
        // and the topic revision wasn't set
        verify(newCSNode, never()).setEntityRevision(anyInt());
        // and the target was set
        verify(newCSNode, times(1)).setTargetId("T-" + randomAlphaString);
    }

    @Test
    public void shouldCreateNewTopicNodeWithCondition() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec topic that doesn't exist
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.condition, randomAlphaString)));
        childNodes.add(specTopic);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(1));
        // and the basic details are correct
        verifyBaseNewTopic(newCSNode);
        // and the topic revision wasn't set
        verify(newCSNode, never()).setEntityRevision(anyInt());
        // and the condition was set
        verify(newCSNode, times(1)).setCondition(randomAlphaString);
    }

    @Test
    public void shouldMergeTopicWithDBIdsWithNoChange() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should not exist in the updated collection, since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the value of the other node wasn't touched
        verify(newCSNode, never()).setAdditionalText(anyString());
        // and the main details haven't changed
        verifyBaseExistingTopic(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithoutDBIdsButIsTheSameTopicFromParserWithNoChange() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should not exist in the updated collection, since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingTopic(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithoutDBIdsButIsTheSameTopicWithNoChange() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, (String) null), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should not exist in the updated collection, since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingTopic(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithDBIdsWhenMultipleTopicsWithNoChange() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        final SpecTopic specTopic2 = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, secondId.toString()), with(SpecTopicMaker.id, secondTopicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        childNodes.add(specTopic2);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getNextNode()).willReturn(newCSNode);
        // And another non matching child node exists in the database
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(newCSNode.getTitle()).willReturn(title);
        given(newCSNode.getEntityId()).willReturn(secondTopicId);
        given(newCSNode.getId()).willReturn(secondId);
        given(newCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(newCSNode);
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);
        // and the two nodes also exist in the content spec children collection
        updatedChildrenNodes.addItem(newCSNode);
        updatedChildrenNodes.addItem(foundCSNode);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the other node should be set for removal, by still being in the "childrenNodes" list
        assertThat(childrenNodes.size(), is(0));
        // and the both nodes should be untouched
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUnchangedItems().size(), is(2));
        // and the main details haven't changed
        verifyBaseExistingTopic(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndIgnoreSameTitle() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingTopic(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndDifferentTitle() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(randomAlphaString);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the title was changed
        verify(foundCSNode, times(1)).setTitle(title);
        // and the node type hasn't changed
        verify(foundCSNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(foundCSNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node topic id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(foundCSNode, never()).setEntityRevision(anyInt());
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndIgnoreSameRevision() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, topicRevision)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(topicRevision);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingTopic(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndDifferentRevision() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, topicRevision)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(secondTopicRevision);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the title wasn't changed
        verify(foundCSNode, never()).setTitle(title);
        // and the node type hasn't changed
        verify(foundCSNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(foundCSNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node topic id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the topic revision was set
        verify(foundCSNode, times(1)).setEntityRevision(topicRevision);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndRevisionRemoved() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(topicRevision);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the title wasn't changed
        verify(foundCSNode, never()).setTitle(title);
        // and the node type hasn't changed
        verify(foundCSNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(foundCSNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node topic id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the topic revision was set to null
        verify(foundCSNode, times(1)).setEntityRevision(null);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndIgnoreSameCondition() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.condition, secondTitle)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getCondition()).willReturn(secondTitle);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the base topic hasn't changed
        verifyBaseExistingTopic(foundCSNode);
        // and the topic condition wasn't changed
        verify(foundCSNode, never()).setCondition(anyString());
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndDifferentCondition() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.condition, secondTitle)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getCondition()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the base topic hasn't changed
        verifyBaseExistingTopic(foundCSNode);
        // and the topic condition was set
        verify(foundCSNode, times(1)).setCondition(secondTitle);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndConditionRemoved() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.condition, (String) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getCondition()).willReturn(randomAlphaString);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the the base topic hasn't changed
        verifyBaseExistingTopic(foundCSNode);
        // and the topic condition was set to null
        verify(foundCSNode, times(1)).setCondition(null);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndIgnoreSameTarget() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.targetId, secondTitle)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getTargetId()).willReturn(secondTitle);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the base topic hasn't changed
        verifyBaseExistingTopic(foundCSNode);
        // and the topic target wasn't changed
        verify(foundCSNode, never()).setTargetId(anyString());
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndDifferentTarget() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.targetId, secondTitle)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getTargetId()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the node type hasn't changed
        verify(foundCSNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(foundCSNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node title wasn't changed
        verify(foundCSNode, never()).setTitle(anyString());
        // and the node topic id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(foundCSNode, never()).setEntityRevision(anyInt());
        // and the topic target was set
        verify(foundCSNode, times(1)).setTargetId(secondTitle);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndTargetRemoved() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.targetId, (String) null)));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getTargetId()).willReturn(randomAlphaString);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the node type hasn't changed
        verify(foundCSNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(foundCSNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node title wasn't changed
        verify(foundCSNode, never()).setTitle(anyString());
        // and the node topic id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(foundCSNode, never()).setEntityRevision(anyInt());
        // and the topic condition was set to null
        verify(foundCSNode, times(1)).setTargetId(null);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndIgnoreInternalTarget() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec topic that was created from a DB entity
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, id.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null),
                        with(SpecTopicMaker.targetId, "T-" + id + "01")));
        childNodes.add(specTopic);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(topicId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getTargetId()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the base topic hasn't changed
        verifyBaseExistingTopic(foundCSNode);
        // and the topic target wasn't set
        verify(foundCSNode, never()).setTargetId(anyString());
    }

    protected void verifyBaseNewTopic(final CSNodeWrapper topicNode) {
        // and the node has the Spec Topic type set
        verify(topicNode, times(1)).setNodeType(CommonConstants.CS_NODE_TOPIC);
        // and the parent node should be null
        verify(topicNode, never()).setParent(any(CSNodeWrapper.class));
        // and the node had the title set
        verify(topicNode, times(1)).setTitle(title);
        // and the node topic id was set
        verify(topicNode, times(1)).setEntityId(topicId);
        // and the topic was added to the node collection
        assertTrue(nodeMap.containsValue(topicNode));
    }

    protected void verifyBaseExistingTopic(final CSNodeWrapper topicNode) {
        // and the node type hasn't changed
        verify(topicNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(topicNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node title wasn't changed
        verify(topicNode, never()).setTitle(anyString());
        // and the node target wasn't changed
        verify(topicNode, never()).setTargetId(anyString());
        // and the node topic id wasn't set
        verify(topicNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(topicNode, never()).setEntityRevision(anyInt());
        // and the topic was added to the node collection
        assertTrue(nodeMap.containsValue(topicNode));
    }
}
