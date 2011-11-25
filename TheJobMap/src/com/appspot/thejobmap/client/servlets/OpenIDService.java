package com.appspot.thejobmap.client.servlets;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("openid")
public interface OpenIDService extends RemoteService {
	public String[][] getUrls();
	
	public String[] isLoggedIn();
}
