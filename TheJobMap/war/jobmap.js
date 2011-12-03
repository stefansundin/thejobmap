/**
 * The Job Map.
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */


/**
 * Initialize environment.
 */
function initialize() {
	var mapOptions = {
		zoom: 5,
		center: new google.maps.LatLng(62.390369, 17.314453),
		mapTypeId: google.maps.MapTypeId.ROADMAP,
		streetViewControl: false,
		mapTypeControl: false,
	};
	var map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);
	
	// Add controls
	jobmap.init(map);
	map.controls[google.maps.ControlPosition.TOP_CENTER].push(jobmap.mapControls);
	
	// Request markers
	jobmap.refreshMarkers();
	
	// Make map resize dynamically
	window.addEventListener("resize", resizeMap, false);
	resizeMap();
	
	// Google analytics
	var _gaq = _gaq || [];
	_gaq.push(['_setAccount', 'UA-27056070-2']);
	_gaq.push(['_trackPageview']);
	
	(function() {
		var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
		ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
		var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
	})();
}

var jobmap = {
	/** Variables */
	map: null,
	markers: [],
	newMarker: null,
	mapControls: null,
	infoWindow: null,
	user: {
		loggedIn: false,
		email: null,
		name: null,
		privileges: null,
	},
	
	/**
	 * Initialize The Job Map.
	 */
	init: function(map) {
		jobmap.map = map;

		// Console
		$('body').keypress(function(e) {
			if (e.which == 167) { // '§'
				$('#console').toggle();
			}
		})
		
		// Create buttons
		var c = $('<div id="JobMapControls"></div>');
		jobmap.mapControls = c[0];
		$('<button id="refreshMarkersButton">Refresh markers</button>').appendTo(c);
		$('<button id="createMarkerButton">Create marker</button>').appendTo(c);
		google.maps.event.addDomListener($("#refreshMarkersButton",c)[0], "click", jobmap.refreshMarkers);
		google.maps.event.addDomListener($("#createMarkerButton",c)[0], "click", jobmap.createMarker);
		
		// Info Window
		jobmap.infoWindow = new google.maps.InfoWindow({});
		google.maps.event.addListener(jobmap.map, 'click', function() {
			jobmap.infoWindow.close();
		});
		
		// User
		$('#panel').append('<div id="account"></div>');
		$('#account').append('<span id="username"> </span>');
		$('<button id="logButton"> </button>').click(jobmap.logButton).appendTo('#account');
		jobmap.getUser();
	},
	
	/** Markers */
	
	/**
	 * Clear all markers from the map.
	 */
	clearMarkers: function() {
		if (jobmap.newMarker != null) {
			jobmap.newMarker.setMap(null);
			jobmap.newMarker = null;
		}
		for (var i=0; i < jobmap.markers.length; i++) {
			jobmap.markers[i].setMap(null);
		}
		jobmap.markers = [];
	},
	
	/**
	 * Fetch markers from server.
	 */
	refreshMarkers: function() {
		jobmap.clearMarkers();
		
		$.getJSON("/rest/marker")
		.done(function(data) {
			printInfo("Received markers: ", data);
			$.each(data, function(key, val) {
				jobmap.addMarker(val);
			});
		});
	},
	
	/**
	 * Add a marker to the map.
	 */
	addMarker: function(m) {
		// Define Marker properties
		/*
		var image = new google.maps.MarkerImage('images/markers/ltulogo.png',
			new google.maps.Size(22, 22),
			new google.maps.Point(0,0),
			new google.maps.Point(10, 22)
		);
		*/
		
		var marker = new google.maps.Marker({
			map: jobmap.map,
			position: new google.maps.LatLng(m.lat, m.lng),
			//icon: image,
			//icon: "",
		});
		jobmap.markers.push(marker);

		// Add listener for a click on the pin
		google.maps.event.addListener(marker, 'click', function() {
			jobmap.infoWindow.setContent(jobmap.createInfo(m));
			jobmap.infoWindow.open(jobmap.map, marker);
		});
	},
	
	/**
	 * Create a new marker.
	 */
	createMarker: function() {
		if (jobmap.newMarker != null) {
			jobmap.newMarker.setMap(null);
		}
		
		jobmap.newMarker = new google.maps.Marker({
			map: jobmap.map,
			position: jobmap.map.getCenter(),
			title: "Drag me!",
			draggable: true,
			animation: google.maps.Animation.BOUNCE,
		});
		google.maps.event.addListenerOnce(jobmap.newMarker, "mouseover", function() {
			jobmap.newMarker.setAnimation(null);
		});
		google.maps.event.addListener(jobmap.newMarker, "click", function() {
			jobmap.infoWindow.setContent(jobmap.createInfo(jobmap.newMarker));
			jobmap.infoWindow.open(jobmap.map, jobmap.newMarker);
		});
		/*
		var infowindow = new google.maps.InfoWindow({
			content: createInfo("Enter details", '<textarea id="markerInfo" placeholder="Write description here"></textarea><br/><button onclick="jobmap.storeMarker();">Store marker</button>')
		});*/
	},
	
	/**
	 * Send a marker to the server.
	 */
	storeMarker: function() {
		var marker = {
			lat: jobmap.newMarker.getPosition().lat(),
			lng: jobmap.newMarker.getPosition().lng(),
			info: $("#markerInfo").val(),
		};
		printInfo("Sending marker: ", marker);
		
		$.ajax({
			url: "/rest/marker",
			type: "POST",
			dataType: "json",
			data: JSON.stringify(marker),
		})
		.done(function(data) {
			printInfo("Reply: ", data);
		})
		.fail(function(xhr,txt) {
			printError("Sending marker failed: "+txt+".");
		});
		
		jobmap.newMarker.setMap(null);
		jobmap.newMarker = null;
		jobmap.addMarker(marker);
	},
	
	/**
	 * Create the contents of an info window for a marker.
	 */
	createInfo: function(marker) {
		if (marker == jobmap.newMarker) {
			return '<b>Enter details</b><p><textarea id="markerInfo" placeholder="Write description here"></textarea><br/><button onclick="jobmap.storeMarker();">Store marker</button></p>';
		}
		return marker.info;
	},
	
	/** User */
	
	/**
	 * Handler for the login/logout button.
	 */
	logButton: function() {
		if (jobmap.user.loggedIn) {
			jobmap.logout();
		}
		else {
			jobmap.loginForm();
		}
	},
	
	/**
	 * Creates the login dialog.
	 */
	loginForm: function() {
		$('<div id="loginForm"></div>').dialog({
			title: "Login with OpenID",
			autoOpen: true,
			dialogClass: "loginDialog",
			modal: true,
			draggable: false,
			resizable: false,
			height: 230,
			width: 260,
			buttons: {
				Cancel: function() {
					$(this).dialog("close");
				}
			},
			close: function() {
				$(this).remove();
			}
		});

		$.getJSON("/rest/openid")
		.done(function(data) {
			printInfo("OpenID providers: ", data);
			var openLoginWindow = function(e) {
				var width = 800;
				var height = 600;
				window.open(e.data.loginUrl, "thejobmap-openid",
					"width="+width+",height="+height+","+
					"left="+($(window).width()/2-width/2)+",top="+($(window).height()/2-height/2)+
					",location=yes,status=yes,resizable=yes");
			};
			
			$('<img src="images/openid/'+data[0].name+'.png" />').click(data[0],openLoginWindow).appendTo('#loginForm');
			var moreProviders = $('<div id="moreProviders"></div>');
			$('<a>+ Show more providers</a>').click(function() {
				$(this).replaceWith(moreProviders);
				$('#loginForm').dialog("option", "height", 400);
			}).appendTo('#loginForm');
			$.each(data.slice(1), function(key, val) {
				$('<img src="images/openid/'+val.name+'.png" />').click(val,openLoginWindow).appendTo(moreProviders);
				jobmap.addMarker(val);
			});
		})
		.fail(function(xhr,txt) {
			printError("Getting OpenID providers failed: "+txt+".");
		});
	},
	
	/**
	 * Gets user info from the server.
	 */
	getUser: function() {
		$.getJSON("/rest/user")
		.done(function(data) {
			$('#loginForm').dialog("destroy");
			if (data.error == "not logged in") {
				printInfo("Not logged in.");
				return;
			}

			printInfo("User: ", data);
			jobmap.user = data;
			
			$('#username').contents().replaceWith(jobmap.user.email);
			$('#logButton').contents().replaceWith('Logout');
		})
		.fail(function(xhr,txt) {
			printError("getUser failed: "+txt+".");
		})
		.always(function() {
			$('#username').contents().replaceWith(jobmap.getUsername());
			$('#logButton').contents().replaceWith(jobmap.user.loggedIn?'Logout':'Login');
			$('#account').fadeIn('slow');
		});
	},
	
	/**
	 * Returns a nicely formatted name for the user.
	 */
	getUsername: function() {
		if (!jobmap.user.loggedIn) {
			return " ";
		}
		if (jobmap.user.name) {
			return jobmap.user.name;
		}
		return jobmap.user.email;
	},
	
	/**
	 * Redirects the user to the logout url.
	 */
	logout: function() {
		window.location.assign(jobmap.user.logoutUrl);
	},
}

// Dynamically resize map
function resizeMap() {
	var page = document.getElementById("page");
	var panel = document.getElementById("panel");
	var viewportHeight = document.body.clientHeight;
	
	page.style.height = (viewportHeight-panel.offsetHeight)+"px";
}

// Console
function print(txt, style, json) {
	var now = new Date();
	var pad = function(n) { return ("0"+n).slice(-2); }
	var timestamp = "["+pad(now.getHours())+":"+pad(now.getMinutes())+":"+pad(now.getSeconds())+"] ";
	$("#console").prepend('<div class="'+style+'">'+timestamp+txt+(json?JSON.stringify(json):"")+'</div>');
}
function printInfo(txt, json) {
	print(txt, 'info', json);
}
function printError(txt, json) {
	print(txt, 'error', json);
	$('#console').show();
}
