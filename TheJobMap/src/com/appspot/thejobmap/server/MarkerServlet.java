package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Text;
import com.google.gson.Gson;

/**
 * This servlet handles markers.
 * 
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class MarkerServlet extends HttpServlet {
	
	private static final long serialVersionUID = -919160328227007218L;
	
	UserServlet userServlet = new UserServlet();
	
	/**
	 * GET - Request of markers.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		req.setCharacterEncoding("UTF-8");
		res.setContentType("application/json; charset=UTF-8");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();
		List<MarkerObj> markers = new ArrayList<MarkerObj>();
		
		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("GET /marker"+path);
		String[] resource = path.split("/");

		// Fetch user details
		Entity entityMe = userServlet.getUser();
		me.convertFromEntity(entityMe);

		// Handle "me"
		if (resource.length > 1 && "me".equals(resource[1])) {
			resource[1] = me.email;
		}
		
		if (resource.length <= 1) {
			// GET /marker/
			// Return list of all public markers
			Query q = new Query("Markers");
			if ("random".equals(me.privileges)) {
				// "Only show my marker to companies"
				q.addFilter("privacy", FilterOperator.NOT_EQUAL, "private");
			}
			List<Entity> dbList = db.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
			for (int i=0; i < dbList.size(); i++) {
				MarkerObj marker = new MarkerObj();
				marker.convertFromEntity(dbList.get(i));
				// Remove extra information if not needed
				if ((entityMe == null || !me.email.equals(marker.author)) && !me.isAdmin()) {
					marker.author = null;
				}
				markers.add(marker);
			}
			writer.write(gson.toJson(markers));
		}
		else if (resource.length == 2
				&& ("city".equals(resource[1]) || "random".equals(resource[1]) || "company".equals(resource[1]))) {
			// GET /marker/<city/random/company>
			// Return list of markers by type
			Query q = new Query("Markers");
			q.addFilter("type", FilterOperator.EQUAL, resource[1]);
			if ("random".equals(me.privileges)) {
				// "Only show my marker to companies"
				q.addFilter("privacy", FilterOperator.NOT_EQUAL, "private");
			}
			List<Entity> dbList = db.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
			for (int i=0; i < dbList.size(); i++) {
				MarkerObj marker = new MarkerObj();
				marker.convertFromEntity(dbList.get(i));
				// Remove extra information if not needed
				if ((entityMe == null || !me.email.equals(marker.author)) && !me.isAdmin()) {
					marker.author = null;
				}
				markers.add(marker);
			}
			writer.write(gson.toJson(markers));
		}
		else if (resource.length == 2) {
			// GET /marker/<id/email>
			// Return marker details
			Entity markerEntity = getMarker(resource[1], false);
			if (markerEntity == null) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "no such marker")));
				writer.close();
				return;
			}
			MarkerObj marker = new MarkerObj();
			marker.convertFromEntity(markerEntity);
			// Check privileges
			if ("private".equals(marker.privacy) && !me.email.equals(marker.author) && !me.isAdmin()) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "private marker")));
				writer.close();
				return;
			}
			// Remove extra information if not needed
			if ((entityMe == null || !me.email.equals(marker.author)) && !me.isAdmin()) {
				marker.author = null;
			}
			writer.write(gson.toJson(marker));
		}
		else {
			throw new ServletException("Unimplemented request.");
		}
		writer.close();
	}

	/**
	 * POST - Addition or update of marker.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		System.out.println("Encoding: "+req.getCharacterEncoding());
		System.out.flush();
		//Logger log = Logger.getLogger(MarkerServlet.class.getName());
		//log.info(req.getCharacterEncoding());
		
		
		// Initialize stuff like streams
		req.setCharacterEncoding("UTF-8");
		res.setContentType("application/json; charset=UTF-8");
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();
		Entity markerEntity = null;
		MarkerObj dbMarker = new MarkerObj();
		
		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("POST /marker"+path);
		System.out.flush();
		//System.out.close();
		String[] resource = path.split("/");
		
		// Fetch user details
		Entity entityMe = userServlet.getUser();
		if (entityMe == null) {
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not logged in")));
			writer.close();
			return;
		}
		me.convertFromEntity(entityMe);
		
		// Handle "me"
		if (resource.length > 1 && "me".equals(resource[1])) {
			resource[1] = me.email;
		}
		
		// Check privileges
		if ("random".equals(me.privileges) && (resource.length <= 1 || !me.email.equals(resource[1]))) {
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not enough privileges")));
			writer.close();
			return;
		}
		
		// Parse input
		MarkerObj marker = gson.fromJson(reader, MarkerObj.class);
		if (!me.isAdmin()) marker.excludeProps();
		reader.close();
		
		if (resource.length <= 1) {
			// POST /marker/
			// New marker
			Key userKey = userServlet.getUserKey();
			markerEntity = new Entity("Markers", userKey);
			marker.updateEntity(markerEntity);
			if (!me.isAdmin() || marker.type == null) {
				markerEntity.setProperty("type", me.privileges);
			}
			markerEntity.setProperty("author", me.email);
			marker.convertFromEntity(markerEntity);
			if (!marker.validate()) {
				throw new ServletException("Invalid entry.");
			}
			
			//log.info("Title: "+marker.title);
			//log.info("Info: "+marker.info);
			// Insert in database
			db.put(markerEntity);
			Long id = markerEntity.getKey().getId();
			
			// Send response
			writer.write(gson.toJson(new ResultObj("ok", id)));
		}
		else if (resource.length == 2) {
			// POST /marker/<id/email>
			// Update marker details
			// Randoms must create their marker this way
			markerEntity = getMarker(resource[1], true);
			if (markerEntity == null) {
				// This happens only if resource[1] was numeric (i.e. not a marker for a random)
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "no such marker")));
				writer.close();
				return;
			}
			dbMarker.convertFromEntity(markerEntity);
			dbMarker.extend(marker, entityMe);
			
			// Update entity properties
			if (!dbMarker.validate()) {
				throw new ServletException("Invalid entry.");
			}
			dbMarker.updateEntity(markerEntity);
			
			// Insert/update in database
			db.put(markerEntity);
			Long id = markerEntity.getKey().getId();
			
			// Send response
			writer.write(gson.toJson(new ResultObj("ok", id)));
		}
		else {
			throw new ServletException("Unimplemented request.");
		}
		writer.close();
	}

	/**
	 * DELETE - Delete marker.
	 */
	protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("application/json; charset=UTF-8");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();
		
		// Check if logged in
		Entity entityMe = userServlet.getUser();
		if (entityMe == null) {
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not logged in")));
			writer.close();
			return;
		}
		me.convertFromEntity(entityMe);
		
		// Parse path
		String path = req.getPathInfo();
		System.out.println("localname: "+req.getLocalName());
		path = (path==null?"/":path);
		System.out.println("DELETE /marker"+path);
		String[] resource = path.split("/");
		
		// Handle "me"
		if (resource.length > 1 && "me".equals(resource[1])) {
			resource[1] = me.email;
		}
		
		if (resource.length == 2) {
			// DELETE /marker/<id>
			// Delete marker
			
			// Check if marker exists
			Entity markerEntity = getMarker(resource[1], false);
			if (markerEntity == null) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "marker does not exist")));
				writer.close();
				return;
			}
			MarkerObj marker = new MarkerObj();
			marker.convertFromEntity(markerEntity);
			
			// Check privileges
			if (!me.email.equals(marker.author) && !me.isAdmin()) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "not enough privileges")));
				writer.close();
				return;
			}
			
			// Delete marker
			db.delete(markerEntity.getKey());
			
			// Send response
			writer.write(gson.toJson(new ResultObj("ok")));
		}
		else {
			throw new ServletException("Unimplemented request.");
		}
		writer.close();
	}
	
	/**
	 * Get the Entity of a marker based on a path.
	 * If id is a string, it is a marker for a random. If it does not exist, then create it.
	 */
	public Entity getMarker(String path, Boolean create) throws ServletException {
		Entity markerEntity = null;
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Key userKey = userServlet.getUserKey();
		
		// Parse path
		int colon = path.indexOf(':');
		if (colon != -1) {
			String email = path.substring(0, colon);
			Long id = Long.parseLong(path.substring(colon+1));
			userKey = userServlet.getUserKey(email);
			Key markerKey = KeyFactory.createKey(userKey, "Markers", id);
			try {
				markerEntity = db.get(markerKey);
			} catch (EntityNotFoundException e) {}
		}
		else if ("me".equals(path)) {
			Query q = new Query("Markers", userKey);
			markerEntity = db.prepare(q).asSingleEntity();
		}
		else {
			try {
				// Try first with a numeric id
				Long id = Long.parseLong(path);
				try {
					Key markerKey = KeyFactory.createKey(userKey, "Markers", id);
					markerEntity = db.get(markerKey);
				} catch (EntityNotFoundException e) {
					// This entity probably belongs to someone else
					// Search the database
					Query q = new Query("Markers");
					List<Entity> dbList = db.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
					for (int i=0; i < dbList.size(); i++) {
						Entity searchEntity = dbList.get(i);
						if (searchEntity.getKey().getId() == id) {
							// Found the marker
							markerEntity = searchEntity;
							break;
						}
					}
				}
			} catch (NumberFormatException e) {
				// If it's not numeric, it is a marker by a random
				userKey = userServlet.getUserKey(path);
				Query q = new Query("Markers", userKey);
				markerEntity = db.prepare(q).asSingleEntity();
			}
		}
		
		// If the Entity does not exist in database, create it if the caller wants us to
		if (markerEntity == null && create) {
			markerEntity = new Entity("Markers", userKey);
			// Get user and set properties
			UserObj user = new UserObj();
			try {
				Entity userEntity = db.get(userKey);
				user.convertFromEntity(userEntity);
			} catch (EntityNotFoundException e) {
				throw new ServletException("User does not exist.");
			}
			markerEntity.setProperty("author", user.email);
			markerEntity.setProperty("type", user.privileges);
			markerEntity.setProperty("info", new Text(""));
		}
		
		return markerEntity;
	}

}