package com.appspot.thejobmap.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class contains methods that are common to all servlets.
 * 
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class TheJobMap {

	/**
	 * Enable cross-site scripting for localhost.
	 * Called from servlets if they want to enable cross-site scripting for themselves.
	 */
	static void setAccessControl(HttpServletRequest req, HttpServletResponse res) {
		String origin = req.getHeader("Origin");
		if (origin == null) return;
		if ("http://localhost".equals(origin) || origin.startsWith("http://localhost:")) {
			res.setHeader("Access-Control-Allow-Origin", origin);
		}
		res.setHeader("Access-Control-Allow-Credentials", "true");
		res.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
		res.setHeader("Access-Control-Allow-Headers", "Content-Type");
		res.setHeader("Access-Control-Max-Age", "86400");
	}
}
