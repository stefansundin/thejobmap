package com.appspot.thejobmap.client;

import com.appspot.thejobmap.client.servlets.OpenIDService;
import com.appspot.thejobmap.client.servlets.OpenIDServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class OpenID {

	private static Boolean loggedIn = false;
	private static String name;
	private static String email;
	private static String privileges;
	private static String logoutUrl;
	
	private static OpenIDServiceAsync openIDService = GWT.create(OpenIDService.class);

	private final static HorizontalPanel accountPanel = new HorizontalPanel();
	private final static Label nameLabel = new Label();
	private final static Button logButton = new Button("Logout");
	
	private final static DialogBox dialogBox = new DialogBox();

	public static void updateGUI() {
		nameLabel.setText(getName());
		if (loggedIn) {
			logButton.setText("Logout");
		}
		else {
			logButton.setText("Login");
		}
	}
	
	public void init() {
		initJSNI();
		checkLoggedIn();
		
		RootPanel.get("panel").add(accountPanel);
		accountPanel.getElement().setId("account");
		accountPanel.add(nameLabel);
		accountPanel.setBorderWidth(1);
		
		accountPanel.add(logButton);
		logButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (loggedIn) {
					logout();
				}
				else {
					createGUI();
				}
			}
		});
		
		updateGUI();
	}

	protected void logout() {
		Window.Location.assign(logoutUrl);
	}

	public static native void initJSNI() /*-{
		$wnd.checkLoggedIn = $entry(@com.appspot.thejobmap.client.OpenID::checkLoggedIn()); 
	}-*/;
	
	public static String getName() {
		if (!loggedIn) {
			return "not logged in";
		}
		if (name != null) {
			return name;
		}
		return email;
	}

	public static void checkLoggedIn() {
		dialogBox.hide();
		
		openIDService.isLoggedIn(
				new AsyncCallback<String[]>() {
					public void onFailure(Throwable caught) {
						Console.printError("Network failure.");
					}

					public void onSuccess(String[] result) {
						if (result == null) {
							Console.printInfo("Not logged in.");
							return;
						}
						
						Console.printInfo("Logged in ("+result+").");
						loggedIn = true;
						name = result[0];
						email = result[1];
						privileges = result[2];
						logoutUrl = result[3];
						
						updateGUI();
					}
				});
	}
	
	private void createGUI() {
		dialogBox.setText("Login");
		dialogBox.setAnimationEnabled(true);
		
		final VerticalPanel dialogVPanel = new VerticalPanel();
		final VerticalPanel providerPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		
		final Label waitLabel = new Label("Waiting for server...");
		dialogVPanel.add(waitLabel);
		dialogBox.setWidget(dialogVPanel);
		dialogVPanel.add(providerPanel);
		
		final Button closeButton = new Button("Close");
		dialogVPanel.add(closeButton);

		// Get login urls from server
		openIDService.getUrls(
				new AsyncCallback<String[][]>() {
					public void onFailure(Throwable caught) {
						Console.printError("Network failure.");
					}

					public void onSuccess(String[][] providers) {
						Console.printInfo("Login services: ("+providers+").");
						waitLabel.removeFromParent();
						
		                class LoginClickHandler implements ClickHandler {
		                	String provider;
		                	String url;
		                	public LoginClickHandler(String provider, String url) {
								this.provider = provider;
								this.url = url;
							}
	                        public void onClick(ClickEvent event) {
	                        	openLoginWindow(provider, url);
	                        }
		                }

		                // Add OpenID links
						for (String[] provider : providers) {
							Image image = new Image("images/openid/"+provider[0]+".png");
			                LoginClickHandler handler = new LoginClickHandler(provider[0], provider[1]);
			                image.addClickHandler(handler);
							image.getElement().getStyle().setCursor(Cursor.POINTER);
			                providerPanel.add(image);
						}
	                }
				});
		
		// Display dialog
		dialogBox.center();
		
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		
		//dialogVPanel.add(new HTML("<b>Logged in?</b> "+userService.isUserLoggedIn()+"<br>"));
		/*dialogVPanel.add(new HTML("<b>Username: </b> "));
		dialogVPanel.add(new HTML("<br>"));
		dialogVPanel.add(new HTML("<b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);*/
	}
	
	private void openLoginWindow(String provider, String url) {
		int width = 800;
		int height = 600;
		int left = Window.getClientWidth()/2 - width/2;
		int top = Window.getClientHeight()/2 - height/2;
		String features = "width=" + width + ",height=" + height + ",left="
				+ left + ",top=" + top + ",screenX=" + left + ",screenY=" + top
				+ ",location=yes,status=yes,resizable=yes";
		Window.open(url, "thejobmap-login", features);
	}
}
