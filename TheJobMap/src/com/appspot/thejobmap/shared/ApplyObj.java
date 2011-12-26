package com.appspot.thejobmap.shared;

public class ApplyObj {
	public String motivation;
	
	public ApplyObj() {}
	
	/**
	 * Function to make the data in this object safe to use.
	 */
	public void sanitize() {
		this.motivation = this.motivation.replaceAll("\\<.*?>","").replaceAll("\\<","&lt;");
		if (this.motivation.length() > 500) {
			this.motivation = this.motivation.substring(0, 500);
		}
	}
}
