package com.strumsoft.wordchainsfree.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.google.ads.AdView;
import com.google.android.gcm.GCMRegistrar;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.adapter.GameListAdapter;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.FacebookChatManager;
import com.strumsoft.wordchainsfree.helper.ServerUtilities;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;
import com.strumsoft.wordchainsfree.model.Game;
import com.strumsoft.wordchainsfree.model.Games;

public class GameListActivity extends Activity implements OnClickListener {
	
	private ListView gameList;
	private Button newGame;
	private AdView adView;
	private GameListAdapter gamesAdapter;
	private Button logout;
	private Button sendFeedback;
	private AlertDialog feedbackDialog;
	private String currUser;
	private String currUserId;
	private Cursor allGamesCur;
	private boolean shouldExecute;
	private boolean showDialog;
	private AlertDialog networkDialog;
	private AlertDialog logoutDialog;
	private TextView loggedInUser;
	private ProgressBar gettingGames;
	private TextView noGames;
	private SharedPreferences myPrefs;
	private MessageContentObserver mco;
	private GetListTask listTask;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.gamelistlayout);
		shouldExecute = true;
		showDialog = true;
		adView = (AdView)this.findViewById(R.id.adview);
		loggedInUser = (TextView) findViewById(R.id.logged_in_user);
		gettingGames = (ProgressBar) findViewById(R.id.getting_games_spinner);
		noGames = (TextView) findViewById(R.id.nogamestext);
		mco = new MessageContentObserver();
        myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		currUser = myPrefs.getString(WordGameActivity.USERNAME_PREFERENCE_KEY, null);
		currUserId = myPrefs.getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
		loggedInUser.setText("Welcome " + currUser);
		gameList = (ListView) findViewById(R.id.games_listview);
		sendFeedback = (Button) findViewById(R.id.feedback_button);
		sendFeedback.setOnClickListener(this);
		logout = (Button) findViewById(R.id.logout_button);
		logout.setOnClickListener(this);
		newGame = (Button) findViewById(R.id.new_game_button);
		newGame.setOnClickListener(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		super.onNewIntent(intent);
	}
	
	@Override
	protected void onResume() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(0);
		Session s = Session.getActiveSession();
		if (s != null) {
            if (s.isClosed() || !s.isOpened()) {
            	AlertDialog.Builder builder = new AlertDialog.Builder(GameListActivity.this);
				builder.setTitle("Error");
				builder.setMessage("Your facebook session has expired");
				builder.setCancelable(false);
				builder.setNeutralButton("Re-Login", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showDialog = false;
						logout.performClick();
					}
				});
				builder.show();
				super.onResume();
				return;
            }
        }
		if (shouldExecute) {
			String gameid = getIntent().getStringExtra("gameid");
			if (gameid != null) {
				Cursor q = getContentResolver().query(WordGameProvider.GAMES_URI,
						new String[] {DBHelper.PIC_URL},
						DBHelper.GAME_ID+"=?",
						new String[] {gameid},
						null);
				if (q.getCount() > 0) {
					q.moveToFirst();
					String picurl = q.getString(q.getColumnIndex(DBHelper.PIC_URL));
					q.close();
					boolean timeAttack = getIntent().getBooleanExtra("timeattack", false);
					if (timeAttack) {
						boolean finished = getIntent().getBooleanExtra("finished", true);
						if (finished) {
							Intent i = new Intent(this, ScoreScreenActivity.class);
							i.putExtra("gameid", gameid);
							i.putExtra("picurl", picurl);
							i.putExtra("oppid", getIntent().getStringExtra("oppid"));
							setIntent(new Intent());
							startActivity(i);
						}
						else {
							Intent i = new Intent(this, TimeAttackGameActivity.class);
							i.putExtra("gameid", gameid);
							i.putExtra("picurl", picurl);
							i.putExtra("oppid", getIntent().getStringExtra("oppid"));
							setIntent(new Intent());
							startActivity(i);
						}
					}
					else {
						Intent i = new Intent(this, PlayGameActivity.class);
						i.putExtra("gameid", gameid);
						i.putExtra("oppid", getIntent().getStringExtra("oppid"));
						i.putExtra("picurl", picurl);
						setIntent(new Intent());
						startActivity(i);
					}
				}
				else {
					getList();
				}
			}
			else {
				getList();
			}
			final String regId = GCMRegistrar.getRegistrationId(this);
			if (regId.equals("")) {
				GCMRegistrar.register(this, Util.SENDER_ID);
			}
			if (!GCMRegistrar.isRegisteredOnServer(this)) {
				ServerUtilities.register(this, GCMRegistrar.getRegistrationId(this));
			}
		}
		else {
			shouldExecute = true;
		}
		super.onResume();
	}
		
	@Override
	protected void onPause() {
		if (listTask != null) {
			listTask.cancel(true);
			listTask = null;
		}
		if (logoutDialog != null && logoutDialog.isShowing()) {
			logoutDialog.cancel();
		}
		getContentResolver().unregisterContentObserver(mco);
		gameList.setAdapter(null);
		allGamesCur = null;
		gamesAdapter = null;
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if (allGamesCur != null) {
			allGamesCur.close();
			allGamesCur = null;
		}
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
	}
	
	private void getList() {
		if (listTask == null) {
			listTask = new GetListTask();
			listTask.execute((Void) null);
		}
	}
	
	public void showRetryDialog() {
		if (networkDialog == null) {
			AlertDialog.Builder b = new Builder(this);
			b.setTitle(R.string.network_error);
			b.setCancelable(false);
			b.setMessage(R.string.network_error_message);
			b.setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					listTask = new GetListTask();
					listTask.execute((Void) null);
					dialog.cancel();
				}
			});
			b.setMessage(R.string.network_required);
			b.setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					finish();
				}
			});
			networkDialog = b.create();
		}
		networkDialog.show();
	}
	
	public void showLogOutDialog() {
		if (logoutDialog == null) {
			AlertDialog.Builder b = new Builder(this);
			b.setTitle(R.string.logout);
			b.setCancelable(false);
			b.setMessage(R.string.confirm_logout);
			b.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					newGame.setEnabled(false);
					dialog.cancel();
					new DoLogoutTask().execute((Void) null);
				}
			});
			b.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			logoutDialog = b.create();
		}
		logoutDialog.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.logout_button:
			if (showDialog) {
				showLogOutDialog();
			}
			else {
				new DoLogoutTask().execute((Void) null);
			}
			break;
		case R.id.new_game_button:
			Intent i = new Intent(this, NewGameActivity.class);
			startActivityForResult(i, 0);
			break;
		case R.id.feedback_button:
			showFeedbackDialog();
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			boolean toLogout = data.getBooleanExtra("logout", false);
			if (toLogout) {
				showDialog = false;
				logout.performClick();
			}
			String newGameId = data.getStringExtra("newgameid");
			if (newGameId != null) {
				shouldExecute = false;
				boolean isTimeAttack = data.getBooleanExtra("istimeattack", false);
				boolean isComputer = data.getBooleanExtra("iscomputer", false);
				if (isTimeAttack) {
					Intent i = new Intent(this, TimeAttackGameActivity.class);
					i.putExtra("gameid", newGameId);
					i.putExtra("oppid", data.getStringExtra("oppid"));
					i.putExtra("picurl", data.getStringExtra("picurl"));
					startActivity(i);
				}
				else if (isComputer) {
					Intent i = new Intent(this, BotGameActivity.class);
					i.putExtra("gameid", newGameId);
					startActivity(i);
				}
				else {
					Intent i = new Intent(this, PlayGameActivity.class);
					i.putExtra("gameid", newGameId);
					i.putExtra("oppid", data.getStringExtra("oppid"));
					i.putExtra("picurl", data.getStringExtra("picurl"));
					startActivity(i);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void refreshGamesList() {
		new AsyncTask<Void, Void, Cursor>() {

			@Override
			protected Cursor doInBackground(Void... params) {
				//asynchronously update the database and remove the spinner and update the adapter after it is done.
				Cursor gamesCur = getContentResolver().query(WordGameProvider.GAMES_URI, null, null, null, null);
				Cursor playersCur = getContentResolver().query(WordGameProvider.USERS_URI, null, null, null, null);
				ListMultimap<String, String> gameidToUsers = ArrayListMultimap.create();
				playersCur.moveToFirst();
				while (!playersCur.isAfterLast()) {
					gameidToUsers.put(playersCur.getString(playersCur.getColumnIndex(DBHelper.GAME_ID)), playersCur.getString(playersCur.getColumnIndex(DBHelper.USERID)));
					playersCur.moveToNext();
				}
				playersCur.close();
				MatrixCursor allGamesCur = new MatrixCursor(new String[] {"_id", DBHelper.GAME_ID, DBHelper.TYPE, DBHelper.PIC_URL, DBHelper.CREATOR, DBHelper.CURRENT_PLAYER, DBHelper.USERLIST, DBHelper.USERIDS, DBHelper.MODE},10);
				gamesCur.moveToFirst();
				while (!gamesCur.isAfterLast()) {
					String gameid = gamesCur.getString(gamesCur.getColumnIndex(DBHelper.GAME_ID));
					List<String> tempList = gameidToUsers.get(gameid);
					String allusers = null;
					String usernames = null;
					if (tempList.size() > 0) {
						allusers = "";
						usernames = "";
						for (int i = 0; i<tempList.size(); i++) {
							if (i != tempList.size()-1) {
								allusers += tempList.get(i)+",";
								usernames += WordGameProvider.getUserName(GameListActivity.this, tempList.get(i))+",";
							}
							else {
								allusers += tempList.get(i);
								usernames += WordGameProvider.getUserName(GameListActivity.this, tempList.get(i));
							}
						}
					}
					allGamesCur.addRow(new Object[] {gamesCur.getString(gamesCur.getColumnIndex("_id")),
							gameid,
							gamesCur.getString(gamesCur.getColumnIndex(DBHelper.TYPE)),
							gamesCur.getString(gamesCur.getColumnIndex(DBHelper.PIC_URL)),
							gamesCur.getString(gamesCur.getColumnIndex(DBHelper.CREATOR)),
							gamesCur.getString(gamesCur.getColumnIndex(DBHelper.CURRENT_PLAYER)),
							usernames,
							allusers,
							gamesCur.getString(gamesCur.getColumnIndex(DBHelper.MODE))});
					gamesCur.moveToNext();
				}
				gamesCur.close();
				return allGamesCur;
			}
			
			protected void onPostExecute(Cursor result) {
				allGamesCur = result;
				if (allGamesCur.getCount() < 1) {
					gameList.setVisibility(View.GONE);
					noGames.setVisibility(View.VISIBLE);
				}
				else {
					noGames.setVisibility(View.GONE);
					gameList.setVisibility(View.VISIBLE);
					if (gamesAdapter == null) {
						gamesAdapter = new GameListAdapter(GameListActivity.this, result, currUserId);
						gameList.setAdapter(gamesAdapter);
						gamesAdapter.notifyDataSetChanged();
					}
					else {
						gamesAdapter.changeCursor(result);
						gamesAdapter.notifyDataSetChanged();
					}
				}
			};
			
		}.execute((Void) null);
	}
	
	private class DoLogoutTask extends AsyncTask<Void, Void, Void> {
		
		protected void onPreExecute() {
			Util.showProgressDialog(GameListActivity.this, null, "Logging Out...");
			gameList.setAdapter(null);
			gamesAdapter = null;
		};
		
		@Override
		protected Void doInBackground(Void... params) {
			getContentResolver().delete(WordGameProvider.REGISTERED_FRIENDS_URI, null, null);
			getContentResolver().delete(WordGameProvider.ALL_URI, null, null);
			FacebookChatManager.getInstance(null).disconnect();
			Session session = Session.getActiveSession();
	        if (session != null && !session.isClosed()) {
	            session.closeAndClearTokenInformation();
	        }
			ServerUtilities.unregister(GameListActivity.this);
			Editor editor = myPrefs.edit();
			editor.clear();
			editor.commit();
			return null;
		}
		
		protected void onPostExecute(Void result) {
			Util.cancelDialog();
			startActivity(new Intent(GameListActivity.this, WordGameActivity.class));
			finish();
		};
		
	}
	
	private class GetListTask extends AsyncTask<Void, Void, String> {
		
		@Override
		protected void onPreExecute() {
			gettingGames.setVisibility(View.VISIBLE);
			noGames.setVisibility(View.GONE);
			gameList.setVisibility(View.GONE);
			super.onPreExecute();
		}
		
		@Override
		protected String doInBackground(Void... z) {
			RestClient rc = new RestClient();
			Map<String, String> username = new HashMap<String, String>();
			username.put("userid", currUserId);
			try {
				username.put("currver", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			String response = null;
			try { //Check client version and if out of date redirect to playstore
				response = rc.get("/getgamelist", username);
//				Log.d("RESPONSE", response);
				if (response.length() < 1) {
					return null;
				}
				if (response.equalsIgnoreCase("client out of date")) {
					return response;
				}
				Games myGames = null;
				try {
					myGames = WordGameProvider.jsonStringToGamesList(response);
				} catch (Exception e) {
					throw new NetworkException("failed");
				}
				getContentResolver().unregisterContentObserver(mco);
				getContentResolver().delete(WordGameProvider.ALL_URI, DBHelper.CURRENT_PLAYER+"!=? OR "+DBHelper.CURRENT_PLAYER+" IS NULL", new String[] {"bot"});
				for (int i = 0; i < myGames.getGames().size(); i++) {
					Game g = myGames.getGames().get(i);
					if (g.getMode().equalsIgnoreCase(getString(R.string.time_attack)) && (g.getMyScore() != null || g.getOppScore() != null)) {
						String oppId = null;
						String userId = myPrefs.getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
						for (int v = 0; v < g.getUserIds().size(); v++) {
							if (!g.getUserIds().get(v).equalsIgnoreCase(userId)) {
								oppId = g.getUserIds().get(v);
								break;
							}
						}
						if (g.getMyScore() != null) {
							ContentValues playersToScore = new ContentValues();
							playersToScore.put(DBHelper.USERID, userId);
							playersToScore.put(DBHelper.GAME_ID, g.getStrId());
							playersToScore.put(DBHelper.SCORE, g.getMyScore());
							getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.SCORE_URI, g.getStrId()), playersToScore);
//							Log.d("INSERTING INTO SCORES", playersToScore.toString());
						}
 						if (g.getOppScore() != null) {
							ContentValues playersToScore = new ContentValues();
							playersToScore.put(DBHelper.USERID, oppId);
							playersToScore.put(DBHelper.GAME_ID, g.getStrId());
							playersToScore.put(DBHelper.SCORE, g.getOppScore());
							getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.SCORE_URI, g.getStrId()), playersToScore);
//							Log.d("INSERTING INTO SCORES", playersToScore.toString());
 						}
					}
					for (int h = 0; h < g.getUserStringList().size(); h++) {
						ContentValues tempUserPair = new ContentValues();
						// Log.d("IMPORTANT",
						// "NAME: "+g.getUserStringList().get(h)+" MAPS TO: "+g.getUserIds().get(h));
						tempUserPair.put(DBHelper.USER_USERNAME, g.getUserStringList().get(h));
						tempUserPair.put(DBHelper.USERID, g.getUserIds().get(h));
						tempUserPair.put(DBHelper.GAME_ID, g.getStrId());
						getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.USERS_URI,g.getStrId()), tempUserPair);
//						Log.d("INSERTING INTO USERS", tempUserPair.toString());
					}
					Iterator<Entry<String, HashMap<String, String>>> it = g.getWordList().entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<String, HashMap<String, String>> pairs = (Map.Entry<String, HashMap<String, String>>) it.next();
						ContentValues wordValues = new ContentValues();
						wordValues.put(DBHelper.WORD_USERNAME, (String) pairs.getValue().entrySet().iterator().next().getValue());
						wordValues.put(DBHelper.WORD, (String) pairs.getValue().entrySet().iterator().next().getKey());
						wordValues.put(DBHelper.GAME_ID, g.getStrId());
						wordValues.put(DBHelper.MY_ORDER,Integer.parseInt(pairs.getKey()));
//						Log.d("INSERTING INTO WORDS", wordValues.toString());
						getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.WORDS_URI, g.getStrId()), wordValues);
						it.remove();
					}
					ContentValues cv = WordGameProvider.gameToContentValues(g);
