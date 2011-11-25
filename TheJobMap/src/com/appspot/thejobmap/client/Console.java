package com.appspot.thejobmap.client;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class Console {
	private static final Console instance = new Console();
	private RootPanel dock = null;
	
	public static void init() {
		getSingleton().initialize();
		printInfo("The Job Map starting up.");
	}
	
	public static Console getSingleton() {
		return instance;
	}
	
	public static void printError(String error) {
		getSingleton().print(error, "error");
	}

	public static void printInfo(String info) {
		getSingleton().print(info, "info");
	}

	private void initialize() {
		dock = RootPanel.get("console");
	}
	
	public void print(String txt, String style) {
		Date now = new Date();
		DateTimeFormat fmt = DateTimeFormat.getFormat("[HH:mm:ss] ");
		Label label = new Label(fmt.format(now)+txt);
		label.setStyleName(style);
		dock.insert(label, 0);
	}
	
	
}
