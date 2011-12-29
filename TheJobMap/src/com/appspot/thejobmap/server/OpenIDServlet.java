package com.appspot.thejobmap.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.shared.OpenIDProviderObj;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;

public class OpenIDServlet extends HttpServlet {
	
	private static final long serialVersionUID = 6738917481662936792L;

	/**
	 * List of OpenID providers.
	 */
	String providers[][] = {
			{ "Google",    "google.com/accounts/o8/id" },
			{ "myOpenID",  "myopenid.com" },
			{ "Yahoo",     "yahoo.com" },
	};
	
	/**
	 * GET - Request of supported OpenID providers.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// Initialize stuff like streams
		req.setCharacterEncoding("UTF-8");
		res.setContentType("application/json; charset=UTF-8");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));
		//DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		
		// Enumerate providers
		UserService userService = UserServiceFactory.getUserService();
		List<OpenIDProviderObj> provs = new ArrayList<OpenIDProviderObj>();
		for (String[] provider : providers) {
			OpenIDProviderObj prov = new OpenIDProviderObj();
			prov.name = provider[0];
			prov.loginUrl = userService.createLoginURL("/special/login", null, provider[1], null);
			provs.add(prov);
		}

		// Send providers to client
		writer.write(gson.toJson(provs));
		writer.close();
	}

}