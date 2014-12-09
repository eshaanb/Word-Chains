package com.strumsoft.wordchainsfree.model;

import com.google.gson.annotations.SerializedName;

public class Artist {

	@SerializedName("artistType")
	public String type;

	@SerializedName("artistName")
	public String name;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
