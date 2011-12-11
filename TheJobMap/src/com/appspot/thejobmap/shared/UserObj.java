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
	public String privileges;
	
	public Boolean cvUploaded;
	public Long lastLogin;
	
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
		this.lastLogin = (Long) entityUser.getProperty("lastLogin");
	}

	/**
	 * Convenience function to extend this UserObj with another UserObj.
	 * Normally used when updating the database.
	 * entityMe param makes sure that the current user has permission to update some properties.
	 */
	public void extend(UserObj other, Entity entityMe) {
		this.name = other.name;
		this.age = other.age;
		this.sex = other.sex;
		this.phonenumber = other.phonenumber;

		String myPrivileges = (String) entityMe.getProperty("privileges");
		if (myPrivileges.equals("admin")) {
			if (other.privileges != null)
				this.privileges = other.privileges;
		}
	}
	
	/**
	 * Convenience function update an entity with data from this UserObj.
	 */
	public void updateEntity(Entity entityUser) {
		entityUser.setProperty("name", this.name);
		entityUser.setProperty("age", this.age);
		entityUser.setProperty("sex", this.sex);
		entityUser.setProperty("phonenumber", this.phonenumber);
		entityUser.setProperty("privileges", this.privileges);
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
