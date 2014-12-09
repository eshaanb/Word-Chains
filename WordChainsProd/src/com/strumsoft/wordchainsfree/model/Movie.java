package com.strumsoft.wordchainsfree.model;

import com.google.gson.annotations.SerializedName;

public class Movie {

	@SerializedName("kind")
	public String type;

	@SerializedName("trackName")
	public String movieName;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return movieName;
	}

	public void setName(String name) {
		this.movieName = name;
	}
}