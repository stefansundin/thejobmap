package com.appspot.thejobmap.server;

import java.util.Date;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.appspot.thejobmap.client.servlets.MarkerService;

public class MarkerServiceImpl extends RemoteServiceServlet implements MarkerService {

	private static final long serialVersionUID = 5552655692270452162L;

	public String storeMarker(String latlong) {
        Key storeKey = KeyFactory.createKey("Markers", "jaha");
        Date date = new Date();
        Entity entry = new Entity("Markers", storeKey);
        entry.setProperty("latlong", latlong);
        entry.setProperty("date", date);
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(entry);
        
		return "done";
	}

}
