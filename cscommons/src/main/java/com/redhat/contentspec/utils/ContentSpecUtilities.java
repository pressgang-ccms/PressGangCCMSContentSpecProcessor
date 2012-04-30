package com.redhat.contentspec.utils;

import com.redhat.topicindex.rest.collections.BaseRestCollectionV1;
import com.redhat.topicindex.rest.entities.TopicV1;

public class ContentSpecUtilities {

	/**
	 * Generates a random target it in the form of T<Line Number>0<Random Number><count>.
	 * I.e. The topic is on line 50 and the target to be created for is topic 4 in a process, the 
	 * output would be T500494
	 * 
	 * @param count The count of topics in the process.
	 * @return The partially random target id.
	 */
	public static String generateRandomTargetId(int line, int count) {
		return generateRandomTargetId(line) + count;
	}
	
	/**
	 * Generates a random target it in the form of T<Line Number>0<Random Number>.
	 * The random number is between 0-49.
	 * 
	 * @param line The line number the topic is on.
	 * @return The partially random target id.
	 */
	public static String generateRandomTargetId(int line) {
		int randomNum = (int) (Math.random() * 50);
		return "T" + line + "0" + randomNum;
	}
	
	/**
	 * Clones a topic so that it will exist independently in memory.
	 * 
	 * @param topic The topic to be cloned.
	 * @return The cloned topic.
	 */
	public static TopicV1 cloneTopic(TopicV1 topic) {
		TopicV1 clone = new TopicV1();
		clone.setId(topic.getId());
		clone.setTitle(topic.getTitle());
		clone.setDescription(topic.getDescription());
		clone.setXml(topic.getXml());
		clone.setXmlErrors(topic.getXmlErrors());
		clone.setHtml(topic.getHtml());
		clone.setLastModified(topic.getLastModified());
		clone.setCreated(topic.getCreated());
		clone.setRevision(topic.getRevision());
		clone.setLocale(topic.getLocale());
		clone.setTags(topic.getTags());
		
		// Clone the collections
		if (topic.getOutgoingRelationships() != null && topic.getOutgoingRelationships().getItems() != null) {
			BaseRestCollectionV1<TopicV1> outgoingRelationships = new BaseRestCollectionV1<TopicV1>();
			for (TopicV1 outgoingRelationship: topic.getOutgoingRelationships().getItems()) {
				outgoingRelationships.addItem(outgoingRelationship);
			}
			clone.setOutgoingRelationships(outgoingRelationships);
		}
		
		if (topic.getIncomingRelationships() != null && topic.getIncomingRelationships().getItems() != null) {
			BaseRestCollectionV1<TopicV1> incomingRelationships = new BaseRestCollectionV1<TopicV1>();
			for (TopicV1 outgoingRelationship: topic.getIncomingRelationships().getItems()) {
				incomingRelationships.addItem(outgoingRelationship);
			}
			clone.setIncomingRelationships(incomingRelationships);
		}

		clone.setProperties(topic.getProperties());
		clone.setSourceUrls_OTM(topic.getSourceUrls_OTM());
		clone.setBugzillaBugs_OTM(topic.getBugzillaBugs_OTM());
		clone.setAddLink(topic.getAddLink());
		clone.setDeleteLink(topic.getDeleteLink());
		clone.setEditLink(topic.getEditLink());
		clone.setExpand(topic.getExpand());
		clone.setSelfLink(topic.getSelfLink());
		return clone;
	}
}
