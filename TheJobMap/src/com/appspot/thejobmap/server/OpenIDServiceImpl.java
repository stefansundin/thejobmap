package com.appspot.thejobmap.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.client.servlets.MarkerService;
import com.appspot.thejobmap.client.servlets.OpenIDService;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class OpenIDServiceImpl extends RemoteServiceServlet implements OpenIDService {

	private static final long serialVersionUID = 3410869956404931778L;

    UserService userService = null;
    User user = null; // or req.getUserPrincipal()
    
	private static final Map<String, String> openIdProviders;
    static {
        openIdProviders = new HashMap<String, String>();
        openIdProviders.put("Google", "www.google.com/accounts/o8/id");
        openIdProviders.put("Yahoo", "yahoo.com");
        openIdProviders.put("MySpace", "myspace.com");
        openIdProviders.put("AOL", "aol.com");
        openIdProviders.put("MyOpenId.com", "myopenid.com");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
    	userService = UserServiceFactory.getUserService();
    	user = userService.getCurrentUser();
    	
        Set<String> attributes = new HashSet();
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        if (user != null) {
            out.println("Hello <i>" + user.getNickname() + "</i>!");
            out.println("[<a href=\""
                    + userService.createLogoutURL(req.getRequestURI())
                    + "\">sign out</a>]");
        } else {
            out.println("Hello world! Sign in at: ");
            for (String providerName : openIdProviders.keySet()) {
                String providerUrl = openIdProviders.get(providerName);
                String loginUrl = userService.createLoginURL(req.getRequestURI(), null, providerUrl, attributes);
                out.println("[<a href=\"" + loginUrl + "\">" + providerName + "</a>] ");
            }
        }
    }
    
    public String isLoggedIn() {
		//return "bajs";
    	userService = UserServiceFactory.getUserService();
    	user = userService.getCurrentUser();
    	if (user == null) {
    		return "Not logged in";
    	}
    	
    	return user.getEmail();
    }

	@Override
	public String login(String latlong) {
		// TODO Auto-generated method stub
		return null;
	}
}