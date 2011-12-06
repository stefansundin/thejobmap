package com.appspot.thejobmap.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * This servlet handles special requests.
 * Right now these are only callbacks from other parts of the website, like OpenID login return and file upload. 
 */
public class SpecialServlet extends HttpServlet {
	
	private static final long serialVersionUID = -3182300033775695742L;

	UserServlet userServlet = new UserServlet();
	
	/**
	 * GET
	 * - OpenID login return page
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("text/html; charset=UTF-8");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		//Gson gson = new Gson();

		// Check if logged in
		User u = UserServiceFactory.getUserService().getCurrentUser();
		String email = null;
		if (u != null) {
			email = u.getEmail();
		}

		// Check path
		String path = req.getPathInfo();
		path = (path==null?"":path);
		if (path.matches("/login")) {
			// Do not proceed if not logged in
			if (u == null) {
				writer.write("Not logged in");
				writer.close();
			}
			
			// Query database
			DatastoreService db = DatastoreServiceFactory.getDatastoreService();
			Query q = new Query("Users");
			q.addFilter("email", Query.FilterOperator.EQUAL, email);
			PreparedQuery pq = db.prepare(q);

			// Create user if it does not exist
			if (pq.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
				userServlet.createUser(email);
			}
			
			// Write response that closes the window and refreshes the login details in the job map
			writer.write("<html>"+
					"<script type=\"text/javascript\">window.opener.jobmap.getUser();window.close();</script>"+
					"<body><h2>You can close this window now.</h2></body>"+
					"</html>");
			writer.close();
		}
		else {
			throw new ServletException("Not implemented yet.");
		}
	}

	/**
	 * POST
	 * - File upload.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		res.setContentType("text/html; charset=UTF-8");
		//BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		//Gson gson = new Gson();
		//UserObj user = new UserObj();

		// CV Upload?
		// This is a special case since this is done through a file upload
		// Apparently since this is used as a callback after the file upload, it can not access the User session object
		// This is why the email is passed through the url instead. We trust this value since it was generated for this user.
		// This is safe since we will get an exception at getUploadedBlobs if nothing was uploaded
		String path = req.getPathInfo();
		path = (path==null?"":path);
		System.out.println(path);
		if (path.matches("/cvUpload") && req.getParameter("email") != null) {
			String email = req.getParameter("email");
			
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
			writer.write("<html>"+
					"<body style=\"margin:0;\">CV uploaded.</body>"+
					"</html>");
			writer.close();
			return;
			
		}
		else {
			throw new ServletException("Not implemented yet.");
		}
	}
}
