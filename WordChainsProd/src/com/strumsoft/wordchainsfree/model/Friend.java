package com.strumsoft.wordchainsfree.model;

import com.google.gson.annotations.SerializedName;

public class Friend {

	@SerializedName("id")
	private String id;
	
	@SerializedName("name")
	private String name;
	
	private String url;
	
	public Friend(String name, String id, String url) {
		this.name = name;
		this.id = id;
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
}
