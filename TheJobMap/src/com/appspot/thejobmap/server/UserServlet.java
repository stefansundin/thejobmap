package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
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
import com.google.appengine.api.blobstore.UploadOptions;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;

/**
 * This servlet handles users.
 * 
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class UserServlet extends HttpServlet {
	
	private static final long serialVersionUID = 2179295545476158168L;
	
	/**
	 * GET - Request of user details.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		req.setCharacterEncoding("UTF-8");
		res.setContentType("application/json; charset=UTF-8");
		res.setHeader("Access-Control-Allow-Origin", "http://localhost:8888/");
		BufferedWriter writer = null; //We can't initialize this yet since serving CV through blobstore does not like it
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
		
		// Fetch user details
		Entity entityMe = getUser();
		if (entityMe == null) {
			// Not logged in
			writer.write(gson.toJson(new ResultObj("fail", "not logged in")));
			writer.close();
			return;
		}
		// Logged in
		me.convertFromEntity(entityMe);
		// Get logout url
		UserService userService = UserServiceFactory.getUserService();
		me.logoutUrl = userService.createLogoutURL("/");
		
		// Handle "me"
		if (resource.length > 1 && "me".equals(resource[1])) {
			resource[1] = me.email;
		}
		
		// Check privileges
		if ((resource.length <= 1 || !me.email.equals(resource[1])) && !me.isAdmin()) {
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not enough privileges")));
			writer.close();
			return;
		}
		
		// Fetch user object if not me
		UserObj user = new UserObj();
		Entity entityUser = entityMe;
		if (resource.length > 1 && !me.email.equals(resource[1])) {
			entityUser = getUser(resource[1]);
			if (entityUser == null) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "no such user")));
				writer.close();
				return;
			}
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
				&& "cv".equals(resource[2])) {
			// GET /user/<email>/cv
			// Return CV
			
			// Make sure CV exists
			if (!user.cvUploaded) {
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
				&& "cv".equals(resource[2])
				&& "uploadUrl".equals(resource[3])) {
			// GET /user/<email>/cv/uploadUrl
			// Return upload url for CV

			// Fail if CV already is uploaded
			if (user.cvUploaded) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "already has cv")));
				writer.close();
				return;
			}
			
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			UploadUrlObj uploadUrl = new UploadUrlObj();
			uploadUrl.uploadUrl = blobstoreService.createUploadUrl("/special/cvUpload?email="+user.email, UploadOptions.Builder.withMaxUploadSizeBytes(1000000));
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
		req.setCharacterEncoding("UTF-8");
		res.setContentType("application/json; charset=UTF-8");
		res.setHeader("Access-Control-Allow-Origin", "http://localhost:8888/");
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
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not enough privileges")));
			writer.close();
			return;
		}
		me.convertFromEntity(entityMe);
		
		// Handle "me"
		if (resource.length > 1 && "me".equals(resource[1])) {
			resource[1] = me.email;
		}
		
		// Check privileges
		if ((resource.length <= 1 || !me.email.equals(resource[1])) && !me.isAdmin()) {
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not enough privileges")));
			writer.close();
			return;
		}
		
		// Fetch user object if not me
		UserObj dbUser = new UserObj();
		Entity entityUser = entityMe;
		if (resource.length > 1 && !me.email.equals(resource[1])) {
			entityUser = getUser(resource[1]);
			if (entityUser == null) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "no such user")));
				writer.close();
				return;
			}
		}
		dbUser.convertFromEntity(entityUser);

		// Parse input
		UserObj user = gson.fromJson(reader, UserObj.class);
		reader.close();
		
		if (resource.length == 2) {
			// POST /user/<email>
			// Update user details
			dbUser.extend(user, entityMe);
			if (!dbUser.validate()) {
				throw new ServletException("Invalid entry.");
			}
			dbUser.updateEntity(entityUser);
			//TODO: Update marker title if this is a random.
			
			// Update database
			db.put(entityUser);
			
			// Send response
			writer.write(gson.toJson(new ResultObj("ok")));
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
		res.setHeader("Access-Control-Allow-Origin", "http://localhost:8888/");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		UserObj me = new UserObj();
		
		// Check if logged in
		Entity entityMe = getUser();
		if (entityMe == null) {
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not logged in")));
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
		if (resource.length > 1 && "me".equals(resource[1])) {
			resource[1] = me.email;
		}
		
		// Check privileges
		if ((resource.length == 1 || !me.email.equals(resource[1])) && !me.isAdmin()) {
			res.setStatus(403);
			writer.write(gson.toJson(new ResultObj("fail", "not enough privileges")));
			writer.close();
			return;
		}
		
		// Fetch user object if not me
		UserObj user = new UserObj();
		Entity entityUser = entityMe;
		if (resource.length > 1 && !me.email.equals(resource[1])) {
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
			// Delete user and all associated information
			
			// Delete CV
			// Cannot be done in same transaction as below since it's an operation in the blobstore
			if (user.cvUploaded) {
				BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
				BlobKey blobKey = new BlobKey((String) entityUser.getProperty("cv"));
				blobstoreService.delete(blobKey);
			}
			
			// Delete user and his/her markers in an atomic transaction
			Transaction txn = db.beginTransaction();
			db.delete(entityUser.getKey());
			Query q = new Query("Markers");
			q.addFilter("author", FilterOperator.EQUAL, user.email);
			Iterator<Entity> it = db.prepare(q).asIterator();
			while (it.hasNext()) {
				Entity entityMarker = it.next();
				System.out.println(entityMarker.getKey());
				db.delete(entityMarker.getKey());
			}
			txn.commit();
		}
		else if (resource.length == 3
				&& "cv".equals(resource[2])) {
			// DELETE /user/<email>/cv
			// Delete CV
			
			// Make sure CV exists
			if (!user.cvUploaded) {
				res.setStatus(403);
				writer.write(gson.toJson(new ResultObj("fail", "user cv does not exist")));
				writer.close();
				return;
			}
			
			// Delete
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			BlobKey blobKey = new BlobKey((String) entityUser.getProperty("cv"));
			blobstoreService.delete(blobKey);
			entityUser.removeProperty("cv");
			db.put(entityUser);
			
			// Send response
			writer.write(gson.toJson(new ResultObj("ok")));
		}
		else {
			throw new ServletException("Unimplemented request.");
		}
		writer.close();
	}
	
	/**
	 * Get datastore key for user.
	 */
	public Key getUserKey(String email) {
		return KeyFactory.createKey("Users", email);
	}
	
	/**
	 * Get datastore key for "me".
	 */
	public Key getUserKey() {
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) { //Not logged in
			return null;
		}
		return getUserKey(u.getEmail());
	}
	
	/**
	 * Get details of user.
	 */
	public Entity getUser(String email) throws ServletException {
		// Query the database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		try {
			Entity entityUser = db.get(KeyFactory.createKey("Users", email));
			return entityUser;
		} catch (EntityNotFoundException e) {
			//throw new ServletException("User is logged in but does not exist in database.");
			//System.out.println("Warning: User "+email+" is logged in but does not exist in database.");
			return null;
		}
	}
	
	/**
	 * Get details for "me".
	 * Used pretty much everywhere.
	 */
	public Entity getUser() throws ServletException {
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) { //Not logged in
			return null;
		}
		return getUser(u.getEmail());
	}
	
	/**
	 * Insert new user into database.
	 */
	Entity createUser(String email) {
		System.out.println("Creating user "+email);
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		
		// Create an entity
		Entity entityUser = new Entity("Users", email);
		Date date = new Date();
		entityUser.setProperty("creationDate", date.getTime());
		String name = email;
		if (email.indexOf('@') != -1) {
			name = email.substring(0, email.indexOf('@'));
		}
		entityUser.setProperty("name", name);
		entityUser.setProperty("birthday", null);
		entityUser.setProperty("sex", "Not telling");
		entityUser.setProperty("phonenumber", null);
		entityUser.setProperty("privileges", "random");
		entityUser.setProperty("lastLogin", new Date().getTime());
		
		// These guys are admins!
		List<String> realAwesomeGuys = Arrays.asList("test@example.com",
				"alexandra.tsampikakis@gmail.com", "stefan@stefansundin.com",
				"alexandra@thejobmap.se", "stefan@thejobmap.se");
		if (realAwesomeGuys.contains(email)) {
			entityUser.setProperty("privileges", "admin");
		}
		
		// Insert in database
		db.put(entityUser);
		return entityUser;
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
			System.out.println("Warning: getPrivileges("+email+"): User does not exist!");
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
