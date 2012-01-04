// ==ClosureCompiler==
// @compilation_level SIMPLE_OPTIMIZATIONS
// @output_file_name jobmap.v1.min.js
// ==/ClosureCompiler==
// http://closure-compiler.appspot.com/home
// <insert jobmap.v1.js here>

/**
 * The Job Map.
 * @author Stefan Sundin
 * @author Alexandra Tsampikakis
 */


/**
 * The jobmap object.
 */
var jobmap = {
	/** Variables */
	baseUrl: null,
	user: null,
	
	/**
	 * Available categories.
	 */
	categories: {administration: 'Administration', construction: 'Construction', projectLeader: 'Project leader', computerScience: 'Computer science',
		disposalPromotion: 'Disposal & promotion', hotelRestaurant: 'Hotel & restaurant', medicalService: 'Health & medical service',
		industrialManufacturing: 'Industrial manufacturing', installation: 'Installation', cultureMedia: 'Culture, media, design', 
		military: 'Military', environmentalScience: 'Environmental science', pedagogical: 'Pedagogical', social: 'Social work', 
		security: 'Security', technical: 'Technical', transport: 'Transport', other: 'Other'},
	
	/**
	 * Initialize The Job Map API.
	 */
	init: function(settings) {
		$.ajaxSetup({
			contentType: 'application/json; charset=UTF-8'
		});
		jobmap.baseUrl = settings.baseUrl;
		if (settings.onload) settings.onload();
	},
	
	/** Markers */
	
	/**
	 * Clear all markers from memory.
	 */
	clearMarkers: function() {
		jobmap.markers = [];
		jobmap.mapMarkers = [];
	},
	
	/**
	 * Fetch markers from server with a certain type.
	 * Make type empty to fetch all types.
	 */
	getMarkers: function(type, callback) {
		$.getJSON(jobmap.baseUrl+'/rest/marker/'+type)
		.done(function(data) {
			callback(data);
		});
	},

	/**
	 * Send a marker to the server.
	 */
	postMarker: function(marker, callback) {
		$.ajax({
			url: jobmap.baseUrl+'/rest/marker/'+(marker.id?marker.id:''),
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(marker)
		})
		.done(function(data) {
			callback(data);
		});
	},

	/**
	 * Delete a marker.
	 */
	deleteMarker: function(id, callback) {
		$.ajax({
			url: jobmap.baseUrl+'/rest/marker/'+id,
			type: 'DELETE'
		})
		.done(function(data) {
			callback(data);
		});
	},

	/**
	 * Apply for a job.
	 */
	applyJob: function(id, application, callback) {
		$.ajax({
			url: jobmap.baseUrl+'/rest/apply/'+id,
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(application)
		})
		.done(function(data) {
			callback(data);
		});
	},
	
	/** User */
	
	/**
	 * Get login urls.
	 */
	getLoginUrls: function(callback) {
		$.getJSON(jobmap.baseUrl+'/rest/openid')
		.done(function(data) {
			callback(data);
		});
	},
	
	/**
	 * Get user info from the server.
	 */
	getUser: function(callback) {
		$.getJSON(jobmap.baseUrl+'/rest/user/me')
		.done(function(data) {
			jobmap.user = (data.result == "fail")?null:data;
			callback(data);
		});
	},
	
	/**
	 * Redirect the user to the logout url.
	 */
	logout: function() {
		window.location.assign(jobmap.user.logoutUrl);
	},
	
	/**
	 * Return true if user is the creator of marker.
	 */
	isOwner: function(marker) {
		return (jobmap.user && jobmap.user.email == marker.author);
	},
	
	/**
	 * Return a nicely formatted name for the user.
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
	 * Update user details.
	 */
	updateUser: function(user, callback) {
		$.ajax({
			url: jobmap.baseUrl+'/rest/user/me',
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(user)
		})
		.done(function(data) {
			$.extend(jobmap.user, user);
			callback(data);
		});
	},

	/**
	 * Get upload url for CV.
	 * Send the file with a normal POST submission to this url.
	 * The file must have name="cv".
	 */
	getCvUploadUrl: function(callback) {
		$.getJSON(jobmap.baseUrl+'/rest/user/me/cv/uploadUrl')
		.done(function(data) {
			callback(data);
		});
	},

	/**
	 * Delete the user's CV.
	 * Should only be called if jobmap.user.cvUploaded == true.
	 */
	deleteCv: function(callback) {
		$.ajax({
			url: jobmap.baseUrl+'/rest/user/me/cv',
			type: 'DELETE'
		})
		.done(function(data) {
			jobmap.user.cvUploaded = false;
			callback(data);
		});
	}

};

jobmap.init(window._jobmap);
