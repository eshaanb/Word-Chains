package com.strumsoft.wordchainsfree.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class Movies {

	@SerializedName("results")
	public ArrayList<Movie> results;

	public ArrayList<Movie> getResults() {
		return results;
	}

	public void setResults(ArrayList<Movie> results) {
		this.results = results;
	}
	
}
