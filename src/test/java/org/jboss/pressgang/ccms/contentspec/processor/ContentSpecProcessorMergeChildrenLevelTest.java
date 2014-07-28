/*
  Copyright 2011-2014 Red Hat

  This file is part of PresGang CCMS.

  PresGang CCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PresGang CCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with PresGang CCMS.  If not, see <http://www.gnu.org/licenses/>.
*/

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
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Node;
import org.jboss.pressgang.ccms.contentspec.SpecNode;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.test.makers.shared.LevelMaker;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.mocks.UpdateableCollectionWrapperMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ContentSpecProcessorMergeChildrenLevelTest extends ContentSpecProcessorTest {
    @Arbitrary Integer id;
    @Arbitrary Integer secondId;
    @Arbitrary String title;
    @Arbitrary String secondTitle;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphaString;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock CSNodeWrapper levelNode;
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
    public void shouldCreateNewLevelNode() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec level that doesn't exist
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(level);
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
        verifyBaseNewLevel(newCSNode);
    }

    @Test
    public void shouldCreateNewChildLevelNode() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec level that doesn't exist
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(level);
        // and the level will return a collection
        given(levelNode.getChildren()).willReturn(updatedChildrenNodes);

        // When merging the children nodes
        try {
            processor.mergeChildren(childNodes, childrenNodes, providerFactory, levelNode, contentSpecWrapper, nodeMap);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a new node should exist in the updated collection
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getAddItems().size(), is(1));
        // and the basic details are correct
        verifyBaseNewLevel(newCSNode);
    }

    @Test
    public void shouldCreateNewLevelNodeWithTarget() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec level that doesn't exist
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.targetId, "T-" + randomAlphaString)));
        childNodes.add(level);
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
        verifyBaseNewLevel(newCSNode);
        // and the target was set
        verify(newCSNode, times(1)).setTargetId("T-" + randomAlphaString);
    }

    @Test
    public void shouldCreateNewLevelNodeWithCondition() throws Exception {
        final List<Node> childNodes = new ArrayList<Node>();
        setUpNodeToReturnNulls(newCSNode);
        // Given a content spec level that doesn't exist
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.condition, randomAlphaString)));
        childNodes.add(level);
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
        verifyBaseNewLevel(newCSNode);
        // and the condition was set
        verify(newCSNode, times(1)).setCondition(randomAlphaString);
    }

    @Test
    public void shouldMergeLevelWithDBIdsWithNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.uniqueId, id.toString())));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
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
        // and the base level hasn't changed
        verifyBaseExistingLevel(foundCSNode);
    }

    @Test
    public void shouldMergeLevelWithoutDBIdsButIsTheSameLevelFromParserWithNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.uniqueId, "L-" + id)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
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
            e.printStackTrace();
            fail("An Exception should not have been thrown. Message: " + e.getMessage());
        }

        // Then a updated node should not exist in the updated collection, since nothing was modified
        assertThat(updatedChildrenNodes.size(), is(0));
        // and the main details haven't changed
        verifyBaseExistingLevel(foundCSNode);
    }

    @Test
    public void shouldMergeLevelWithoutDBIdsButIsTheSameLevelWithNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
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
        verifyBaseExistingLevel(foundCSNode);
    }

    @Test
    public void shouldMergeMultipleLevelsWithoutDBIdsWithSameTitleButDifferentNodeType() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given two content spec levels, with the same title
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(level);
        final Level section = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.SECTION), with(LevelMaker.title, title)));
        childNodes.add(section);
        // And a matching child node exists in the database for the chapter
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // And a matching child node exists in the database for the section
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_SECTION);
        given(newCSNode.getTitle()).willReturn(title);
        given(newCSNode.getEntityId()).willReturn(null);
        given(newCSNode.getId()).willReturn(secondId);
        given(newCSNode.getEntityRevision()).willReturn(null);
        given(newCSNode.getContentSpec()).willReturn(contentSpecWrapper);
        // and is in the child nodes collection
        childrenNodes.add(newCSNode);
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
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(2));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        assertSame(updatedChildrenNodes.getUpdateItems().get(1), newCSNode);
        // and the main details haven't changed for the chapter
        verifyBaseExistingLevel(foundCSNode);
        // and the main details haven't changed for the section
        verifyBaseExistingLevel(newCSNode);
    }

    @Test
    public void shouldMergeLevelWithDBIdsWhenMultipleLevelsWithNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        final Level level2 = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, secondTitle)));
        childNodes.add(level);
        childNodes.add(level2);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        given(foundCSNode.getNextNode()).willReturn(newCSNode);
        // And another non matching child node exists in the database
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(newCSNode.getTitle()).willReturn(secondTitle);
        given(newCSNode.getEntityId()).willReturn(null);
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

        // Then all the nodes should have been processed and none left for removal
        assertThat(childrenNodes.size(), is(0));
        // and the both nodes should be untouched
        assertThat(updatedChildrenNodes.size(), is(2));
        assertThat(updatedChildrenNodes.getUnchangedItems().size(), is(2));
        // and the main details haven't changed
        verifyBaseExistingLevel(foundCSNode);
    }


    @Test
    public void shouldMergeLevelWithDBIdsAndIgnoreSameCondition() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.condition, secondTitle)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        // and the base level hasn't changed
        verifyBaseExistingLevel(foundCSNode);
        // and the level condition wasn't changed
        verify(foundCSNode, never()).setCondition(anyString());
    }

    @Test
    public void shouldMergeLevelWithDBIdsAndDifferentCondition() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.condition, secondTitle)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        // and the base level hasn't changed
        verifyBaseExistingLevel(foundCSNode);
        // and the level condition was set
        verify(foundCSNode, times(1)).setCondition(secondTitle);
    }

    @Test
    public void shouldMergeLevelWithDBIdsAndConditionRemoved() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.condition, (String) null)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        // and the the base level hasn't changed
        verifyBaseExistingLevel(foundCSNode);
        // and the level condition was set to null
        verify(foundCSNode, times(1)).setCondition(null);
    }

    @Test
    public void shouldMergeLevelWithDBIdsAndIgnoreSameTarget() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.targetId, secondTitle)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        // and the base level hasn't changed
        verifyBaseExistingLevel(foundCSNode);
        // and the level target wasn't changed
        verify(foundCSNode, never()).setTargetId(anyString());
    }

    @Test
    public void shouldMergeLevelWithDBIdsAndDifferentTarget() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.targetId, secondTitle)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        // and the node level id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the level revision wasn't set
        verify(foundCSNode, never()).setEntityRevision(anyInt());
        // and the level target was set
        verify(foundCSNode, times(1)).setTargetId(secondTitle);
    }

    @Test
    public void shouldMergeLevelWithDBIdsAndTargetRemoved() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title),
                with(LevelMaker.targetId, (String) null)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
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
        // and the node level id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the level revision wasn't set
        verify(foundCSNode, never()).setEntityRevision(anyInt());
        // and the level condition was set to null
        verify(foundCSNode, times(1)).setTargetId(null);
    }

    @Test
    public void shouldMergeLevelWithoutDBIdsWithLevelTypeChange() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.PREFACE), with(LevelMaker.title, title)));
        childNodes.add(level);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
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

        // Then a updated node should exist in the updated collection, since the type was modified
        assertThat(updatedChildrenNodes.size(), is(1));
        assertThat(updatedChildrenNodes.getUpdateItems().size(), is(1));
        assertSame(updatedChildrenNodes.getUpdateItems().get(0), foundCSNode);
        // and the node type hasn't changed
        verify(foundCSNode, times(1)).setNodeType(CommonConstants.CS_NODE_PREFACE);
        // and the content spec wasn't changed
        verify(foundCSNode, never()).setContentSpec(any(ContentSpecWrapper.class));
        // and the node title wasn't changed
        verify(foundCSNode, never()).setTitle(anyString());
        // and the node level id wasn't set
        verify(foundCSNode, never()).setEntityId(anyInt());
        // and the level revision wasn't set
        verify(foundCSNode, never()).setEntityRevision(anyInt());
        // and the level condition was set to null
        verify(foundCSNode, never()).setTargetId(anyString());
    }

    @Test
    public void shouldMergeLevelWithoutDBIdsWithMultipleLevelsAndNoChanges() {
        final List<Node> childNodes = new ArrayList<Node>();
        // Given a content spec level that was created from a DB entity
        final Level level = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.PREFACE), with(LevelMaker.title, title)));
        final Level level2 = make(a(LevelMaker.Level, with(LevelMaker.levelType, LevelType.CHAPTER), with(LevelMaker.title, title)));
        childNodes.add(level);
        childNodes.add(level2);
        // And a matching child node exists in the database
        given(foundCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_CHAPTER);
        given(foundCSNode.getTitle()).willReturn(title);
        given(foundCSNode.getEntityId()).willReturn(null);
        given(foundCSNode.getId()).willReturn(id);
        given(foundCSNode.getEntityRevision()).willReturn(null);
        // And a second matching child node exists in the database
        given(newCSNode.getNodeType()).willReturn(CommonConstants.CS_NODE_PREFACE);
        given(newCSNode.getTitle()).willReturn(title);
        given(newCSNode.getEntityId()).willReturn(null);
        given(newCSNode.getId()).willReturn(secondId);
        given(newCSNode.getEntityRevision()).willReturn(null);
        given(newCSNode.getNextNode()).willReturn(foundCSNode);
        // and is in the child nodes collection (in the opposite order to test the right node will be selected)
        childrenNodes.add(foundCSNode);
        childrenNodes.add(newCSNode);
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
        // and the base level hasn't changed
        verifyBaseExistingLevel(foundCSNode);
        verifyBaseExistingLevel(newCSNode);
    }

    protected void verifyBaseNewLevel(final CSNodeWrapper levelNode) {
        // and the node has the Spec Topic type set
        verify(levelNode, atLeast(1)).setNodeType(CommonConstants.CS_NODE_CHAPTER);
        // and the parent node should be null
        verify(levelNode, never()).setParent(any(CSNodeWrapper.class));
        // and the node had the title set
        verify(levelNode, times(1)).setTitle(title);
        // and the node topic id wasn't set
        verify(levelNode, never()).setEntityId(anyInt());
        // and the topic revision wasn't set
        verify(levelNode, never()).setEntityRevision(anyInt());
        // and the level was added to the node collection
        assertTrue(nodeMap.containsValue(levelNode));
    }

    protected void verifyBaseExistingLevel(final CSNodeWrapper levelNode) {
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
        // and the level was added to the node collection
        assertTrue(nodeMap.containsValue(levelNode));
    }
}
