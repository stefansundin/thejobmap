package com.appspot.thejobmap.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.client.servlets.OpenIDService;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class OpenIDServiceImpl extends RemoteServiceServlet implements OpenIDService {

	private static final long serialVersionUID = 3410869956404931778L;

    UserService userService = null;
    User user = null;

    /**
     * Return list of OpenID providers to the client.
     */
    public String[][] getUrls() {
    	String providers[][] = {
    			{ "google",    "google.com/accounts/o8/id" },
    			{ "myopenid",  "myopenid.com" },
    			{ "yahoo",     "yahoo.com" },
    			//{ "wordpress", "wordpress.com" },
    			//{ "myspace",  "myspace.com" },
    			//{ "aol",      "aol.com" },
    	};
    	
    	userService = UserServiceFactory.getUserService();
    	user = userService.getCurrentUser();
    	for (String[] provider : providers) {
    		provider[1] = userService.createLoginURL("/thejobmap/openid", null, provider[1], null);
		}
    	
    	return providers;
    }
    
    /**
     * Check if the user is logged in, and return details like email and privileges.
     * Used on startup and after logging in.
     */
	public String[] isLoggedIn() { 
		userService = UserServiceFactory.getUserService();
		user = userService.getCurrentUser();
		if (user == null) {
			return null;
		}
		
		String[] ret = new String[4];
		ret[0] = user.getNickname();
		ret[1] = user.getEmail();
		ret[2] = "user";
		Boolean devmode = (getServletContext().getServerInfo().indexOf("Development") != -1);
		if (devmode) {
			ret[3] = userService.createLogoutURL("/");
		}
		else {
			ret[3] = userService.createLogoutURL("/TheJobMap.html?gwt.codesvr=127.0.0.1:9997");
		}
		
		return ret;
	}

    /**
     * The user will arrive directly to this method in the popup window after logging in with OpenID.
     * This JavaScript code will invoke a method on the main window and then close itself.
     */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		userService = UserServiceFactory.getUserService();
		user = userService.getCurrentUser();
		
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		
		out.print("<html><script type=\"text/javascript\">"+
			"window.opener.checkLoggedIn();"+
			"window.close();"+
			"</script><body>"+
			"<h2>You can close this window now.</h2>"+
			"</body></html>");
	}
}