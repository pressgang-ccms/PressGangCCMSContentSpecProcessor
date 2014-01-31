package org.jboss.pressgang.ccms.contentspec.processor;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
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
import org.jboss.pressgang.ccms.contentspec.File;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.FileMaker;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeChildrenFileTest extends ContentSpecProcessorTest {
    @Arbitrary Integer fileId;
    @Arbitrary Integer secondFileId;
    @Arbitrary Integer id;
    @Arbitrary Integer secondId;
    @Arbitrary Integer fileRevision;
    @Arbitrary Integer secondFileRevision;
    @Arbitrary String title;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphaString;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock CSNodeWrapper fileListNode;
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

        // and the found file is already assigned to the file list
        when(foundCSNode.getParent()).thenReturn(fileListNode);
        when(foundCSNode.getContentSpec()).thenReturn(contentSpecWrapper);

        // and the parent file list node is setup
        when(fileListNode.getNodeType()).thenReturn(CommonConstants.CS_NODE_META_DATA);
        when(fileListNode.getContentSpec()).thenReturn(contentSpecWrapper);
    }

    @Test
    public void shouldCreateNewFileNode() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec file that doesn't exist
        final File file = make(a(FileMaker.File, with(FileMaker.title, title), with(FileMaker.id, fileId)));
        childNodes.add(file);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(1));
        // and the basic details are correct
        verifyBaseNewFile(newCSNode);
    }

    @Test
    public void shouldCreateNewFileNodeWithRevision() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec file that doesn't exist
        final File file = make(a(FileMaker.File, with(FileMaker.title, title), with(FileMaker.id, fileId), with(FileMaker.revision,
                fileRevision)));
        childNodes.add(file);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(1));
        // and the basic details are correct
        verifyBaseNewFile(newCSNode);
        // and the file revision was set
        verify(newCSNode, times(1)).setEntityRevision(fileRevision);
    }

    @Test
    public void shouldMergeFileWithDBIdsWithNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file that was created from a DB entity
        final File file = make(a(FileMaker.File, with(FileMaker.uniqueId, id.toString()), with(FileMaker.title, title), with(FileMaker.id,
                fileId), with(FileMaker.revision, fileRevision)));
        childNodes.add(file);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getEntityRevision()).willReturn(fileRevision);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should not exist in the updated collection, since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the base file hasn't changed
        verifyBaseExistingFile(foundCSNode);
    }

    @Test
    public void shouldMergeFileWithoutDBIdsButIsTheSameFileWithNoChange() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content file that was created from a DB entity
        final File file = make(
                a(FileMaker.File, with(FileMaker.id, fileId), with(FileMaker.title, title), with(FileMaker.revision,
                        (Integer) null)));
        childNodes.add(file);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should not exist in the updated collection, since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingFile(foundCSNode);
    }

    @Test
    public void shouldMergeFileWithDBIdsWhenMultipleFilesWithNoChange() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file that was created from a DB entity
        final File file = make(a(FileMaker.File, with(FileMaker.uniqueId, id.toString()), with(FileMaker.title, title), with(FileMaker.id,
                fileId), with(FileMaker.revision, (Integer) null)));
        final File file2 = make(a(FileMaker.File, with(FileMaker.uniqueId, secondId.toString()), with(FileMaker.title, title),
                with(FileMaker.id, secondFileId), with(FileMaker.revision, (Integer) null)));
        childNodes.add(file);
        childNodes.add(file2);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getNextNode()).willReturn(newCSNode);
        // And another non matching child node exists in the database
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(newCSNode.getTitle()).willReturn(title);
        given(newCSNode.getEntityId()).willReturn(secondFileId);
        given(newCSNode.getId()).willReturn(secondId);
        given(newCSNode.getEntityRevision()).willReturn(null);
        given(newCSNode.getParent()).willReturn(fileListNode);
        // and is in the child nodes collection
        childrenNodes.add(newCSNode);
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);
        // and the two nodes also exist in the content spec children collection
        updatedChildrenNodes.addItem(newCSNode);
        updatedChildrenNodes.addItem(foundCSNode);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then the other node should be set for removal, by still being in the "childrenNodes" list
        assertThat(childrenNodes.size(), is(0));
        // and the both nodes should be untouched
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUnchangedItems().size(), is(2));
        // and the main details haven't changed
        verifyBaseExistingFile(foundCSNode);
    }

    @Test
    public void shouldMergeFileWithDBIdsAndIgnoreSameTitle() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file that was created from a DB entity
        final File file = make(a(FileMaker.File, with(FileMaker.uniqueId, id.toString()), with(FileMaker.title, title), with(FileMaker.id,
                fileId), with(FileMaker.revision, (Integer) null)));
        childNodes.add(file);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingFile(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndDifferentTitle() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file that was created from a DB entity
        final File file = make(a(FileMaker.File, with(FileMaker.uniqueId, id.toString()), with(FileMaker.title, title), with(FileMaker.id,
                fileId), with(FileMaker.revision, (Integer) null)));
        childNodes.add(file);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(randomAlphaString);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
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
        // and the parent wasn't changed
        verify(foundCSNode, never()).setParent(any(CSNodeWrapper.class));
        // and the node topic id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(foundCSNode, never()).setEntityRevision(anyInt());
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndIgnoreSameRevision() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file that was created from a DB entity
        final File file = make(a(FileMaker.File, with(FileMaker.uniqueId, id.toString()), with(FileMaker.title, title), with(FileMaker.id,
                fileId), with(FileMaker.revision, fileRevision)));
        childNodes.add(file);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(fileRevision);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node shouldn't exist since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingFile(foundCSNode);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndDifferentRevision() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file that was created from a DB entity
        final File file = make(a(FileMaker.File, with(FileMaker.uniqueId, id.toString()), with(FileMaker.title, title), with(FileMaker.id,
                fileId), with(FileMaker.revision, fileRevision)));
        childNodes.add(file);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(secondFileRevision);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
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
        // and the parent wasn't changed
        verify(foundCSNode, never()).setParent(any(CSNodeWrapper.class));
        // and the node topic id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the topic revision was set
        verify(foundCSNode, times(1)).setEntityRevision(fileRevision);
    }

    @Test
    public void shouldMergeTopicWithDBIdsAndRevisionRemoved() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec file that was created from a DB entity
        final File file = make(a(FileMaker.File, with(FileMaker.uniqueId, id.toString()), with(FileMaker.title, title), with(FileMaker.id,
                fileId), with(FileMaker.revision, (Integer) null)));
        childNodes.add(file);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_FILE);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(fileId);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(fileRevision);
        // and is in the child nodes collection
        childrenNodes.add(foundCSNode);
        // and the file list will return a collection
        given(fileListNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, fileListNode, contentSpecWrapper, nodeMap);
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

    protected void verifyBaseNewFile(final CSNodeWrapper fileNode) {
        // and the node has the Spec Topic type set
        verify(fileNode, atLeast(1)).setNodeType(CommonConstants.CS_NODE_FILE);
        // and the parent node should be null
        verify(fileNode, never()).setParent(fileListNode);
        // and the node had the title set
        verify(fileNode, times(1)).setTitle(title);
        // and the node entity id was set
        verify(fileNode, times(1)).setEntityId(fileId);
    }

    protected void verifyBaseExistingFile(final CSNodeWrapper fileNode) {
        // and the node type hasn't changed
        verify(fileNode, never()).setNodeType(anyInt());
        // and the content spec wasn't changed
        verify(fileNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the parent wasn't changed
        verify(fileNode, never()).setParent(any(CSNodeWrapper.class));
        // and the node title wasn't changed
        verify(fileNode, never()).setTitle(anyString());
        // and the node target wasn't changed
        verify(fileNode, never()).setTargetId(anyString());
        // and the node entity id wasn't set
        verify(fileNode, never()).setEntityId(anyInt());
        // and the node entity revision wasn't set
        verify(fileNode, never()).setEntityRevision(anyInt());
    }
}
