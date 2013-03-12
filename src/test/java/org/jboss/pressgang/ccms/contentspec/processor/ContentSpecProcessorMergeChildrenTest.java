package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
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
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.CollectionWrapperMock;
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

    CollectionWrapperMock<CSNodeWrapper> childrenNodes;
    UpdateableCollectionWrapperMock<CSNodeWrapper> updatedChildrenNodes;
    Map<SpecNode, CSNodeWrapper> nodeMap;

    @Before
    public void setUpCollections() {
        childrenNodes = new CollectionWrapperMock<CSNodeWrapper>();
        updatedChildrenNodes = new UpdateableCollectionWrapperMock<CSNodeWrapper>();
        nodeMap = new HashMap<SpecNode, CSNodeWrapper>();

        when(contentSpecNodeProvider.newCSNode()).thenReturn(newCSNode, newCSNode2, newCSNode3);
    }

    @Test
    public void shouldSetPreviousNextNodeForNewNodes() throws Exception {
        final List<Node> childNodes = new LinkedList<Node>();
        // Given a new Content Spec Topic and Level node
        final SpecTopic specTopic = make(
                a(SpecTopicMaker.SpecTopic, with(SpecTopicMaker.uniqueId, "L-" + id), with(SpecTopicMaker.id, topicId.toString()),
                        with(SpecTopicMaker.title, title), with(SpecTopicMaker.revision, (Integer) null)));
        childNodes.add(specTopic);
        final Level specLevel = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(specLevel);
        // and creating the new node succeeded
        given(contentSpecNodeProvider.createCSNode(eq(newCSNode))).willReturn(newCSNode);
        given(contentSpecNodeProvider.createCSNode(eq(newCSNode2))).willReturn(newCSNode2);
        // and the level and node has a id set
        given(newCSNode.getId()).willReturn(id);
        given(newCSNode2.getId()).willReturn(secondId);
        // and some methods should return null, since mockito will return 0 for primitive wrappers
        given(newCSNode.getNextNodeId()).willReturn(null);
        given(newCSNode.getPreviousNodeId()).willReturn(null);
        given(newCSNode2.getNextNodeId()).willReturn(null);
        given(newCSNode2.getPreviousNodeId()).willReturn(null);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then check that two nodes are updated
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(2));
        // and the level node has the spec topic set as it's previous
        verify(newCSNode2, times(1)).setPreviousNodeId(id);
        verify(newCSNode2, never()).setNextNodeId(anyInt());
        // and the spec topic has the level as it's next
        verify(newCSNode, times(1)).setNextNodeId(secondId);
        verify(newCSNode, never()).setPreviousNodeId(anyInt());
    }

    @Test
    public void shouldSetPreviousNextNodeForMultipleNewNodes() throws Exception {
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
        // and creating the new node succeeded
        given(contentSpecNodeProvider.createCSNode(eq(newCSNode))).willReturn(newCSNode);
        given(contentSpecNodeProvider.createCSNode(eq(newCSNode2))).willReturn(newCSNode2);
        given(contentSpecNodeProvider.createCSNode(eq(newCSNode3))).willReturn(newCSNode3);
        // and the nodes has a id set
        given(newCSNode.getId()).willReturn(id);
        given(newCSNode2.getId()).willReturn(secondId);
        given(newCSNode3.getId()).willReturn(thirdId);
        // and some methods should return null, since mockito will return 0 for primitive wrappers
        given(newCSNode.getNextNodeId()).willReturn(null);
        given(newCSNode.getPreviousNodeId()).willReturn(null);
        given(newCSNode2.getNextNodeId()).willReturn(null);
        given(newCSNode2.getPreviousNodeId()).willReturn(null);
        given(newCSNode3.getNextNodeId()).willReturn(null);
        given(newCSNode3.getPreviousNodeId()).willReturn(null);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then check that two nodes are updated
        assertThat(updatedChildrenNodes.size(), is(3));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(3));
        // and the level node has the spec topic set as it's previous
        verify(newCSNode3, times(1)).setPreviousNodeId(secondId);
        verify(newCSNode3, never()).setNextNodeId(anyInt());
        // and the spec topic has the level as it's next
        verify(newCSNode2, times(1)).setNextNodeId(thirdId);
        verify(newCSNode2, times(1)).setPreviousNodeId(id);
        // and the metadata has the spec topic as it's next
        verify(newCSNode, times(1)).setNextNodeId(secondId);
        verify(newCSNode, never()).setPreviousNodeId(anyInt());
    }

    @Test
    public void shouldSetPreviousNextNodeForMultipleExistingUnorderedNodes() throws Exception {
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
        childrenNodes.addItem(newCSNode2);
        childrenNodes.addItem(newCSNode);
        childrenNodes.addItem(newCSNode3);
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
        given(newCSNode.getNextNodeId()).willReturn(null);
        given(newCSNode.getPreviousNodeId()).willReturn(thirdId);
        given(newCSNode2.getNextNodeId()).willReturn(thirdId);
        given(newCSNode2.getPreviousNodeId()).willReturn(null);
        given(newCSNode3.getNextNodeId()).willReturn(id);
        given(newCSNode3.getPreviousNodeId()).willReturn(secondId);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the get new node should not have been called
        verify(contentSpecNodeProvider, never()).newCSNode();
        // and check that two nodes are updated
        assertThat(updatedChildrenNodes.size(), is(3));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(3));
        // and the level node has the spec topic set as it's previous and nothing as the next node
        verify(newCSNode3, never()).setPreviousNodeId(anyInt());
        verify(newCSNode3, times(1)).setNextNodeId(null);
        // and the spec topic has the level as it's next and the meta data as the previous node
        verify(newCSNode2, never()).setNextNodeId(anyInt());
        verify(newCSNode2, times(1)).setPreviousNodeId(id);
        // and the metadata has the spec topic as it's next
        verify(newCSNode, times(1)).setNextNodeId(secondId);
        verify(newCSNode, times(1)).setPreviousNodeId(null);
    }

    @Test
    public void shouldIgnoreTextNodes() {
        final List<Node> childNodes = new LinkedList<Node>();
        // Given a text node
        final TextNode node = new TextNode("\n");
        childNodes.add(node);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the updated nodes list should be empty
        assertThat(updatedChildrenNodes.size(), is(0));
    }
}
