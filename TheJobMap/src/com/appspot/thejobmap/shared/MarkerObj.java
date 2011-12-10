package com.appspot.thejobmap.shared;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

public class MarkerObj {
	public String id;
	public Double lat;
	public Double lng;
	public String type;
	public String near;
	public String info;

	public String author;
	public Long creationDate;
	
	public MarkerObj() {}

	/**
	 * Convenience function to convert database entity to MarkerObj.
	 */
	public void convertFromEntity(Entity entityMarker) {
		Key markerKey = entityMarker.getKey();
		if (markerKey.getId() == 0) {
			this.id = markerKey.getName();
		}
		else {
			this.id = new Long(markerKey.getId()).toString();
		}
		this.lat = (Double) entityMarker.getProperty("lat");
		this.lng = (Double) entityMarker.getProperty("lng");
		this.type = (String) entityMarker.getProperty("type");
		this.near = (String) entityMarker.getProperty("near");
		this.info = (String) entityMarker.getProperty("info");
		this.creationDate = (Long) entityMarker.getProperty("creationDate");
		this.author = (String) entityMarker.getProperty("author");
	}
	
	/**
	 * Convenience function update an entity with data from this MarkerObj.
	 * entityMe param makes sure that the current user has permission to update some properties.
	 */
	public void updateEntity(Entity entityMarker, Entity entityMe) {
		String myPrivileges = (String) entityMe.getProperty("privileges");
		String myEmail = (String) entityMe.getKey().getName();
		String markerAuthor = (String) entityMarker.getProperty("author");
		if (!myEmail.equals(markerAuthor) && !myPrivileges.equals("admin")) {
			return;
		}
		// Set entity properties
		entityMarker.setProperty("lat", this.lat);
		entityMarker.setProperty("lng", this.lng);
		entityMarker.setProperty("near", this.near);
		entityMarker.setProperty("info", this.info);
		entityMarker.setProperty("updatedDate", new Date().getTime());
		if (myPrivileges.equals("admin")) {
			if (this.type != null)
				entityMarker.setProperty("type", this.type);
			if (this.author != null)
				entityMarker.setProperty("author", this.author);
			if (this.creationDate != null)
				entityMarker.setProperty("creationDate", this.creationDate);
		}
	}

	/**
	 * Validate object.
	 */
	public Boolean validate() {
		List<String> type = Arrays.asList("random", "company", "admin", "city");
		
		if (!type.contains(this.type) || this.lat.isNaN() || this.lng.isNaN()) {
			return false;
		}
		return true;
	}
}
