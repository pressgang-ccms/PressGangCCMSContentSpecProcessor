/*
  Copyright 2011-2014 Red Hat

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
import org.jboss.pressgang.ccms.contentspec.Comment;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeChildrenCommentTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String comment;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String comment2;
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

        // and the found comment is already assigned to the content spec
        when(foundCSNode.getContentSpec()).thenReturn(contentSpecWrapper);
    }

    @Test
    public void shouldCreateNewCommentNode() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec comment that doesn't exist
        final Comment comment = new Comment(this.comment);
        childNodes.add(comment);
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
        verifyBaseNewComment(newCSNode);
    }

    @Test
    public void shouldMergeCommentWithDBIds() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        comment.setUniqueId(id.toString());
        childNodes.add(comment);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn(this.comment2);
        given(foundCSNode.getId()).willReturn(id);
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
        assertSame(updatedChildrenNodes.getItems().get(0), foundCSNode);
        // and the value was set
        verify(foundCSNode, times(1)).setTitle("# " + this.comment);
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    @Test
    public void shouldIgnoreMergeCommentWithoutDBIdsWhenTextIsTheSame() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        final Comment comment2 = new Comment(this.comment2);
        childNodes.add(comment);
        childNodes.add(comment2);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn("# " + this.comment);
        given(foundCSNode.getNextNode()).willReturn(newCSNode);
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(newCSNode.getTitle()).willReturn("# " + this.comment2);
        given(newCSNode.getId()).willReturn(id);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        childrenNodes.add(newCSNode);
        updatedChildrenNodes.addItem(foundCSNode);
        updatedChildrenNodes.addItem(newCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUnchangedItems().size(), is(2));
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    @Test
    public void shouldIgnoreMergeCommentWithDBIdsWhenTextIsTheSame() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        comment.setUniqueId(id.toString());
        childNodes.add(comment);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn("# " + this.comment);
        given(foundCSNode.getId()).willReturn(id);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        updatedChildrenNodes.addItem(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then no nodes should have been updated
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUnchangedItems().size(), is(1));
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    @Test
    public void shouldMergeCommentWithoutDBIdsWhenMultipleComments() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        final Comment comment2 = new Comment(this.comment2);
        childNodes.add(comment);
        childNodes.add(comment2);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn("# " + this.comment);
        given(foundCSNode.getNextNode()).willReturn(newCSNode);
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(newCSNode.getTitle()).willReturn("# " + randomAlphaString);
        given(newCSNode.getId()).willReturn(id);
        given(newCSNode.getContentSpec()).willReturn(contentSpecWrapper);
        // and is in the child nodes collection (in reverse order)
        childrenNodes.add(newCSNode);
        childrenNodes.add(foundCSNode);
        updatedChildrenNodes.addItem(newCSNode);
        updatedChildrenNodes.addItem(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the found node should be unchanged
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUnchangedItems().size(), is(1));
        // and the new node be updated
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertTrue(updatedChildrenNodes.getUpdateItems().contains(newCSNode));
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    @Test
    public void shouldMergeCommentWithDBIdsWhenMultipleComments() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec comment that was created from a DB entity
        final Comment comment = new Comment(this.comment);
        comment.setUniqueId(id.toString());
        childNodes.add(comment);
        // and a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_COMMENT);
        given(foundCSNode.getTitle()).willReturn("# " + this.comment);
        given(foundCSNode.getId()).willReturn(id);
        // and another node exists that won't match
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_META_DATA);
        given(newCSNode.getTitle()).willReturn(randomAlphaString);
        // and is in the child nodes collection
        childrenNodes.add(newCSNode);
        childrenNodes.add(foundCSNode);
        updatedChildrenNodes.addItem(newCSNode);
        updatedChildrenNodes.addItem(foundCSNode);
        // and the content spec will return a collection
        given(contentSpecWrapper.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, null, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should not exist in the updated collection, since nothing changed
        assertThat(updatedChildrenNodes.size(), is(2));
        // and the other node should be set for removal, by still being in the "childrenNodes" list
        assertThat(childrenNodes.size(), is(1));
        assertTrue(childrenNodes.contains(newCSNode));
        // and the value of the other node wasn't touched
        verify(newCSNode, never()).setTitle(anyString());
        // and the main details haven't changed
        verifyBaseExistingComment(foundCSNode);
    }

    protected void verifyBaseNewComment(final CSNodeWrapper commentNode) {
        // and the node has the Spec Topic type set
        verify(commentNode, times(1)).setNodeType(CommonConstants.CS_NODE_COMMENT);
        // and the parent node should be null
        verify(commentNode, never()).setParent(any(CSNodeWrapper.class));
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
