package com.appspot.thejobmap.client.servlets;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Service for providing markers to the map.
 */
@RemoteServiceRelativePath("marker")
public interface MarkerService extends RemoteService {
	public String storeMarker(String latlong);
}
