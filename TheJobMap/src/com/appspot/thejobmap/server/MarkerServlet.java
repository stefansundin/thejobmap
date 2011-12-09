package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.shared.MarkerObj;
import com.appspot.thejobmap.shared.ResultObj;
import com.appspot.thejobmap.shared.UserObj;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.gson.Gson;

public class MarkerServlet extends HttpServlet {
	
	private static final long serialVersionUID = -919160328227007218L;
	
	UserServlet userServlet = new UserServlet();
	
	/**
	 * GET - Request of markers.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("application/json; charset=UTF-8");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();
		
		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("GET /marker"+path);
		String[] resource = path.split("/");

		// Fetch user details
		Entity entityMe = userServlet.getUser();
		me.convertFromEntity(entityMe);
		
		// Get marker id
		if (path.equals("")) {
			//FIXME
		}
		else {
			//long id = Long.parseLong(path);
			//FIXME
		}
		
		// Query database for markers
		Query q = new Query("Markers");
		List<Entity> dbList = db.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
		
		// Transfer markers to serializable array
		List<MarkerObj> markers = new ArrayList<MarkerObj>();
		for (int i=0; i < dbList.size(); i++) {
			MarkerObj marker = new MarkerObj();
			marker.convertFromEntity(dbList.get(i));
			if (!"admin".equals(me.privileges) && !marker.author.equals(me.email)) {
				marker.creationDate = null;
				marker.author = null;
			}
			markers.add(marker);
		}
		
		// Send markers to client
		writer.write(gson.toJson(markers));
		writer.close();
	}

	/**
	 * POST - Addition of marker.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("application/json; charset=UTF-8");
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();
		
		// Parse input
		MarkerObj marker = gson.fromJson(reader, MarkerObj.class);
		reader.close();
		
		// Check user
		Entity entityMe = userServlet.getUser();
		if (entityMe == null) {
			ResultObj result = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(result));
			writer.close();
			return;
		}
		me.convertFromEntity(entityMe);
		
		// Some stuff
		Entity entity;
		Date date = new Date();
		
		// Get marker id
		String path = req.getPathInfo();
		path = (path==null?"/":path).substring(1);
		if (path.equals("")) {
			// This is a new marker
			Key dbKey = KeyFactory.createKey("Markers", "jobmap");
			entity = new Entity("Markers", dbKey);
			entity.setProperty("creationDate", date.getTime());
		}
		else {
			long id = Long.parseLong(path);
			
			// Query the database
			Key dbKey = KeyFactory.createKey("Markers", "jobmap");
			Key markerKey = KeyFactory.createKey(dbKey, "Markers", id);
			try {
				entity = db.get(markerKey);
			} catch (EntityNotFoundException e) {
				throw new ServletException("Marker does not exist.");
			}
		}
		
		// Set entity properties
		entity.setProperty("lat", marker.lat);
		entity.setProperty("lng", marker.lng);
		entity.setProperty("type", marker.type);
		entity.setProperty("near", marker.near);
		entity.setProperty("info", marker.info);
		entity.setProperty("author", me.email);
		entity.setProperty("updatedDate", date.getTime());
		
		// Insert/update in database
		db.put(entity);
		
		// Send response
		ResultObj result = new ResultObj("ok");
		writer.write(gson.toJson(result));
		writer.close();
	}

}