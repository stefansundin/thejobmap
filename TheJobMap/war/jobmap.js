
function initialize() {
	var initialLocation = new google.maps.LatLng(62.390369, 17.314453);
	var myOptions = {
		zoom: 5,
		center: initialLocation,
		mapTypeId: google.maps.MapTypeId.ROADMAP
	};
	var map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);

	// Make map resize dynamically
	window.addEventListener("resize", resizeMap, false);
	resizeMap();
	
	// Define Marker properties
	var image = new google.maps.MarkerImage('images/markers/ltulogo.png',
		new google.maps.Size(22, 22),
		new google.maps.Point(0,0),
		new google.maps.Point(10, 22)
	);

	// Add Marker
	var marker1 = new google.maps.Marker({
		position: new google.maps.LatLng(65.617753,22.137108),
		map: map,
		icon: image
	});

	// Add listener for a click on the pin
	google.maps.event.addListener(marker1, 'click', function() {
		infowindow1.open(map, marker1);
	});

	// Add information window
	var infowindow1 = new google.maps.InfoWindow({
		content:  createInfo('Luleå university of technology', '<a title="Click to view our website" href="http://www.ltu.se">Our Website</a>')
	});

	// Create information window
	function createInfo(title, content) {
		return '<div class="infowindow"><strong>'+ title +'</strong><p>'+content+'</p></div>';
	}
}

function resizeMap() {
    var page = document.getElementById("page");
    var panel = document.getElementById("panel");
    var viewportHeight = document.body.clientHeight;
    
    page.style.height = (viewportHeight-panel.offsetHeight)+"px";
}
