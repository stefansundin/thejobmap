package com.appspot.thejobmap.shared;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

public class MarkerObj {
	public String id;
	public Double lat;
	public Double lng;
	public String type;
	public String near;
	public String title;
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
		this.title = (String) entityMarker.getProperty("title");
		this.info = ((Text) entityMarker.getProperty("info")).getValue();
		this.creationDate = (Long) entityMarker.getProperty("creationDate");
		this.author = (String) entityMarker.getProperty("author");
	}

	/**
	 * Convenience function to extend this MarkerObj with another MarkerObj.
	 * Normally used when updating the database.
	 * entityMe param makes sure that the current user has permission to update some properties.
	 */
	public void extend(MarkerObj other, Entity entityMe) {
		this.lat = other.lat;
		this.lng = other.lng;
		this.near = other.near;
		this.info = other.info;

		String myName = (String) entityMe.getProperty("name");
		String myPrivileges = (String) entityMe.getProperty("privileges");
		
		if (myPrivileges.equals("random")) {
			this.title = myName;
		}
		else {
			if (other.title != null)
				this.title = other.title;
		}
		
		if (myPrivileges.equals("admin")) {
			if (other.type != null)
				this.type = other.type;
			if (other.author != null)
				this.author = other.author;
			if (other.creationDate != null)
				this.creationDate = other.creationDate;
		}
	}
	
	/**
	 * Convenience function update an entity with data from this MarkerObj.
	 */
	public void updateEntity(Entity entityMarker) {
		entityMarker.setProperty("lat", this.lat);
		entityMarker.setProperty("lng", this.lng);
		entityMarker.setProperty("near", this.near);
		entityMarker.setProperty("title", this.title);
		entityMarker.setProperty("info", new Text(this.info));
		entityMarker.setProperty("type", this.type);
		entityMarker.setProperty("author", this.author);
		entityMarker.setProperty("creationDate", this.creationDate);
		entityMarker.setProperty("updatedDate", new Date().getTime());
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
