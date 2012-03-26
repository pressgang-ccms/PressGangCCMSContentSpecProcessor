package com.redhat.contentspec.rest.utils;

import java.util.HashMap;

import com.redhat.topicindex.rest.collections.BaseRestCollectionV1;
import com.redhat.topicindex.rest.entities.BaseRESTEntityV1;

public class RESTCache {

	private HashMap<String, BaseRestCollectionV1<? extends BaseRESTEntityV1<?>>> queries = new HashMap<String, BaseRestCollectionV1<? extends BaseRESTEntityV1<?>>>();
	private HashMap<String, Object> singleQueries = new HashMap<String, Object>();
	
	public void add(String key, BaseRestCollectionV1<? extends BaseRESTEntityV1<?>> value) {
		queries.put(key, value);
	}

	public boolean containsKey(String key) {
		return queries.containsKey(key) || singleQueries.containsKey(key);
	}
	
	public void add(String key, Object value) {
		singleQueries.put(key, value);
	}
	
	public Object get(String key) {
		if (!containsKey(key)) return null;
		return queries.containsKey(key) ? queries.get(key) : singleQueries.get(key);
	}
	
	public void expire(String key) {
		if (queries.containsKey(key)) {
			queries.remove(key);
		}
		if (singleQueries.containsKey(key)) {
			singleQueries.remove(key);
		}
	}
	
	public void expireByRegex(String regex) {
		for (String key: queries.keySet()) {
			if (key.matches(regex)) queries.remove(key);
		}
		for (String key: singleQueries.keySet()) {
			if (key.matches(regex)) singleQueries.remove(key);
		}
	}
}
