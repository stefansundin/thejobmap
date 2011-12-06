package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.shared.ResultObj;
import com.appspot.thejobmap.shared.UploadUrlObj;
import com.appspot.thejobmap.shared.UserObj;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;

public class UserServlet extends HttpServlet {
	
	private static final long serialVersionUID = 2179295545476158168L;
	
	/**
	 * GET - Request of user details.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("application/json; charset=UTF-8");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		//DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj user = new UserObj();
		
		// Check if logged in
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		if (u == null) {
			ResultObj result = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(result));
			writer.close();
			return;
		}
		
		// Logged in
		user.loggedIn = true;
		user.email = u.getEmail();
		
		// Get logout url
		Boolean devmode = (getServletContext().getServerInfo().indexOf("Development") != -1);
		if (devmode) {
			// This isn't needed anymore since we no longer use GWT
			user.logoutUrl = userService.createLogoutURL("/TheJobMap.html?gwt.codesvr=127.0.0.1:9997");
		}
		else {
			user.logoutUrl = userService.createLogoutURL("/");
		}
		
		// Fetch user details
		Entity entity = getUser();
		if (entity == null) {
			// User is logged in but does not exist in database (due to delays)
			// Return stub
			writer.write(gson.toJson(user));
			writer.close();
			return;
		}

		// Get CV
		String path = req.getPathInfo();
		path = (path==null?"":path);
		if (path.matches("/cv")) {
			res.setContentType("application/pdf");
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			BlobKey blobKey = new BlobKey((String) entity.getProperty("cv"));
			blobstoreService.serve(blobKey, res);
			return;
		}
		else if (path.matches("/cv/getUploadUrl")) {
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			UploadUrlObj uploadUrl = new UploadUrlObj();
			uploadUrl.uploadUrl = blobstoreService.createUploadUrl("/special/cvUpload?email="+user.email);
			writer.write(gson.toJson(uploadUrl));
			writer.close();
			return;
		}
		
		// Get user info
		user.name = (String) entity.getProperty("name");
		user.age = (String) entity.getProperty("age");
		user.sex = (String) entity.getProperty("sex");
		user.phonenumber = (String) entity.getProperty("phonenumber");
		user.education = (String) entity.getProperty("education");
		user.workExperience = (String) entity.getProperty("workExperience");
		user.privileges = (String) entity.getProperty("privileges");
		
		// Send to client
		writer.write(gson.toJson(user));
		writer.close();
	}

	/**
	 * POST - Update user details.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("application/json; charset=UTF-8");
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj user = new UserObj();

		// Check if logged in
		Entity entity = getUser();
		if (entity == null) {
			ResultObj result = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(result));
			writer.close();
			return;
		}
		
		// Parse input
		user = gson.fromJson(reader, UserObj.class);
		reader.close();
		
		entity.setProperty("name", user.name);
		entity.setProperty("age", user.age);
		entity.setProperty("sex", user.sex);
		entity.setProperty("phonenumber", user.phonenumber);
		entity.setProperty("education", user.education);
		entity.setProperty("workExperience", user.workExperience);
		entity.setProperty("privileges", user.privileges);
		
		// Update database
		db.put(entity);
		
		// Send response
		ResultObj result = new ResultObj("ok");
		writer.write(gson.toJson(result));
		writer.close();
	}

	/**
	 * Get user when placing a marker.
	 */
	public Entity getUser() {
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) { //Not logged in
			return null;
		}
		
		// Query the database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, u.getEmail());
		PreparedQuery pq = db.prepare(q);
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			//throw new IllegalArgumentException("User does not exist!");
			return null;
		}
		
		// Return user entity
		Entity entity = pq.asSingleEntity();
		return entity;
	}
	
	/**
	 * Insert new user into database.
	 */
	void createUser(String email) {
		System.out.println("Creating user "+email);
		// Does the user already exist?
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, email);
		PreparedQuery pq = db.prepare(q);
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) != 0) {
			throw new IllegalArgumentException("User already exists!");
		}

		// Create an entry
		Key storeKey = KeyFactory.createKey("Users", "jobmap");
		Date date = new Date();
		Entity entry = new Entity("Users", storeKey);
		entry.setProperty("creationDate", date.getTime());
		entry.setProperty("email", email);
		entry.setProperty("name", null);
		entry.setProperty("age", null);
		entry.setProperty("sex", null);
		entry.setProperty("phonenumber", null);
		entry.setProperty("education", null);
		entry.setProperty("workExperience", null);

		if (email.matches("test@example.com")
		 || email.matches("alexandra.tsampikakis@gmail.com")
		 || email.matches("recover89@gmail.com")) {
			entry.setProperty("privileges", "admin");
		}
		else {
			entry.setProperty("privileges", null);
		}
		
		// Insert in database
		db.put(entry);
	}
	
	/**
	 * Get privileges for user.
	 */
	public String getPrivileges(String email) {
		// Query the database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, email);
		PreparedQuery pq = db.prepare(q);
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			//throw new IllegalArgumentException("User does not exist!");
			System.out.println("Error: User does not exist!");
			return "random";
		}
		
		// Return privileges
		Entity entry = pq.asSingleEntity();
		String privileges = (String) entry.getProperty("privileges");
		return privileges;
	}
	
	/**
	 * Get privileges for current user.
	 */
	public String getPrivileges() {
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) { //Not logged in
			return "random";
		}
		String email = u.getEmail();
		return getPrivileges(email);
	}
}
