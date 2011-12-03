package com.appspot.thejobmap.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
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
			{ "google",    "google.com/accounts/o8/id" },
			{ "myopenid",  "myopenid.com" },
			{ "yahoo",     "yahoo.com" },
	};
	
	/**
	 * 
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Gson gson = new Gson();
		
		// Enumerate providers
		UserService userService = UserServiceFactory.getUserService();
		List<OpenIDProviderObj> provs = new ArrayList<OpenIDProviderObj>();
		for (String[] provider : providers) {
			OpenIDProviderObj prov = new OpenIDProviderObj();
			prov.name = provider[0];
			prov.loginUrl = userService.createLoginURL("/openid-return.html", null, provider[1], null);
			provs.add(prov);
		}

		// Send markers to client
		OutputStream out = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		writer.write(gson.toJson(provs));
		writer.close();
	}

}