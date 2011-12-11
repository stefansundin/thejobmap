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
	
	// Initialize The Job Map
	jobmap.init(map);
	
	// Make map resize dynamically
	window.addEventListener('resize', resizeMap, false);
	resizeMap();
}

var jobmap = {
	/** Variables */
	map: null,
	markers: [],
	mapMarkers: [],
	newMarker: null,
	myMarkers: [],
	updatedMarkers: [],
	mapControls: null,
	mapOverlay: null,
	infoWindow: null,
	pins: {},
	user: null,
	
	/**
	 * Initialize The Job Map.
	 */
	init: function(map) {
		jobmap.map = map;
		
		// Console
		$('body').keypress(function(e) {
			if (e.which == 167) { // '§'
				$('#console').removeClass('big').toggle();
			}
			else if (e.which == 189) { // shift+'§'
				$('#console').addClass('big').show();
			}
		});

		// Init OverlayView
		jobmap.mapOverlay = new google.maps.OverlayView();
		jobmap.mapOverlay.draw = function() {};
		jobmap.mapOverlay.setMap(jobmap.map);
		
		// Create map controls
		var mapControls = $('<div id="MapControls"></div>').css('opacity','0');
		$('<button id="refreshMarkersButton">Refresh markers</button>').click(jobmap.refreshMarkers).appendTo(mapControls);
		$('<button id="createMarkerButton">Create my marker</button>').click(jobmap.createMarker).attr('disabled',true).appendTo(mapControls);
		$('<button id="saveMarkerButton">Save markers</button>').click(jobmap.saveMarkers).attr('disabled',true).appendTo(mapControls);
		jobmap.mapControls = mapControls;
		map.controls[google.maps.ControlPosition.TOP_CENTER].push(mapControls[0]);
		
		// Info Window
		jobmap.infoWindow = new google.maps.InfoWindow({
			maxWidth: 400,
		});
		google.maps.event.addListener(jobmap.map, 'click', function() {
			jobmap.infoWindow.close();
		});
		
		// Define pins
		var shadow = new google.maps.MarkerImage(
			'images/pins/shadow.png',
			new google.maps.Size(59, 32),
			new google.maps.Point(0, 0),
			new google.maps.Point(16, 32)
		);
		var shadow_pushpin = new google.maps.MarkerImage(
			'images/pins/pushpin_shadow.png',
			new google.maps.Size(59, 32),
			new google.maps.Point(0, 0),
			new google.maps.Point(9, 32)
		);
		jobmap.pins.red = {
			icon: new google.maps.MarkerImage(
				'images/pins/red-dot.png',
				new google.maps.Size(32, 32),
				new google.maps.Point(0, 0),
				new google.maps.Point(16, 32)
			),
			shadow: shadow,
		};
		jobmap.pins.green = {
			icon: new google.maps.MarkerImage(
				'images/pins/green-dot.png',
				new google.maps.Size(32, 32),
				new google.maps.Point(0, 0),
				new google.maps.Point(16, 32)
			),
			shadow: shadow,
		};
		jobmap.pins.blue = {
			icon: new google.maps.MarkerImage(
				'images/pins/blue-dot.png',
				new google.maps.Size(32, 32),
				new google.maps.Point(0, 0),
				new google.maps.Point(16, 32)
			),
			shadow: shadow,
		};
		jobmap.pins.pushpin = {
			icon: new google.maps.MarkerImage(
				'images/pins/red-pushpin.png',
				new google.maps.Size(32, 32),
				new google.maps.Point(0, 0),
				new google.maps.Point(9, 32)
			),
			shadow: shadow_pushpin,
		};
		jobmap.pins.company = jobmap.pins.red;
		jobmap.pins.me = jobmap.pins.green;
		jobmap.pins.random = jobmap.pins.blue;
		jobmap.pins.city = jobmap.pins.pushpin;
		jobmap.pins.admin = jobmap.pins.pushpin;
		
		// User
		$('<div id="account"></div>').appendTo('#panel');
		$('<a id="accname"></a>').click(function() {
			jobmap.updateUserForm();
		}).addClass('hidden').appendTo('#account');
		$('<button id="logButton"></button>').click(jobmap.logButton).appendTo('#account');
		jobmap.getUser();
		
		// Markers
		jobmap.refreshMarkers();
		
		// Side menu
		jobmap.sideMenu();
	},
	
	//The side menu
	sideMenu: function(){
		$('<div id="accordion"><h3><a href="#"><b>Find a job</b></a></h3><div>'+
		'<p>Click on a city to see the available jobs in the area. Then uncheck the boxes for the categories you are not interested in.</p>'+
		'<p><b>Filter jobs:</b></p>'+
		'<label><input type="checkbox" id="administration" />Administration</label><br/>'+
		'<label><input type="checkbox" id="construction" />Construction</label><br/>'+
		'<label><input type="checkbox" id="projectLeader" />Project leader</label><br/>'+
		'<label><input type="checkbox" id="computerScience" />Computer science</label><br/>'+
		'<label><input type="checkbox" id="disposalPromotion" />Disposal & promotion</label><br/>'+
		'<label><input type="checkbox" id="hotelRestaurant" />Hotel & restaurant</label><br/>'+
		'<label><input type="checkbox" id="medicalService" />Health & medical service</label><br/>'+
		'<label><input type="checkbox" id="industrialManufacturing" />Industrial manufacturing </label><br/>'+
		'<label><input type="checkbox" id="installation" />Installation/maintenance</label><br/>'+
		'<label><input type="checkbox" id="cultureMedia" />Culture, media, design</label><br/>'+
		'<label><input type="checkbox" id="military" />Military</label><br/>'+
		'<label><input type="checkbox" id="environmentalScience" />Environmental science</label><br/>'+
		'<label><input type="checkbox" id="pedagogical" />Pedagogical</label><br/>'+
		'<label><input type="checkbox" id="social" />Social work</label><br/>'+
		'<label><input type="checkbox" id="security" />Security</label><br/>'+
		'<label><input type="checkbox" id="technical" />Technical</label><br/>'+
		'<label><input type="checkbox" id="transport" />Transport</label><br/>'+
		'<label><input type="checkbox" id="other" />Other</label>'+
		'</div>'+
		
		'<h3><a href="#"><b>Log in</b></a></h3><div>'+
		'<p>The first time you log in, you can either update '+
		'your profile with personal information or just view all '+
		'the markers on the map. When you are logged in you can apply '+
		'for the jobs you are interested in.</p>'+
		'</div>'+
		
		'<h3><a href="#"><b>Apply for a job</b></a></h3><div>'+
		'<p>If you want to apply for a job, just click on the marker '+
		'and press apply for job. Make sure you have a CV uploaded, write a '+
		'personal note to the company and then press send.</p>'+  
		'</div>'+
		
		'<h3><a href="#"><b>Put yourself on the map</b></a></h3><div>'+
		'<p>Place a marker where you live so all the '+
		'companies can see you. You can choose if you want to be visible '+
		'for just companies or both companies and other poeple who is looking for a job.</p>'+
		'</div>'+
		
		'<h3><a href="#"><b>Information for company</b></a></h3><div>'+
		'<p>If you are a company and you want to put markers on the map, then send us an email and we will upgrade your account. '+
		' </p>'+
		'<p><b>Email: </b><a href="#">company@thejobmap.se</a></p>'+
		'</div>'+
		
		'<h3><a href="#"><b>About The Job Map</b></a></h3><div><p>'+
		'The Job Map is a project in course M7011E, Luleå university of technology, '+
		'made by Alexandra Tsampikakis and Stefan Sundin 2011. </p></div>').appendTo('#sidebar');
		
		$('#accordion input').attr('checked', true);
		$( "#accordion" ).accordion({ fillSpace: true });
	},
	
	/** Markers */
	
	/**
	 * Clear all markers from the map.
	 */
	clearMarkers: function() {
		$('#saveMarkerButton',jobmap.adminControls).attr('disabled', true);
		if (jobmap.newMarker != null) {
			jobmap.newMarker.setMap(null);
			jobmap.newMarker = null;
		}
		for (var i=0; i < jobmap.mapMarkers.length; i++) {
			jobmap.mapMarkers[i].setMap(null);
		}
		jobmap.markers = [];
		jobmap.mapMarkers = [];
	},
	
	/**
	 * Fetch markers from server.
	 */
	refreshMarkers: function() {
		$.getJSON('/rest/marker')
		.done(function(data) {
			printInfo('Received '+data.length+' markers: ', data);
			jobmap.clearMarkers();
			$.each(data, function(key, val) {
				jobmap.addMarker(val);
			});
		})
		.fail(function(xhr,txt) {
			printError('Getting markers failed: '+txt+'.');
		});
	},
	
	/**
	 * Add a marker to the map.
	 */
	addMarker: function(marker) {
		//printInfo('Adding marker at ('+marker.lat+','+marker.lng+')');
		
		// Construct mapMarker
		var mapMarker = new google.maps.Marker({
			//map: jobmap.map,
			position: new google.maps.LatLng(marker.lat, marker.lng),
			draggable: jobmap.canEdit(marker),
		});
		
		// Check if we already have the marker
		var alreadyAdded = false;
		if (marker.id) {
			for (var i=0; i < jobmap.markers.length; i++) {
				if (jobmap.markers[i].id == marker.id) {
					alreadyAdded = true;
					var oldMapMarker = jobmap.markers[i].mapMarker;
					oldMapMarker.setPosition(mapMarker.position);
					marker.mapMarker = mapMarker = oldMapMarker;
					jobmap.markers[i] = marker;
					break;
				}
			}
		}
		if (!alreadyAdded) {
			// This is a new marker, add it
			mapMarker.setMap(jobmap.map);
			marker.mapMarker = mapMarker;
			jobmap.markers.push(marker);
			jobmap.mapMarkers.push(mapMarker);
		}
		
		// Set marker icon
		var pin = jobmap.pins[(!jobmap.isAdmin()&&jobmap.isOwner(marker))?'me':marker.type];
		mapMarker.setIcon(pin.icon);
		mapMarker.setShadow(pin.shadow);
		
		// Is this my marker?
		if (jobmap.isOwner(marker)) {
			jobmap.myMarkers.push(marker);
			if (jobmap.user.privileges == 'random') {
				$('#createMarkerButton',jobmap.mapControls).contents().replaceWith('Edit my marker');
			}
		}
		
		// Add listeners
		google.maps.event.addListener(mapMarker, 'click', function() {
			jobmap.setInfoWindow(marker);
			jobmap.infoWindow.open(jobmap.map, mapMarker);
		});
		google.maps.event.addListener(mapMarker, 'dragend', function() {
			jobmap.updatedMarkersPush(marker);
		});
	},
	
	/**
	 * Create a new marker.
	 */
	createMarker: function() {
		// Has the user already placed a marker?
		if (jobmap.user.privileges == 'random' && jobmap.myMarkers.length != 0) {
			google.maps.event.trigger(jobmap.myMarkers[0].mapMarker, 'click');
			return;
		}
		
		// Remove old newMarker if on the map
		if (jobmap.newMarker != null) {
			jobmap.newMarker.setMap(null);
		}
		
		// Create a new marker on the map for the user
		jobmap.newMarker = new google.maps.Marker({
			map: jobmap.map,
			position: new google.maps.LatLng(
					jobmap.mapOverlay.getProjection().fromContainerPixelToLatLng(new google.maps.Point(0,150)).lat(),
					jobmap.map.getCenter().lng()),
			title: 'Drag me!',
			draggable: true,
			animation: google.maps.Animation.BOUNCE,
			icon: jobmap.pins.me.icon,
			shadow: jobmap.pins.me.shadow,
		});
		google.maps.event.addListenerOnce(jobmap.newMarker, 'mouseover', function() {
			jobmap.newMarker.setAnimation(null);
		});
		google.maps.event.addListener(jobmap.newMarker, 'click', function() {
			jobmap.setInfoWindow(jobmap.newMarker);
			jobmap.infoWindow.open(jobmap.map, jobmap.newMarker);
		});
	},
	
	/**
	 * Send a marker to the server.
	 */
	postMarker: function(marker) {
		var newMarker = (!marker);
		var id;
		var json;
		if (marker) {
			// Editing existing marker
			id = marker.id;
			var mapMarker = marker.mapMarker;
			marker.lat = mapMarker.getPosition().lat();
			marker.lng = mapMarker.getPosition().lng();
			delete marker.mapMarker;
			json = JSON.stringify(marker);
			marker.mapMarker = mapMarker;
		}
		else {
			// New marker
			marker = {
				lat: jobmap.newMarker.getPosition().lat(),
				lng: jobmap.newMarker.getPosition().lng(),
				info: $('#markerInfo').val(),
				title: $('#markerTitle').val() || jobmap.user.name,
			};
			json = JSON.stringify(marker);
			if (jobmap.user.privileges == 'random') {
				id = 'me';
				//marker.title = $('#markerTitle').val() || jobmap.user.name;
			}
			marker.creationDate = new Date().getTime();
		}
		printInfo('Sending marker: ', json);
		printInfo('id: '+id);
		
		$.ajax({
			url: '/rest/marker/'+(id?id:''),
			type: 'POST',
			dataType: 'json',
			data: json,
		})
		.done(function(data) {
			jobmap.infoWindow.close();
			if (newMarker) {
				jobmap.newMarker.setMap(null);
				jobmap.newMarker = null;
				marker.author = jobmap.user.email;
				if (jobmap.user.privileges == 'random') {
					marker.id = jobmap.user.email;
				}
				jobmap.addMarker(marker);
			}
		})
		.fail(function(xhr,txt) {
			printError('Sending marker failed: '+txt+'.');
		});
	},

	/**
	 * Pushes marker to updatedMarkers, but makes sure there are no duplicates.
	 * Also updates mapMarker properties (like icon).
	 */
	updatedMarkersPush: function(marker) {
		// Update mapMarker
		var pin = jobmap.pins[(!jobmap.isAdmin()&&jobmap.isOwner(marker))?'me':marker.type];
		marker.mapMarker.setIcon(pin.icon);
		marker.mapMarker.setShadow(pin.shadow);
		
		// Check if marker is already in list
		for (var i=0; i < jobmap.updatedMarkers.length; i++) {
			if (marker == jobmap.updatedMarkers[i]) {
				return;
			}
		}
		
		jobmap.updatedMarkers.push(marker);
		$('#saveMarkerButton',jobmap.adminControls).attr('disabled', false);
	},
	
	/**
	 * Send all updated markers to postMarker().
	 */
	saveMarkers: function() {
		$('#saveMarkerButton',jobmap.adminControls).attr('disabled', true);
		printInfo('Saving '+jobmap.updatedMarkers.length+' markers.');
		for (var i=0; i < jobmap.updatedMarkers.length; i++) {
			jobmap.postMarker(jobmap.updatedMarkers[i]);
		}
		jobmap.updatedMarkers = [];
	},
	
	/**
	 * Create the contents of an info window for a marker.
	 */
	setInfoWindow: function(marker, mode) {
		if (!mode && marker == jobmap.newMarker) mode='new';
		if (!mode) mode='view';
		
		var pad = function(n) { return ('0'+n).slice(-2); };
		var creationDate = new Date(marker.creationDate);
		var timestamp = creationDate.getFullYear()+'-'+pad(creationDate.getMonth()+1)+'-'+pad(creationDate.getDate());
		
		var info = $('<div id="infoWindow"></div>');
		if (mode == 'new') {
			$(info).append('<h3>Enter details</h3>');
			if (jobmap.user.privileges != 'random') {
				$('<input id="markerTitle" placeholder="Marker title" />').appendTo(info);
			}
			$(info).append('<textarea id="markerInfo" placeholder="Write description here"></textarea>');
			$(info).append('<br/>');
			$('<button>Store marker</button>').click(function() {
				jobmap.postMarker();
			}).appendTo(info);
		}
		else if (mode == 'edit') {
			$(info).append('<h3>Edit marker</h3>');
			if (jobmap.user.privileges != 'random') {
				$('<input id="markerTitle" placeholder="Marker title" />').val(marker.title).appendTo(info);
			}
			$('<textarea id="markerInfo" placeholder="Write description here"></textarea>').val(marker.info).appendTo(info);
			$(info).append('<br/>');
			$('<button>Save changes</button>').click(function() {
				marker.title = $('#markerTitle').val() || marker.title;
				marker.info = $('#markerInfo').val();
				marker.type = $('#markerType').val() || marker.type;
				jobmap.updatedMarkersPush(marker);
				jobmap.infoWindow.close();
			}).appendTo(info);
			if (jobmap.isAdmin()) {
				$('<span>Type: </span>').add(($('<select id="markerType"></select>')
						.append($('<option>random</option>'))
						.append($('<option>company</option>'))
						.append($('<option>city</option>'))
						.append($('<option>admin</option>'))
					).val(marker.type)).appendTo(info);
			}
		}
		else if (mode == 'view') {
			$('<h2></h2>').text(marker.title || "Titel").appendTo(info);
			$('<div id="desc"></div>').text(marker.info).appendTo(info);
			if (marker.type != 'city') {
				$(info).append('<hr/>');
			}
			if (jobmap.isOwner(marker) || jobmap.canEdit(marker)) {
				$('<button>Edit marker</button>').click(function() {
					if (!jobmap.canEdit(marker)) {
						alert('Please refresh markers to edit a newly added marker.');
						return;
					}
					jobmap.setInfoWindow(marker, 'edit');
				}).appendTo(info);
			}
			if (marker.type != 'city') {
				$('<div id="creationDate"></div>').text('Created on '+timestamp+'.').appendTo(info);
			}
		}
		
		jobmap.infoWindow.setContent(info[0]);
	},
	
	/** User */
	
	/**
	 * Handler for the login/logout button.
	 */
	logButton: function() {
		if (jobmap.user) {
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
			position: ['right', 50],
			height: 230,
			width: 260,
			modal: true,
			autoOpen: true,
			draggable: false,
			resizable: false,
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
			});
		})
		.fail(function(xhr,txt) {
			printError('Getting OpenID providers failed: '+txt+'.');
		});
	},
	
	/**
	 * Gets user info from the server.
	 */
	getUser: function(who) {
		if (!who) who='me';
		
		$.getJSON('/rest/user/'+who)
		.done(function(data) {
			$('#loginForm').dialog('destroy');
			if (data.info == 'not logged in') {
				printInfo('Not logged in.');
				return;
			}
			printInfo('User ('+who+'): ', data);
			jobmap.user = data;
			
			// Update controls
			$('#createMarkerButton',jobmap.mapControls).attr('disabled', false);
			$('#accname').empty().append(jobmap.getUsername()).removeClass('hidden');
			$(jobmap.mapControls).animate({opacity:1}, 'slow');
			if (jobmap.isAdmin()) {
				$('<button id="adminButton">Admin</button>').click(jobmap.admin).appendTo('#account');
			}
			
			// Go through already added markers
			if (jobmap.user.privileges == 'company') {
			
			}
			$.each(jobmap.markers, function(i, marker) {
				if (jobmap.isOwner(marker)) {
					marker.mapMarker.setIcon(jobmap.pins.me);
					marker.mapMarker.setShadow(jobmap.pins.shadow);
				}
				if (jobmap.canEdit(marker)) {
					marker.mapMarker.setDraggable(true);
				}
			});
		})
		.fail(function(xhr,txt) {
			printError('getUser failed: '+txt+'.');
		})
		.always(function() {
			$('#logButton').empty().append(jobmap.user?'Logout':'Login');
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
	 * Open the admin dialog.
	 */
	admin: function() {
		if ($('#adminDialog').length) return;
		
		$('<div id="adminDialog"></div>').dialog({
			title: 'Admin',
			dialogClass: 'adminDialog',
			position: ['right', 40],
			height: 500,
			width: 360,
			autoOpen: true,
			buttons: {
				Done: function() {
					$(this).dialog('close');
				},
			},
			close: function() {
				$(this).remove();
			},
		});
		
		$('<h4>List of users:</h4>').appendTo('#adminDialog');
		$.getJSON('/rest/user')
		.done(function(data) {
			if (data.result == 'fail') {
				printError('Fail: '+data.info+'.');
				return;
			}
			printInfo('Users: ', data);
			
			$.each(data, function(key, val) {
				$('<a></a>').append(val.email).click(function() {
					jobmap.updateUserForm(val);
				}).appendTo('#adminDialog');
			});
			
		})
		.fail(function(xhr,txt) {
			printError('getUsers failed: '+txt+'.');
		});
	},
	
	/**
	 * Returns true if user is admin, false otherwise.
	 */
	isAdmin: function() {
		return (jobmap.user && jobmap.user.privileges == 'admin');
	},
	
	/**
	 * Returns true if user is the creator of marker.
	 */
	isOwner: function(marker) {
		return (jobmap.user && jobmap.user.email == marker.author);
	},
	
	/**
	 * Returns true if user has capabilities to edit marker.
	 */
	canEdit: function(marker) {
		return (marker.id && (jobmap.isAdmin() || jobmap.isOwner(marker)));
	},
	
	/**
	 * Returns a nicely formatted name for the user.
	 */
	getUsername: function() {
		if (!jobmap.user) {
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
	updateUserForm: function(user) {
		if (!user) user=jobmap.user;
		var who = jobmap.who = (user==jobmap.user?'me':user.email);
		if ($('#updateUserForm').length) return;
		
		$('<div id="updateUserForm"></div>').dialog({
			title: (who=='me'?'Your personal information':who),
			dialogClass: 'updateUserForm',
			position: ['right', 70],
			height: 530,
			width: 380,
			buttons: {
				Save: function() {
					var userObj = {
						name: $('#userName').val(),
						age: $('#userAge').val(),
						sex: $('#userSex').val(),
						phonenumber: $('#userPhonenumber').val(),
					};
					if (jobmap.isAdmin()) {
						$.extend(userObj, {
							privileges: $('#userPrivileges').val(),
						});
					}
					printInfo('Sending user ('+who+') details: ', userObj);
					
					$.ajax({
						url: '/rest/user/'+who,
						type: 'POST',
						dataType: 'json',
						data: JSON.stringify(userObj),
					})
					.done(function(data) {
						printInfo('Reply: ', data);
						$('#updateUserForm').dialog('close');
						$.extend(user, userObj);
						$('#accname').empty().append(jobmap.getUsername());
					})
					.fail(function(xhr,txt) {
						printError('Sending user details failed: '+txt+'.');
					});
					
					// Delete CV?
					if ($('#userDeleteCv').attr('checked')) {
						printInfo('Deleting CV');

						$.ajax({
							url: '/rest/user/'+who+'/cv',
							type: 'DELETE',
						})
						.done(function(data) {
							printInfo('Reply: ', data);
							user.cvUploaded = false;
						})
						.fail(function(xhr,txt) {
							printError('Delete CV failed: '+txt+'.');
						});
					}
				},
				Cancel: function() {
					$(this).dialog('close');
				},
			},
			close: function() {
				$(this).remove();
			},
		})
		.keypress(function(e) {
			if(e.which == 13) {
				$("#updateUserForm").parents('.ui-dialog').first().find('.ui-button').first().click();
			}
		});
		$('<p>Email: </p>').add($('<input type="text" id="userEmail" readonly />').val(user.email)).appendTo('#updateUserForm');
		$('<p>Name: </p>').add($('<input type="text" id="userName" placeholder="Your name" />').val(user.name)).appendTo('#updateUserForm');
		if (jobmap.user.privileges != 'company') {
		$('<p>Age: </p>').add($('<input type="number" id="userAge" placeholder="Your age" />').val(user.age)).appendTo('#updateUserForm');
		$('<p>Sex: </p>').add(($('<select id="userSex"></select>')
				.append($('<option>Not telling</option>'))
				.append($('<option>Male</option>'))
				.append($('<option>Female</option>'))
				.append($('<option>Other</option>'))
			).val(user.sex)).appendTo('#updateUserForm');
		}
		$('<p>Phone number: </p>').add($('<input type="tel" id="userPhonenumber" placeholder="Your phone number" />').val(user.phonenumber)).appendTo('#updateUserForm');
		if (jobmap.isAdmin()) {
			$('<p>Privileges: </p>').add(($('<select id="userPrivileges"></select>')
				.append($('<option>random</option>'))
				.append($('<option>company</option>'))
				.append($('<option>admin</option>'))
			).val(user.privileges)).appendTo('#updateUserForm');
		}
		$('<hr/>').appendTo('#updateUserForm');
		
		// CV
		if (jobmap.user.privileges != 'company') {
			if (user.cvUploaded) {
				$('<p><a href="/rest/user/'+who+'/cv" target="_blank">View my CV</a></p>').appendTo('#updateUserForm');
				$('<p><label><input type="checkbox" id="userDeleteCv" />Delete CV</label></p>').appendTo('#updateUserForm');
			}
			else {
				jobmap.cvFrameLoaded = false;
				$('<p>Upload CV (pdf only, maximum size is 1 MB): </p>').add('<p><iframe src="/upload-cv.html" id="cvIframe" scrolling="no" frameborder="0" onload="jobmap.cvFrameOnload();"></iframe></p>').appendTo('#updateUserForm');
			}
		}
	},
	
	/**
	 * Called when the iframe loads to insert the uploadUrl to the upload form.
	 */
	cvFrameOnload: function() {
		if (jobmap.cvFrameLoaded) {
			// Second onload. CV was probably uploaded successfully.
			printInfo('cvUploaded!!');
			if (jobmap.who == 'me') {
				jobmap.user.cvUploaded = true;
			}
			return;
		}
		jobmap.cvFrameLoaded = true;
		
		$.getJSON('/rest/user/'+jobmap.who+'/cv/uploadUrl')
		.done(function(data) {
			printInfo('CV upload url: ', data);
			$('#cvIframe').contents().find('#form').attr('action', data.uploadUrl);
		})
		.fail(function(xhr,txt) {
			printError('Getting CV upload url failed: '+txt+'.');
		});
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
	var pad = function(n) { return ('0'+n).slice(-2); };
	var timestamp = '['+pad(now.getHours())+':'+pad(now.getMinutes())+':'+pad(now.getSeconds())+'] ';
	var line = $('<div class="'+style+'">'+timestamp+txt+'</div>');
	if (json) {
		if (typeof json != 'string') json=JSON.stringify(json);
		line = line.append('<tt>'+json+'</tt>');
	}
	$('#console').prepend(line);
}
function printInfo(txt, json) {
	print(txt, json, 'info');
}
function printError(txt, json) {
	print(txt, json, 'error');
	$('#console').show();
}

// Google Analytics
var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-27056070-2']);
_gaq.push(['_trackPageview']);

(function() {
	var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
	ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
	var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();
