package com.appspot.thejobmap.client.servlets;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MarkerServiceAsync {
	public void storeMarker(Double latitude, Double longitude, String markersInfo, AsyncCallback<String> callback);
	public void getMarker(String city, AsyncCallback<Double[][]> callback);
}
