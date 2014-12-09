package com.strumsoft.wordchainsfree.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class TVShows {

	@SerializedName("results")
	public ArrayList<TVShow> results;

	public ArrayList<TVShow> getResults() {
		return results;
	}

	public void setResults(ArrayList<TVShow> results) {
		this.results = results;
	}
	
}
