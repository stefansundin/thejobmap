package com.appspot.thejobmap.shared;

import java.util.Arrays;
import java.util.List;

import com.google.appengine.api.datastore.Entity;

public class UserObj {
	public String email;
	public String name;
	public Long birthday;
	public String sex;
	public String phonenumber;
	public String privileges;
	
	public Boolean cvUploaded;
	public Long lastLogin;
	
	public String logoutUrl;
	public String auth_key;
	
	public UserObj() {
		privileges = "random";
	}
	
	/**
	 * Convenience function to convert database entity to UserObj.
	 */
	public void convertFromEntity(Entity entityUser) {
		if (entityUser == null) {
			return;
		}
		email = entityUser.getKey().getName();
		name = (String) entityUser.getProperty("name");
		birthday = (Long) entityUser.getProperty("birthday");
		sex = (String) entityUser.getProperty("sex");
		phonenumber = (String) entityUser.getProperty("phonenumber");
		privileges = (String) entityUser.getProperty("privileges");
		cvUploaded = (Boolean) entityUser.hasProperty("cv");
		lastLogin = (Long) entityUser.getProperty("lastLogin");
	}

	/**
	 * Convenience function to extend this UserObj with another UserObj.
	 * Normally used when updating the database.
	 * entityMe param makes sure that the current user has permission to update some properties.
	 */
	public void extend(UserObj other, Entity entityMe) {
		name = other.name;
		birthday = other.birthday;
		sex = other.sex;
		phonenumber = other.phonenumber;

		String myPrivileges = (String) entityMe.getProperty("privileges");
		if (myPrivileges.equals("admin")) {
			if (other.privileges != null) privileges = other.privileges;
		}
	}
	
	/**
	 * Convenience function update an entity with data from this UserObj.
	 */
	public void updateEntity(Entity entityUser) {
		entityUser.setProperty("name",        name);
		entityUser.setProperty("birthday",    birthday);
		entityUser.setProperty("sex",         sex);
		entityUser.setProperty("phonenumber", phonenumber);
		entityUser.setProperty("privileges",  privileges);
	}
	
	/**
	 * Validate object. This automatically sanitize()s the object too.
	 */
	public Boolean validate() {
		sanitize();
		
		List<String> sex = Arrays.asList("Not telling", "Male", "Female", "Other");
		List<String> privileges = Arrays.asList("random", "company", "admin");
		
		if (!sex.contains(sex) || !privileges.contains(privileges)) {
			return false;
		}
		return true;
	}

	/**
	 * Function to make the user-supplied data in this object safe to use.
	 */
	public void sanitize() {
		name = name.replaceAll("\\<.*?>","");
		phonenumber = phonenumber.replaceAll("\\<.*?>","");
	}
	
	/**
	 * Is the user an admin?
	 */
	public Boolean isAdmin() {
		return ("admin".equals(privileges));
	}
}
