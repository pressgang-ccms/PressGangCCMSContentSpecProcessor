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

package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.KeyValueNode;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.TextNode;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.LevelMaker;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.SpecTopicMaker;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.CSRelatedNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeChildrenTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer topicId;
    @Arbitrary Integer secondId;
    @Arbitrary Integer thirdId;
    @Arbitrary String title;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphaString;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock CSNodeWrapper newCSNode;
    @Mock CSNodeWrapper newCSNode2;
    @Mock CSNodeWrapper newCSNode3;
    @Mock CSNodeWrapper foundCSNode;
    @Mock CSRelatedNodeWrapper relatedNodeWrapper;

    List<CSNodeWrapper> childrenNodes;
    UpdateableCollectionWrapperMock<CSNodeWrapper> updatedChildrenNodes;
    UpdateableCollectionWrapperMock<CSRelatedNodeWrapper> relatedNodes;
    Map<SpecNode, CSNodeWrapper> nodeMap;

    @Before
    public void setUpCollections() {
        childrenNodes = new LinkedList<CSNodeWrapper>();
        updatedChildrenNodes = new UpdateableCollectionWrapperMock<CSNodeWrapper>();
        relatedNodes = new UpdateableCollectionWrapperMock<CSRelatedNodeWrapper>();
        nodeMap = new HashMap<SpecNode, CSNodeWrapper>();

        when(contentSpecNodeProvider.newCSNode()).thenReturn(newCSNode, newCSNode2, newCSNode3);
        when(contentSpecNodeProvider.newCSNodeCollection()).thenReturn(new UpdateableCollectionWrapperMock<CSNodeWrapper>());
    }

    @Test
    public void shouldSetNextNodeForNewNodes() throws Exception {
        final List<Node> childNodes = new LinkedList<Node>();
        // Given a new Content Spec Topic and Level node
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        final Level specLevel = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(specLevel);
        // and the level and node have no id since they are new
        given(newCSNode.getId()).willReturn(null);
        given(newCSNode2.getId()).willReturn(null);
        // and some methods should return null
        given(newCSNode.getNextNode()).willReturn(null);
        given(newCSNode2.getNextNode()).willReturn(null);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then check that two nodes are updated
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(2));
        // and the collection was reset
        verify(contentSpecWrapper, times(1)).setChildren(updatedChildrenNodes);
        // and the level node has the spec topic set as it's previous
        verify(newCSNode2, never()).setNextNode(any(CSNodeWrapper.class));
        // and the spec topic has the level as it's next
        verify(newCSNode, times(1)).setNextNode(newCSNode2);
    }

    @Test
    public void shouldSetNextNodeForMultipleNewNodes() throws Exception {
        final List<Node> childNodes = new LinkedList<Node>();
        // Given a new MetaData, Content Spec Topic and Level node
        final KeyValueNode<String> metaData = new KeyValueNode<String>("Title", randomAlphaString);
        childNodes.add(metaData);
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        final Level specLevel = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(specLevel);
        // and the new nodes have no id
        given(newCSNode.getId()).willReturn(null);
        given(newCSNode2.getId()).willReturn(null);
        given(newCSNode3.getId()).willReturn(null);
        // and some methods should return null, since mockito will return 0 for primitive wrappers
        given(newCSNode.getNextNode()).willReturn(null);
        given(newCSNode2.getNextNode()).willReturn(null);
        given(newCSNode3.getNextNode()).willReturn(null);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then check that two nodes are updated
        assertThat(updatedChildrenNodes.size(), is(3));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(3));
        // and the collection was reset
        verify(contentSpecWrapper, times(1)).setChildren(updatedChildrenNodes);
        // and the level node has the spec topic set as it's previous
        verify(newCSNode3, never()).setNextNode(any(CSNodeWrapper.class));
        // and the spec topic has the level as it's next
        verify(newCSNode2, times(1)).setNextNode(newCSNode3);
        // and the metadata has the spec topic as it's next
        verify(newCSNode, times(1)).setNextNode(newCSNode2);
    }

    @Test
    public void shouldSetNextNodeForMultipleExistingUnorderedNodes() throws Exception {
        final List<Node> childNodes = new LinkedList<Node>();
        // Given a new MetaData, Content Spec Topic and Level node
        final KeyValueNode<String> metaData = new KeyValueNode<String>("Title", randomAlphaString);
        childNodes.add(metaData);
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, secondId.toString()), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        final Level specLevel = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.uniqueId, thirdId.toString())));
        childNodes.add(specLevel);
        // and we have a list of existing nodes
        childrenNodes.add(newCSNode2);
        childrenNodes.add(newCSNode);
        childrenNodes.add(newCSNode3);
        // and the nodes has a id set
        given(newCSNode.getId()).willReturn(id);
        given(newCSNode2.getId()).willReturn(secondId);
        given(newCSNode3.getId()).willReturn(thirdId);
        // and the meta data has a title
        given(newCSNode.getTitle()).willReturn("Title");
        // and the nodes have the right types
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_META_DATA);
        given(newCSNode2.getNodeType()).willReturn(CommonConstants.CS_NODE_TOPIC);
        given(newCSNode3.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        // and the previous/next already exists
        given(newCSNode.getNextNode()).willReturn(null);
        given(newCSNode2.getNextNode()).willReturn(newCSNode3);
        given(newCSNode3.getNextNode()).willReturn(newCSNode);
        // and the nodes belong to the content spec
        given(newCSNode.getContentSpec()).willReturn(contentSpecWrapper);
        given(newCSNode2.getContentSpec()).willReturn(contentSpecWrapper);
        given(newCSNode3.getContentSpec()).willReturn(contentSpecWrapper);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the get new node should not have been called
        verify(contentSpecNodeProvider, never()).newCSNode();
        // and the collection was reset
        verify(contentSpecWrapper, times(1)).setChildren(updatedChildrenNodes);
        // and check that two nodes are updated
        assertThat(updatedChildrenNodes.size(), is(3));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(3));
        // and the level node has the spec topic set as it's previous and nothing as the next node
        verify(newCSNode3, times(1)).setNextNode(null);
        // and the spec topic has the level as it's next and the meta data as the previous node
        verify(newCSNode2, never()).setNextNode(any(CSNodeWrapper.class));
        // and the metadata has the spec topic as it's next
        verify(newCSNode, times(1)).setNextNode(newCSNode2);
    }

    @Test
    public void shouldIgnoreTextNodes() {
        final List<Node> childNodes = new LinkedList<Node>();
        // Given a text node
        final TextNode node = new TextNode("\n");
        childNodes.add(node);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes list should be empty
        assertThat(updatedChildrenNodes.size(), is(0));
    }

    @Test
    public void shouldRemoveNodesAndIncomingRelationshipsThatAreNoLongerUsed() {
        // Given no nodes
        final List<Node> childNodes = new LinkedList<Node>();
        // and a node exists in the current children collection, that has incoming relationships
        childrenNodes.add(newCSNode);
        given(newCSNode.getRelatedFromNodes()).willReturn(relatedNodes);
        relatedNodes.addItem(relatedNodeWrapper);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes list should have the exiting node marked for removal
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getRemoveItems().size(), is(1));
        assertEquals(updatedChildrenNodes.getRemoveItems().get(0), newCSNode);
        // and the incoming relationships have been removed
        assertThat(relatedNodes.size(), is(1));
        assertThat(relatedNodes.getRemoveItems().size(), is(1));
        assertEquals(relatedNodes.getRemoveItems().get(0), relatedNodeWrapper);
    }
}
