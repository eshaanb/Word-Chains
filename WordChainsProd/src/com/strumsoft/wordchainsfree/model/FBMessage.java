package com.strumsoft.wordchainsfree.model;

public class FBMessage {

	private String body;
	private int count;
	private String userid;
	private String picurl;
	
	public FBMessage(String body, int count, String userid, String picurl) {
		this.body = body;
		this.count = count;
		this.userid = userid;
		this.picurl = picurl;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public String getUserid() {
		return userid;
	}
	
	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPicurl() {
		return picurl;
	}

	public void setPicurl(String picurl) {
		this.picurl = picurl;
	}
	
}
