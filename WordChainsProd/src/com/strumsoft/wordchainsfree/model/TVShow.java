package com.strumsoft.wordchainsfree.model;

import com.google.gson.annotations.SerializedName;

public class TVShow {

	@SerializedName("collectionType")
	public String type;

	@SerializedName("artistName")
	public String showName;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return showName;
	}

	public void setName(String name) {
		this.showName = name;
	}
	
}
