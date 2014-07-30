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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
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
import org.jboss.pressgang.ccms.contentspec.FileList;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.FileListMaker;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeChildrenFileListTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer secondId;
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

        // and the found level is already assigned to the content spec
        when(foundCSNode.getContentSpec()).thenReturn(contentSpecWrapper);
    }

    @Test
    public void shouldCreateNewFileListNode() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec file list that doesn't exist
        final FileList fileList = make(a(FileListMaker.FileList));
        childNodes.add(fileList);
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
        verifyBaseNewFileList(newCSNode);
    }

    @Test
    public void shouldMergeLevelWithDBIdsWithNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file list that was created from a DB entity
        final FileList fileList = make(a(FileListMaker.FileList, with(FileListMaker.uniqueId, id.toString())));
        childNodes.add(fileList);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_META_DATA);
        given(foundCSNode.getTitle()).willReturn(CommonConstants.CS_FILE_TITLE);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        // and the base file list hasn't changed
        verifyBaseExistingFileList(foundCSNode);
    }

    @Test
    public void shouldMergeLevelWithoutDBIdsButIsTheSameLevelWithNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file list that was created from a DB entity
        final FileList fileList = make(a(FileListMaker.FileList));
        childNodes.add(fileList);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_META_DATA);
        given(foundCSNode.getTitle()).willReturn(CommonConstants.CS_FILE_TITLE);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        verifyBaseExistingFileList(foundCSNode);
    }

    protected void verifyBaseNewFileList(final CSNodeWrapper levelNode) {
        // and the node has the Spec Topic type set
        verify(levelNode, atLeast(1)).setNodeType(CommonConstants.CS_NODE_META_DATA);
        // and the parent node should be null
        verify(levelNode, never()).setParent(any(CSNodeWrapper.class));
        // and the node had the title set
        verify(levelNode, times(1)).setTitle(CommonConstants.CS_FILE_TITLE);
        // and the node topic id wasn't set
        verify(levelNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(levelNode, never()).setEntityRevision(anyInt());
    }

    protected void verifyBaseExistingFileList(final CSNodeWrapper levelNode) {
        // and the node type hasn't changed
        verify(levelNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(levelNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node title wasn't changed
        verify(levelNode, never()).setTitle(anyString());
        // and the node target wasn't changed
        verify(levelNode, never()).setTargetId(anyString());
        // and the node topic id wasn't set
        verify(levelNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(levelNode, never()).setEntityRevision(anyInt());
    }
}
