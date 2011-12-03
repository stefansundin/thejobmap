package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.shared.MarkerObj;
import com.appspot.thejobmap.shared.ResultObj;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.gson.Gson;

public class MarkerServlet extends HttpServlet {
	
	private static final long serialVersionUID = -919160328227007218L;
	
	/**
	 * 
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Query database for markers
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Markers");
		List<Entity> dbret = db.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
		
		// Transfer markers to serializable array
		List<MarkerObj> markers = new ArrayList<MarkerObj>();
		for (int i=0; i < dbret.size(); i++) {
			MarkerObj marker = new MarkerObj();
			marker.lat = (Double) dbret.get(i).getProperty("lat");
			marker.lng = (Double) dbret.get(i).getProperty("lng");
			marker.info = (String) dbret.get(i).getProperty("info");
			markers.add(marker);
		}
		
		// Send markers to client
		OutputStream out = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		Gson gson = new Gson();
		writer.write(gson.toJson(markers));
		writer.close();
	}

	/**
	 * 
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Parse input
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		Gson gson = new Gson();
		MarkerObj marker = gson.fromJson(reader, MarkerObj.class);
		reader.close();
		
		// Put in an entry
		Key storeKey = KeyFactory.createKey("Markers", "jaha");
		Date date = new Date();
		Entity entry = new Entity("Markers", storeKey);
		entry.setProperty("lat", marker.lat);
		entry.setProperty("lng", marker.lng);
		entry.setProperty("info", marker.info);
		entry.setProperty("date", date.getTime());
		
		// Insert in database
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		db.put(entry);
		
		// Send response
		OutputStream output = resp.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
		
		ResultObj res = new ResultObj("ok");
		writer.write(gson.toJson(res));
		writer.close();
	}

}