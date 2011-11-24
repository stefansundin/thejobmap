package com.appspot.thejobmap.client;

import com.appspot.thejobmap.client.servlets.OpenIDService;
import com.appspot.thejobmap.client.servlets.OpenIDServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class OpenID {

	private final OpenIDServiceAsync openIDService = GWT.create(OpenIDService.class);

	final VerticalPanel accountPanel = new VerticalPanel();
	final DialogBox dialogBox = new DialogBox();
	final HTML serverResponseLabel = new HTML();

	public void init() {
		RootPanel.get("panel").add(accountPanel);
		
		final Button loginButton = new Button("Login");
		accountPanel.add(loginButton);
		
		loginButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				createGUI();
			}
		});
	}
	
	private void createGUI() {
		dialogBox.setAnimationEnabled(true);
		
		final Label emailLabel = new Label();
		
		openIDService.isLoggedIn(
				new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						dialogBox.setText("Failure!");
						serverResponseLabel.addStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML("Network problem.");
					}

					public void onSuccess(String result) {
						serverResponseLabel.removeStyleName("serverResponseLabelError");
						emailLabel.setText(result);
					}
				});

		final Button closeButton = new Button("Close");
		closeButton.getElement().setId("closeButton");
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});

		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		//dialogVPanel.add(new HTML("<b>Logged in?</b> "+userService.isUserLoggedIn()+"<br>"));
		dialogVPanel.add(new HTML("<b>Username: </b> "));
		dialogVPanel.add(emailLabel);
		dialogVPanel.add(new HTML("<br>"));
		dialogVPanel.add(new HTML("<b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);
		dialogBox.center();
	}
	
}
