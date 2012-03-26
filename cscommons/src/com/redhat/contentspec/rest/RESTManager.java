package com.redhat.contentspec.rest;

import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.redhat.contentspec.rest.utils.RESTCache;
import com.redhat.contentspec.utils.logging.ErrorLoggerManager;
import com.redhat.topicindex.rest.sharedinterface.RESTInterfaceV1;

/*
 * A class to store and manage database reading and writing via REST Interface
 */
public class RESTManager {

	private final RESTReader reader;
	private final RESTWriter writer;
	private final RESTInterfaceV1 client;
	private final RESTCache cache = new RESTCache();
	
	public RESTManager(ErrorLoggerManager elm, String serverUrl) {
		RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
		client = ProxyFactory.create(RESTInterfaceV1.class, (serverUrl.endsWith("/") ? serverUrl : (serverUrl + "/")) + "seam/resource/rest");
		reader = new RESTReader(client, cache);
		writer = new RESTWriter(reader, client, cache);
	}
	
	public RESTReader getReader() {
		return reader;
	}
	
	public RESTWriter getWriter() {
		return writer;
	}
	
	public RESTInterfaceV1 getRESTClient() {
		return client;
	}
}
