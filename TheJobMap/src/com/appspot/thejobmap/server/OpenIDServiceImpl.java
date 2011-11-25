package com.appspot.thejobmap.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.client.servlets.OpenIDService;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class OpenIDServiceImpl extends RemoteServiceServlet implements OpenIDService {

	private static final long serialVersionUID = 3410869956404931778L;

    UserService userService = null;
    User user = null; // or req.getUserPrincipal()
    /*
	private static final Map<String, String> openIdProviders;
    static {
        openIdProviders = new HashMap<String, String>();
        openIdProviders.put("Google", "www.google.com/accounts/o8/id");
        openIdProviders.put("Yahoo", "yahoo.com");
        openIdProviders.put("MySpace", "myspace.com");
        openIdProviders.put("AOL", "aol.com");
        openIdProviders.put("MyOpenId.com", "myopenid.com");
    }
    */

    public String[][] getUrls() {
    	String providers[][] = {
    			{ "google",   "google.com/accounts/o8/id" },
    			{ "yahoo",    "yahoo.com" },
    			{ "myspace",  "myspace.com" },
    			{ "aol",      "aol.com" },
    			{ "myopenid", "myopenid.com" },
    	};
    	
    	userService = UserServiceFactory.getUserService();
    	user = userService.getCurrentUser();
    	for (String[] provider : providers) {
    		provider[1] = userService.createLoginURL("/thejobmap/openid", null, provider[1], null);
		}
    	return providers;
    }
    
    
    
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
    	userService = UserServiceFactory.getUserService();
    	user = userService.getCurrentUser();

		resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        out.print("<html><script type=\"text/javascript\">"+
        		"window.opener.checkLoggedIn();"+
        		"window.close();"+
        		"</script></html>");
    }
    
    
    public String[] isLoggedIn() { 
    	userService = UserServiceFactory.getUserService();
    	user = userService.getCurrentUser();
    	if (user == null) {
    		//ret[0] = "Not logged in";
    		return null;
    	}
    	
    	String[] ret = new String[4];
    	ret[0] = user.getNickname();
    	ret[1] = user.getEmail();
    	ret[2] = "user";
    	//ret[3] = userService.createLogoutURL("/");
    	ret[3] = userService.createLogoutURL("/TheJobMap.html?gwt.codesvr=127.0.0.1:9997");
    	return ret;
    }

	@Override
	public String login(String latlong) {
		// TODO Auto-generated method stub
		return null;
	}
}