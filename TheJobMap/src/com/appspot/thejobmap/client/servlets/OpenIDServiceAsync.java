package com.appspot.thejobmap.client.servlets;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface OpenIDServiceAsync {
	public void getUrls(AsyncCallback<String[][]> callback);
	
	public void isLoggedIn(AsyncCallback<String[]> callback);
}
