package com.strumsoft.wordchainsfree.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.google.ads.AdView;
import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.ImageViewButton;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;
import com.strumsoft.wordchainsfree.model.Game;
import com.strumsoft.wordchainsfree.model.Games;

public class NewGameActivity extends Activity implements OnClickListener {

	private Button createGame;
	private Button facebookFriends;
	private AdView adView;
	private String fbFriendsResp;
	private Spinner gameType;
	private Spinner modeType;
	private String oppId;
	private CheckBox vsComputer;
	private ImageViewButton infoButton;
	private String picUrl;
	private SharedPreferences myPrefs;
	
	public static int FRIEND_EMAIL_REQUESTCODE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.newgamelayout);
		adView = (AdView) findViewById(R.id.adview);
		infoButton = (ImageViewButton) findViewById(R.id.question_mark);
		infoButton.setOnClickListener(this);
		setVolumeControlStream(AudioManager.STREAM_MUSIC); //TODO: Play a sound when a new game is created (maybe)
		myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		facebookFriends = (Button) findViewById(R.id.from_friends);
		vsComputer = (CheckBox) findViewById(R.id.vscomputer_checkbox);
		vsComputer.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton button, boolean checked) {
				if (checked && !facebookFriends.getText().toString().trim().equalsIgnoreCase("Choose a Friend!")) {
					showConflictDialog();
				}
			}
		});
		facebookFriends.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fbFriendsResp == null) {
					Toast.makeText(NewGameActivity.this, "Still getting your friends list. Please wait.", Toast.LENGTH_SHORT).show();
					return;
				}
				Intent i = new Intent(NewGameActivity.this, FriendsListActivity.class);
				i.putExtra("response", fbFriendsResp);
				startActivityForResult(i, FRIEND_EMAIL_REQUESTCODE);
			}
		});
		setupFacebook();
		modeType = (Spinner) findViewById(R.id.mode_spinner);
		gameType = (Spinner) findViewById(R.id.game_type_spinner);
		createGame = (Button) findViewById(R.id.submit);
		createGame.setOnClickListener(this);
		super.onCreate(savedInstanceState);
		
	}
	
	private void setupFacebook() {
		getFbFriends();
//        if (facebook.shouldExtendAccessToken()) {
//        	facebook.extendAccessToken(this, new ServiceListener() {
//    			@Override
//    			public void onFacebookError(FacebookError e) {
//    				showNetworkDialog();
//    				Log.e("ERROR", e.getMessage());
//    			}
//    			
//    			@Override
//    			public void onError(Error e) {
//    				showNetworkDialog();
//    				Log.e("ERROR", e.getMessage());
//    			}
//    			
//    			@Override
//    			public void onComplete(Bundle values) {
//    				SharedPreferences.Editor editor = myPrefs.edit();
//                    editor.putString("access_token", facebook.getAccessToken());
//                    editor.putLong("access_expires", facebook.getAccessExpires());
//                    editor.commit();
//    				mRunner = new AsyncFacebookRunner(facebook);
//    				getFbFriends();
//    			}
//    		});
//        }
//        else {
//        	mRunner = new AsyncFacebookRunner(facebook);
//			getFbFriends();
//        }
	}
	
	private void showConflictDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle(R.string.conflict);
		b.setCancelable(false);
		b.setMessage(R.string.friend_and_computer_error);
		b.setPositiveButton(getString(R.string.friend), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				vsComputer.setChecked(false);
			}
		});
		b.setNegativeButton(getString(R.string.computer), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				facebookFriends.setText("Choose a Friend!");
			}
		});
		b.show();
	}
	
	private void showNetworkDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle(R.string.network_error);
		b.setCancelable(false);
		b.setMessage(R.string.network_required);
		b.setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent i = new Intent(Intent.ACTION_MAIN);
				i.addCategory(Intent.CATEGORY_HOME);
		        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
				finish();
			}
		});
		b.show();
	}
	
	private void getFbFriends() {
		String query = "select name, uid, pic_square from user where uid in (select uid2 from friend where uid1=me()) order by name";
        Bundle params = new Bundle();
        params.putString("q", query);
        Session session = Session.getActiveSession();
        Request request = new Request(session,
            "/fql",                         
            params,                         
            HttpMethod.GET,                 
            new Request.Callback() {
                public void onCompleted(Response response) {
                	try {
                		if (response.getGraphObject() == null) {
                			AlertDialog.Builder builder = new AlertDialog.Builder(NewGameActivity.this);
            				builder.setTitle("Error");
            				builder.setMessage("Your facebook session has expired.");
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
                		else {
//    						Log.d("FRIENDS", response.getGraphObject().getInnerJSONObject().getJSONArray("data").toString());
    	                	fbFriendsResp = response.getGraphObject().getInnerJSONObject().getJSONArray("data").toString();
                		}
					} catch (JSONException e) {
						e.printStackTrace();
					}
                }
        }); 
        Request.executeBatchAsync(request); 
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && data != null) {
			if (data.getBooleanExtra("logout", false)) {
				Intent i = new Intent();
				i.putExtra("logout", true);
				setResult(0, i);
				finish();
			}
		}
		else if (requestCode == FRIEND_EMAIL_REQUESTCODE && data != null) {
			facebookFriends.setText(data.getStringExtra("name"));
			oppId = data.getStringExtra("id");
			picUrl = data.getStringExtra("picurl");
			if (vsComputer.isChecked()) {
				showConflictDialog();
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.submit:
			if (vsComputer.isChecked()) {
				final String type = gameType.getItemAtPosition(gameType.getSelectedItemPosition()).toString();
				final String mode = modeType.getItemAtPosition(modeType.getSelectedItemPosition()).toString();
				if (type.equalsIgnoreCase("all words") || type.equalsIgnoreCase("magic cards") || type.equalsIgnoreCase("countries") || type.equalsIgnoreCase("pokemon")) {
					if (mode.equalsIgnoreCase("time attack")) {
						Toast.makeText(this, "When playing against a bot please select the Word Chainer mode.", Toast.LENGTH_LONG).show();
					}
					else {
						new AsyncTask<Void, Void, String>() {

							protected void onPreExecute() {
								vsComputer.setEnabled(false);
								Util.showProgressDialog(NewGameActivity.this, "", "Creating game...");
							}
							
							@Override
							protected String doInBackground(Void... nothing) {
								ContentValues cv = new ContentValues();
								String id = Long.toString(System.currentTimeMillis());
								cv.put(DBHelper.GAME_ID, id);
								cv.put(DBHelper.TYPE, type);
								cv.putNull(DBHelper.PIC_URL);
						        cv.put(DBHelper.CURRENT_PLAYER, "bot");
						        cv.putNull(DBHelper.CREATOR);
								getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.GAMES_URI, id), cv);
								return id;
							}
							
							protected void onPostExecute(String result) {
								vsComputer.setEnabled(true);
								Util.cancelDialog();
								Toast.makeText(NewGameActivity.this, "New Game Created!", Toast.LENGTH_SHORT).show();
								Intent i = new Intent();
								i.putExtra("newgameid", result);
								i.putExtra("iscomputer", true);
								setResult(0, i);
								finish();
							}
							
						}.execute((Void) null);
					}	
				}
				else {
					Toast.makeText(this, "When playing against a bot please select either all words, magic cards, countries, or pokemon.", Toast.LENGTH_LONG).show();
				}
			}
			else {
				final String opponent = this.facebookFriends.getText().toString().trim();
				if (opponent.length() > 0 && !opponent.equalsIgnoreCase("Choose a Friend!")) {
					new AsyncTask<Void, Void, String>() {

						String newId = null;
						String mode = null;
						
						protected void onPreExecute() {
							createGame.setEnabled(false);
							Util.showProgressDialog(NewGameActivity.this, "", "Creating game...");
						};
						
						@Override
						protected String doInBackground(Void... nothing) {
							String userid = myPrefs.getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
							mode = modeType.getItemAtPosition(modeType.getSelectedItemPosition()).toString();
							RestClient doNewGame = new RestClient();
							Map<String, String> params = new HashMap<String, String>();
							params.put("userid", userid);
							params.put("mode", mode);
							params.put("oppid", oppId);
							params.put("oppname", opponent);
							params.put("picurl", picUrl);
							params.put("gametype", gameType.getItemAtPosition(gameType.getSelectedItemPosition()).toString());
							try {
								newId = doNewGame.post("/newgame", params);
								Map<String, String> username = new HashMap<String, String>();
								username.put("userid", myPrefs.getString(WordGameActivity.USERID_PREFERENCE_KEY, null));
								try {
									username.put("currver", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
								} catch (NameNotFoundException e) {
									e.printStackTrace();
								}
								String gameListResp = doNewGame.get("/getgamelist", username);
								if (gameListResp.equalsIgnoreCase("client out of date")) {
									return gameListResp;
								}
//								Log.d("RESPONSE", gameListResp);
								Games myGames = WordGameProvider.jsonStringToGamesList(gameListResp);
								getContentResolver().delete(WordGameProvider.ALL_URI, null, null);
								for (int i = 0; i < myGames.getGames().size(); i++) {
									Game g = myGames.getGames().get(i);
									for (int h = 0; h < g.getUserStringList().size(); h++) {
										ContentValues tempUserPair = new ContentValues();
										tempUserPair.put(DBHelper.USER_USERNAME, g.getUserStringList().get(h));
										tempUserPair.put(DBHelper.USERID, g.getUserIds().get(h));
										tempUserPair.put(DBHelper.GAME_ID, g.getStrId());
										getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.USERS_URI,g.getStrId()), tempUserPair);
										Log.d("INSERTING INTO USERS", tempUserPair.toString());
									}
									Iterator<Entry<String, HashMap<String, String>>> it = g
											.getWordList().entrySet().iterator();
									while (it.hasNext()) {
										Map.Entry<String, HashMap<String, String>> pairs = (Map.Entry<String, HashMap<String, String>>) it.next();
										ContentValues wordValues = new ContentValues();
										wordValues.put(DBHelper.WORD_USERNAME, (String) pairs.getValue().entrySet().iterator().next().getValue());
										wordValues.put(DBHelper.WORD, (String) pairs.getValue().entrySet().iterator().next().getKey());
										wordValues.put(DBHelper.GAME_ID, g.getStrId());
										wordValues.put(DBHelper.MY_ORDER,Integer.parseInt(pairs.getKey()));
//										Log.d("INSERTING INTO WORDS", wordValues.toString());
										getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.WORDS_URI, g.getStrId()), wordValues);
										it.remove();
									}
									ContentValues cv = WordGameProvider.gameToContentValues(g);
//									Log.d("INSERTING INTO GAMES", cv.toString());
									getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.GAMES_URI, g.getStrId()), cv);
								}
							} catch (NetworkException e) {
//								Log.e("ERROR", e.getMessage());
								return e.getMessage();
							}
							return null;
						}
						
						protected void onPostExecute(String result) {
							createGame.setEnabled(true);
							Util.cancelDialog();
							if (result != null) {
								if (result.equalsIgnoreCase("client out of date")) {
									showUpdateDialog();
								}
								else {
									showNetworkDialog();
								}
							}
							else {
								Toast.makeText(NewGameActivity.this, "New Game Created!", Toast.LENGTH_SHORT).show();
								Intent i = new Intent();
								i.putExtra("newgameid", newId);
								i.putExtra("oppid", oppId);
								i.putExtra("picurl", picUrl);
								i.putExtra("iscomputer", false);
								if (mode.equalsIgnoreCase("Time Attack")) {
									i.putExtra("istimeattack", true);
								}
								setResult(0, i);
								finish();
							}

						};
						
					}.execute((Void) null);
				}
				else {
					Toast.makeText(this, "Please select a valid facebook friend to have as an opponent.", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		case R.id.question_mark:
			infoButton.setEnabled(false);
			showInfoDialog();
			break;
		}
	}
	
	@Override
	protected void onDestroy() {
		if (adView != null) {
		      adView.destroy();
		}
		super.onDestroy();
	}
	
	public void showInfoDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle(R.string.info_title);
		b.setMessage(R.string.info_body);
		b.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				infoButton.setEnabled(true);
				dialog.cancel();
			}
		});
		AlertDialog dialog = b.create();
		dialog.show();
	}
	
	public void showUpdateDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle(R.string.client_error);
		b.setCancelable(false);
		b.setMessage(R.string.out_of_date_error);
		b.setPositiveButton(getString(R.string.update), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=com.strumsoft.wordgame"));
				startActivity(intent);
			}
		});
		b.setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				finish();
			}
		});
		AlertDialog dialog = b.create();
		dialog.show();
	}
	
}
