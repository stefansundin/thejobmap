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
		user.loggedIn = true;
		user.email = u.getEmail();

		// Query database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, u.getEmail());
		PreparedQuery pq = db.prepare(q);
		
		// Does the user exist?
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			createUser(user.email);
			writer.write(gson.toJson(user));
			writer.close();
			return;
		}
		
		// Get user entity
		Entity entity = pq.asSingleEntity();

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
			uploadUrl.uploadUrl = blobstoreService.createUploadUrl("/rest/user/cv");
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

		// Query database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, user.email);
		PreparedQuery pq = db.prepare(q);

		// Does the user exist?
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			createUser(user.email);
			ResultObj res = new ResultObj("ok", "created new user");
			writer.write(gson.toJson(res));
			writer.close();
			return;
		}
		
		// Get user entity
		Entity entry = pq.asSingleEntity();

		// CV Upload?
		String path = req.getPathInfo();
		System.out.println(req.getPathInfo());
		if (path != null && path.matches("/cv")){
			// Get blob key
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
			BlobKey blobKey = blobs.get("myFile");
			
			// Store cv key in user details
			entry.setProperty("cv", blobKey.getKeyString());
			db.put(entry);
			
			// Finished
			writer.write("CV uploaded!");
			writer.close();
			return;
		}
		
		// Parse input
		user = gson.fromJson(reader, UserObj.class);
		reader.close();
		
		
		entry.setProperty("name", user.name);
		entry.setProperty("age", user.age);
		entry.setProperty("sex", user.sex);
		entry.setProperty("phonenumber", user.phonenumber);
		entry.setProperty("education", user.education);
		entry.setProperty("workExperience", user.workExperience);
		entry.setProperty("privileges", user.privileges);
		
		// Update database
		db.put(entry);
		
		// Send response
		ResultObj res = new ResultObj("ok");
		writer.write(gson.toJson(res));
		writer.close();
	}
	
	/**
	 * Insert new user into database.
	 */
	private void createUser(String email) {
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
			throw new IllegalArgumentException("User does not exist!");
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