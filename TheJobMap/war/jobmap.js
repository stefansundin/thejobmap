
var map;

function initialize() {
	var myOptions = {
		zoom: 5,
		center: new google.maps.LatLng(62.390369, 17.314453),
		mapTypeId: google.maps.MapTypeId.ROADMAP,
		streetViewControl: false,
	    mapTypeControl: false,
	};
	map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);

	// Make map resize dynamically
	window.addEventListener("resize", resizeMap, false);
	resizeMap();
	
	// Define Marker properties
	/*
	var image = new google.maps.MarkerImage('images/markers/ltulogo.png',
		new google.maps.Size(22, 22),
		new google.maps.Point(0,0),
		new google.maps.Point(10, 22)
	);
	*/
	
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

function addMarker(latitude, longitude, title, info) {
	var marker = new google.maps.Marker({
		position: new google.maps.LatLng(latitude, longitude),
		map: map,
		//icon: image,
	});

	// Add listener for a click on the pin
	google.maps.event.addListener(marker, 'click', function() {
		infowindow.open(map, marker);
	});

	// Add information window
	var infowindow = new google.maps.InfoWindow({
		content: createInfo(title, info)
	});

	// Create information window
	function createInfo(title, content) {
		return '<div class="infowindow"><strong>'+ title +'</strong><p>'+content+'</p></div>';
	}
	
}

// Dynamically resize map
function resizeMap() {
    var page = document.getElementById("page");
    var panel = document.getElementById("panel");
    var viewportHeight = document.body.clientHeight;
    
    page.style.height = (viewportHeight-panel.offsetHeight)+"px";
}
