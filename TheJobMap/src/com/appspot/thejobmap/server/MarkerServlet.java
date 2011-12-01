package com.appspot.thejobmap.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.thejobmap.shared.MarkerObj;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.gson.Gson;

public class MarkerServlet extends HttpServlet {

	private static final long serialVersionUID = 9L;

	private static final String targetServer = "http://www.ltu.se/";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp, false);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp, true);
	}

	protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, 
			boolean isPost) throws ServletException, IOException {

		/*
		StringBuffer sb = new StringBuffer();
		if(req.getQueryString() != null){
			String target = req.getQueryString();
			if(!target.startsWith("http://")) {
				sb.append(targetServer);
			}
			sb.append(req.getQueryString());
		}
		*/

		DatastoreService db = DatastoreServiceFactory.getDatastoreService();
		Gson gson = new Gson();
		
		if (isPost) {
			ServletInputStream in = req.getInputStream();
			//InputStreamReader inputr = new InputStreamReader(in);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			//BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			//String line = reader.readLine();
			//System.out.println(line);
			MarkerObj marker = gson.fromJson(reader, MarkerObj.class);
			reader.close();
			
			//MarkerObj marker = null;
System.out.println(gson.toJson(marker));
			
	        Key storeKey = KeyFactory.createKey("Markers", "jaha");
	        Date date = new Date();
	        Entity entry = new Entity("Markers", storeKey);
	        entry.setProperty("latitude", marker.lat);
	        entry.setProperty("longitude", marker.lng);
	        entry.setProperty("information", marker.info);
	        entry.setProperty("date", date.getTime());
	        
	        db.put(entry);
			
    		OutputStream output = resp.getOutputStream();
    		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
            writer.write("Jaha!");
            writer.close();
		}
		else {
			
			Query q = new Query("Markers");
			List<Entity> dbret = db.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
			
			List<MarkerObj> markers = new ArrayList<MarkerObj>();
			for (int i=0; i < dbret.size(); i++) {
				MarkerObj marker = new MarkerObj();
				marker.lat = (Double) dbret.get(i).getProperty("latitude");
				marker.lng = (Double) dbret.get(i).getProperty("longitude");
				marker.info = (String) dbret.get(i).getProperty("info");
				markers.add(marker);
			}

    		OutputStream out = resp.getOutputStream();
    		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
    		
System.out.println(gson.toJson(markers));
			writer.write(gson.toJson(markers));
            writer.close();
		}

	}

}