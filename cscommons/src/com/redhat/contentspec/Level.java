package com.redhat.contentspec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.redhat.contentspec.enums.LevelType;

/**
 * A Class that represents a Level inside of a Content Specification. A Level can either be a Chapter, Section or Appendix.
 * A Level can have children Levels and Content Specifications within it.
 * 
 * @author lnewson
 *
 */
public class Level extends SpecNode {
	
	protected final List<SpecTopic> topics = new ArrayList<SpecTopic>();
	protected final List<Level> levels = new ArrayList<Level>();
	protected final LinkedList<Node> nodes = new LinkedList<Node>();
	protected final LevelType type;
	private String targetId = null;
	protected String title = null;
	
	/**
	 * Constructor
	 * 
	 * @param title The title of the Level.
	 * @param type The type that the Level is (Chapter, Section, etc...).
	 * @param specLine The Content Specification Line that is used to create the Level.
	 * @param lineNumber The Line Number of Level in the Content Specification.
	 */
	public Level(String title, int lineNumber, String specLine, LevelType type) {
		super(lineNumber, specLine);
		this.type = type;
		this.title = title;
	}
	
	/**
	 * Constructor
	 * 
	 * @param title The title of the Level.
	 * @param type The type that the Level is (Chapter, Section, etc...).
	 */
	public Level(String title, LevelType type) {
		super();
		this.type = type;
		this.title = title;
	}	
	
	// Start of the basic getter/setter methods for this Level.
	
	/**
	 * Gets the title of the Level.
	 * 
	 * @return The title of the Level.
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Sets the title for the Level.
	 * 
	 * @param title The title for the Level.
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * Gets the parent of the Level.
	 * 
	 * @return The parent of the level.
	 */
	@Override
	public Level getParent() {
		return (Level) parent;
	}
	
	/**
	 * Sets the parent for the level.
	 * 
	 * @param parent A Level that will act as the parent to this level.
	 */
	protected void setParent(Level parent) {
		super.setParent(parent);
	}
	
	/**
	 * Gets a List of all the Content Specification Topics for the level.
	 * 
	 * Note: The topics may not be in order.
	 * 
	 * @return A List of Content Specification Topics that exist within the level.
	 */
	public List<SpecTopic> getSpecTopics() {
		return topics;
	}
	
	/**
	 * Adds a Content Specification Topic to the Level. If the Topic already has a parent, then it is removed from that parent
	 * and added to this level.
	 * 
	 * @param specTopic The Content Specification Topic to be added to the level.
	 */
	public void appendSpecTopic(SpecTopic specTopic) {
		topics.add(specTopic);
		nodes.add(specTopic);
		if (specTopic.getParent() != null) {
			specTopic.getParent().removeSpecTopic(specTopic);
		}
		specTopic.setParent(this);
	}
	
	/**
	 * Removes a Content Specification Topic from the level and removes the level as the topics parent.
	 * 
	 * @param specTopic The Content Specification Topic to be removed from the level.
	 */
	public void removeSpecTopic(SpecTopic specTopic) {
		topics.remove(specTopic);
		nodes.remove(specTopic);
		specTopic.setParent(null);
	}
	
	/**
	 * Gets a List of all the child levels in this level.
	 * 
	 * Note: The topics may not be in order.
	 * 
	 * @return A List of child levels.
	 */
	public List<Level> getChildLevels() {
		return levels;
	}

	/**
	 * Adds a Child Level to the Level. If the Child Level already has a parent, then it is removed from that parent
	 * and added to this level.
	 * 
	 * @param childLevel A Child Level to be added to the Level.
	 */
	public void appendChild(SpecNode child) {
		if (child instanceof Level) {
			levels.add((Level) child);
		} else if (child instanceof SpecTopic) {
			topics.add((SpecTopic) child);
		}
		nodes.add(child);
		if (child.getParent() != null) {
			child.removeParent();
		}
		child.setParent(this);
	}
	
	/**
	 * Removes a Child Level from the level and removes the level as the Child Levels parent.
	 * 
	 * @param childLevel The Child Level to be removed from the level.
	 */
	public void removeChild(SpecNode child) {
		if (child instanceof Level) {
			levels.remove((Level) child);
		} else if (child instanceof SpecTopic) {
			topics.remove((SpecTopic) child);
		}
		nodes.remove(child);
	}
	
	/**
	 * Gets the number of Content Specification Topics in the Level.
	 * 
	 * @return The number of Content Specification Topics.
	 */
	public int getNumberOfSpecTopics() {
		return topics.size();
	}
	
	/**
	 * Gets the number of Child Levels in the Level.
	 * 
	 * @return The number of Child Levels
	 */
	public int getNumberOfChildLevels() {
		return levels.size();
	}
	
