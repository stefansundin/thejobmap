package com.appspot.thejobmap.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class User {
	private static Boolean loggedIn = false;
	private static String name;
	private static String email;
	private static String privileges;
	
	final static HTML serverResponseLabel = new HTML();
	//final static UserServiceAsync userService = GWT.create(UserService.class);
	private final static DialogBox dialogBox = new DialogBox();
	
	private final static HorizontalPanel accountPanel = new HorizontalPanel();
	private final static Label nameLabel = new Label();
	private final static Button profileButton = new Button("My profile");
	final static Button closeButton = new Button("Close");
	
	public void viewProfile(){
		dialogBox.setText("Profile");
		dialogBox.setAnimationEnabled(true);
		
		closeButton.getElement().setId("closeButton");
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		
		serverResponseLabel.setText("Waiting.");
		
		final TextBox cityField = new TextBox();
		cityField.setText("Luleå");
	}
}
