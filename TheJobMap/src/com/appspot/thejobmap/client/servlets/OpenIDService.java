package com.appspot.thejobmap.client.servlets;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Service for providing markers to the map.
 */
@RemoteServiceRelativePath("openid")
public interface OpenIDService extends RemoteService {
	public String login(String latlong);
	
	public String isLoggedIn();
}
