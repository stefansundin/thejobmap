package com.appspot.thejobmap.shared;

import java.util.Arrays;
import java.util.List;

import com.google.appengine.api.datastore.Entity;

public class UserObj {
	public String email;
	public String name;
	public String age;
	public String sex;
	public String phonenumber;
	public Boolean cvUploaded;
	
	public String privileges;
	public String logoutUrl;
	public String auth_key;

	public UserObj() {}
	
	/**
	 * Convenience function to convert database entity to UserObj.
	 */
	public void convertFromEntity(Entity entityUser) {
		if (entityUser == null) {
			this.privileges = "random";
			return;
		}
		this.email = entityUser.getKey().getName();
		this.name = (String) entityUser.getProperty("name");
		this.age = (String) entityUser.getProperty("age");
		this.sex = (String) entityUser.getProperty("sex");
		this.phonenumber = (String) entityUser.getProperty("phonenumber");
		this.privileges = (String) entityUser.getProperty("privileges");
		this.cvUploaded = (Boolean) entityUser.hasProperty("cv");
	}

	/**
	 * Convenience function update an entity with data from this UserObj.
	 * entityMe param makes sure that the current user has permission to update some properties.
	 */
	public void updateEntity(Entity entityUser, Entity entityMe) {
		String myPrivileges = (String) entityMe.getProperty("privileges");
		if (!entityMe.equals(entityUser) && !myPrivileges.equals("admin")) {
			return;
		}
		// Set entity properties
		entityUser.setProperty("name", this.name);
		entityUser.setProperty("age", this.age);
		entityUser.setProperty("sex", this.sex);
		entityUser.setProperty("phonenumber", this.phonenumber);
		if (this.privileges != null && myPrivileges.equals("admin")) {
			entityUser.setProperty("privileges", this.privileges);
		}
	}
	
	/**
	 * Validate object.
	 */
	public Boolean validate() {
		List<String> sex = Arrays.asList("Not telling", "Male", "Female", "Other");
		List<String> privileges = Arrays.asList("random", "company", "admin");
		
		if (!sex.contains(this.sex) || !privileges.contains(this.privileges) /*|| Integer.parseInt(this.age) < 0*/) {
			return false;
		}
		return true;
	}
}
