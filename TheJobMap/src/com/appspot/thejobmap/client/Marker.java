package com.appspot.thejobmap.client;

import java.util.Arrays;

import com.appspot.thejobmap.client.servlets.MarkerService;
import com.appspot.thejobmap.client.servlets.MarkerServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Marker {

	final static MarkerServiceAsync markerService = GWT.create(MarkerService.class);
	
	final static DialogBox dialogBox = new DialogBox();
	final static HTML serverResponseLabel = new HTML();
	final static VerticalPanel serverResponseResult = new VerticalPanel();
	final static Button closeButton = new Button("Close");
	
	/**
	 * Initialize markers.
	 */
	public void init() {
		initJSNI();
		getCityMarkers("");
		
		RootPanel.get("sidebar").add(new HTML("<br>"));

		final Button createMarkerButton = new Button("Create Marker");
		createMarkerButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				createMarker();
			}
		});
		RootPanel.get("sidebar").add(createMarkerButton);
		RootPanel.get("sidebar").add(new HTML("<br>"));

		final Button refreshMarkersButton = new Button("Refresh Markers");
		refreshMarkersButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				clearMarkers();
				getCityMarkers("");
			}
		});
		RootPanel.get("sidebar").add(refreshMarkersButton);
		RootPanel.get("sidebar").add(new HTML("<br>"));
		
		final Button addMarkerButton = new Button("Add Marker");
		addMarkerButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				createGUI();
			}
		});
		RootPanel.get("sidebar").add(addMarkerButton);
		RootPanel.get("sidebar").add(new HTML("<br>"));

		
		final Button getMarkerButton = new Button("Show Marker");
		getMarkerButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				showMarker();
			}
		});
		RootPanel.get("sidebar").add(getMarkerButton);
	}

	/**
	 * JSNI
	 */
	public static native void initJSNI() /*-{
		$wnd.storeMarker = $entry(@com.appspot.thejobmap.client.Marker::storeMarker(Ljava/lang/String;)); 
	}-*/;
	public static native void addMarkerToMap(Double latitude, Double longitude, String title, String info) /*-{
		$wnd.addMarker(latitude, longitude, title, info); 
	}-*/;
	public static native void createMarker() /*-{
		$wnd.createMarker(); 
	}-*/;
	public static native void clearMarkers() /*-{
		$wnd.clearMarkers(); 
	}-*/;
	
	
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

		serverResponseLabel.setText("Waiting.");
		
		final TextBox latlngField = new TextBox();
		latlngField.setText("65.619569,22.150519");

		final Button sendButton = new Button("Send");
		sendButton.getElement().setId("sendButton");
		sendButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Click send button
				String latlng = latlngField.getText();
				storeMarker(latlng);
				serverResponseLabel.setText("Sending...");
			}
		});
		
		VerticalPanel dialogPanel = new VerticalPanel();
		dialogPanel.addStyleName("dialogVPanel");
		dialogPanel.add(new HTML("<b>Latlong:</b>"));
		dialogPanel.add(latlngField);
		dialogPanel.add(sendButton);
		dialogPanel.add(new HTML("<br><b>Server status:</b>"));
		dialogPanel.add(serverResponseLabel);
		dialogPanel.add(closeButton);
		dialogBox.setWidget(dialogPanel);
		dialogBox.center();
	}
	
	public static void storeMarker(String latlng) {
		String[] latlongs = latlng.split(",");
		Double latitude = Double.parseDouble(latlongs[0]);
		Double longitude = Double.parseDouble(latlongs[1]);
		Console.printInfo("Sending marker: ["+latitude+","+longitude+"]");
		
		markerService.storeMarker(latitude, longitude,
				new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						Console.printError("Network failure (markerService.storeMarker).");
					}

					public void onSuccess(String result) {
						serverResponseLabel.setText("Done.");
						if (result == null) {
							Console.printError("Not logged in (markerService.storeMarker).");
							return;
						}
						
						closeButton.setFocus(true);
					}
				});
	}
	
	
	
	/**
	 * Window for choosing city to show markers in
	 */
	public void showMarker(){
		dialogBox.setText("Write a city to show available jobs");
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

		final Button sendButton = new Button("Send");
		sendButton.getElement().setId("sendButton");
		sendButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				// Click send button
				String city = cityField.getText();
				getCityMarkers(city);
			}
		});
		
		VerticalPanel dialogPanel = new VerticalPanel();
		dialogPanel.addStyleName("dialogVPanel");
		dialogPanel.add(new HTML("<b>City:</b>"));
		dialogPanel.add(cityField);
		dialogPanel.add(sendButton);
		dialogPanel.add(new HTML("<br><b>Server status:</b>"));
		dialogPanel.add(serverResponseLabel);
		dialogPanel.add(closeButton);
		dialogBox.setWidget(dialogPanel);
		dialogBox.center();
	}
	
	/**
	 * Get all the markers for a chosen city.
	 */
	private void getCityMarkers(String city) {
		serverResponseLabel.setText("Waiting for server...");
		markerService.getMarker(city,
				new AsyncCallback<Double[][]>() {
					public void onFailure(Throwable caught) {
						Console.printError("Network failure (markerService.getMarker).");
					}

					public void onSuccess(Double[][] result) {
						Console.printInfo("New markers: "+Arrays.deepToString(result));
						serverResponseLabel.setText("Done.");
						
						// Send markers to JS
						for (int i=0; i<result.length; i++) {
							addMarkerToMap(result[i][0], result[i][1], "Vafan", "Jaha!");
						}
					}
				});
	}
}
