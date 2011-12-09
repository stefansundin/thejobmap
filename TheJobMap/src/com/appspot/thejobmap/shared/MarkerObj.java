package com.appspot.thejobmap.shared;

import com.google.appengine.api.datastore.Entity;

public class MarkerObj {
	public Long id;
	public Double lat;
	public Double lng;
	public String type;
	public String near;
	public String info;
	
	public Long creationDate;
	public String author;
	
	public MarkerObj() {}

	/**
	 * Convenience function to convert database entity to MarkerObj.
	 */
	public void convertFromEntity(Entity entityMarker) {
		this.id = entityMarker.getKey().getId();
		this.lat = (Double) entityMarker.getProperty("lat");
		this.lng = (Double) entityMarker.getProperty("lng");
		this.type = (String) entityMarker.getProperty("type");
		this.near = (String) entityMarker.getProperty("near");
		this.info = (String) entityMarker.getProperty("info");
		this.creationDate = (Long) entityMarker.getProperty("creationDate");
		this.author = (String) entityMarker.getProperty("author");
	}
	
}
