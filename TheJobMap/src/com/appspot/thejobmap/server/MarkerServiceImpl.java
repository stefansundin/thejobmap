package com.appspot.thejobmap.server;

import java.util.Date;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.appspot.thejobmap.client.servlets.MarkerService;

public class MarkerServiceImpl extends RemoteServiceServlet implements MarkerService {

	private static final long serialVersionUID = 5552655692270452162L;

	/**
	 * Store a new marker in the database.
	 */
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
	
	/**
	 * Get markers from the database and return them in an array to the client
	 */
	public String[] getMarker(String latlong) {
		DatastoreService getLatlong = DatastoreServiceFactory.getDatastoreService();
		
		// Check if the category exists
		Query q = new Query("Markers");
		List<Entity> markers = getLatlong.prepare(q).asList(FetchOptions.Builder.withLimit(5));
		
		//Saves all the latlongs in an array
		String [] allLatlong = new String [markers.size()];
		for(int i=0; i<allLatlong.length; i++){
			allLatlong[i] = (String) markers.get(i).getProperty("latlong");
			//System.out.println(allLatlong[i]);
		}
		return allLatlong;
	}
}
