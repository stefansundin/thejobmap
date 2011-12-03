package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.shared.ResultObj;
import com.appspot.thejobmap.shared.UserObj;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;

public class UserServlet extends HttpServlet {
	
	private static final long serialVersionUID = 2179295545476158168L;
	
	/**
	 * 
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Gson gson = new Gson();
		
		// Open output stream
		OutputStream out = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		
		// Check if logged in
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) {
			ResultObj res = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(res));
			writer.close();
			return;
		}
		
		// Query database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, u.getEmail());
		PreparedQuery pq = db.prepare(q);
		
		// Does the user exist?
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			//createUser();
			ResultObj res = new ResultObj("fail", "not implemented");
			writer.write(gson.toJson(res));
			writer.close();
			return;
		}

		// Get user info
		Entity entity = pq.asSingleEntity();
		UserObj user = new UserObj();
		user.email = (String) entity.getProperty("email");
		user.name = (String) entity.getProperty("name");
		user.privileges = (String) entity.getProperty("privileges");
		
		// Send to client
		writer.write(gson.toJson(user));
		writer.close();
	}

	/**
	 * 
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Gson gson = new Gson();

		// Parse input
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		UserObj user = gson.fromJson(reader, UserObj.class);
		reader.close();
		
		// Open output stream
		OutputStream out = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		
		// Check if logged in
		User u = UserServiceFactory.getUserService().getCurrentUser();
		if (u == null) {
			ResultObj res = new ResultObj("fail", "not logged in");
			writer.write(gson.toJson(res));
			writer.close();
			return;
		}

		// Query database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Users");
		q.addFilter("email", Query.FilterOperator.EQUAL, u.getEmail());
		PreparedQuery pq = db.prepare(q);

		// Does the user exist?
		if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
			//createUser();
			ResultObj res = new ResultObj("fail", "not implemented");
			writer.write(gson.toJson(res));
			writer.close();
			return;
		}

		// Set new user info
		Entity entry = pq.asSingleEntity();
		entry.setProperty("email", user.email);
		entry.setProperty("name", user.name);
		entry.setProperty("privileges", user.privileges);
		
		// Update database
		db.put(entry);
		
		// Send response
		ResultObj res = new ResultObj("ok");
		writer.write(gson.toJson(res));
		writer.close();
	}

}