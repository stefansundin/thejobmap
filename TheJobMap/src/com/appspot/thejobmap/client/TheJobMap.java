package com.appspot.thejobmap.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

/**
 * The master class of The Job Map.
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class TheJobMap implements EntryPoint {
	private final Marker marker = GWT.create(Marker.class);
	private final OpenID openID = GWT.create(OpenID.class);
	
	/**
	 * Entry point.
	 */
	public void onModuleLoad() {
		// Initialize
		Console.init();
		marker.init();
		openID.init();
	}
}
