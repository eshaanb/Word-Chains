/**
 * RestClient.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.strumsoft.wordchainsfree.httplayer;

import static org.codehaus.jackson.map.DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.codehaus.jackson.map.SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS;
import static org.codehaus.jackson.map.SerializationConfig.Feature.INDENT_OUTPUT;
import static org.codehaus.jackson.map.SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY;
import static org.codehaus.jackson.map.SerializationConfig.Feature.USE_ANNOTATIONS;
import static org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS;
import static org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_NULL_PROPERTIES;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.util.Log;

import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.model.Artists;
import com.strumsoft.wordchainsfree.model.Movies;
import com.strumsoft.wordchainsfree.model.TVShows;

public class RestClient {

    //INSTANCE;

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(RestClient.class.getSimpleName());
    private static final String HOST = "54.204.128.244:8080";
    private static final String ITUNES_HOST = "itunes.apple.com";
    private static final String GAMES_DB_HOST = "thegamesdb.net";
    private static final String SCHEME = "http";
    private HttpClient httpclient;

    private ObjectMapper mapper;

    /**
     * 
     * Constructor
     */
    @SuppressWarnings("deprecation")
    public RestClient() {
        httpclient = new DefaultHttpClient();
        mapper = new ObjectMapper();
        mapper.configure(USE_ANNOTATIONS, true);
        mapper.configure(INDENT_OUTPUT, true);
        mapper.configure(WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(WRITE_NULL_PROPERTIES, false);
        mapper.configure(FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public String post(String path, Map<String, String> params) throws NetworkException {
        try {
        	URI uri = URIUtils.createURI(SCHEME, HOST, -1, path, 
            	    null, null);
            HttpPost post = new HttpPost(uri);
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
            	postParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams);
            post.setEntity(entity);
//            Log.d("POSTING WITH PARAMS", params.toString());
            HttpResponse response = httpclient.execute(post);

            return parse(response);
        } catch (Exception e) {
            throw new NetworkException(e.getMessage());
        }
    }

    public String put(String path, Map<String, String> params) throws NetworkException {
        try {
        	URI uri = URIUtils.createURI(SCHEME, HOST, -1, path, 
            	    null, null);
            HttpPut put = new HttpPut(uri);
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
            	postParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams);
            put.setEntity(entity);
            HttpResponse response = httpclient.execute(put);
            return parse(response);
        } catch (Exception e) {
            throw new NetworkException(e.getMessage());
        }
    }

    public String get(String path, Map<String, String> params) throws NetworkException {
        try {
        	URI uri = null;
        	if (params != null) {
        		List<NameValuePair> getParams = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                	getParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
            	uri = URIUtils.createURI(SCHEME, HOST, -1, path, 
            	    URLEncodedUtils.format(getParams, "UTF-8"), null);
        	}
        	else {
        		uri = URIUtils.createURI(SCHEME, HOST, -1, path, 
                	    null, null);
        	}
    		HttpGet get = new HttpGet(uri);
//            log.debug("[request] GET => url={}", get.getURI());
            HttpResponse response = httpclient.execute(get);
            return parse(response);
        } catch (Exception e) {
            throw new NetworkException(e.getMessage());
        }
    }
    
    public boolean itunesGetMusic(String term) throws NetworkException {
    	 try {
         	URI uri = null;
         	List<NameValuePair> params = new ArrayList<NameValuePair>();
         	params.add(new BasicNameValuePair("media", "music"));
         	params.add(new BasicNameValuePair("entity", "musicArtist"));
         	params.add(new BasicNameValuePair("term", term));
         	uri = URIUtils.createURI(SCHEME, ITUNES_HOST, -1, "/search", 
         			URLEncodedUtils.format(params, "UTF-8"), null);
         	HttpGet get = new HttpGet(uri);
//             log.debug("[request] GET => url={}", get.getURI());
             Artists arts = WordGameProvider.jsonStringToArtists(parse(httpclient.execute(get)));
             for (int i = 0; i<arts.getResults().size(); i++) {
            	 if (arts.getResults().get(i).getType().equalsIgnoreCase("artist") && arts.getResults().get(i).getName().equalsIgnoreCase(term)) {
            		 return true;
            	 }
             }
             return false;
         } catch (Exception e) {
             throw new NetworkException(e.getMessage());
         }
    }
    
    public boolean itunesGetMovie(String term) throws NetworkException {
   	 try {
        	URI uri = null;
        	List<NameValuePair> params = new ArrayList<NameValuePair>();
        	params.add(new BasicNameValuePair("media", "movie"));
        	params.add(new BasicNameValuePair("entity", "movie"));
        	params.add(new BasicNameValuePair("term", term));
        	uri = URIUtils.createURI(SCHEME, ITUNES_HOST, -1, "/search", 
        			URLEncodedUtils.format(params, "UTF-8"), null);
        	HttpGet get = new HttpGet(uri);
//            log.debug("[request] GET => url={}", get.getURI());
            Movies movies = WordGameProvider.jsonStringToMovies(parse(httpclient.execute(get)));
            for (int i = 0; i<movies.getResults().size(); i++) {
            	String rawMovie = movies.getResults().get(i).getName();
            	String movie = rawMovie.replaceAll("\\(\\d\\d\\d\\d\\)", "").trim();
            	if (movies.getResults().get(i).getType().equalsIgnoreCase("feature-movie") && movie.equalsIgnoreCase(term)) {
            		return true;
           	 	}
            }
            return false;
        } catch (Exception e) {
            throw new NetworkException(e.getMessage());
        }
   }
    
    public boolean itunesGetTVShow(String term) throws NetworkException {
   	 	try {
   	 		URI uri = null;
        	List<NameValuePair> params = new ArrayList<NameValuePair>();
        	params.add(new BasicNameValuePair("media", "tvShow"));
        	params.add(new BasicNameValuePair("entity", "tvSeason"));
        	params.add(new BasicNameValuePair("term", term));
        	uri = URIUtils.createURI(SCHEME, ITUNES_HOST, -1, "/search", 
        			URLEncodedUtils.format(params, "UTF-8"), null);
        	HttpGet get = new HttpGet(uri);
//            log.debug("[request] GET => url={}", get.getURI());
            TVShows shows = WordGameProvider.jsonStringToTVShows(parse(httpclient.execute(get)));
            for (int i = 0; i<shows.getResults().size(); i++) {
           	 if (shows.getResults().get(i).getType().equalsIgnoreCase("TV Season") && shows.getResults().get(i).getName().equalsIgnoreCase(term)) {
           		 return true;
           	 }
            }
            return false;
        } catch (Exception e) {
            throw new NetworkException(e.getMessage());
        }
    }
    
    public boolean getGameFromGamesDB(String gameName) throws NetworkException {
   	 	try {
   	 		URI uri = null;
        	List<NameValuePair> params = new ArrayList<NameValuePair>();
        	params.add(new BasicNameValuePair("name", gameName));
        	uri = URIUtils.createURI(SCHEME, GAMES_DB_HOST, -1, "/api/GetGamesList.php", 
        			URLEncodedUtils.format(params, "UTF-8"), null);
        	HttpGet get = new HttpGet(uri);
//            log.debug("[request] GET => url={}", get.getURI());
            Pattern pattern = 
                    Pattern.compile("<GameTitle>"+gameName+"</GameTitle>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = 
                    pattern.matcher(parse(httpclient.execute(get)));
            return matcher.find();
        } catch (Exception e) {
            throw new NetworkException(e.getMessage());
        }
   }
    
    
    private String parse(HttpResponse response) throws IOException, NetworkException {
        StatusLine status = response.getStatusLine();
        String responseBody = null;
        HttpEntity entity = null;
        HttpEntity temp = response.getEntity();
        if (temp != null) {
            entity = new BufferedHttpEntity(temp);
            responseBody = EntityUtils.toString(entity);
        }
//        log.debug("[response] {}", status);
//        log.debug("[response] {}", responseBody);

        if (status.getStatusCode() != 200) {
            throw new NetworkException(status.toString());
        }
        return responseBody;
    }
}
