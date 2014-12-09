package com.strumsoft.wordchainsfree.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;


public class Game {
	
	@SerializedName("uniqueid")
	public String strId;
	
	@SerializedName("currplayer")
	public String currPlayer;
	
	@SerializedName("gamecreator")
	public String gameCreator;
	
	@SerializedName("mode")
	public String mode;
	
	@SerializedName("myscore")
	private String myScore;
	
	@SerializedName("oppscore")
	private String oppScore;
	
	@SerializedName("picurl")
	public String picUrl;
	
	@SerializedName("userlist")
	public ArrayList<String> userStringList;
	
	@SerializedName("userids")
	public ArrayList<String> userIds;

	@SerializedName("type")
	public String type;

	@SerializedName("wordlist")
	private LinkedHashMap<String, HashMap<String, String>> wordList;
	
	public Game(ArrayList<String> userIds, ArrayList<String> players, String gameCreator, String strId, LinkedHashMap<String, HashMap<String, String>> wordList, String currPlayer, String type, String picUrl, String mode) {
		this.userIds = userIds;
		this.picUrl = picUrl;
		this.userStringList = players;
		this.gameCreator = gameCreator;
		this.strId = strId;
		this.wordList = wordList;
		this.currPlayer = currPlayer;
		this.type = type;
		this.mode = mode;
	}

	public String getMyScore() {
		return myScore;
	}

	public void setMyScore(String myScore) {
		this.myScore = myScore;
	}

	public String getOppScore() {
		return oppScore;
	}

	public void setOppScore(String oppScore) {
		this.oppScore = oppScore;
	}

	public Map<String, HashMap<String, String>> getWordList() {
		return wordList;
	}

	public void setWordList(LinkedHashMap<String, HashMap<String, String>> wordList) {
		this.wordList = wordList;
	}

	public String getStrId() {
		return strId;
	}
	
	public void setStrId(String strId) {
		this.strId = strId;
	}

	public String getCurrPlayer() {
		return currPlayer;
	}

	public void setCurrPlayer(String currPlayer) {
		this.currPlayer = currPlayer;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getGameCreator() {
		return gameCreator;
	}

	public void setGameCreator(String gameCreator) {
		this.gameCreator = gameCreator;
	}

	public ArrayList<String> getUserStringList() {
		return userStringList;
	}

	public void setUserStringList(ArrayList<String> userStringList) {
		this.userStringList = userStringList;
	}
		
	public ArrayList<String> getUserIds() {
		return userIds;
	}

	public void setUserIds(ArrayList<String> userIds) {
		this.userIds = userIds;
	}

	public String getPicUrl() {
		return picUrl;
	}

	public void setPicUrl(String picUrl) {
		this.picUrl = picUrl;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public String getMode() {
		return mode;
	}
	
	public String getLastWord() {
		if (wordList.size() < 1) {
			return null;
		}
		List<HashMap<String, String>> words = new ArrayList<HashMap<String, String>>(wordList.values());
		return words.get(words.size()-1).entrySet().iterator().next().getKey();
	}
}
