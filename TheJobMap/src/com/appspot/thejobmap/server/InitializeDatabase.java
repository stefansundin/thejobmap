package com.appspot.thejobmap.server;

import com.google.gwt.core.client.GWT;

public class InitializeDatabase {
	
	private final MarkerServiceImpl markerServiceImpl = GWT.create(MarkerServiceImpl.class);
		
	String[][] cities = {
			{ "Luleå", "65.58572,22.159424" },
			{ "Kiruna", "67.858985,20.214844" },
			{ "Piteå", "65.321005,21.478271" },
			{ "Skellefteå", "64.75539,20.950928" },
			{ "Umeå", "63.826134,20.258789" },
			{ "Örnsköldsvik", "63.826134,20.258789" },
			{ "Östersund", "63.179151,14.633789" },
			{ "Sundsvall", "63.179151,14.633789" },
			{ "Gävle", "60.675869,17.138672" },
			{ "Borlänge", "60.488351,15.438538" },
			{ "Uppsala", "60.488351,15.438538" },
			{ "Västerås", "59.619158,16.556396" },
			{ "Karlstad", "59.379387,13.50769" },
			{ "Örebro", "59.275705,15.210571" },
			{ "Stockholm", "59.333189,18.06427" },
			{ "Södretälje", "59.197032,17.63031" },
			{ "Norrköping", "58.594024,16.188354" },
			{ "Linköping", "58.420415,15.628052" },
			{ "Trollhättan", "58.28423,12.288208" },
			{ "Jönköping", "57.783304,14.161377" },
			{ "Borås", "57.723219,12.941895" },
			{ "Göteborg", "57.699745,11.988831" },
			{ "Varberg", "57.107911,12.252502" },
			{ "Växjö", "56.8805,14.80957" },
			{ "Kalmar", "56.662265,16.364136" },
			{ "Halmstad", "56.678865,12.85675" },
			{ "Kristianstad", "56.032157,14.15863" },
			{ "Gotland", "57.657158,18.709717" },
			{ "Öland", "56.67132,16.638794" },
			{ "Helsingborg", "56.0475,12.696075" },
			{ "Karlskrona", "56.163906,15.5896" },
			{ "Malmö", "55.603954,13.002319" },
			{ "Lund", "55.703903,13.193207" }
	};
	
	public void init(){
		for (int i=0; i<cities.length; i++){
			String[] latlongs = cities[i][1].split(",");
			Double latitude = Double.parseDouble(latlongs[0]);
			Double longitude = Double.parseDouble(latlongs[1]);
			markerServiceImpl.storeMarker(latitude, longitude);
		}
	}
}
