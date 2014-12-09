package com.strumsoft.wordchainsfree.model;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class Picture {

	HashMap<String, String> myUrlMappings;
	
	public Picture(HashMap<String, String> myUrlMappings) {
		this.myUrlMappings = myUrlMappings;
	}

	public HashMap<String, String> getMappings() {
		return myUrlMappings;
	}

	public void setUrlMappings(HashMap<String, String> myUrlMappings) {
		this.myUrlMappings = myUrlMappings;
	}
	
}
