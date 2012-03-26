package com.redhat.contentspec.rest;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.core.PathSegment;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.resteasy.specimpl.PathSegmentImpl;

import com.redhat.contentspec.constants.CSConstants;
import com.redhat.contentspec.entities.*;
import com.redhat.contentspec.rest.utils.RESTCache;
import com.redhat.contentspec.utils.ExceptionUtilities;
import com.redhat.topicindex.rest.collections.BaseRestCollectionV1;
import com.redhat.topicindex.rest.entities.*;
import com.redhat.topicindex.rest.expand.ExpandDataDetails;
import com.redhat.topicindex.rest.expand.ExpandDataTrunk;
import com.redhat.topicindex.rest.sharedinterface.RESTInterfaceV1;
import com.redhat.ecs.commonutils.CollectionUtilities;

@SuppressWarnings("unchecked")
public class RESTReader {
	
	private final Logger log = Logger.getLogger(RESTReader.class);
	
	private final RESTInterfaceV1 client;
	private final ObjectMapper mapper = new ObjectMapper();
	private final RESTCache cache;
	
	public RESTReader(RESTInterfaceV1 client, RESTCache cache) {
	    this.client = client;
	    this.cache = cache;
	}
	
	// CATEGORY QUERIES
	
	/*
	 * Gets a specific category tuple from the database as specified by the categories ID.
	 */
	public CategoryV1 getCategoryById(int id) {
		try {
			if (cache.containsKey("CategoryID-" + id)) {
				return (CategoryV1)cache.get("CategoryID-" + id);
			} else {
				CategoryV1 category = client.getJSONCategory(id, null);
				cache.add("CategoryID-" + id, category);
				return category;
			}
		} catch (Exception e) {
			log.debug(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Gets a List of all categories tuples for a specified name.
	 */
	public List<CategoryV1> getCategoriesByName(String name) {
		List<CategoryV1> output = new ArrayList<CategoryV1>();
		
		try {
			
			final BaseRestCollectionV1<CategoryV1> categories;
			if (cache.containsKey("Categories")) {
				categories = (BaseRestCollectionV1<CategoryV1>)cache.get("Categories");
			} else {
				/* We need to expand the Categories collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				expand.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("categories"))));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				
				categories = client.getJSONCategories(expandEncodedString);
				cache.add("Categories", categories);
			}
		
			if (categories != null) {
				for (CategoryV1 cat: categories.getItems()) {
					if (cat.getName().equals(name)) {
						output.add(cat);
					}
				}
			}

			return output;
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	/*
	 * Gets a Category item assuming that tags can only have one category
	 */
	public CategoryV1 getCategoryByTagId(int tagId) {		
		TagV1 tag = getTagById(tagId);
		if (tag == null) return null;
		
		return tag.getCategories().getItems().size() > 0 ? tag.getCategories().getItems().get(0) : null;
	}
	
	// TAG QUERIES
	
	/*
	 * Gets a specific tag tuple from the database as specified by the tags ID.
	 */
	public TagV1 getTagById(int id) {
		try {
			if (cache.containsKey("TagID-" + id)) {
				return (TagV1)cache.get("TagID-" + id);
			} else {
				/* We need to expand the Categories collection in most cases so expand it anyway*/
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				expand.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("categories")), new ExpandDataTrunk(new ExpandDataDetails("properties"))));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				TagV1 tag = client.getJSONTag(id, expandEncodedString);
				cache.add("TagID-" + id, tag);
				return tag;
			}
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	/*
	 * Gets a List of all tag tuples for a specified name.
	 */
	public List<TagV1> getTagsByName(String name) {
		List<TagV1> output = new ArrayList<TagV1>();
		
		try {
			
			final BaseRestCollectionV1<TagV1> tags;
			if (cache.containsKey("Tags")) {
				tags = (BaseRestCollectionV1<TagV1>)cache.get("Tags");
			} else {
				/* We need to expand the Tags & Categories collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				final ExpandDataTrunk expandTags = new ExpandDataTrunk(new ExpandDataDetails("tags"));
				expandTags.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("categories"))));
				expand.setBranches(CollectionUtilities.toArrayList(expandTags));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				
				tags = client.getJSONTags(expandEncodedString);
				cache.add("Tags", tags);
			}
			
			// Iterate through the list of tags and check if the tag is a Type and matches the name.
			if (tags != null) {
				for (TagV1 tag: tags.getItems()) {
					if (tag.getName().equals(name)) {
						output.add(tag);
					}
				}
			}
			
			return output;
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	/*
	 * Gets a List of Tag tuples for a specified its TopicID relationship through TopicToTag.
	 */
	public List<TagV1> getTagsByTopicId(int topicId) {
		final TopicV1 topic;
		if (cache.containsKey("TopicID-" + topicId)) {
			topic = (TopicV1)cache.get("TopicID-" + topicId);
		} else {
			topic = getTopicById(topicId, null);
		}
		
		return topic == null ? null : topic.getTags().getItems();
	}
	
	// TOPIC QUERIES
	
	/*
	 * Gets a specific tag tuple from the database as specified by the tags ID.
	 */
	public TopicV1 getTopicById(int id, Integer rev) {
		try {
			TopicV1 topic = null;
			if (rev == null) {
				if (cache.containsKey("TopicID-" + id)) {
					topic = (TopicV1)cache.get("TopicID-" + id);
				} else {
					/* We need to expand the all the items in the topic collection */
					final ExpandDataTrunk expand = new ExpandDataTrunk();
					final ExpandDataTrunk expandTags = new ExpandDataTrunk(new ExpandDataDetails("tags"));
					expandTags.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("categories")), new ExpandDataTrunk(new ExpandDataDetails("properties"))));
					expand.setBranches(CollectionUtilities.toArrayList(expandTags, new ExpandDataTrunk(new ExpandDataDetails("sourceUrls")), 
							new ExpandDataTrunk(new ExpandDataDetails("properties")), new ExpandDataTrunk(new ExpandDataDetails("outgoingRelationships")),
							new ExpandDataTrunk(new ExpandDataDetails("incomingRelationships"))));
					
					final String expandString = mapper.writeValueAsString(expand);
					final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
					topic = client.getJSONTopic(id, expandEncodedString);
					cache.add("TopicID-" + id, topic);
				}
				return topic;
			} else {
				// Get the data for a revision so only expand all the details in the revisions
				TopicV1 currentTopic = null;
				if (cache.containsKey("TopicRevisionID-" + id)) {
					currentTopic = (TopicV1)cache.get("TopicRevisionID-" + id);
				} else {
					/* We need to expand the all the items in the topic collection */
					final ExpandDataTrunk expand = new ExpandDataTrunk();
					final ExpandDataTrunk expandTags = new ExpandDataTrunk(new ExpandDataDetails("tags"));
					final ExpandDataTrunk expandRevs = new ExpandDataTrunk(new ExpandDataDetails("revisions"));
					expandTags.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("categories"))));
					expandRevs.setBranches(CollectionUtilities.toArrayList(expandTags, new ExpandDataTrunk(new ExpandDataDetails("sourceUrls")), 
							new ExpandDataTrunk(new ExpandDataDetails("properties")), new ExpandDataTrunk(new ExpandDataDetails("outgoingRelationships")),
							new ExpandDataTrunk(new ExpandDataDetails("incomingRelationships"))));
					expand.setBranches(CollectionUtilities.toArrayList(expandTags, expandRevs));
					
					final String expandString = mapper.writeValueAsString(expand);
					final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
					currentTopic = client.getJSONTopic(id, expandEncodedString);
					cache.add("TopicRevisionID-" + id, currentTopic);
				}
				// Get the revison of the topic that is closest to the rev number but is still less then the revision number
				for (TopicV1 topicRev: currentTopic.getRevisions().getItems()) {
					if ((Integer)topicRev.getRevision() <= rev) {
						if (topic != null && (Integer)topic.getRevision() < (Integer)topicRev.getRevision()) {
							topic = topicRev;
						} else if (topic == null) {
							topic = topicRev;
						}
					}
				}
				return topic;
			}
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	/*
	 * Gets a list of Revision's from the TopicIndex database for a specific topic
	 */
	public List<Object[]> getTopicRevisionsById(Integer topicId) {
		List<Object[]> results = new ArrayList<Object[]>();
		try {
			
			final TopicV1 topic;
			if (cache.containsKey("TopicRevisionIDs-" + topicId)) {
				topic = (TopicV1)cache.get("TopicRevisionIDs-" + topicId);
			} else if (cache.containsKey("TopicRevisionID-" + topicId)){
				topic = (TopicV1) cache.get("TopicRevisionID-" + topicId);
			} else {
				/* We need to expand the Revisions collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				expand.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("revisions"))));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				
				topic = client.getJSONTopic(topicId, expandEncodedString);
				cache.add("TopicRevisionIDs-" + topicId, topic);
			}
			
			if (topic != null) {
				for (TopicV1 topicRev: topic.getRevisions().getItems()) {
					Object[] revision =  new Object[2];
					revision[0] = topicRev.getRevision();
					revision[1] = topicRev.getLastModified();
					results.add(revision);
				}
			}
			return results;
		} catch (Exception e) {
			log.debug(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Gets a List of TopicSourceUrl tuples for a specified its TopicID relationship through TopicToTopicSourceUrl.
	 */
	public List<TopicSourceUrlV1> getSourceUrlsByTopicId(int topicId) {
		final TopicV1 topic;
		if (cache.containsKey("TopicID-" + topicId)) {
			topic = (TopicV1)cache.get("TopicID-" + topicId);
		} else {
			topic = getTopicById(topicId, null);
		}
		return topic == null ? null : topic.getSourceUrls_OTM().getItems();
	}
	
	// SNAPSHOT TOPIC QUERIES
	
	public SnapshotV1 getTopicSnapshotById(Integer id) {
		try {
			final SnapshotV1 snapshot;
			if (cache.containsKey("SnapshotID-" + id)) {
				snapshot = (SnapshotV1)cache.get("SnapshotID-" + id);
			} else {
				/* We need to expand the all the items in the topic collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				expand.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("snapshottopics"))));
				
				final String expandString = mapper.writeValueAsString(expand);
				String expandEncodedString = null;expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				snapshot = client.getJSONSnapshot(id, expandEncodedString);
				cache.add("SnapshotID-" + id, snapshot);
			}
			return snapshot == null ? null : snapshot;
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	// USER QUERIES
	
	/*
	 * Gets a List of all User tuples for a specified name.
	 */
	public List<UserV1> getUsersByName(String userName) {
		List<UserV1> output = new ArrayList<UserV1>();
		
		try {
			
			final BaseRestCollectionV1<UserV1> users;
			if (cache.containsKey("Users")) {
				users = (BaseRestCollectionV1<UserV1>)cache.get("Users");
			} else {
				/* We need to expand the Users collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				expand.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("users"))));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				users = client.getJSONUsers(expandEncodedString);
				cache.add("Users", users);
			}
			
			if (users != null) { 
				for (UserV1 user: users.getItems()) {
					if (user.getName().equals(userName)) {
						output.add(user);
					}
				}
			}
			
			return output;
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	/*
	 * Gets a specific User tuple from the database as specified by the tags ID.
	 */
	public UserV1 getUserById(int id) {
		try {
			if (cache.containsKey("UserID-" + id)) {
				return (UserV1)cache.get("UserID-" + id);
			} else {
				UserV1 user = client.getJSONUser(id, null);
				cache.add("UserID-" + id, user);
				return user;
			}
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	// CONTENT SPEC QUERIES
	
	/*
	 * Gets a ContentSpec tuple for a specified id.
	 */
	public TopicV1 getContentSpecById(int id, Integer rev) {
		TopicV1 cs = getTopicById(id, rev);
		if (cs == null) return null;
		List<TagV1> topicTypes = cs.getTagsInCategoriesByID(CollectionUtilities.toArrayList(CSConstants.TYPE_CATEGORY_ID));
		for (TagV1 type: topicTypes) {
			if (type.getId().equals(CSConstants.CONTENT_SPEC_TAG_ID)) return cs;
		}
		return null;
	}
	
	/*
	 * Gets a list of Revision's from the CSProcessor database for a specific content spec
	 */
	public List<Object[]> getContentSpecRevisionsById(Integer csId) {
		List<Object[]> results = new ArrayList<Object[]>();
		try {
			final TopicV1 topic;
			if (cache.containsKey("TopicRevisionIDs-" + csId)) {
				topic = (TopicV1)cache.get("TopicRevisionIDs-" + csId);
			} else if (cache.containsKey("TopicRevisionID-" + csId)){
				topic = (TopicV1) cache.get("TopicRevisionID-" + csId);
			} else {
				/* We need to expand the Revisions collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				expand.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("revisions")), new ExpandDataTrunk(new ExpandDataDetails("tags"))));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				
				topic = client.getJSONTopic(csId, expandEncodedString);
				// Check that the topic is a content spec
				if (!topic.isTaggedWith(CSConstants.CONTENT_SPEC_TAG_ID)) return null;
				
				// Add the content spec to the cache
				cache.add("TopicRevisionIDs-" + csId, topic);
			}
			
			if (topic != null) {
				for (TopicV1 topicRev: topic.getRevisions().getItems()) {
					Object[] revision =  new Object[2];
					revision[0] = topicRev.getRevision();
					revision[1] = topicRev.getLastModified();
					results.add(revision);
				}
			}
			return results;
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	/*
	 * Gets a list of all content specifications in the database or the first 50 if limit is set
	 */
	public List<TopicV1> getContentSpecs(Integer startPos, Integer limit) {
		List<TopicV1> results = new ArrayList<TopicV1>();
		
		try {
			BaseRestCollectionV1<TopicV1> topics;
			
			// Set the startPos and limit to zero if they are null
			startPos = startPos == null ? 0 : startPos;
			limit = limit == null ? 0 : limit;
			
			String key = "ContentSpecs-start-" + startPos + "-end-" + (startPos + limit);
			if (cache.containsKey(key)) {
				topics = (BaseRestCollectionV1<TopicV1>) cache.get(key);
			} else {
				/* We need to expand the topics collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				ExpandDataDetails expandDataDetails = new ExpandDataDetails("topics");
				if (startPos != 0 && startPos != null) {
					expandDataDetails.setStart(startPos);
				}
				if (limit != 0 && limit != null) {
					expandDataDetails.setEnd(startPos + limit);
				}
				
				final ExpandDataTrunk expandTopics = new ExpandDataTrunk(expandDataDetails);
				final ExpandDataTrunk expandTags = new ExpandDataTrunk(new ExpandDataDetails("tags"));
				expandTags.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("categories"))));
				expandTopics.setBranches(CollectionUtilities.toArrayList(expandTags, new ExpandDataTrunk(new ExpandDataDetails("properties"))));
				
				expand.setBranches(CollectionUtilities.toArrayList(expandTopics));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
	
				PathSegment path = new PathSegmentImpl("query;tag" + CSConstants.CONTENT_SPEC_TAG_ID + "=1;", false);
				topics = client.getJSONTopicsWithQuery(path, expandEncodedString);
				cache.add(key, topics);
			}
			
			return topics.getItems();
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return results;
	}
	
	/*
	 * Gets a list of Revision's from the CSProcessor database for a specific content spec
	 */
	public Integer getLatestCSRevById(Integer csId) {
		TopicV1 cs = getTopicById(csId, null);
		if (cs != null) {
			return (Integer) cs.getRevision();
		}
		return null;
	}
	
	/*
	 * Get the Pre Processed Content Specification for a ID and Revision
	 */
	public TopicV1 getPreContentSpecById(Integer id, Integer revision) {
		TopicV1 cs = getContentSpecById(id, revision);
		List<Object[]> specRevisions = getContentSpecRevisionsById(id);
		
		if (specRevisions == null) return null;
		
		// Create a sorted set of revision ids that are less the the current revision
		SortedSet<Integer> sortedSpecRevisions = new TreeSet<Integer>();
		for (Object[] specRev: specRevisions) {
			if ((Integer)specRev[0] <= (Integer)cs.getRevision()) {
				sortedSpecRevisions.add((Integer) specRev[0]);
			}
		}
		
		if (sortedSpecRevisions.size() == 0) return null;
		
		// Find the Pre Content Spec from the revisions
		TopicV1 preContentSpec = null;
		Integer specRev = sortedSpecRevisions.last();
		while (specRev != null) {
			TopicV1 contentSpecRev = getContentSpecById(id, specRev);
			if (contentSpecRev.getProperty(CSConstants.CSP_TYPE_PROPERTY_TAG_ID) != null
					&& contentSpecRev.getProperty(CSConstants.CSP_TYPE_PROPERTY_TAG_ID).getValue().equals(CSConstants.CSP_PRE_PROCESSED_STRING)) {
				preContentSpec = contentSpecRev;
				break;
			}
			specRev = sortedSpecRevisions.headSet(specRev).isEmpty() ? null : sortedSpecRevisions.headSet(specRev).last();
		}
		return preContentSpec;
	}
	
	/*
	 * Get the Pre Processed Content Specification for a ID and Revision
	 */
	public TopicV1 getPostContentSpecById(Integer id, Integer revision) {
		TopicV1 cs = getContentSpecById(id, revision);
		List<Object[]> specRevisions = getContentSpecRevisionsById(id);
		
		if (specRevisions == null) return null;
		
		// Create a sorted set of revision ids that are less the the current revision
		SortedSet<Integer> sortedSpecRevisions = new TreeSet<Integer>();
		for (Object[] specRev: specRevisions) {
			if ((Integer)specRev[0] <= (Integer)cs.getRevision()) {
				sortedSpecRevisions.add((Integer) specRev[0]);
			}
		}
		
		if (sortedSpecRevisions.size() == 0) return null;
		
		// Find the Pre Content Spec from the revisions
		TopicV1 postContentSpec = null;
		Integer specRev = sortedSpecRevisions.last();
		while (specRev != null) {
			TopicV1 contentSpecRev = getContentSpecById(id, specRev);
			if (contentSpecRev.getProperty(CSConstants.CSP_TYPE_PROPERTY_TAG_ID) != null
					&& contentSpecRev.getProperty(CSConstants.CSP_TYPE_PROPERTY_TAG_ID).getValue().equals(CSConstants.CSP_POST_PROCESSED_STRING)) {
				postContentSpec = contentSpecRev;
				break;
			}
			specRev = sortedSpecRevisions.headSet(specRev).isEmpty() ? null : sortedSpecRevisions.headSet(specRev).last();
		}
		return postContentSpec;
	}
	
	// MISC QUERIES
	
	/*
	 * Gets a List of all type tuples for a specified name.
	 */
	public TagV1 getTypeByName(String name) {
		List<TagV1> tags = getTagsByName(name);
		
		// Iterate through the list of tags and check if the tag is a Type and matches the name.
		if (tags != null) {
			for (TagV1 tag: tags) {
				if (tag.isInCategory(CSConstants.TYPE_CATEGORY_ID) && tag.getName().equals(name)) {
					return tag;
				}
			}
		}
		return null;
	}

	/*
	 * Gets an Image File for a specific ID
	 */
	public ImageV1 getImageById(int id) {
		try {
			return client.getJSONImage(id, null);
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
	
	// AUTHOR INFORMATION QUERIES
	
	/*
	 * Gets the Author Tag for a specific topic 
	 */
	public TagV1 getAuthorForTopic(int topicId, Integer rev) {
		if (rev == null) {
			
			final List<TagV1> tags = this.getTagsByTopicId(topicId);
			
			if (tags != null) {
				for (TagV1 tag: tags) {
					if (tag.isInCategory(CSConstants.WRITER_CATEGORY_ID)) return tag;
				}
			}
		} else {
			final TopicV1 topic = this.getTopicById(topicId, rev);
			if (topic != null) {
				for (TopicV1 topicRevision: topic.getRevisions().getItems()) {
					if (topicRevision.getRevision().equals(rev)) {
						List<TagV1> writerTags = topicRevision.getTagsInCategoriesByID(CollectionUtilities.toArrayList(CSConstants.WRITER_CATEGORY_ID));
						if (writerTags.size() == 1) return writerTags.get(0);
						break;
					}
				}
			}
		}
		return null;
	}
	
	/*
	 * Gets the Author Information for a specific author
	 */
	public AuthorInformation getAuthorInformation(Integer authorId) {
		AuthorInformation authInfo = new AuthorInformation();
		authInfo.setAuthorId(authorId);
		TagV1 tag = getTagById(authorId);
		if (tag != null && tag.getProperty(CSConstants.FIRST_NAME_PROPERTY_TAG_ID) != null
				&& tag.getProperty(CSConstants.LAST_NAME_PROPERTY_TAG_ID) != null) {
			authInfo.setFirstName(tag.getProperty(CSConstants.FIRST_NAME_PROPERTY_TAG_ID).getValue());
			authInfo.setLastName(tag.getProperty(CSConstants.LAST_NAME_PROPERTY_TAG_ID).getValue());
			if (tag.getProperty(CSConstants.EMAIL_PROPERTY_TAG_ID) != null) {
				authInfo.setEmail(tag.getProperty(CSConstants.EMAIL_PROPERTY_TAG_ID).getValue());
			}
			if (tag.getProperty(CSConstants.ORGANIZATION_PROPERTY_TAG_ID) != null) {
				authInfo.setOrganization(tag.getProperty(CSConstants.ORGANIZATION_PROPERTY_TAG_ID).getValue());
			}
			if (tag.getProperty(CSConstants.ORG_DIVISION_PROPERTY_TAG_ID) != null) {
				authInfo.setOrgDivision(tag.getProperty(CSConstants.ORG_DIVISION_PROPERTY_TAG_ID).getValue());
			}
			return authInfo;
		}
		return null;
	}
	
	/*
	 * Gets a list of all content specifications in the database
	 */
	public int getNumberOfContentSpecs() {
		List<TopicV1> contentSpecs = getContentSpecs(0, 0);
		return contentSpecs.size();
	}
	
	/*
	 * Gets a list of snapshots in the database for the the content spec id specified or all if the id is null.
	 */
	/*public List<CSSnapshot> getSnapshots(Integer csId, int startPos, int limit) {
		Session sess = sm.getCSSession();
		try {
			Query query;
			if (csId != null) {
				query = sess.createQuery("from CSSnapshot where scopeId = " + csId);
			} else {
				query = sess.createQuery("from CSSnapshot");
			}
			if (startPos != 0) {
				query.setFirstResult(startPos);
			}
			if (limit != 0) {
				query.setMaxResults(limit);
			}
			return query.list();
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return new ArrayList<CSSnapshot>();
	}*/
	
	/*
	 * Gets the number of snapshots in the database for the the content spec id specified or all if the id is null.
	 */
	/*public long getNumberOfSnapshots(Integer csId) {
		Session sess = sm.getCSSession();
		try {
			Query query;
			if (csId != null) {
				query = sess.createQuery("select count(*) from CSSnapshot where scopeId = " + csId);
			} else {
				query = sess.createQuery("select count(*) from CSSnapshot");
			}
			return (Long)query.uniqueResult();
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return 0;
	}*/
	
	/*
	 * Gets snapshot for the specified id or null if one isn't found
	 */
	/*public CSSnapshot getSnapshotById(int id) {
		Session sess = sm.getCSSession();
		try {
			return (CSSnapshot) sess.get(CSSnapshot.class, id);
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}*/
	
	/*
	 * Gets a list of all content specifications in the database that match the search string
	 */
	/*public List<CSSnapshot> searchSnapshots(String searchText, Integer startPos, Integer limit) {
		Session sess = sm.getCSSession();
		try {
			Query query = sess.createQuery("from CSSnapshot where snapshotName like '%" + searchText + "%'");
			if (startPos != null) {
				query.setFirstResult(startPos);
			}
			if (limit != null) {
				query.setMaxResults(limit);
			}
			return query.list();
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return new ArrayList<CSSnapshot>();
	}*/
	
	public SnapshotTopicV1 getSnapshotTopicByTopicAndRevId(Integer topicId, Integer rev) {
		try {
			final BaseRestCollectionV1<SnapshotTopicV1> snapshotTopics;
			if (cache.containsKey("SnapshotTopics")) {
				snapshotTopics = (BaseRestCollectionV1<SnapshotTopicV1>)cache.get("SnapshotTopics");
			} else {
				/* We need to expand the Snapshot Topics collection */
				final ExpandDataTrunk expand = new ExpandDataTrunk();
				expand.setBranches(CollectionUtilities.toArrayList(new ExpandDataTrunk(new ExpandDataDetails("snapshottopics"))));
				
				final String expandString = mapper.writeValueAsString(expand);
				final String expandEncodedString = URLEncoder.encode(expandString, "UTF-8");
				
				snapshotTopics = client.getJSONSnapshotTopics(expandEncodedString);
				cache.add("SnapshotTopics", snapshotTopics);
			}
		
			// List through the snapshotTopics and see if a topic exists for the Topic Id and Revision
			if (snapshotTopics != null) {
				for (SnapshotTopicV1 snapshotTopic: snapshotTopics.getItems()) {
					if (snapshotTopic.getTopicId().equals(topicId) && snapshotTopic.getTopicRevision().equals(rev)) {
						return snapshotTopic;
					}
				}
			}
		} catch (Exception e) {
			log.error(ExceptionUtilities.getStackTrace(e));
		}
		return null;
	}
}
