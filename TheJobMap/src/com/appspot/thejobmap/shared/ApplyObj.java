package com.appspot.thejobmap.shared;

public class ApplyObj {
	public String motivation;
	
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
