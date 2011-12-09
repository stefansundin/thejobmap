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

import com.appspot.thejobmap.shared.ResultObj;
import com.appspot.thejobmap.shared.UploadUrlObj;
import com.appspot.thejobmap.shared.UserObj;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
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
		//BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		BufferedWriter writer = null; //We can't initialize this here since serving CV through blobstore does not like it
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();

		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("GET /user"+path);
		String[] resource = path.split("/");
		
		// Initialize writer if not /cv
		if (!path.endsWith("/cv")) {
			writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		}
		
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
		me.email = u.getEmail();
		me.logoutUrl = userService.createLogoutURL("/");
		
		// Fetch user details
		Entity entityMe = getUser();
		if (entityMe == null) {
			// User is logged in but does not exist in database yet (due to delays)
			// Return stub
			writer.write(gson.toJson(me));
			writer.close();
			return;
		}
		me.convertFromEntity(entityMe);
		
		// Handle "me"
		if (resource.length >= 2 && resource[1].matches("me")) {
			resource[1] = me.email;
		}
		
		// Check privileges
		if ((resource.length <= 1 || !resource[1].matches(me.email)) && !me.privileges.matches("admin")) {
			ResultObj result = new ResultObj("fail", "not enough privileges");
			writer.write(gson.toJson(result));
			writer.close();
			return;
		}
		
		// Fetch user object if not me
		UserObj user = new UserObj();
		Entity entityUser = entityMe;
		if (resource.length > 1 && !resource[1].matches(me.email)) {
			entityUser = getUser(resource[1]);
		}
		user.convertFromEntity(entityUser);
		user.logoutUrl = me.logoutUrl;
		
		if (resource.length <= 1) {
			// GET /user/
			// Return list of all users
			Query q = new Query("Users");
			List<Entity> dbList = db.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
			List<UserObj> users = new ArrayList<UserObj>();
			for (int i=0; i < dbList.size(); i++) {
				UserObj aUser = new UserObj();
				aUser.convertFromEntity(dbList.get(i));
				users.add(aUser);
			}
			writer.write(gson.toJson(users));
		}
		else if (resource.length == 2) {
			// GET /user/<email>
			// Return user details
			writer.write(gson.toJson(user));
		}
		else if (resource.length == 3
				&& resource[2].matches("cv")) {
			// GET /user/<email>/cv
			// Return CV
			
			// Make sure CV exists
			if (!entityUser.hasProperty("cv")) {
				res.setContentType("text/plain");
				writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
				writer.write("Could not find CV. Try again soon.");
				writer.close();
				return;
			}
			
			// Get blob
			res.setContentType("application/pdf");
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			BlobKey blobKey = new BlobKey((String) entityUser.getProperty("cv"));
			
			// Serve metadata
			BlobInfoFactory blobInfoFactory = new BlobInfoFactory(db);
			BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blobKey);
			res.setHeader("Content-disposition", "inline; filename="+blobInfo.getFilename());
			
			// Serve CV
			blobstoreService.serve(blobKey, res);
			return;
		}
		else if (resource.length == 4
				&& resource[2].matches("cv")
				&& resource[3].matches("uploadUrl")) {
			// GET /user/<email>/cv/uploadUrl
			// Return upload url for CV
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			UploadUrlObj uploadUrl = new UploadUrlObj();
			uploadUrl.uploadUrl = blobstoreService.createUploadUrl("/special/cvUpload?email="+user.email);
			writer.write(gson.toJson(uploadUrl));
		}
		else {
			throw new ServletException("Unimplemented request.");
		}
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
		UserObj me = new UserObj();
		
		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("POST /user"+path);
		String[] resource = path.split("/");
		
		// Fetch user details
		Entity entityMe = getUser();
		if (entityMe == null) {
			// User is logged in but does not exist in database yet (due to delays)
			// Return stub
			writer.write(gson.toJson(me));
			writer.close();
			return;
		}
		me.convertFromEntity(entityMe);
		
		// Handle "me"
		if (resource.length >= 2 && resource[1].matches("me")) {
			resource[1] = me.email;
		}
		
		// Check privileges
		if ((resource.length <= 1 || !resource[1].matches(me.email)) && !me.privileges.matches("admin")) {
			ResultObj result = new ResultObj("fail", "not enough privileges");
			writer.write(gson.toJson(result));
			writer.close();
			return;
		}
		
		// Fetch user object if not me
		UserObj user = new UserObj();
		Entity entityUser = entityMe;
		if (resource.length > 1 && !resource[1].matches(me.email)) {
			entityUser = getUser(resource[1]);
		}
		user.convertFromEntity(entityUser);
		
		if (resource.length == 2) {
			// POST /user/<email>
			// Return user details
			user = gson.fromJson(reader, UserObj.class);
			reader.close();
			
			// Update entity properties
			user.updateEntity(entityUser, entityMe);
			if (!user.validate()) {
				throw new ServletException("Invalid entry.");
			}
			
			// Update database
			db.put(entityUser);
			
			// Send response
			ResultObj result = new ResultObj("ok");
			writer.write(gson.toJson(result));
		}
		else {
			throw new ServletException("Unimplemented request.");
		}
		writer.close();
	}
	
	/**
	 * DELETE - Delete user details.
	 */
	protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("application/json; charset=UTF-8");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();
		
		// Check if logged in
		Entity entityMe = getUser();
		if (entityMe == null) {
			ResultObj result = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(result));
			writer.close();
			return;
		}
		
		// Get user info
		me.convertFromEntity(entityMe);
		
		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("DELETE /user"+path);
		String[] resource = path.split("/");
		
		// Handle "me"
		if (resource.length >= 2 && resource[1].matches("me")) {
			resource[1] = me.email;
		}
		
		// Check privileges
		if ((resource.length == 1 || !resource[1].matches(me.email)) && !me.privileges.matches("admin")) {
			ResultObj result = new ResultObj("fail", "not enough privileges");
			writer.write(gson.toJson(result));
			writer.close();
			return;
		}
		
		// Fetch user object if not me
		UserObj user = new UserObj();
		Entity entityUser = entityMe;
		if (resource.length >= 3 && !resource[1].matches(me.email)) {
			entityUser = getUser(resource[1]);
		}
		user.convertFromEntity(entityUser);
		
		if (resource.length == 1) {
			// DELETE /user/
			// This would be a bad idea.
			throw new ServletException("Unimplemented request.");
		}
		else if (resource.length == 2) {
			// DELETE /user/<email>
			// Delete user
		}
		else if (resource.length == 3
				&& resource[2].matches("cv")) {
			// DELETE /user/<email>/cv
			// Delete CV
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			BlobKey blobKey = new BlobKey((String) entityUser.getProperty("cv"));
			blobstoreService.delete(blobKey);
			entityUser.removeProperty("cv");
			db.put(entityUser);
			
			// Send response
			ResultObj result = new ResultObj("ok");
			writer.write(gson.toJson(result));
		}
		else {
			throw new ServletException("Unimplemented request.");
		}
		writer.close();
	}

	/**
	 * Get details of user.
	 */
	public Entity getUser(String email) {
		// Query the database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		try {
			Entity entityUser = db.get(KeyFactory.createKey("Users", email));
			return entityUser;
		} catch (EntityNotFoundException e) {
			return null;
		}
	}

	/**
	 * Get details for me.
	 * Used pretty much everywhere.
	 */
	public Entity getUser() {
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) { //Not logged in
			return null;
		}
		return getUser(u.getEmail());
	}
	
	/**
	 * Insert new user into database.
	 */
	void createUser(String email) {
		System.out.println("Creating user "+email);
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		
		// Create an entity
		Entity entityUser = new Entity("Users", email);
		Date date = new Date();
		entityUser.setProperty("creationDate", date.getTime());
		//entityUser.setProperty("email", email);
		entityUser.setProperty("name", null);
		entityUser.setProperty("age", null);
		entityUser.setProperty("sex", null);
		entityUser.setProperty("phonenumber", null);

		if (email.matches("test@example.com")
		 || email.matches("alexandra.tsampikakis@gmail.com")
		 || email.matches("recover89@gmail.com")) {
			entityUser.setProperty("privileges", "admin");
		}
		else {
			entityUser.setProperty("privileges", "random");
		}
		
		// Insert in database
		db.put(entityUser);
	}
	
	/**
	 * Get privileges for user.
	 */
	public String getPrivileges(String email) {
		// Query the database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		try {
			Entity entityUser = db.get(KeyFactory.createKey("Users", email));
			String privileges = (String) entityUser.getProperty("privileges");
			return privileges;
		} catch (EntityNotFoundException e) {
			//throw new IllegalArgumentException("User does not exist!");
			System.out.println("Error: getPrivileges("+email+"): User does not exist!");
			return "random";
		}
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
