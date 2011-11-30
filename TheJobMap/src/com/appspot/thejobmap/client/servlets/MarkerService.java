package com.appspot.thejobmap.client.servlets;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("marker")
public interface MarkerService extends RemoteService {
	public String storeMarker(Double latitude, Double longitude, String markersInfo);
	public Double[][] getMarker(String city);
}
