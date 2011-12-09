package com.appspot.thejobmap.shared;

import com.google.appengine.api.datastore.Entity;

public class UserObj {
	public String email;
	public String name;
	public String age;
	public String sex;
	public String phonenumber;
	public String education;
	public String workExperience;
	public Boolean cvUploaded;
	
	public String privileges;
	public String logoutUrl;
	public String auth_key;

	public UserObj() {}
	
	public void convertFromEntity(Entity entityUser) {
		//this.email = (String) entityUser.getProperty("email");
		this.email = entityUser.getKey().getName();
		this.name = (String) entityUser.getProperty("name");
		this.age = (String) entityUser.getProperty("age");
		this.sex = (String) entityUser.getProperty("sex");
		this.phonenumber = (String) entityUser.getProperty("phonenumber");
		this.education = (String) entityUser.getProperty("education");
		this.workExperience = (String) entityUser.getProperty("workExperience");
		this.privileges = (String) entityUser.getProperty("privileges");
		this.cvUploaded = (Boolean) entityUser.hasProperty("cv");
	}
	
	public void updateEntity(Entity entityUser) {
		// Set entity properties
		entityUser.setProperty("name", this.name);
		entityUser.setProperty("age", this.age);
		entityUser.setProperty("sex", this.sex);
		entityUser.setProperty("phonenumber", this.phonenumber);
		entityUser.setProperty("education", this.education);
		entityUser.setProperty("workExperience", this.workExperience);
		if (this.privileges != null) entityUser.setProperty("privileges", this.privileges);
	}
}
