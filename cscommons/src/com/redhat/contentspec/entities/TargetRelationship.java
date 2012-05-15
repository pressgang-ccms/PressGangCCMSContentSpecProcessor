package com.redhat.contentspec.entities;

import com.redhat.contentspec.Level;
import com.redhat.contentspec.SpecNode;
import com.redhat.contentspec.SpecTopic;
import com.redhat.contentspec.enums.RelationshipType;

/**
 * A class to specify a relationship between a topic and a level.
 */
public class TargetRelationship extends Relationship {

	private final SpecTopic topic;
	private final SpecNode secondaryNode;
	
	public TargetRelationship(SpecTopic topic, Level level, RelationshipType type) {
		super(topic.getId(), level.getTargetId(), type);
		this.topic = topic;
		this.secondaryNode = level;
	}
	
	public TargetRelationship(SpecTopic topic, SpecTopic secondaryTopic, RelationshipType type) {
		super(topic.getId(), secondaryTopic.getTargetId(), type);
		this.topic = topic;
		this.secondaryNode = secondaryTopic;
	}

	public SpecTopic getTopic() {
		return topic;
	}

	public SpecNode getSecondaryElement() {
		return secondaryNode;
	}
	
}