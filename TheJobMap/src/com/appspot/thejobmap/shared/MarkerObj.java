package com.appspot.thejobmap.shared;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

/**
 * This is the object with information about a marker that is passed between the server and the client.
 * The server sends it when the user requests markers,
 * and the client sends it when the user wants to update or add a marker.
 * 
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */
public class MarkerObj {
	public String id;
	public Double lat;
	public Double lng;
	public String type;
	public String cat;
	public String near;
	public String title;
	public String info;
	public String author;
	public String privacy;
	public Long creationDate;
	public Long updatedDate;
	public Long numApply;

	/**
	 * The no-arg constructor with default values for this object.
	 */
	public MarkerObj() {
		privacy = "public";
		numApply = 0L;
		creationDate = new Date().getTime();
		updatedDate = new Date().getTime();
	}

	/**
	 * Convenience function to convert database entity to MarkerObj.
	 */
	public void convertFromEntity(Entity entityMarker) {
		Key markerKey = entityMarker.getKey();
		if (markerKey.getId() == 0) {
			id = markerKey.getName();
		}
		else {
			id = new Long(markerKey.getId()).toString();
		}
		lat =          (Double) entityMarker.getProperty("lat");
		lng =          (Double) entityMarker.getProperty("lng");
		type =         (String) entityMarker.getProperty("type");
		cat =          (String) entityMarker.getProperty("cat");
		near =         (String) entityMarker.getProperty("near");
		title =        (String) entityMarker.getProperty("title");
		info =         ((Text) entityMarker.getProperty("info")).getValue();
		numApply =     ((Long) entityMarker.getProperty("numApply"));
		creationDate = (Long) entityMarker.getProperty("creationDate");
		updatedDate =  (Long) entityMarker.getProperty("updatedDate");
		author =       (String) entityMarker.getProperty("author");
		privacy =      (String) entityMarker.getProperty("privacy");
	}

	/**
	 * Convenience function to extend this MarkerObj with another MarkerObj.
	 * Normally used when updating the database.
	 * entityMe param makes sure that the current user has permission to update some properties.
	 */
	public void extend(MarkerObj other, Entity entityMe) {
		lat = other.lat;
		lng = other.lng;
		cat = other.cat;
		near = other.near;
		info = other.info;
		privacy = other.privacy;
		updatedDate = new Date().getTime();

		String myName = (String) entityMe.getProperty("name");
		String myPrivileges = (String) entityMe.getProperty("privileges");
		
		if (myPrivileges.equals("random")) {
			title = myName;
		}
		else {
			if (other.title != null) title = other.title;
		}
		
		if (myPrivileges.equals("admin")) {
			if (other.type != null)         type = other.type;
			if (other.author != null)       author = other.author;
			if (other.creationDate != null) creationDate = other.creationDate;
		}
	}
	
	/**
	 * Convenience function update an entity with data from this MarkerObj.
	 */
	public void updateEntity(Entity entityMarker) {
		entityMarker.setProperty("lat",          lat);
		entityMarker.setProperty("lng",          lng);
		entityMarker.setProperty("near",         near);
		entityMarker.setProperty("title",        title);
		entityMarker.setProperty("info",         new Text(info));
		entityMarker.setProperty("cat",          cat);
		entityMarker.setProperty("type",         type);
		entityMarker.setProperty("author",       author);
		entityMarker.setProperty("privacy",      privacy);
		entityMarker.setProperty("numApply",     numApply);
		entityMarker.setProperty("creationDate", creationDate);
		entityMarker.setProperty("updatedDate",  updatedDate);
	}

	/**
	 * Validate object.
	 * This automatically sanitize()s the object too.
	 */
	public Boolean validate() {
		sanitize();
		
		List<String> all_type = Arrays.asList("random", "company", "admin", "city");
		List<String> all_privacy = Arrays.asList("public", "private");
		List<String> categories = Arrays.asList("administration", "construction", "projectLeader",
				"computerScience", "disposalPromotion", "hotelRestaurant", "medicalService",
				"industrialManufacturing", "installation", "cultureMedia", "military",
				"environmentalScience", "pedagogical", "social", "security", "technical",
				"transport", "other");
		
		if (lat.isNaN() || lng.isNaN()
			|| !all_type.contains(type)
			|| !all_privacy.contains(privacy)
			|| ("company".equals(type) && !categories.contains(cat))) {
			return false;
		}
		return true;
	}

	/**
	 * Function to make the user-supplied data in this object safe to use.
	 */
	public void sanitize() {
		if (near != null)  near = near.replaceAll("\\<.*?>","");
		if (title != null) title = title.replaceAll("\\<.*?>","");
		if (info != null)  info = info.replaceAll("\\<.*?>","");
	}
	
	/**
	 * Increment numApply.
	 */
	public void incApply() {
		numApply++;
	}
}
