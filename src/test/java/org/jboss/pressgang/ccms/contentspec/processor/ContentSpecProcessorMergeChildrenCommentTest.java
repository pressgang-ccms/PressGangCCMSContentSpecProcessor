package org.jboss.pressgang.ccms.contentspec.processor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.Comment;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.CollectionWrapperMock;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeChildrenCommentTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String comment;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphaString;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock CSNodeWrapper newCSNode;
    @Mock CSNodeWrapper foundCSNode;

    CollectionWrapperMock<CSNodeWrapper> childrenNodes;
    UpdateableCollectionWrapperMock<CSNodeWrapper> updatedChildrenNodes;
    Map<SpecNode, CSNodeWrapper> nodeMap;

    @Before
    public void setUpCollections() {
        childrenNodes = new CollectionWrapperMock<CSNodeWrapper>();
        updatedChildrenNodes = new UpdateableCollectionWrapperMock<CSNodeWrapper>();
        nodeMap = new HashMap<SpecNode, CSNodeWrapper>();

        when(contentSpecNodeProvider.newCSNode()).thenReturn(newCSNode);

        // and the found comment is already assigned to the content spec
        when(foundCSNode.getContentSpec()).thenReturn(contentSpecWrapper);
    }

    @Test
    public void shouldCreateNewMetaDataNodeWithoutParent() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec comment that doesn't exist
        final Comment comment = new Comment(this.comment);
        childNodes.add(comment);
        // and creating the new node succeeded
        given(contentSpecNodeProvider.createCSNode(eq(newCSNode))).willReturn(newCSNode);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should not exist in the updated collection, since it base details are created to get an id
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the basic details are correct
        verifyBaseNewComment(newCSNode);
    }

    @Test
    public void shouldCreateNewMetaDataNodeWithParent() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec comment that doesn't exist
        final Comment comment = new Comment(this.comment);
        childNodes.add(comment);
        // and creating the new node succeeded
        given(contentSpecNodeProvider.createCSNode(eq(newCSNode))).willReturn(newCSNode);
        // and a parent node
        CSNodeWrapper parentNode = mock(CSNodeWrapper.class);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, parentNode, contentSpecWrapper, updatedChildrenNodes,
                    nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        // and the node has the Spec Topic type set
        verify(newCSNode, times(1)).setNodeType(CommonConstants.CS_NODE_COMMENT);
        // and the parent node should be null
        verify(newCSNode, times(1)).setParent(parentNode);
        // and the content spec was set
        verify(newCSNode, times(1)).setContentSpec(contentSpecWrapper);
        // and the node had the comment set
        verify(newCSNode, times(1)).setTitle("# " + this.comment);
        // and the node topic id was set
        verify(newCSNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(newCSNode, never()).setEntityRevision(anyInt());
    }

    @Test
    public void shouldMergeCommentWithDBIds() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        childNodes.add(comment);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn(this.comment);
        // and is in the child nodes collection
        childrenNodes.addItem(foundCSNode);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getItems().get(0), foundCSNode);
        // and the value was set
        verify(foundCSNode, times(1)).setTitle("# " + this.comment);
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    @Test
    public void shouldIgnoreMergeCommentWithDBIdsWhenTextIsTheSame() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        childNodes.add(comment);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn("# " + this.comment);
        given(foundCSNode.getPreviousNodeId()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.addItem(foundCSNode);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    @Test
    public void shouldMergeMetaDataWithDBIdsWhenMultipleMetaData() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec meta data that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        childNodes.add(comment);
        // and a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn(this.comment);
        // and another node exists that won't match
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_META_DATA);
        given(newCSNode.getTitle()).willReturn(randomAlphaString);
        // and is in the child nodes collection
        childrenNodes.addItem(newCSNode);
        childrenNodes.addItem(foundCSNode);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, updatedChildrenNodes, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the other node should be set for removal
        assertThat(updatedChildrenNodes.getRemoveItems().size(), is(1));
        // and the value was set
        verify(foundCSNode, times(1)).setTitle("# " + this.comment);
        // and the value of the other node wasn't touched
        verify(newCSNode, never()).setTitle(anyString());
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    protected void setUpNodeToReturnNulls(final CSNodeWrapper nodeMock) {
        when(nodeMock.getRevision()).thenReturn(null);
        when(nodeMock.getAdditionalText()).thenReturn(null);
        when(nodeMock.getEntityRevision()).thenReturn(null);
        when(nodeMock.getEntityId()).thenReturn(null);
        when(nodeMock.getTitle()).thenReturn(null);
        when(nodeMock.getTargetId()).thenReturn(null);
        when(nodeMock.getNextNodeId()).thenReturn(null);
        when(nodeMock.getPreviousNodeId()).thenReturn(null);
    }

    protected void verifyBaseNewComment(final CSNodeWrapper commentNode) {
        // and the node has the Spec Topic type set
        verify(commentNode, times(1)).setNodeType(CommonConstants.CS_NODE_COMMENT);
        // and the parent node should be null
        verify(commentNode, never()).setParent(any(CSNodeWrapper.class));
        // and the content spec was set
        verify(commentNode, times(1)).setContentSpec(contentSpecWrapper);
        // and the node had the comment set
        verify(commentNode, times(1)).setTitle("# " + comment);
        // and the node topic id wasn't set
        verify(commentNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(commentNode, never()).setEntityRevision(anyInt());
    }

    protected void verifyBaseExistingComment(final CSNodeWrapper commentNode) {
        // and the node type hasn't changed
        verify(commentNode, never()).setNodeType(anyInt());
        // and the parent node should be null
        verify(commentNode, never()).setParent(any(CSNodeWrapper.class));
        // and the content spec wasn't changed
        verify(commentNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node additional text wasn't changed
        verify(commentNode, never()).setAdditionalText(anyString());
        // and the node topic id wasn't set
        verify(commentNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(commentNode, never()).setEntityRevision(anyInt());
    }
}