	/**
	 * Inserts a node before the another node in the level
	 * 
	 * @param newNode The node to be inserted.
	 * @param oldNode The node that the new node should be inserted in front of.
	 * @return True if the node was inserted correctly otherwise false.
	 */
	public boolean insertBefore(SpecNode newNode, SpecNode oldNode) {
		if (oldNode == null || newNode == null) return false;
		int index = nodes.indexOf(oldNode);
		if (index != -1) {
			// Remove the parent from the new node if one exists
			if (newNode.getParent() != null) {
				newNode.removeParent();
			}
			// Add the node to the relevant list
			if (newNode instanceof Level) {
				levels.add((Level)newNode);
			} else if (newNode instanceof SpecTopic) {
				topics.add((SpecTopic)newNode);
			}
			// Insert the node
			if (index == 0) {
				nodes.addFirst(newNode);
			} else {
				nodes.add(index - 1, newNode);
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the type of level.
	 * 
	 * @return A LevelType that represents the type of level.
	 */
	public LevelType getType() {
		return type;
	}
	
	/**
	 * Get the Target ID for the level if one exists.
	 * 
	 * @return A String that represents a Target ID if one exists otherwise null.
	 */
	public String getTargetId() {
		return targetId;
	}

	/**
	 * Set the Target ID for the level.
	 * 
	 * @param targetId The Target ID to associate with the level.
	 */
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}
	
	/**
	 * Appends a Comment node to the Level.
	 * 
	 * @param comment The Comment Node to be appended.
	 */
	public void appendComment(Comment comment) {
		nodes.add(comment);
		if (comment.getParent() != null) {
			comment.getParent().removeComment(comment);
		}
		comment.setParent(this);
	}
	
	/**
	 * Creates and appends a Comment node to the Level.
	 * 
	 * @param comment The Comment to be appended to the level.
	 */
	public void appendComment(String comment) {
		appendComment(new Comment(comment));
	}
	
	/**
	 * Removes a Comment Node from the Level.
	 * 
	 * @param comment The Comment node to be removed.
	 */
	public void removeComment(Comment comment) {
		nodes.remove(comment);
	}
	
	/**
	 * Gets a ordered linked list of the child nodes within the level.
	 * 
	 * @return The ordered list of child nodes for the level.
	 */
	public LinkedList<Node> getChildNodes() {
		return nodes;
	}
	
	/**
	 * Gets the total number of Children nodes for the level and its child levels.
	 * 
	 * @return The total number of child nodes for the level and child levels.
	 */
	protected Integer getTotalNumberOfChildren() {
		Integer numChildrenNodes = 0;
		for (Level childLevel: levels) {
			numChildrenNodes += childLevel.getTotalNumberOfChildren();
		}
		return nodes.size() + numChildrenNodes;
	}
	
	@Override
	public Integer getStep() {
		if (getParent() == null) return null;
		Integer previousNode = 0;
		
		// Get the position of the level in its parents nodes
		Integer nodePos = getParent().nodes.indexOf(this);
		
		// If the level isn't the first node then get the previous nodes step
		if (nodePos > 0) {
			Node node = getParent().nodes.get(nodePos - 1);
			previousNode = node.getStep();
			// If the add node is a level then add the number of nodes it contains
			if (node instanceof Level) {
				previousNode = (previousNode == null ? 0 : previousNode) + ((Level)node).getTotalNumberOfChildren();
			}
		// The node is the first item so use the parent levels step
		} else {
			previousNode = getParent().getStep();
		}
		// Make sure the previous nodes step isn't 0
		previousNode = previousNode == null ? 0 : previousNode;
		
		// Add one since we got the previous nodes step
		return previousNode + 1;
	}
	
	@Override
	public String getText() {
		if (text == null) {
			String options = getOptionsString();
			String output = type != LevelType.BASE ? 
					(
						type.getTitle() + ": " + title + (
							// Add the target id if one exists
							targetId == null ? "" : (" [" + targetId + "]")
						) + (
							// Add any options
							options.equals("") ? "" : (" [" + options + "]")
						)
					) : "";
			setText(output);
			return output;
		} else {		
			return text;
		}
	}

	/**
	 * Returns a String Representation of the Level.
	 */
	@Override
	public String toString() {
		String spacer = "";
		for (int i = 1; i < getColumn(); i++) {
			spacer += "  ";
		}
		String output = spacer + getText() + "\n";
		for (Node node: nodes) {
			output += node.toString(); 
			if (node instanceof Level) {
				if (((Level)node).getType() == LevelType.CHAPTER && !node.equals(nodes.getLast())) {
					output += "\n";
				}
			}
		}
		return output;
	}

	@Override
	protected void removeParent() {
		getParent().removeChild(this);
		setParent(null);
	}
}