//					Log.d("INSERTING INTO GAMES", cv.toString());
					getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.GAMES_URI, g.getStrId()), cv);
					if (isCancelled()) {
						break;
					}
				}
			} catch (NetworkException e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						gettingGames.setVisibility(View.GONE);
						showRetryDialog();
					}
				});
//				Log.e("ERROR", e.getMessage());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				showUpdateDialog();
			}
			else {
				gettingGames.setVisibility(View.GONE);
				gameList.setVisibility(View.VISIBLE);
				refreshGamesList();
		        getContentResolver().registerContentObserver(WordGameProvider.GAMES_URI, true, mco);
			}
			super.onPostExecute(result);
		}
		
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
				intent.setData(Uri.parse("market://details?id=com.strumsoft.wordgamefree"));
				startActivity(intent);
				dialog.cancel();
			}
		});
		b.setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				finish();
			}
		});
		networkDialog = b.create();
		networkDialog.show();
	}
	
	private void showFeedbackDialog()  {
		if (feedbackDialog == null) {
			AlertDialog.Builder b = new Builder(this);
			feedbackDialog = b.create();
			final View customDialog = getLayoutInflater().inflate(R.layout.feedback_dialog, null);
			Button submit = (Button) customDialog.findViewById(R.id.send_button);
			submit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					final String message = ((EditText) customDialog.findViewById(R.id.message_to_send)).getText().toString();
					if (message.trim().length() > 0) {
						new AsyncTask<Void, Void, String>() {
							
							ProgressDialog pd;
							
							protected void onPreExecute() {
								pd = new ProgressDialog(GameListActivity.this);
								pd.setMessage("Sending...");
								pd.setCancelable(false);
								pd.show();
							};
							
							@Override
							protected String doInBackground(Void... params) {
								RestClient rc = new RestClient();
								Map<String, String> messageMap = new HashMap<String, String>();
								messageMap.put("feedback", currUser + " --- " + message);
								try {
									rc.post("/feedback", messageMap);
								} catch (NetworkException e) {
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(GameListActivity.this, "A network error occured. Please try again.", Toast.LENGTH_LONG).show();
										}
									});
									return null;
								}
								return "Feedback Sent!";
							}
							protected void onPostExecute(String result) {
								if (pd != null && pd.isShowing()) {
									pd.cancel();
									pd = null;
								}
								if (result != null) {
									feedbackDialog.cancel();
									Toast.makeText(GameListActivity.this, result, Toast.LENGTH_LONG).show();
								}
							};
						}.execute((Void) null);
						
					}
					else {
						Toast.makeText(GameListActivity.this, "Please enter a non-empty message.", Toast.LENGTH_LONG).show();
					}
				}
			});
			feedbackDialog.setView(customDialog, 0, 0, 0, 0);
			feedbackDialog.show();
		}
		else if (!feedbackDialog.isShowing()) {
			feedbackDialog.show();
		}
	}
	
	//Options menu adding for send feedback options menu
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		menu.add(getString(R.string.send_feedback));
//		return super.onCreateOptionsMenu(menu);
//	}
//	
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		showFeedbackDialog();
//		return super.onOptionsItemSelected(item);
//	}
	
	class MessageContentObserver extends ContentObserver {

		public MessageContentObserver() {
			super(null);
		}

		public void onChange(boolean selfChange) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					refreshGamesList();
				}
			});
		}
	}
}
