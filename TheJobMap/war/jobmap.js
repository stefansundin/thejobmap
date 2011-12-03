
var map = null;

function initialize() {
	var mapOptions = {
		zoom: 5,
		center: new google.maps.LatLng(62.390369, 17.314453),
		mapTypeId: google.maps.MapTypeId.ROADMAP,
		streetViewControl: false,
		mapTypeControl: false,
	};
	map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);
	
	// Add controls
	jobmap.init(map);
	map.controls[google.maps.ControlPosition.TOP_CENTER].push(jobmap.mapControls[0]);
	
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
	map: null,
	markers: [],
	newMarker: null,
	mapControls: null,
	
	init: function(map) {
		jobmap.map = map;

		// Create buttons
		var c = $('<div id="JobMapControls"></div>');
		$(c).append('<button id="RefreshMarkersButton">Refresh markers</button>');
		google.maps.event.addDomListener($("#RefreshMarkersButton",c)[0], "click", jobmap.refreshMarkers);
		$(c).append('<button id="CreateMarkerButton">Create marker</button>');
		google.maps.event.addDomListener($("#CreateMarkerButton",c)[0], "click", jobmap.createMarker);
		jobmap.mapControls = c;
		
		// User
		$('#panel').append('<div id="account">hej</div>');
		$('#account').append('<button>Login</button>').click(jobmap.loginForm);
	},
	
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
			infowindow.open(map, marker);
		});

		// Add information window
		var infowindow = new google.maps.InfoWindow({
			content: createInfo("Titel", m.info)
		});
	},
	
	createMarker: function() {
		if (jobmap.newMarker != null) {
			jobmap.newMarker.setMap(null);
		}
		
		jobmap.newMarker = new google.maps.Marker({
			map: jobmap.map,
			position: map.getCenter(),
			title: "Drag me!",
			draggable: true,
			animation: google.maps.Animation.BOUNCE,
		});
		google.maps.event.addListenerOnce(jobmap.newMarker, "mouseover", function() {
			jobmap.newMarker.setAnimation(null);
		});
		google.maps.event.addListener(jobmap.newMarker, "click", function() {
			infowindow.open(map, jobmap.newMarker);
		});
		
		var infowindow = new google.maps.InfoWindow({
			content: createInfo("Enter details", '<textarea id="markerInfo" placeholder="Write description here"></textarea><br/><button onclick="jobmap.storeMarker();">Store marker</button>')
		});
	},
	
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
	
	loginForm: function() {
		$('<div id="loginForm"></div>').dialog({
			title: "Login with OpenID",
			autoOpen: true,
			dialogClass: "loginDialog",
			//position: ["right", "top"],
			modal: true,
			draggable: false,
			resizable: false,
			height: 300,
			width: 350,
			buttons: {
				Cancel: function() {
					$( this ).dialog( "close" );
				}
			},
		});

		$.getJSON("/rest/openid")
		.done(function(data) {
			printInfo("OpenID providers: ", data);
			$.each(data, function(key, val) {
				$('#loginForm').append('<img src="images/openid/'+val.name+'.png" />').click(function() {
					window.open(val.loginUrl, "thejobmap-login",
							"width=800,height=600,"+
							"left="+($(window).width()/2-800/2)+",top="+($(window).height()/2-600/2)+
							",location=yes,status=yes,resizable=yes");
				});
				jobmap.addMarker(val);
			});
		});
	},
	
	checkLoggedIn: function() {
		$.getJSON("/rest/user")
		.done(function(data) {
			printInfo("User info: ", data);
			$('#account').prepend(data.email);
		});
	}
}



// Create information window
function createInfo(title, content) {
	return '<div class="infowindow"><strong>'+ title +'</strong><p>'+content+'</p></div>';
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
}
