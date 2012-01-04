// ==ClosureCompiler==
// @compilation_level SIMPLE_OPTIMIZATIONS
// @output_file_name jobmap.min.js
// @code_url http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js
// @code_url http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.js
// ==/ClosureCompiler==
// http://closure-compiler.appspot.com/home
// <insert jobmap.js here>

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
		mapTypeControl: false
	};
	var map = new google.maps.Map(document.getElementById('map_canvas'), mapOptions);
	
	// Initialize The Job Map
	jobmap.init(map);
	
	// Make map resize dynamically
	window.addEventListener('resize', resizeMap, false);
	resizeMap();
}

/**
 * The jobmap object.
 */
var jobmap = {
	/** Variables */
	map: null,
	markers: [],
	filter: {type:['city'], cat:[]},
	newMarker: null,
	myMarkers: [],
	searchMatches: [],
	mapControls: null,
	mapOverlay: null,
	infoWindow: null,
	pins: {},
	user: null,
	
	/**
	 * Available categories.
	 */
	categories: {
		administration: 'Administration', construction: 'Construction', projectLeader: 'Project leader', computerScience: 'Computer science',
		disposalPromotion: 'Disposal & promotion', hotelRestaurant: 'Hotel & restaurant', medicalService: 'Health & medical service',
		industrialManufacturing: 'Industrial manufacturing', installation: 'Installation', cultureMedia: 'Culture, media, design', 
		military: 'Military', environmentalScience: 'Environmental science', pedagogical: 'Pedagogical', social: 'Social work', 
		security: 'Security', technical: 'Technical', transport: 'Transport', other: 'Other'
	},
	
	/**
	 * Initialize The Job Map.
	 */
	init: function(map) {
		jobmap.map = map;
		
		// Setup ajax
		$.ajaxSetup({
			contentType: 'application/json; charset=UTF-8'
		});
		$(document).ajaxError(function(e, xhr, settings, exception) {
			printError(settings.type+' '+settings.url+' failed: ', xhr.responseText.replace(/<.*?>/g,"").trim());
		});
		
		// Console
		$(document).keypress(function(e) {
			if (document.activeElement != document.body) return;
			if (e.which == 167) { // '§'
				$('#console').removeClass('big').toggle();
			}
			else if (e.which == 189) { // shift+'§'
				$('#console').addClass('big').show();
			}
		});

		// Zoom
		google.maps.event.addListener(map, 'zoom_changed', jobmap.zoomChanged);
		jobmap.zoom = jobmap.map.getZoom();

		// InfoWindow
		jobmap.infoWindow = new google.maps.InfoWindow({
			maxWidth: 400
		});
		google.maps.event.addListener(jobmap.map, 'click', function() {
			jobmap.infoWindow.close();
		});
		
		// Side menu
		jobmap.sideMenu();
		
		// OverlayView
		jobmap.mapOverlay = new google.maps.OverlayView();
		jobmap.mapOverlay.draw = function() {};
		jobmap.mapOverlay.setMap(jobmap.map);
		
		// Create map controls
		var mapControls = $('<div id="MapControls"></div>').css('opacity','0');
		$('<button id="refreshMarkersButton">Refresh markers</button>').click(function() {
			jobmap.clearSearch();
			jobmap.clearMarkers();
			jobmap.getMarkers('');
		}).appendTo(mapControls);
		$('<button id="createMarkerButton"></button>').click(jobmap.createMarker).appendTo(mapControls);
		$('<button id="zoomOutButton">Zoom out</button>').click(jobmap.resetZoom).appendTo(mapControls);
		jobmap.mapControls = mapControls;
		map.controls[google.maps.ControlPosition.TOP_CENTER].push(mapControls[0]);
		
		// Define pins
		// [size], [origin], [point]
		var shadow = {
			pin:     [[59,32], [0,34],  [16,32]],
			pushpin: [[59,32], [61,34], [9,32]]
		};
		$.each(shadow, function(i, m) {
			shadow[i] = new google.maps.MarkerImage('/images/pins.png',
				new google.maps.Size(m[0][0],  m[0][1]),
				new google.maps.Point(m[1][0], m[1][1]),
				new google.maps.Point(m[2][0], m[2][1])
			);
		});
		var pins = {
			red:     [[32,32], [0,0],  [16,32], shadow.pin],
			green:   [[32,32], [32,0], [16,32], shadow.pin],
			blue:    [[32,32], [64,0], [16,32], shadow.pin],
			pushpin: [[32,32], [96,0], [9,32],  shadow.pushpin],
			search:  [[32,32], [128,0],[16,32], shadow.pin]
		};
		$.each(pins, function(i, m) {
			jobmap.pins[i] = {
				icon: new google.maps.MarkerImage('/images/pins.png',
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
				'<div><span class="pinLegend city"></span> = A city</div>'+
				'<div><span class="pinLegend red"></span> = A job offer</div>'+
				'<div><span class="pinLegend search"></span> = Search result</div>'+
				'<div><span class="pinLegend green"></span> = You</div>'+
				'<div><span class="pinLegend blue"></span> = Someone else</div>'+
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
		
		// Search
		$('<div id="search"></div>')
		.append($('<input type="search" id="q" placeholder="Examples: mechanic, nurse, teacher" />')
			.keypress(function(e) {
				if (e.which == 13) jobmap.search();
				$(this).attr('oldvalue', $(this).val());
			})
			.click(function() {
				if ($(this).val() == '' && $(this).attr('oldvalue') != '') jobmap.clearSearch();
			}))
		.append(' ')
		.append($('<button>Search jobs</button>')
			.click(jobmap.search))
		.append($('<p id="searchResult"></p>'))
		.appendTo('#panel');
		
		// Make requests
		jobmap.getMarkers('city');
		setTimeout(function() {
			jobmap.getUser();
		}, 50);
		setTimeout(function() {
			jobmap.getMarkers('');
		}, 500);
	},
	
	/**
	 * Initialize the side menu.
	 */
	sideMenu: function() {
		$('<div id="accordion">'+
		'<h3><a href="#"><b>Find a job</b></a></h3>'+
		'<div>'+
		'<p>Click on a city to see available jobs in the area.</p>'+
		'<p>Then uncheck the boxes for the categories you are not interested in.</p>'+
		'<p><b>Filter jobs:</b></p>'+
		'<div id="categoryList"></div>'+
		'</div>'+
		
		'<h3><a href="#"><b>Log in</b></a></h3>'+
		'<div>'+
		'<p>You must log in to apply for jobs on The Job Map.</p>'+
		'<p>When you log in for the first time, you should update your profile and upload a CV. Your CV is automatically attached to your job applications.</p>'+
		'<p>When that is done, feel free to apply for the jobs your are interested in.</p>'+
		'</div>'+
		
		'<h3><a href="#"><b>Apply for a job</b></a></h3>'+
		'<div>'+
		'<p>To apply for a job, simply click the marker and then press the button called "Apply for job". You will be asked to write a short motivation to send along with your application.</p>'+
		'<p>Make sure you have a CV uploaded since it will be attached to your job application.</p>'+
		'</div>'+
		
		'<h3><a href="#"><b>Put yourself on the map</b></a></h3>'+
		'<div>'+
		'<p>Press the "Create my marker" button on the top of the map to put yourself on the map. Place it where you live so the companies in your area can see you.</p>'+
		'<p>You can choose if you want the visibility of your marker to be limited to companies only.</p>'+
		'</div>'+
		
		'<h3><a href="#"><b>Information for companies</b></a></h3>'+
		'<div>'+
		'<p>If you are a company and you want to put job offers on the map, then send us an email and we will upgrade your account.</p>'+
		'<p>You must send the email from the same account that you use to login to The Job Map. You must also include details about your company.</p>'+
		'<p><b>Email: </b><a href="mailto:company@thejobmap.se?subject=Company%20upgrade%20request" target="_blank">company@thejobmap.se</a></p>'+
		'</div>'+
		
		'<h3><a href="#"><b>About The Job Map</b></a></h3>'+
		'<div>'+
		'<p>The Job Map is a project by <b>Alexandra Tsampikakis</b> and <b>Stefan Sundin</b>.</p>'+
		'<p>You can contact us at <i>firstname</i>@thejobmap.se.</p>'+
		'</div>'+
		'</div>').appendTo('#sidebar');
		
		$.each(jobmap.categories, function(id, cat){
			$('<label><input type="checkbox" value="'+id+'" /> '+cat+'</label><br/>').change(jobmap.filterMarkers).appendTo('#categoryList');
			jobmap.filter.cat.push(id);
		});
		$('<label><input type="checkbox" id="showRandoms" /> Display job searchers</label><br/>').change(jobmap.filterMarkers).appendTo('#categoryList');
		
		$('#accordion input').attr('checked', true);
		$('#accordion').accordion({ fillSpace: true });
	},
	
	/** Zoom */
	
	/**
	 * Filter markers based on zoom level.
	 */
	zoomChanged: function() {
		//printInfo('old zoom: '+jobmap.zoom+', new zoom: '+jobmap.map.getZoom());
		
		var old_zoom = jobmap.zoom;
		jobmap.zoom = jobmap.map.getZoom();
		if ((old_zoom <= 7 && jobmap.zoom <= 7) || (old_zoom > 7 && jobmap.zoom > 7)) {
			return; // No update is required
		}
		
		if (jobmap.zoom > 7) {
			jobmap.filter.type = ['company', 'admin'];
			if ($('#showRandoms')[0].checked) {
				jobmap.filter.type.push('random');
			}
		}
		else {
			jobmap.filter.type = ['city'];
		}
		
		jobmap.filterMarkers();
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
		$.each(jobmap.markers, function(key, marker) {
			marker.mapMarker.setMap(null);
		});
		jobmap.markers = [];
	},
	
	/**
	 * Fetch markers from server.
	 */
	getMarkers: function(type) {
		$.getJSON('/rest/marker/'+(type?type:''))
		.done(function(data) {
			printInfo('Received '+data.length+' markers: ', data);
			$.each(data, function(key, marker) {
				jobmap.addMarker(marker);
			});
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

		// Check if we already have the marker
		var alreadyAdded = false;
		for (var i=0; i < jobmap.markers.length; i++) {
			if (jobmap.markers[i].id == marker.id) {
				// Update existing marker
				alreadyAdded = true;
				var oldMapMarker = jobmap.markers[i].mapMarker;
				oldMapMarker.setPosition(mapMarker.position);
				marker.mapMarker = mapMarker = oldMapMarker;
				jobmap.markers[i] = marker;
				break;
			}
		}
		if (!alreadyAdded) {
			// This is a new marker
			marker.mapMarker = mapMarker;
			jobmap.markers.push(marker);
		}

		// Set marker icon
		var pin = jobmap.pins[(!jobmap.isAdmin()&&jobmap.isOwner(marker))?'me':marker.type];
		mapMarker.setIcon(pin.icon);
		mapMarker.setShadow(pin.shadow);
		
		// Is this my marker?
		if (jobmap.isOwner(marker)) {
			jobmap.myMarkers.push(marker);
			if (jobmap.user.privileges == 'random') {
				$('#createMarkerButton',jobmap.mapControls).text('Edit my marker');
				google.maps.event.trigger(jobmap.map, 'resize');
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
		if (marker.type == 'city') {
			google.maps.event.addListener(mapMarker, 'dblclick', function() {
				jobmap.infoWindow.close();
				jobmap.zoomToMarker(marker);
			});
		}
		
		// Add marker
		if (jobmap.markerVisible(marker)) {
			mapMarker.setMap(jobmap.map);
		}
	},

	/**
	 * Filter markers based on type and categories.
	 */
	filterMarkers: function() {
		// Update category filter
		jobmap.filter.cat = [];
		$('#categoryList :checked').each(function() {
			jobmap.filter.cat.push($(this).val());
		});
		
		// Debug output
		printInfo('Filtering for: ', jobmap.filter);
		
		// Filter markers
		$.each(jobmap.markers, function(i, marker) {
			var show = jobmap.markerVisible(marker);
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
	 * Returns whether or not a marker should be visible with the current filters, and so on.
	 */
	markerVisible: function(marker) {
		return (jobmap.showAll
			|| (jobmap.isOwner(marker) && !jobmap.isAdmin())
			|| (jobmap.filter.type.indexOf(marker.type) != -1 && (marker.type != 'company' || jobmap.filter.cat.indexOf(marker.cat) != -1))
			|| (jobmap.searchMatches.indexOf(marker) != -1));
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
				type: ($('#markerType').val() || jobmap.user.privileges)
			};
			if (marker.type == 'random') {
				marker.privacy = ($('#markerPrivacy')[0].checked?'private':'public');
			}
			else if (marker.type == 'company') {
				marker.cat = $('#markerCat').val();
			}
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
				marker.id = data.id;
				jobmap.addMarker(marker);
			}
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
				if (jobmap.user.privileges == 'random') {
					$('#createMarkerButton',jobmap.mapControls).text('Create my marker');
					google.maps.event.trigger(jobmap.map, 'resize');
				}
			}
			for (var i=0; i < jobmap.markers.length; i++) {
				if (marker.id == jobmap.markers[i].id) {
					delete jobmap.markers[i].id;
					break;
				}
			}
		});
	},

	/**
	 * Applies for a job.
	 */
	applyJob: function(marker) {
		var application = {
			motivation: $('#applyInfo').val()
		}
		printInfo('Sending job application: ', application);
		
		$.ajax({
			url: '/rest/apply/'+marker.id,
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(application)
		})
		.done(function(data) {
			printInfo('Reply: ', data);
			marker.numApply++;
		})
		.fail(function(xhr,txt) {
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
			marker = {
				type: jobmap.user.privileges,
				title: jobmap.user.name,
				mapMarker: jobmap.newMarker
			};
			jobmap.isAdmin() && (marker.type = 'company');
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
					if (!jobmap.user.birthday) {
						return alert('You have to enter your birthday before you can apply for jobs. Click your name in the top right corner to do so.');
					}
					if (!jobmap.user.cvUploaded) {
						return alert('You have to upload a CV before you can apply for jobs. Click your name in the top right corner to do so.');
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
				$('<input id="markerTitle" placeholder="Marker title" />').val(marker.title).appendTo(info);
			}
			if (jobmap.isAdmin()) {
				$(info).append(' ');
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
						if (type == 'company') $('#markerCat').css('visibility', 'visible');
						else $('#markerCat').css('visibility', 'hidden');
					}).appendTo(info);
			}
			$('<p></p>').append($('<textarea id="markerInfo"></textarea>')
				.attr('placeholder',(jobmap.user.privileges=='random'
					?'Write a little text about yourself here. It\'s what the companies will see first, so be intuitive.'
					:'Write the job description here. It\'s what the job searchers will see first, so be intuitive.'))
				.val(marker.info))
				.appendTo(info);
			// Add categories
			if (marker.type == 'company' || jobmap.isAdmin()) {
				var markerCat = $('<select id="markerCat"></select>');
				$.each(jobmap.categories, function(id,cat) {
					$('<option></option>').val(id).text(cat).appendTo(markerCat);
				});
				$(markerCat).val(marker.cat);
				if (marker.type != 'company' && jobmap.isAdmin()) $(markerCat).css('visibility', 'hidden');
				$('<p></p>').append(markerCat).appendTo(info);
			}
			if (marker.type == 'random') {
				$('<p><label><input type="checkbox" id="markerPrivacy" /> Only show my marker to companies</label></p>').appendTo(info);
				$('#markerPrivacy',info).attr('checked', (marker.privacy == 'private'));
			}
			// Save button
			var buttons = $('<p></p>').append(
				$('<button></button>').text((mode=='edit'?'Save changes':'Store marker')).click(function() {
					if (mode == 'edit') {
						marker.title = $('#markerTitle').val() || marker.title;
						marker.info = $('#markerInfo').val();
						marker.type = $('#markerType').val() || marker.type;
						if (marker.type == 'company') {
							marker.cat = $('#markerCat').val() || marker.cat;
						}
						else if (marker.type == 'random') {
							marker.privacy = ($('#markerPrivacy')[0].checked?'private':'public');
						}
						jobmap.postMarker(marker);
						jobmap.infoWindow.close();
					}
					else {
						jobmap.postMarker();
					}
				})
			).appendTo(info);
			if (mode == 'edit') {
				$('<button>Delete marker</button>').click(function() {
					jobmap.deleteMarker(marker);
				}).appendTo(buttons);
			}
		}
		
		jobmap.infoWindow.setContent(info[0]);
	},

	/** Search */
	
	/**
	 * Searches markers.
	 */
	search: function() {
		jobmap.clearSearch();
		
		// Get query
		var q = $('#q').val().trim();
		if (!q) {
			$('#searchResult').text('Please enter a search term.');
			return;
		}
		
		// Search
		printInfo('Searching for: ', q);
		$.each(jobmap.markers, function(key, marker) {
			if (marker.type == 'company' && (marker.title.indexOf(q) != -1 || marker.info.indexOf(q) != -1)) {
				jobmap.searchMatches.push(marker);
			}
		});
		
		// Did we find anything?
		if (jobmap.searchMatches.length == 0) {
			$('#searchResult').text('No jobs matched.');
			return;
		}
		
		// Display results
		$('#searchResult').text(jobmap.searchMatches.length+' job'+(jobmap.searchMatches.length>1?'s':'')+' matched.');
		$.each(jobmap.searchMatches, function(key, marker) {
			marker.mapMarker.setIcon(jobmap.pins.search.icon);
			marker.mapMarker.setShadow(jobmap.pins.search.shadow);
			marker.mapMarker.setMap(jobmap.map);
		});
		
		// Reposition map
		if (jobmap.searchMatches.length == 1) {
			// Only one match, move the center and open the info window
			jobmap.map.setCenter(jobmap.searchMatches[0].mapMarker.getPosition());
			google.maps.event.trigger(jobmap.searchMatches[0].mapMarker, 'click');
		}
		else {
			// More than one match, calculate bounds and show all matches
			var bounds = new google.maps.LatLngBounds();
			$.each(jobmap.searchMatches, function(key, marker) {
				bounds.extend(marker.mapMarker.getPosition());
			});
			jobmap.map.fitBounds(bounds);
		}
	},
	
	/**
	 * Reset markers matched by a previous search.
	 */
	clearSearch: function() {
		$('#searchResult').text('');

		// Restore matches
		$.each(jobmap.searchMatches, function(key, marker) {
			var pin = jobmap.pins[(!jobmap.isAdmin()&&jobmap.isOwner(marker))?'me':marker.type];
			marker.mapMarker.setIcon(pin.icon);
			marker.mapMarker.setShadow(pin.shadow);
		});
		jobmap.searchMatches = [];
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
		$('<img src="images/openid/google.png" id="openid-google" title="Login with your Google account" />').appendTo('#loginForm');
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
				$('<img src="images/openid/'+val.name.toLowerCase()+'.png" title="Login with your '+val.name+' account" />').click(val,openLoginWindow).appendTo(moreProviders);
			});
		});
	},
	
	/**
	 * Gets user info from the server.
	 * Called when initializing and when logging in.
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
			
			// Update map controls
			$('#createMarkerButton',jobmap.mapControls).text('Create '+(jobmap.user.privileges=='random'?'my':'a')+' marker');
			$(jobmap.mapControls).animate({opacity:1}, 'slow');
			google.maps.event.trigger(jobmap.map, 'resize');
			
			// Update userbar
			$('#accname').text(jobmap.getUsername()).removeClass('hidden');
			if (jobmap.isAdmin()) {
				$('<button id="adminButton">Admin</button>').click(jobmap.admin).insertBefore('#logButton');
			}
			
			// Go through already added markers
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
		.always(function() {
			$('#logButton').text(jobmap.user?'Logout':'Login');
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
			jobmap.showAll = $('#showAllMarkers')[0].checked;
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
		
		// Define buttons and their actions
		var buttons = {
			Save: function() {
				var userObj = {
					name: $('#userName').val(),
					sex: $('#userSex').val(),
					phonenumber: $('#userPhonenumber').val()
				};
				
				if (jobmap.user.privileges == 'random') {
					var birthday = $('#userBirthday').val();
					if (birthday != "") {
						var bday = new Date(birthday);
						userObj.birthday = bday.getTime();
						if (isNaN(userObj.birthday)) {
							return alert('You must enter a valid date for your birthday. Use the format YYYY-MM-DD.');
						}
					}
				}
				else if (jobmap.isAdmin()) {
					userObj.privileges = $('#userPrivileges').val();
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
					$('#accname').text(jobmap.getUsername());
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
					});
				}
			},
			Cancel: function() {
				$(this).dialog('close');
			}
		};
		// Define admin buttons
		if (jobmap.isAdmin()) {
			buttons = {
				Save: buttons.Save,
				'Delete user': function() {
					if (!confirm('Are you sure you want to delete this user: '+who+'?')) return;
					
					$.ajax({
						url: '/rest/user/'+who,
						type: 'DELETE'
					})
					.done(function(data) {
						alert('User was successfully deleted.');
						$('#updateUserForm').dialog('close');
					});
				},
				Cancel: buttons.Cancel
			}
		}
		
		// Create dialog
		$('<div id="updateUserForm"></div>').dialog({
			title: (who=='me'?'Your personal information':who),
			dialogClass: 'updateUserForm',
			position: ['right', 70],
			height: 530,
			width: 380,
			buttons: buttons,
			close: function() {
				$(this).remove();
			}
		})
		.keypress(function(e) {
			// Enable enter keypress to save
			if (e.which == 13) {
				$('#updateUserForm').parents('.ui-dialog').first().find('.ui-button').first().click();
			}
		});
		
		// Add elements to dialog
		$('<p>Email: </p>').add($('<input type="text" id="userEmail" readonly />').val(user.email)).appendTo('#updateUserForm');
		$('<p>Name: </p>').add($('<input type="text" id="userName" placeholder="Your name" />').val(user.name)).appendTo('#updateUserForm');
		if (jobmap.user.privileges != 'company') {
			var pad = function(n) { return ('0'+n).slice(-2); };
			var bday = "";
			if (user.birthday) {
				bday = new Date(user.birthday);
				bday = bday.getFullYear()+'-'+pad(bday.getMonth()+1)+'-'+pad(bday.getDate());
			}
			$('<p>Date of birth: </p>').add($('<input type="text" id="userBirthday" placeholder="YYYY-MM-DD" />').val(bday)).appendTo('#updateUserForm');
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
		});
	}
};

/**
 * Dynamically resize map height.
 * This can not be done with CSS.
 */
function resizeMap() {
	var page = document.getElementById('page');
	var panel = document.getElementById('panel');
	var viewportHeight = document.body.clientHeight;
	page.style.height = (viewportHeight-panel.offsetHeight)+'px';
}

/**
 * Console functions.
 */
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

/**
 * Google Analytics
 */
var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-27056070-2']);
_gaq.push(['_trackPageview']);

(function() {
	var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
	ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
	var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();
