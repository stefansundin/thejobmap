package com.appspot.thejobmap.client;

import java.util.Arrays;

import com.appspot.thejobmap.client.servlets.OpenIDService;
import com.appspot.thejobmap.client.servlets.OpenIDServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Client side part of the OpenID authentication.
 */
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
	
	/**
	 * Initialize.
	 */
	public void init() {
		initJSNI();
		checkLoggedIn();
		
		RootPanel.get("panel").add(accountPanel);
		accountPanel.getElement().setId("account");
		accountPanel.add(nameLabel);
		
		accountPanel.add(logButton);
		logButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (loggedIn) {
					logout();
				}
				else {
					createLoginGUI();
				}
			}
		});
		
		updateGUI();
	}
	
	/**
	 * Export GWT functions to JavaScript.
	 */
	public static native void initJSNI() /*-{
		$wnd.checkLoggedIn = $entry(@com.appspot.thejobmap.client.OpenID::checkLoggedIn()); 
	}-*/;
	
	/**
	 * Update GUI.
	 */
	public static void updateGUI() {
		if (loggedIn) {
			nameLabel.setText(getName());
			logButton.setText("Logout");
		}
		else {
			nameLabel.setText("");
			logButton.setText("Login");
		}
	}
	
	/**
	 * Return a human friendly name for the user.
	 */
	public static String getName() {
		if (!loggedIn) {
			return null;
		}
		if (name != null) {
			return name;
		}
		return email;
	}

	/**
	 * Ask the server if the user is logged in.
	 */
	public static void checkLoggedIn() {
		dialogBox.hide();
		
		openIDService.isLoggedIn(
				new AsyncCallback<String[]>() {
					public void onFailure(Throwable caught) {
						Console.printError("Network failure (openIDService.isLoggedIn).");
					}

					public void onSuccess(String[] result) {
						if (result == null) {
							Console.printInfo("Not logged in.");
							return;
						}
						
						Console.printInfo("Logged in ("+Arrays.deepToString(result)+").");
						loggedIn = true;
						name = result[0];
						email = result[1];
						privileges = result[2];
						logoutUrl = result[3];
						
						updateGUI();
					}
				});
	}
	
	/**
	 * Creates login GUI.
	 */
	private void createLoginGUI() {
		dialogBox.getElement().setId("login");
		dialogBox.setText("Login with OpenID");
		dialogBox.setAnimationEnabled(true);
		
		final VerticalPanel dialogPanel = new VerticalPanel();
		dialogPanel.addStyleName("dialogVPanel");
		dialogPanel.setSpacing(10);
		
		final Label waitLabel = new Label("Waiting for server...");
		dialogPanel.add(waitLabel);
		dialogBox.setWidget(dialogPanel);
		
		final VerticalPanel providerPanel = new VerticalPanel();
		dialogPanel.add(providerPanel);
		
		final Button closeButton = new Button("Close");
		dialogPanel.add(closeButton);

		// Get login urls from server
		openIDService.getUrls(
				new AsyncCallback<String[][]>() {
					public void onFailure(Throwable caught) {
						Console.printError("Network failure (openIDService.getUrls).");
					}

					public void onSuccess(String[][] providers) {
						Console.printInfo("Login services: ("+Arrays.deepToString(providers)+").");
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
		                
		                final VerticalPanel providerPanelDeep = new VerticalPanel();
		                providerPanelDeep.setVisible(false);
		                
		                // Add "show more providers" link
		                final Anchor showMore = new Anchor("+ Show more providers");
		                showMore.addClickHandler(new ClickHandler() {
		                	public void onClick(ClickEvent event) {
	                        	providerPanelDeep.setVisible(true);
	                        	showMore.setVisible(false);
	                        }
		                });
		                
		                // Add OpenID links
						for (String[] provider : providers) {
							Image image = new Image("images/openid/"+provider[0]+".png");
			                LoginClickHandler handler = new LoginClickHandler(provider[0], provider[1]);
			                image.addClickHandler(handler);
							
							if (provider[0].matches("google")) {
				                providerPanel.add(image);
							}
							else {
								providerPanelDeep.add(image);
							}
						}
						providerPanel.add(showMore);
						providerPanel.add(providerPanelDeep);
	                }
				});
		
		// Display dialog
		dialogBox.show();
		
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
	}

	/**
	 * Opens a popup window to the OpenID provider's login screen.
	 */
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

	/**
	 * Sends the user to the OpenID provider's logout screen, which then redirects back.
	 */
	protected void logout() {
		Window.Location.assign(logoutUrl);
	}

}
