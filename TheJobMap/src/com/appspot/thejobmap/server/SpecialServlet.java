package com.appspot.thejobmap.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
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
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		//Gson gson = new Gson();
		
		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("GET /special"+path);
		
		// Check if logged in
		User u = UserServiceFactory.getUserService().getCurrentUser();
		String email = null;
		if (u != null) {
			email = u.getEmail();
		}

		// Login?
		if (path.matches("/login")) {
			// Do not proceed if not logged in
			if (u == null) {
				writer.write("Not logged in");
				writer.close();
				return;
			}

			// Create user if it does not exist
			Entity entityUser = userServlet.getUser(email);
			if (entityUser == null) {
				userServlet.createUser(email);
			}
			else {
				// Update lastLogin
				entityUser.setProperty("lastLogin", new Date().getTime());
				db.put(entityUser);
			}
			
			// Write response that closes the window and refreshes the login details in the job map
			writer.write("<html>"+
					"<script type=\"text/javascript\">window.opener.jobmap.getUser();window.close();</script>"+
					"<body><h2>You can close this window now.</h2></body>"+
					"</html>");
		}
		else {
			throw new ServletException("Not implemented yet.");
		}
		writer.close();
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
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		//Gson gson = new Gson();
		//UserObj user = new UserObj();
		
		// Parse path
		String path = req.getPathInfo();
		path = (path==null?"/":path);
		System.out.println("POST /special"+path);
		
		// CV Upload?
		// This is a special case since this is done through a file upload
		// Apparently since this is used as a callback after the file upload, it can not access the User session object
		// This is why the email is passed through the url instead. We trust this value since it the upload url was generated for this user.
		// This is safe since we will get an exception at getUploadedBlobs() if nothing was uploaded
		if (path.matches("/cvUpload") && req.getParameter("email") != null) {
			String email = req.getParameter("email");
			Entity entityUser = userServlet.getUser(email);
			
			// Get blob key
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			Map<String,BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
			BlobKey blobKey = blobs.get("cv");
			
			// User tried to upload multiple files, or file with wrong name, or already has CV
			if (blobs.size() > 1 || blobKey == null || entityUser.hasProperty("cv")) {
				// Delete all blobs
				for (BlobKey blob : blobs.values()) {
					blobstoreService.delete(blob);
				}
				
				// Send response
				writer.write("<html>"+
						"<body style=\"margin:0;\">Stop trying to hax plz.</body>"+
						"</html>");
				writer.close();
				return;
			}
			
			// Make sure it's a pdf
			BlobInfoFactory blobInfoFactory = new BlobInfoFactory(db);
			BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blobKey);
			if (!blobInfo.getContentType().matches("application/pdf")) {
				// Not a pdf, delete.
				blobstoreService.delete(blobKey);
				
				// Send response
				writer.write("<html>"+
						"<body style=\"margin:0;\">We only accept pdf files.</body>"+
						"</html>");
				writer.close();
				return;
			}

			// Add blob key to user
			entityUser.setProperty("cv", blobKey.getKeyString());
			db.put(entityUser);
			
			// Finished
			writer.write("<html>"+
					"<body style=\"margin:0;\">CV uploaded.</body>"+
					"</html>");
		}
		else {
			throw new ServletException("Not implemented yet.");
		}
		writer.close();
	}
}
