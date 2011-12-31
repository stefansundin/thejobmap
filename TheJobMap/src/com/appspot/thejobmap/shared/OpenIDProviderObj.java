package com.appspot.thejobmap.shared;

/**
 * This is the object with OpenID providers that is sent to the client when the user wants to log in.
 * An array with this object is returned to the client when it requests login urls.
 * 
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class OpenIDProviderObj {
	public String name;
	public String loginUrl;

	/**
	 * The no-args constructor.
	 */
	public OpenIDProviderObj() {}
}
