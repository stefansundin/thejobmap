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
	var map = new google.maps.Map(document.getElementById('map_canvas'), mapOptions);
	
	// Add controls
	jobmap.init(map);
	map.controls[google.maps.ControlPosition.TOP_CENTER].push(jobmap.mapControls);
	
	// Request markers
	jobmap.refreshMarkers();
	
	// Make map resize dynamically
	window.addEventListener('resize', resizeMap, false);
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
	mapMarkers: [],
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
		google.maps.event.addDomListener($('#refreshMarkersButton',c)[0], 'click', jobmap.refreshMarkers);
		google.maps.event.addDomListener($('#createMarkerButton',c)[0], 'click', jobmap.createMarker);
		
		// Info Window
		jobmap.infoWindow = new google.maps.InfoWindow({});
		google.maps.event.addListener(jobmap.map, 'click', function() {
			jobmap.infoWindow.close();
		});
		
		// User
		$('<div id="account"></div>').appendTo('#panel');
		$('<span id="username"> </span>').click(jobmap.updateUserForm).appendTo('#account');
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
		for (var i=0; i < jobmap.mapMarkers.length; i++) {
			jobmap.mapMarkers[i].setMap(null);
		}
		jobmap.mapMarkers = [];
	},
	
	/**
	 * Fetch markers from server.
	 */
	refreshMarkers: function() {
		jobmap.clearMarkers();
		
		$.getJSON('/rest/marker')
		.done(function(data) {
			printInfo('Received markers: ', data);
			$.each(data, function(key, val) {
				jobmap.addMarker(val);
			});
		});
	},
	
	/**
	 * Add a marker to the map.
	 */
	addMarker: function(marker) {
		// Construct mapMarker
		var mapMarker = new google.maps.Marker({
			map: jobmap.map,
			position: new google.maps.LatLng(marker.lat, marker.lng),
			draggable: jobmap.isAdmin(),
		});
		
		/*
		// Check if we already have the marker
		$.each(jobmap.markers, function(i, m) {
			if (m.id == marker.id) {
				var oldMapMarker = jobmap.markers[i].mapMarker;
				oldMapMarker.setPosition(mapMarker.position);
				jobmap.markers[i] = marker;
				mapMarker = oldMapMarker;
				return false;
			}
			else if (i == jobmap.markers.length-1) {
				marker.mapMarker = mapMarker;
				jobmap.markers.push(marker);
				jobmap.mapMarkers.push(mapMarker);
				mapMarker.setMap(jobmap.map);
			}
		});
		*/

		marker.mapMarker = mapMarker;
		jobmap.markers.push(marker);
		jobmap.mapMarkers.push(mapMarker);
		
		// Add listener for a click on the pin
		google.maps.event.addListener(mapMarker, 'click', function() {
			jobmap.infoWindow.setContent(jobmap.createInfo(marker));
			jobmap.infoWindow.open(jobmap.map, mapMarker);
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
			title: 'Drag me!',
			draggable: true,
			animation: google.maps.Animation.BOUNCE,
		});
		google.maps.event.addListenerOnce(jobmap.newMarker, 'mouseover', function() {
			jobmap.newMarker.setAnimation(null);
		});
		google.maps.event.addListener(jobmap.newMarker, 'click', function() {
			jobmap.infoWindow.setContent(jobmap.createInfo(jobmap.newMarker));
			jobmap.infoWindow.open(jobmap.map, jobmap.newMarker);
		});
	},
	
	/**
	 * Send a marker to the server.
	 */
	storeMarker: function() {
		var marker = {
			lat: jobmap.newMarker.getPosition().lat(),
			lng: jobmap.newMarker.getPosition().lng(),
			info: $('#markerInfo').val(),
		};
		printInfo('Sending marker: ', marker);
		
		$.ajax({
			url: '/rest/marker',
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(marker),
		})
		.done(function(data) {
			printInfo('Reply: ', data);
		})
		.fail(function(xhr,txt) {
			printError('Sending marker failed: '+txt+'.');
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
			title: 'Login with OpenID',
			dialogClass: 'loginDialog',
			autoOpen: true,
			modal: true,
			draggable: false,
			resizable: false,
			height: 230,
			width: 260,
			buttons: {
				Cancel: function() {
					$(this).dialog('close');
				},
			},
			close: function() {
				$(this).remove();
			},
		});

		$.getJSON('/rest/openid')
		.done(function(data) {
			printInfo('OpenID providers: ', data);
			var openLoginWindow = function(e) {
				var width = 800;
				var height = 600;
				window.open(e.data.loginUrl, 'thejobmap-openid',
					'width='+width+',height='+height+','+
					'left='+($(window).width()/2-width/2)+',top='+($(window).height()/2-height/2)+
					',location=yes,status=yes,resizable=yes');
			};
			
			$('<img src="images/openid/'+data[0].name+'.png" />').click(data[0],openLoginWindow).appendTo('#loginForm');
			var moreProviders = $('<div id="moreProviders"></div>');
			$('<a>+ Show more providers</a>').click(function() {
				$(this).replaceWith(moreProviders);
				$('#loginForm').dialog('option', 'height', 400);
			}).appendTo('#loginForm');
			$.each(data.slice(1), function(key, val) {
				$('<img src="images/openid/'+val.name+'.png" />').click(val,openLoginWindow).appendTo(moreProviders);
				jobmap.addMarker(val);
			});
		})
		.fail(function(xhr,txt) {
			printError('Getting OpenID providers failed: '+txt+'.');
		});
	},
	
	/**
	 * Gets user info from the server.
	 */
	getUser: function() {
		$.getJSON('/rest/user')
		.done(function(data) {
			$('#loginForm').dialog('destroy');
			if (data.info == 'not logged in') {
				printInfo('Not logged in.');
				return;
			}

			printInfo('User: ', data);
			jobmap.user = data;
			
			$('#username').contents().replaceWith(jobmap.user.email);
			$('#logButton').contents().replaceWith('Logout');
		})
		.fail(function(xhr,txt) {
			printError('getUser failed: '+txt+'.');
		})
		.always(function() {
			$('#username').contents().replaceWith(jobmap.getUsername());
			$('#logButton').contents().replaceWith(jobmap.user.loggedIn?'Logout':'Login');
			$('#account').fadeIn('slow');
		});
	},
	
	/**
	 * Redirects the user to the logout url.
	 */
	logout: function() {
		window.location.assign(jobmap.user.logoutUrl);
	},

	/**
	 * Returns true if user is admin, false otherwise.
	 */
	isAdmin: function() {
		return (jobmap.user.privileges == 'admin');
	},
	
	/**
	 * Returns a nicely formatted name for the user.
	 */
	getUsername: function() {
		if (!jobmap.user.loggedIn) {
			return ' ';
		}
		if (jobmap.user.name) {
			return jobmap.user.name;
		}
		return jobmap.user.email;
	},
	
	/**
	 * 
	 */
	updateUserForm: function() {
		if (!jobmap.user.loggedIn || $('#updateUserForm').length) return;
		$('<div id="updateUserForm"></div>').dialog({
			title: 'Your personal information',
			dialogClass: 'userDialog',
			autoOpen: true,
			resizable: false,
			height: 500,
			width: 360,
			buttons: {
				Save: function() {
					var user = {
						name: $('#userName').val(),
						age: $('#userAge').val(),
						sex: $('#userSex').val(),
						phonenumber: $('#userPhonenumber').val(),
						education: $('#userEducation').val(),
						workExperience: $('#userWorkExperience').val(),
					};
					printInfo('Sending user details: ', user);
					
					$.ajax({
						url: '/rest/user',
						type: 'POST',
						dataType: 'json',
						data: JSON.stringify(user),
					})
					.done(function(data) {
						printInfo('Reply: ', data);
						$.extend(jobmap.user, user);
						$('#updateUserForm').dialog('close');
					})
					.fail(function(xhr,txt) {
						printError('Sending user details failed: '+txt+'.');
					});
				},
				Cancel: function() {
					$(this).dialog('close');
				},
			},
			close: function() {
				$(this).remove();
			},
		});
		$('<p>Email: </p>').append($('<input type="text" id="userEmail" readonly />').val(jobmap.user.email)).appendTo('#updateUserForm');
		$('<p>Name: </p>').append($('<input type="text" id="userName" placeholder="Your name" />').val(jobmap.user.name)).appendTo('#updateUserForm');
		$('<p>Age: </p>').append($('<input type="text" id="userAge" placeholder="Your age" />').val(jobmap.user.age)).appendTo('#updateUserForm');
		$('<p>Sex: </p>').append($('<input type="text" id="userSex" placeholder="Your sex" />').val(jobmap.user.sex)).appendTo('#updateUserForm');
		$('<p>Phone number: </p>').append($('<input type="text" id="userPhonenumber" placeholder="Your phone number" />').val(jobmap.user.phonenumber)).appendTo('#updateUserForm');
		$('<p>Education: </p>').append($('<input type="text" id="userEducation" placeholder="Your education" />').val(jobmap.user.education)).appendTo('#updateUserForm');
		$('<p>Work Experience: </p>').append($('<input type="text" id="userWorkExperience" placeholder="Number of years" />').val(jobmap.user.workExperience)).appendTo('#updateUserForm');
		$('<p>Upload CV: </p>').append($('<iframe src="/upload-cv.html" id="cvIframe" width="200" height="25" scrolling="no" frameborder="0" onload="jobmap.cvFrameOnload();"></iframe>')).appendTo('#updateUserForm');
		
		jobmap.cvFrameLoaded = false;
		jobmap.cvUploadUrl = false;
		// Get CV upload url
		$.getJSON('/rest/user/cv/getUploadUrl')
		.done(function(data) {
			printInfo('CV upload url: ', data);
			jobmap.cvUploadUrl = data.uploadUrl;
			if (jobmap.cvFrameLoaded) {
				$('#cvIframe').contents().find('#form').attr('action', jobmap.cvUploadUrl);
			}
		})
		.fail(function(xhr,txt) {
			printError('Getting CV upload url failed: '+txt+'.');
		});
	},
	
	/**
	 * Called when the iframe loads to insert the uploadUrl to the upload form.
	 */
	cvFrameOnload: function() {
		jobmap.cvFrameLoaded = true;
		if (jobmap.cvUploadUrl) {
			$('#cvIframe').contents().find('#form').attr('action', jobmap.cvUploadUrl);
		}
	},
};

// Dynamically resize map
function resizeMap() {
	var page = document.getElementById('page');
	var panel = document.getElementById('panel');
	var viewportHeight = document.body.clientHeight;
	
	page.style.height = (viewportHeight-panel.offsetHeight)+'px';
}

// Console
function print(txt, json, style) {
	if (!style) style = 'info';
	if (json && json.result == 'fail') style = 'error';
	var now = new Date();
	var pad = function(n) { return ('0'+n).slice(-2); }
	var timestamp = '['+pad(now.getHours())+':'+pad(now.getMinutes())+':'+pad(now.getSeconds())+'] ';
	$('#console').prepend('<div class="'+style+'">'+timestamp+txt+(json?JSON.stringify(json):'')+'</div>');
}
function printInfo(txt, json) {
	print(txt, json, 'info');
}
function printError(txt, json) {
	print(txt, json, 'error');
	$('#console').show();
}
