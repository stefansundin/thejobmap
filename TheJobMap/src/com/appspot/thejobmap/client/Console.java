package com.appspot.thejobmap.client;

import java.util.Date;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Console {
	private static final Console instance = new Console();
	private VerticalPanel dock = new VerticalPanel();
	
	public static void init() {
		getSingleton().initialize();
	}
	
	public static Console getSingleton() {
		return instance;
	}
	
	public static void printError(String error) {
		getSingleton().print(error);
	}

	public static void printInfo(String info) {
		printError(info);
	}

	private void initialize() {
		dock.getElement().setId("console");
		RootPanel.get().add(dock);
	}
	
	public void print(String txt) {
		Date date = new Date();
		Label errorLabel = new Label(date.toString()+": "+txt);
		dock.add(errorLabel);
	}
	
}
