package com.strumsoft.wordchainsfree.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.adapter.FriendsListAdapter;
import com.strumsoft.wordchainsfree.adapter.RegisteredFriendsListAdapter;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;
import com.strumsoft.wordchainsfree.model.Friend;

public class FriendsListActivity extends Activity {

	private ListView registeredFriends;
	private RelativeLayout spinnerLayout;
	private ListView fbFriendsList;
    protected static JSONArray jsonArray;
    private CheckRegFriendsTask check;
	private SharedPreferences sp;
	private RegisteredFriendsListAdapter regFriendsAdapter;
	public static final String gotRegUsers = "gotregusers";
	private ArrayList<Friend> allFriends;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.friendslistlayout);
		sp = PreferenceManager.getDefaultSharedPreferences(FriendsListActivity.this);
		spinnerLayout = (RelativeLayout) findViewById(R.id.spinner_layout);
		registeredFriends = (ListView) findViewById(R.id.registered_friends);
		fbFriendsList = (ListView) findViewById(R.id.friends_list);
		try {
			jsonArray = new JSONArray(getIntent().getStringExtra("response"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		check = new CheckRegFriendsTask();
		check.execute((Void) null);
		FriendsListAdapter fla = new FriendsListAdapter(this, jsonArray);
		fbFriendsList.setAdapter(fla);
		fbFriendsList.invalidate();
		super.onCreate(savedInstanceState);
	}

	public void sendBackFriend(String name, String id, String url) {
		Intent data = new Intent();
		data.putExtra("name", name);
		data.putExtra("id", id);
		data.putExtra("picurl", url);
		setResult(NewGameActivity.FRIEND_EMAIL_REQUESTCODE, data);
		finish();
	}
	
	class CheckRegFriendsTask extends AsyncTask<Void, Void, Cursor> {
		
		@Override
		protected void onPreExecute() {
			spinnerLayout.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		
		@Override
		protected Cursor doInBackground(Void... params) {
			allFriends = new ArrayList<Friend>();
			try {
				for (int i = 0; i<jsonArray.length(); i++) {
					try {
						JSONObject obj = jsonArray.getJSONObject(i);
						Friend f = new Friend(obj.getString("name"), Long.toString(obj.getLong("uid")), obj.getString("pic_square").replace("\\", ""));
						allFriends.add(f);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				AlertDialog.Builder builder = new AlertDialog.Builder(FriendsListActivity.this);
				builder.setTitle("Error");
				builder.setMessage("Your facebook session has expired");
				builder.setCancelable(false);
				builder.setNeutralButton("Re-Login", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent();
						i.putExtra("logout", true);
						setResult(0, i);
						finish();
					}
				});
				builder.show();
			}
			if (sp.getString(gotRegUsers, null) == null || !sp.getString(gotRegUsers, null).equalsIgnoreCase("processing")) {
				Editor edit = sp.edit();
				edit.putString(gotRegUsers, "processing");
				edit.commit();
				if (sp.getString(gotRegUsers, null) != null) {
					edit.commit();
					final Cursor c = getContentResolver().query(WordGameProvider.REGISTERED_FRIENDS_URI,
							null,
							null,
							null,
							null);
					edit.commit();
					if (c.getCount() > 0) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateSpinner(c);
							}
						});
					}
				}
				RestClient rc = new RestClient();
				String allFriendIds = "";
				for (int i = 0; i<allFriends.size(); i++) {
					if (i == 0) {
						allFriendIds += allFriends.get(i).getId();
					}
					else {
						allFriendIds += ","+allFriends.get(i).getId();
					}
				}
				Map<String, String> csvusers = new HashMap<String, String>();
				csvusers.put("csvusers", allFriendIds);
				String resp = null;
				try {
					resp = rc.get("/checkregister", csvusers);
//						Log.d("RESPONSE", resp);
				} catch (NetworkException e) {
					edit.putString(gotRegUsers, "failed");
					edit.commit();
					return null;
				}
				if (resp.length() > 0) {
					getContentResolver().delete(WordGameProvider.REGISTERED_FRIENDS_URI, null, null);
					ArrayList<String> regUserIds = new ArrayList<String>(Arrays.asList(resp.split(",")));
					for (int i = 0; i < regUserIds.size(); i++) {
						ContentValues tempValues = new ContentValues();
						tempValues.put(DBHelper.USERID, regUserIds.get(i));
						getContentResolver().insert(WordGameProvider.REGISTERED_FRIENDS_URI, tempValues);
					}
					edit.putString(gotRegUsers, "done");
					edit.commit();
					return getContentResolver().query(WordGameProvider.REGISTERED_FRIENDS_URI, null, null, null, null);
				}
				edit.putString(gotRegUsers, "done");
				edit.commit();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Cursor result) {
			updateSpinner(result);
			super.onPostExecute(result);
		}
	}
	
	private void updateSpinner(Cursor result) {
		if (result != null && sp.getString(gotRegUsers, null) != null && !sp.getString(gotRegUsers, null).equalsIgnoreCase("failed")) {
			regFriendsAdapter = new RegisteredFriendsListAdapter(FriendsListActivity.this, result, allFriends);
			registeredFriends.setAdapter(regFriendsAdapter);
			spinnerLayout.setVisibility(View.GONE);
			registeredFriends.setVisibility(View.VISIBLE);
			findViewById(R.id.reg_friends_layout).setVisibility(View.VISIBLE);
		}
		else {
			spinnerLayout.setVisibility(View.GONE);
			findViewById(R.id.active_friends_header).setVisibility(View.GONE);
			findViewById(R.id.reg_friends_layout).setVisibility(View.GONE);
		}
	}
	
}
