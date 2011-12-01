
var map = null;
var newMarker = null;
var markers = [];

function initialize() {
	var myOptions = {
		zoom: 5,
		center: new google.maps.LatLng(62.390369, 17.314453),
		mapTypeId: google.maps.MapTypeId.ROADMAP,
		streetViewControl: false,
	    mapTypeControl: false,
	};
	map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);

	// Request markers
	refreshMarkers();
	
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

function refreshMarkers() {
	var marker = {
			lat: 12.34,
			lng: 44.56,
			info: "hej",
	}
	
	$.getJSON('/rest/marker',
	function(data) {
		console.log(data);
		$.each(data,function(key, val) {
			addMarker(val.lat, val.lng, val.info);
		});
	});
}

function addMarker(latitude, longitude, markersInfo) {
	// Define Marker properties
	/*
	var image = new google.maps.MarkerImage('images/markers/ltulogo.png',
		new google.maps.Size(22, 22),
		new google.maps.Point(0,0),
		new google.maps.Point(10, 22)
	);
	*/
	
	var marker = new google.maps.Marker({
		map: map,
		position: new google.maps.LatLng(latitude, longitude),
		//icon: image,
		//icon: "",
	});
	markers.push(marker);

	// Add listener for a click on the pin
	google.maps.event.addListener(marker, 'click', function() {
		infowindow.open(map, marker);
	});

	// Add information window
	var infowindow = new google.maps.InfoWindow({
		content: createInfo("Titel", markersInfo)
	});
}

function createMarker() {
	if (newMarker != null) {
		newMarker.setMap(null);
	}
	
	newMarker = new google.maps.Marker({
		map: map,
		position: map.getCenter(),
		title: "New marker",
		draggable: true,
	});
	
	google.maps.event.addListener(newMarker, 'click', function() {
		infowindow.open(map, newMarker);
	});
	var infowindow = new google.maps.InfoWindow({
		content: createInfo(newMarker.title,'<textarea id="markersInfo" placeholder="Write description here"></textarea><button onclick="storeMarker1();">Store marker</button>')
	});
}

function storeMarker1() {
	var marker = {
			lat: newMarker.getPosition().lat(),
			lng: newMarker.getPosition().lng(),
			info: document.getElementById("markersInfo").value,
	}
/*
	$.post('/rest/marker',marker,
	function(data) {
		console.log(data);
	}, 'json');
*/
	
	$.ajax({
	  type: 'POST',
	  url: '/rest/marker',
	  data: JSON.stringify(marker),
	  success: function(data){
		console.log(data);
	  },
	  dataType: 'json',
	  //contentType: 'application/json',
	});
	
	newMarker.setMap(null);
	newMarker = null;
	/*
	var split = latlng.indexOf(",");
	var lat = latlng.substring(0, split);
	var lng = latlng.substring(split+1);
	addMarker(lat, lng, markerInfo);
	*/
}

function clearMarkers() {
	if (newMarker != null) {
		newMarker.setMap(null);
		newMarker = null;
	}
	for (var i=0; i < markers.length; i++) {
		markers[i].setMap(null);
	}
	markers = [];
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
