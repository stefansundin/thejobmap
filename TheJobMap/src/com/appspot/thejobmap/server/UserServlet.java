package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Gson gson = new Gson();
		UserObj user = new UserObj();
		
		// Open output stream
		OutputStream out = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

		// Check if logged in
		UserService userService = UserServiceFactory.getUserService();
		User u = userService.getCurrentUser();
		if (u == null) {
			ResultObj res = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(res));
			writer.close();
			return;
		}
		user.email = u.getEmail();

		// Query database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, u.getEmail());
		PreparedQuery pq = db.prepare(q);

		// Get user entity
		Entity entity;

		// Does the user exist?
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			entity = createUser(user.email);
		}
		else {
			entity = pq.asSingleEntity();
		}
		
		// Logged in
		user.loggedIn = true;
		
		// Get CV
		String path = req.getPathInfo();
		path = (path==null?"":path);
		if (path.matches("/cv")) {
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			BlobKey blobKey = new BlobKey((String) entity.getProperty("cv"));
			blobstoreService.serve(blobKey, resp);
			return;
		}
		else if (path.matches("/cv/getUploadUrl")) {
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			UploadUrlObj uploadUrl = new UploadUrlObj();
			uploadUrl.uploadUrl = blobstoreService.createUploadUrl("/rest/user/"+user.email+"/cv");
			writer.write(gson.toJson(uploadUrl));
			writer.close();
			return;
		}
		
		
		// Get logout url
		Boolean devmode = (getServletContext().getServerInfo().indexOf("Development") != -1);
		if (devmode) {
			user.logoutUrl = userService.createLogoutURL("/TheJobMap.html?gwt.codesvr=127.0.0.1:9997");
		}
		else {
			user.logoutUrl = userService.createLogoutURL("/");
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
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Gson gson = new Gson();
		UserObj user = new UserObj();

		// Open streams
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		OutputStream out = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

		// CV Upload?
		// This is a special case since this is done through a file upload
		// Apparently since this is used as a callback after the file upload, it can not access the User session object
		// This is why the email is passed through the url instead. We trust this value since it was generated for this user.
		// This is safe since we will get an exception at getUploadedBlobs if nothing was uploaded
		String path = req.getPathInfo();
		path = (path==null?"":path);
		if (path.endsWith("/cv")) {
			String email = path.substring(1, path.indexOf("/cv"));
			
			// Get blob key
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			Map<String,BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
			BlobKey blobKey = blobs.get("cv");

			// Add blob key to user
			DatastoreService db = DatastoreServiceFactory.getDatastoreService();
			Query q = new Query("Users");
			q.addFilter("email", Query.FilterOperator.EQUAL, email);
			PreparedQuery pq = db.prepare(q);
			Entity entity = pq.asSingleEntity();
			entity.setProperty("cv", blobKey.getKeyString());
			db.put(entity);
			
			// Finished
			writer.write("CV uploaded!");
			writer.close();
			return;
			
		}
		
		// Check if logged in
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) {
			user.email = "alexandra.tsampikakis@gmail.com";
			/*
			ResultObj res = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(res));
			writer.close();
			return;
			*/
			
		}
		else {
			user.email = u.getEmail();
		}
		System.out.println(user.email);

		// Query database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, user.email);
		PreparedQuery pq = db.prepare(q);
		Entity entity;

		// Does the user exist?
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			entity = createUser(user.email);
		}
		else {
			// Get user entity
			entity = pq.asSingleEntity();
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
		ResultObj res = new ResultObj("ok");
		writer.write(gson.toJson(res));
		writer.close();
	}
	
	/**
	 * Insert new user into database.
	 */
	private Entity createUser(String email) {
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
		return entry;
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