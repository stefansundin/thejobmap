package com.appspot.thejobmap.client;

import com.appspot.thejobmap.client.servlets.MarkerService;
import com.appspot.thejobmap.client.servlets.MarkerServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Marker {

	private final MarkerServiceAsync markerService = GWT.create(MarkerService.class);
	
	final DialogBox dialogBox = new DialogBox();
	final Label textToServerLabel = new Label();
	final HTML serverResponseLabel = new HTML();
	final Button closeButton = new Button("Close");
	
	public void init() {

		// MarkerService
		final Button addMarkerButton = new Button("Add Marker");
		
		RootPanel.get("sidebar").add(new HTML("<br>"));
		RootPanel.get("sidebar").add(addMarkerButton);
		
		addMarkerButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				createGUI();
			}
		});
	}
	
	private void createGUI() {
		// First ask for input
		
		
		dialogBox.setText("Add marker to map");
		dialogBox.setAnimationEnabled(true);
		
		closeButton.getElement().setId("closeButton");
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});

		//final Label textToServerLabel = new Label();
		//final HTML serverResponseLabel = new HTML();
		serverResponseLabel.setText("Waiting.");
		
		final TextBox latlongField = new TextBox();
		latlongField.setText("65.619569,22.150519");

		final Button sendButton = new Button("Send");
		sendButton.getElement().setId("sendButton");
		sendButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Click send button
				String latlong = latlongField.getText();
				storeLatlong(latlong);
			}
		});
		
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Latlong:</b>"));
		dialogVPanel.add(latlongField);
		dialogVPanel.add(sendButton);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);
		dialogBox.center();
	}
	
	private void storeLatlong(String latlong) {
		textToServerLabel.setText(latlong);
		serverResponseLabel.setText("Sending... ");
		markerService.storeMarker(latlong,
				new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						dialogBox.setText("Failure!");
						serverResponseLabel.addStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML("Network problem.");
						closeButton.setFocus(true);
					}

					public void onSuccess(String result) {
						dialogBox.setText("Success!");
						serverResponseLabel.removeStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(result);
						closeButton.setFocus(true);
					}
				});

	}
}
