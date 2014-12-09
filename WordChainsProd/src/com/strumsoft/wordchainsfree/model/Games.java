package com.strumsoft.wordchainsfree.model;

import java.util.ArrayList;

import org.codehaus.jackson.annotate.JsonSubTypes;

import com.google.gson.annotations.SerializedName;

public class Games {

	@SerializedName("games")
	public ArrayList<Game> games;

	public ArrayList<Game> getGames() {
		return games;
	}

	public void setGames(ArrayList<Game> games) {
		this.games = games;
	}
	
}
