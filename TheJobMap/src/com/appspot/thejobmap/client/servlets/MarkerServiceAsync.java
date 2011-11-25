package com.appspot.thejobmap.client.servlets;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MarkerServiceAsync {
	public void storeMarker(String latlong, AsyncCallback<String> callback);
	public void getMarker(String latlong, AsyncCallback<String[]> callback);
}
