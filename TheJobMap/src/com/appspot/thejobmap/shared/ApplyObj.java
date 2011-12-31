package com.appspot.thejobmap.shared;

/**
 * This is the object that is sent to the server when the user wants to apply for a job.
 * 
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class ApplyObj {
	public String motivation;

	/**
	 * The no-args constructor.
	 */
	public ApplyObj() {}
	
	/**
	 * Function to make the user-supplied data in this object safe to use.
	 */
	public void sanitize() {
		motivation = motivation.replaceAll("\\<.*?>","").replaceAll("\\<","&lt;");
		if (motivation.length() > 500) {
			motivation = motivation.substring(0, 500);
		}
	}
}
