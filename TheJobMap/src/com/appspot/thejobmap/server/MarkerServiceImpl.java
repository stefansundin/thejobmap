package com.appspot.thejobmap.server;

import com.appspot.thejobmap.client.MarkerService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class MarkerServiceImpl extends RemoteServiceServlet implements MarkerService {

	public String myMethod(String s) {
		return s;
	}

}
