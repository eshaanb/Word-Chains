package com.strumsoft.wordchainsfree.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class Artists {

	@SerializedName("results")
	public ArrayList<Artist> results;

	public ArrayList<Artist> getResults() {
		return results;
	}

	public void setResults(ArrayList<Artist> results) {
		this.results = results;
	}
	
}
