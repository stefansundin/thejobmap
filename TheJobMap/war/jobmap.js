/**
 * The Job Map.
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */

/* http://closure-compiler.appspot.com/home

// ==ClosureCompiler==
// @compilation_level SIMPLE_OPTIMIZATIONS
// @output_file_name jobmap.min.js
// @code_url http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js
// @code_url http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.js
// ==/ClosureCompiler==

// <insert jobmap.js here>
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
		mapTypeControl: false
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
	filter: ['city'],
	mapMarkers: [],
	newMarker: null,
	myMarkers: [],
	mapControls: null,
	mapOverlay: null,
	infoWindow: null,
	pins: {},
	user: null,
	
	//Array with categories
	categories: {administration: 'Administration', construction: 'Construction', projectLeader: 'Project leader', computerScience: 'Computer science',
		disposalPromotion: 'Disposal & promotion', hotelRestaurant: 'Hotel & restaurant',medicalService: 'Health & medical service',
		industrialManufacturing: 'Industrial manufacturing',installation: 'Installation', cultureMedia: 'Culture, media, design', 
		military: 'Military', environmentalScience: 'Environmental science', pedagogical: 'Pedagogical', social: 'Social work', 
		security: 'Security', technical: 'Technical', transport: 'Transport', other: 'Other'},
	
	/**
	 * Initialize The Job Map.
	 */
	init: function(map) {
		$.ajaxSetup({
			contentType: 'application/json; charset=utf-8'
		});
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
		$('<button id="zoomOutButton">Zoom out</button>').click(jobmap.resetZoom).appendTo(mapControls);
		jobmap.mapControls = mapControls;
		map.controls[google.maps.ControlPosition.TOP_CENTER].push(mapControls[0]);
		
		// Info Window
		jobmap.infoWindow = new google.maps.InfoWindow({
			maxWidth: 400
		});
		google.maps.event.addListener(jobmap.map, 'click', function() {
			jobmap.infoWindow.close();
		});
		
		// Define pins
		// [size], [origin], [point]
		var shadow = {
			pin:     [[59,32], [0,34],  [16,32]],
			pushpin: [[59,32], [61,34], [9,32]]
		};
		$.each(shadow, function(i, m) {
			shadow[i] = new google.maps.MarkerImage(
					'/images/pins.png',
				new google.maps.Size(m[0][0],  m[0][1]),
				new google.maps.Point(m[1][0], m[1][1]),
				new google.maps.Point(m[2][0], m[2][1])
			);
		});
		var pins = {
			red:     [[32,32], [0,0],  [16,32], shadow.pin],
			green:   [[32,32], [32,0], [16,32], shadow.pin],
			blue:    [[32,32], [64,0], [16,32], shadow.pin],
			pushpin: [[32,32], [96,0], [9,32],  shadow.pushpin]
		};
		$.each(pins, function(i, m) {
			jobmap.pins[i] = {
				icon: new google.maps.MarkerImage(
						'/images/pins.png',
					new google.maps.Size(m[0][0],  m[0][1]),
					new google.maps.Point(m[1][0], m[1][1]),
					new google.maps.Point(m[2][0], m[2][1])
				),
				shadow: m[3]
			};
		});
		jobmap.pins.company = jobmap.pins.red;
		jobmap.pins.me      = jobmap.pins.green;
		jobmap.pins.random  = jobmap.pins.blue;
		jobmap.pins.city    = jobmap.pins.pushpin;
		jobmap.pins.admin   = jobmap.pins.pushpin;

		// Create pin legend
		var pinLegend = $('<div id="PinLegend">'+
				'<span class="right ui-icon ui-icon-circle-close" title="Close"></span>'+
				'<div><span class="city"></span> = A city</div>'+
				'<div><span class="red"></span> = A job offer</div>'+
				'<div><span class="green"></span> = You</div>'+
				'<div><span class="blue"></span> = Someone else</div>'+
				'</div>').css('opacity','0');
		$('.ui-icon',pinLegend).click(function() {
			$(pinLegend).fadeOut('fast');
		});
		map.controls[google.maps.ControlPosition.TOP_RIGHT].push(pinLegend[0]);
		setTimeout(function() {
			$(pinLegend).animate({opacity:1}, 1000);
		}, 1000);
		
		// User
		$('<div id="account"></div>').appendTo('#panel');
		$('<a id="accname"></a>').click(function() {
			jobmap.updateUserForm();
		}).addClass('hidden').appendTo('#account');
		$('<button id="logButton"></button>').click(jobmap.logButton).appendTo('#account');
		jobmap.getUser();
		
		// Add listeners
		google.maps.event.addListener(map, 'zoom_changed', jobmap.zoomChanged);
		
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
		'<div id="categorieList"></div>'+
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
		
		'<h3><a href="#"><b>Information for companies</b></a></h3><div>'+
		'<p>If you are a company and you want to put markers on the map, then send us an email and we will upgrade your account. '+
		' </p>'+
		'<p><b>Email: </b><a href="#">company@thejobmap.se</a></p>'+
		'</div>'+
		
		'<h3><a href="#"><b>About The Job Map</b></a></h3><div><p>'+
		'The Job Map is a project in course M7011E, Luleå university of technology, '+
		'made by Alexandra Tsampikakis and Stefan Sundin 2011. </p></div>').appendTo('#sidebar');
		
		$.each(jobmap.categories, function(id, cat){
			$('<label><input type="checkbox" id="'+id+'" /> '+cat+'</label><br/>').click(jobmap.filterMarkers).appendTo('#categorieList');
		});
		$('<label><input type="checkbox" id="showRandoms" /> Display job searchers</label><br/>').appendTo('#categorieList');
		
		$('#accordion input').attr('checked', true);
		$( "#accordion" ).accordion({ fillSpace: true });
	},
	
	/** Zoom */
	
	/**
	 * Filter markers based on zoom level.
	 */
	zoomChanged: function() {
		var zoom = jobmap.map.getZoom();
		var filter_old = jobmap.filter.slice();
		if (zoom > 7) {
			jobmap.filter = ['company', 'random', 'admin'];
		}
		else {
			jobmap.filter = ['city'];
		}
		
		// Filter markers if necessary
		/*var array_diff = function(a,b) {
			return a.filter(function(i) { return !(b.indexOf(i) > -1 || ); });
		};
		var filter_diff = array_diff(jobmap.filter, filter_old);
		if (filter_diff.length > 0) {*/
			jobmap.filterMarkers();
		//}
	},
	
	/**
	 * Zoom out button pressed:
	 */
	resetZoom: function() {
		jobmap.map.setZoom(5);
		jobmap.zoomChanged();
		jobmap.map.setCenter(new google.maps.LatLng(62.390369, 17.314453));
	},
	
	/**
	 * Zoom in if the user (anyone) pressed zoom when viewing a city marker.
	 */
	zoomToMarker: function(marker) {
		jobmap.map.setCenter(marker.mapMarker.getPosition());
		jobmap.map.setZoom(8);
		jobmap.zoomChanged();
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
			$.each(data, function(key, marker) {
				jobmap.markers.push(marker);
				jobmap.addMarker(marker);
			});
			jobmap.filterMarkers();
		})
		.fail(function(xhr,txt) {
			printError('Getting markers failed: '+txt+'.');
		});
	},

	/**
	 * Show markers by type.
	 */
	filterMarkers: function() {
		var selectedCategories = [];
		$('#categorieList :checked').each(function() {
			selectedCategories.push($(this).attr('id'));
		});
		$.each(jobmap.markers, function(i, marker) {
			var show = (jobmap.showAll
					|| (jobmap.isOwner(marker) && !jobmap.isAdmin())
					|| (jobmap.filter.indexOf(marker.type) != -1
							&& (marker.type != 'company' || selectedCategories.indexOf(marker.cat) != -1)));
			var now = (marker.mapMarker && marker.mapMarker.getMap() != null);
			if (show && !now) {
				marker.mapMarker.setMap(jobmap.map);
			}
			else if (!show && now) {
				marker.mapMarker.setMap(null);
			}
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
			title: marker.title
		});

		/*
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
		*/
		marker.mapMarker = mapMarker;
		jobmap.mapMarkers.push(mapMarker);

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
			jobmap.postMarker(marker);
		});
		
		// Add marker
		if (jobmap.filter.indexOf(marker.type) != -1
		 || (jobmap.isOwner(marker) && !jobmap.isAdmin())) {
			mapMarker.setMap(jobmap.map);
		}
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
			shadow: jobmap.pins.me.shadow
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
				title: ($('#markerTitle').val() || jobmap.user.name),
				type: ($('#markerType').val() || jobmap.user.privileges),
				cat: ($('#markerCat').val() || null),
				privacy: (($('#markerPrivacy').val()=='on'?'private':'public') || null)
			};
			json = JSON.stringify(marker);
			if (jobmap.user.privileges == 'random') {
				id = 'me';
				//marker.title = $('#markerTitle').val() || jobmap.user.name;
			}
			marker.creationDate = new Date().getTime();
			marker.author = jobmap.user.email;
		}
		printInfo('Sending marker: ', json);
		
		$.ajax({
			url: '/rest/marker/'+(id?id:''),
			type: 'POST',
			dataType: 'json',
			data: json
		})
		.done(function(data) {
			printInfo('Reply: ', data);
			jobmap.infoWindow.close();
			if (newMarker) {
				jobmap.newMarker.setMap(null);
				jobmap.newMarker = null;
				marker.author = jobmap.user.email;
				if (jobmap.user.privileges == 'random') {
					marker.id = jobmap.user.email;
				}
				else {
					marker.id = data.id;
				}
				jobmap.addMarker(marker);
			}
		})
		.fail(function(xhr,txt) {
			printError('Sending marker failed: '+txt+'.');
		});
	},

	/**
	 * Delete a marker.
	 */
	deleteMarker: function(marker) {
		$.ajax({
			url: '/rest/marker/'+marker.id,
			type: 'DELETE'
		})
		.done(function(data) {
			printInfo('Reply: ', data);
			marker.mapMarker.setMap(null);
			if (marker == jobmap.myMarkers[0]) {
				jobmap.myMarkers.splice(0, 1);
				$('#createMarkerButton',jobmap.mapControls).contents().replaceWith('Create my marker');
			}
			for (var i=0; i < jobmap.markers.length; i++) {
				if (marker.id == jobmap.markers[i].id) {
					delete jobmap.markers[i].id;
					break;
				}
			}
		})
		.fail(function(xhr,txt) {
			printError('Delete marker failed: '+txt+'.');
		});
	},

	/**
	 * Applies for a job.
	 */
	applyJob: function(marker) {
		$.ajax({
			url: '/rest/apply/'+marker.id,
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify({motivation:$('#applyInfo').val()})
		})
		.done(function(data) {
			printInfo('Reply: ', data);
			marker.numApply++;
		})
		.fail(function(xhr,txt) {
			printError('applyJob failed: '+txt+'.');
			$('#applyButton').text('Send application').attr('disabled', false);
			$('#applyInfo').attr('disabled', false);
		});
	},

	/**
	 * Create the contents of an info window for a marker.
	 */
	setInfoWindow: function(marker, mode) {
		if (!mode && marker == jobmap.newMarker) {
			mode = 'new';
			marker = {mapMarker: jobmap.newMarker};
		}
		if (!mode) mode='view';
		
		var pad = function(n) { return ('0'+n).slice(-2); };
		var creationDate = new Date(marker.creationDate);
		var timestamp = creationDate.getFullYear()+'-'+pad(creationDate.getMonth()+1)+'-'+pad(creationDate.getDate());
		
		var info = $('<div id="infoWindow"></div>').addClass(mode).addClass(marker.type);
		if (mode == 'view') {
			$('<h2></h2>').text(marker.title || "Titel").appendTo(info);
			$('<div id="desc"></div>').text(marker.info).appendTo(info);
			if (marker.type != 'city') {
				$(info).append('<hr/>');
			}
			if (jobmap.canEdit(marker)) {
				$('<button>Edit marker</button>').click(function() {
					jobmap.setInfoWindow(marker, 'edit');
					jobmap.infoWindow.open(jobmap.map, marker.mapMarker);
				}).appendTo(info);
			}
			if (marker.type == 'company' && jobmap.user && jobmap.user.privileges == 'random') {
				$('<button>Apply for job</button>').click(function() {
					if (!jobmap.user) {
						alert('You must log in first.');
						return;
					}
					jobmap.setInfoWindow(marker, 'apply');
					jobmap.infoWindow.open(jobmap.map, marker.mapMarker);
				}).appendTo(info);
			}
			if (marker.type == 'city') {
				$('<button id="zoomButton">Press here to zoom</button>').click(function() {
					jobmap.infoWindow.close();
					jobmap.zoomToMarker(marker);
				}).appendTo(info);
			}
			if (marker.type != 'city') {
				$('<div class="creationDate"></div>').text('Created on '+timestamp+'.').appendTo(info);
			}
			if (marker.type == 'company') {
				$('<span class="category" title="Category"></span>').text('Category: '+jobmap.categories[marker.cat]).append('<br/>').appendTo(info);
				$('<span class="numApply" title="Number of people who have applied for this job"></span>').text('Applications: '+marker.numApply).appendTo(info);
			}
		}
		else if (mode == 'apply') {
			$('<h3>Apply for job</h3>').appendTo(info);
			$('<textarea id="applyInfo"></textarea>')
				.attr('placeholder', 'Write a short motivation why we should hire you. Maximum 500 letters. Along with this text, your personal details and your CV will be attached automatically.')
				.appendTo(info);
			$('<br/>').appendTo(info);
			$('<button id="applyButton">Send application</button>').click(function() {
				var motivation = $('#applyInfo').val();
				if (motivation.length > 500) {
					alert('The motivation is limited to 500 characters. You have used '+motivation.length+'.');
					return;
				}
				$('#applyButton').text('Mail sent').attr('disabled', true);
				$('#applyInfo').attr('disabled', true);
				jobmap.applyJob(marker);
			}).appendTo(info);
		}
		else if (mode == 'edit' || mode == 'new') {
			$('<h3></h3>').text((mode=='edit'?'Edit marker':'Enter details')).appendTo(info);
			if (jobmap.user.privileges != 'random') {
				$('<input id="markerTitle" placeholder="Marker title" />').val(marker.title || jobmap.user.name).appendTo(info);
			}
			if (jobmap.isAdmin()) {
				$('<select id="markerType">'+
					'<option>company</option>'+
					'<option>city</option>'+
					'<option>admin</option>'+
					'<option>random</option>'+
					'</select>')
					.val(marker.type).change(function() {
						var type = $('#markerType').val();
						// Update mapMarker icon
						var pin = jobmap.pins[type];
						marker.mapMarker.setIcon(pin.icon);
						marker.mapMarker.setShadow(pin.shadow);
						// Hide or show category
						if (type == 'company') $('#markerCat').show();
						else $('#markerCat').hide();
					}).appendTo(info);
			}
			$('<textarea id="markerInfo"></textarea>')
				.attr('placeholder',(jobmap.user.privileges=='random'
					?'Write a little text about yourself here. It\'s what the companies will see first, so be intuitive.'
					:'Write the job description here. It\'s what the job searchers will see first, so be intuitive.'))
				.val(marker.info)
				.appendTo(info);
			$('<br/>').appendTo(info);
			// Add categories
			if (marker.type == 'company' || jobmap.user.privileges == 'company' || jobmap.isAdmin()) {
				var markerCat = $('<select id="markerCat"></select>').appendTo(info);
				$.each(jobmap.categories, function(id,cat) {
					$('<option></option>').val(id).text(cat).appendTo(markerCat);
				});
				$(markerCat).val(marker.cat);
			}
			if (jobmap.user.privileges == 'random') {
				printInfo((marker.privacy=='private'));
				$('<label><input type="checkbox" id="markerPrivacy" /> Only show my marker to companies</label>').appendTo(info);
				$('#markerPrivacy',info).attr('checked', (marker.privacy == 'private'));
			}
			// Save button
			$('<button></button>').text((mode=='edit'?'Save changes':'Store marker')).click(function() {
				if (mode == 'edit') {
					marker.title = $('#markerTitle').val() || marker.title;
					marker.info = $('#markerInfo').val();
					marker.type = $('#markerType').val() || marker.type;
					marker.cat = $('#markerCat').val() || marker.cat;
					marker.privacy = ($('#markerPrivacy').val()=='on'?'private':'public') || marker.privacy;
					jobmap.postMarker(marker);
					jobmap.infoWindow.close();
				}
				else {
					jobmap.postMarker();
				}
			}).appendTo(info);
			if (mode == 'edit') {
				$('<button>Delete marker</button>').click(function() {
					jobmap.deleteMarker(marker);
				}).appendTo(info);
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
		jobmap.infoWindow.close();
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
				}
			},
			close: function() {
				$(this).remove();
			}
		});

		// Add google image now to make it appear faster
		$('<img src="images/openid/google.png" id="openid-google" />').appendTo('#loginForm');
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
			
			$('#openid-google').click(data[0],openLoginWindow);
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
				if (jobmap.isOwner(marker) && !jobmap.isAdmin()) {
					marker.mapMarker.setIcon(jobmap.pins.me.icon);
					marker.mapMarker.setShadow(jobmap.pins.me.shadow);
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
	 * Opens the admin dialog.
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
				}
			},
			close: function() {
				$(this).remove();
			}
		});
		
		// Settings
		$('<h4>Settings:</h4>').appendTo('#adminDialog');
		$('<label><input type="checkbox" id="showAllMarkers" /> Always show all markers</label>').appendTo('#adminDialog');
		$('#showAllMarkers').click(function() {
			jobmap.showAll = ($('#showAllMarkers').val() == 'on');
			jobmap.filterMarkers();
		});
		$('<br/>').appendTo('#adminDialog');
		$('<br/>').appendTo('#adminDialog');
		
		// List of users
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
						phonenumber: $('#userPhonenumber').val()
					};
					if (jobmap.isAdmin()) {
						$.extend(userObj, {
							privileges: $('#userPrivileges').val()
						});
					}
					printInfo('Sending user ('+who+') details: ', userObj);
					
					$.ajax({
						url: '/rest/user/'+who,
						type: 'POST',
						dataType: 'json',
						data: JSON.stringify(userObj)
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
							type: 'DELETE'
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
				}
			},
			close: function() {
				$(this).remove();
			}
		})
		.keypress(function(e) {
			if (e.which == 13) {
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
				$('<p><label><input type="checkbox" id="userDeleteCv" /> Delete CV</label></p>').appendTo('#updateUserForm');
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
	}
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
