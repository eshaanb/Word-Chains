package com.strumsoft.wordchainsfree.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdView;
import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.adapter.PlayedWordlistAdapter;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;
import com.strumsoft.wordchainsfree.model.Game;

public class PlayGameActivity extends Activity implements OnClickListener {

	private TextView lastWord;
	private TextView turnNotifier;
	private AdView adView;
	private EditText wordToEnter;
	private Button forfeitButton;
	private Game thisGame;
	private Button submit;
	private boolean isMyTurn;
	private boolean shouldToast;
	private AsyncTask<Void, Void, String> sendWordTask;
	private String lastPlayed;
	private NewWordObserver nwo;
	private int dp42;
	private ListView wordList;
	private Button messagingButton;
	private GetGameInfoTask gameInfoTask;
	private PlayedWordlistAdapter wordListAdapter;
	private TextView.OnEditorActionListener myListener = new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) { 
				onClick(null);
			}
			return true;
		}
	};
	private ProgressBar sendingWord;
	private String currPlayerId;
	private ArrayList<String> words;
	private ArrayList<String> playerids;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.playgamelayout);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		dp42 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, getResources().getDisplayMetrics());
		messagingButton = (Button) findViewById(R.id.message_friend);
		wordList = (ListView) findViewById(R.id.wordlist);
		adView = (AdView)this.findViewById(R.id.adview);
		sendingWord = (ProgressBar) findViewById(R.id.sending_word);
		shouldToast = true;
		nwo = new NewWordObserver();
		currPlayerId = PreferenceManager.getDefaultSharedPreferences(PlayGameActivity.this).getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getContentResolver().registerContentObserver(Uri.withAppendedPath(WordGameProvider.WORDS_URI, getIntent().getStringExtra("gameid")), true, nwo);
		words = new ArrayList<String>();
		playerids = new ArrayList<String>();
        wordListAdapter = new PlayedWordlistAdapter(this, words, playerids, currPlayerId, null);
        wordList.setAdapter(wordListAdapter);
		turnNotifier = (TextView) findViewById(R.id.turn_notifier);
		lastWord = (TextView) findViewById(R.id.lastword);
		wordToEnter = (EditText) findViewById(R.id.word_to_enter);
		messagingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(PlayGameActivity.this);
				if (myPrefs.getBoolean(WordGameActivity.XMPP_LOGGEDIN, false)) {
					Intent i = new Intent(PlayGameActivity.this, MessagingActivity.class);
					i.putExtra("picurl", getIntent().getStringExtra("picurl"));
					i.putExtra("oppid", getIntent().getStringExtra("oppid"));
					startActivityForResult(i, 0);
				}
				else {
					AlertDialog.Builder builder = new AlertDialog.Builder(PlayGameActivity.this);
					builder.setTitle("Error");
					builder.setCancelable(false);
					builder.setMessage("Your facebook session has expired");
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
			}
		});
		wordToEnter.setOnEditorActionListener(myListener);
		submit = (Button) findViewById(R.id.sendword);
		forfeitButton = (Button) findViewById(R.id.forfeit);
		forfeitButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showForfeitDialog();
			}
		});
		submit.setOnClickListener(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			boolean toLogout = data.getBooleanExtra("logout", false);
			if (toLogout) {
				Intent i = new Intent();
				i.putExtra("logout", true);
				setResult(0, i);
				finish();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void showForfeitDialog() {
		Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle("Forfeit");
		dialogBuilder.setMessage("Are you sure you would like to forfeit this game?");
		dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface d, int arg1) {
				d.cancel();
			}
		});
		dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				new AsyncTask<Void, Void, Boolean>() {
					
					@Override
					protected void onPreExecute() {
						shouldToast = false;
						getContentResolver().unregisterContentObserver(nwo);
						Util.showProgressDialog(PlayGameActivity.this, "Processing", "Forfeiting game...");
					}
					
					@Override
					protected Boolean doInBackground(Void... params) {
						String gameid = getIntent().getStringExtra("gameid");
						RestClient rc = new RestClient();
						try {
							Map<String, String> forfeitParams = new HashMap<String, String>();
							forfeitParams.put("gameid",gameid);
							forfeitParams.put("userid",currPlayerId);
							rc.put("/forfeit", forfeitParams);
							getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.GAMES_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
							getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.WORDS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
							getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.USERS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
						} catch (NetworkException e) {
							return false;
						}
						return true;
					}
					
					protected void onPostExecute(Boolean result) {
						Util.cancelDialog();
						if (result) {
							Toast.makeText(PlayGameActivity.this, "Game forfeited!", Toast.LENGTH_LONG).show();
							finish();
						}
						else {
							Toast.makeText(PlayGameActivity.this, "Request failed, please try again.", Toast.LENGTH_LONG).show();
							getContentResolver().registerContentObserver(Uri.withAppendedPath(WordGameProvider.WORDS_URI, getIntent().getStringExtra("gameid")), true, nwo);
							shouldToast = true;
						}
					}
					
				}.execute((Void) null);
			}
		});
		dialogBuilder.setCancelable(false);
		dialogBuilder.show();
	}
	
	@Override
	protected void onPause() {
		getContentResolver().unregisterContentObserver(nwo);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
        getContentResolver().registerContentObserver(Uri.withAppendedPath(WordGameProvider.WORDS_URI, getIntent().getStringExtra("gameid")), true, nwo);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(0);
		if (gameInfoTask == null || gameInfoTask.getStatus() != AsyncTask.Status.RUNNING) {
			gameInfoTask = new GetGameInfoTask("dialog");
			gameInfoTask.execute((Void) null);
		}
		super.onResume();
	}

	@Override
	protected void onStop() {
		if (sendWordTask != null && sendWordTask.getStatus() == AsyncTask.Status.RUNNING) {
			sendWordTask.cancel(true);
			sendWordTask = null;
		}
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		if (adView != null) {
		      adView.destroy();
		}
		super.onDestroy();
	}
	
	@Override
	public void onClick(View v) {
		if (isMyTurn) {
			final String answer = wordToEnter.getText().toString().trim();
			if (answer.length() < 1) {
				Toast.makeText(PlayGameActivity.this, "Please enter a valid word", Toast.LENGTH_LONG).show();
				return;
			}
			String testString = answer.replaceAll("[^A-Za-z0-9]", "");
			if (lastPlayed != null) {
				String lastWordCorrected = lastPlayed.replaceAll("[^A-Za-z0-9]", "");
				if (testString.length() < 1 || (!lastWordCorrected.substring(lastWordCorrected.length() - 1).equalsIgnoreCase(testString.substring(0, 1)))) {
					Toast toast = new Toast(getApplicationContext());
					int dp90 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
					toast.setGravity(Gravity.CENTER_VERTICAL, 0, -dp90);
					toast.setDuration(Toast.LENGTH_LONG);
					View view = LayoutInflater.from(this).inflate(R.layout.customtoast, null);
					((TextView) view.findViewById(R.id.text)).setText("Your word must start with "+lastWordCorrected.substring(lastWordCorrected.length() - 1).toUpperCase()+"!");
					toast.setView(view);
					toast.show();
					MediaPlayer player = MediaPlayer.create(PlayGameActivity.this, R.raw.invalidword);
					player.start();
					return;
				}
			}
			wordToEnter.setOnEditorActionListener(null);
			submit.setEnabled(false);
			sendWordTask = new AsyncTask<Void, Void, String>() {
				
				protected void onPreExecute() {
					getContentResolver().unregisterContentObserver(nwo);
					sendingWord.setVisibility(View.VISIBLE);
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp42);
					lp.setMargins((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()), 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0);
					lp.addRule(RelativeLayout.LEFT_OF, R.id.sending_word);
					submit.setLayoutParams(lp);
				};
				
				@Override
				protected String doInBackground(Void... params) {
					shouldToast = false;
					if (thisGame.getType().equalsIgnoreCase("pokemon") && !Util.isValidPokemon(answer, getAssets())) {
						MediaPlayer player = MediaPlayer.create(PlayGameActivity.this, R.raw.invalidword);
						player.start();
						return "Please enter a valid pokemon!";
					}
					if (thisGame.getType().equalsIgnoreCase("all words") && !Util.isValidWord(answer, getAssets())) {
						MediaPlayer player = MediaPlayer.create(PlayGameActivity.this, R.raw.invalidword);
						player.start();
						return "Please enter a valid word!";
					}
					if (thisGame.getType().equalsIgnoreCase("countries") && !Util.isValidCountry(answer, getAssets())) {
						MediaPlayer player = MediaPlayer.create(PlayGameActivity.this, R.raw.invalidword);
						player.start();
						return "Please enter a valid country!";
					}
					if (thisGame.getType().equalsIgnoreCase("magic cards") && !Util.isValidMagicCard(answer, getAssets())) {
						MediaPlayer player = MediaPlayer.create(PlayGameActivity.this, R.raw.invalidword);
						player.start();
						return "Please enter a valid magic card!";
					}
//					if (thisGame.getType().equalsIgnoreCase("Musicians") && !Util.isValidArtist(answer, getAssets())) {
//						return "Please enter a valid artist!";
//					}
					RestClient rc = new RestClient();
					if (thisGame.getType().equalsIgnoreCase("Musicians")) {
						try {
							boolean isValid = rc.itunesGetMusic(answer);
							if (!isValid) {
								return getString(R.string.invalid_artist);
							}
						} catch (NetworkException e) {
//							Log.e("ERROR", e.getMessage());
							return getString(R.string.network_fail);
						}
					}
					if (thisGame.getType().equalsIgnoreCase("Movies And TV Shows")) {
						try {
							boolean isMovieOrTv = rc.itunesGetMovie(answer);
							if (!isMovieOrTv) {
								isMovieOrTv = rc.itunesGetTVShow(answer);
								if (!isMovieOrTv) {
									return getString(R.string.invalid_show_or_movie);
								}
							}
							
						} catch (NetworkException e) {
//							Log.e("ERROR", e.getMessage());
							return getString(R.string.network_fail);
						}
					}
					if (thisGame.getType().equalsIgnoreCase("Video Games")) {
						try {
							boolean isGame = rc.getGameFromGamesDB(answer);
							if (!isGame) {
								return getString(R.string.invalid_video_game);
							}
						} catch (NetworkException e) {
//							Log.e("ERROR", e.getMessage());
							return getString(R.string.network_fail);
						}
					}
					String gameid = getIntent().getStringExtra("gameid");
					String returnString = null;
					Map<String, String> username = new HashMap<String, String>();
					username.put("userid", currPlayerId);
					username.put("gameid",gameid);
					username.put("word",answer);
					try {
						String successOrFail = rc.put("/domove", username);
						if (successOrFail.startsWith("This word has already been")) {
							throw new NetworkException("This word has already been played!");
						}
						if (successOrFail.startsWith("Opponent has forfeited")) {
							throw new NetworkException("Opponent has forfeited! You Win!");
						}
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								wordToEnter.setText("");
							}
						});
						returnString = "Word Played!";
						Map<String, String> domoveParams = new HashMap<String, String>();
						domoveParams.put("gameid",getIntent().getStringExtra("gameid"));
						HashMap<String, String> wordToUser = new HashMap<String, String>();
						wordToUser.put(answer, currPlayerId);
						thisGame.getWordList().put(""+thisGame.getWordList().size(), wordToUser);
						thisGame.setCurrPlayer(getIntent().getStringExtra("oppid"));
						getContentResolver().update(Uri.withAppendedPath(WordGameProvider.GAMES_URI, thisGame.getStrId()),
								WordGameProvider.gameToContentValues(thisGame),
								DBHelper.GAME_ID+"=?",
								new String[] {thisGame.getStrId()});
						ContentValues cv = new ContentValues();
						cv.put(DBHelper.WORD_USERNAME, currPlayerId);
						cv.put(DBHelper.WORD, answer);
						cv.put(DBHelper.GAME_ID, gameid);
						cv.put(DBHelper.MY_ORDER, thisGame.getWordList().size()-1);
						getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.WORDS_URI, thisGame.getStrId()), cv);
						final String turnmanName = WordGameProvider.getUserName(PlayGameActivity.this, getIntent().getStringExtra("oppid"));
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								lastWord.setVisibility(View.GONE);
								lastPlayed = answer;
								turnNotifier.setText("It is "+turnmanName+"'s turn!");
								words.add(lastPlayed);
								playerids.add(currPlayerId);
								wordListAdapter.notifyDataSetChanged();
							}
						});
						isMyTurn = false;
					} catch (NetworkException e) {
//						Log.e("ERROR", e.getMessage());
						return e.getMessage();
					}
					return returnString;
				}
				
				protected void onPostExecute(String result) {
			        getContentResolver().registerContentObserver(Uri.withAppendedPath(WordGameProvider.WORDS_URI, getIntent().getStringExtra("gameid")), true, nwo);
					sendingWord.setVisibility(View.GONE);
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp42);
					lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
					lp.setMargins((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()), 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0);
					submit.setLayoutParams(lp);
					if (result.startsWith("Opponent has forfeited")) {
						showForfeitedDialog();
					}
					else {
						if (result != null) {
							if (!result.equalsIgnoreCase("Word Played!")) {
								MediaPlayer player = MediaPlayer.create(PlayGameActivity.this, R.raw.invalidword);
								player.start();
							}
							else {
								InputMethodManager imm = (InputMethodManager)getSystemService(
									      Context.INPUT_METHOD_SERVICE);
									imm.hideSoftInputFromWindow(wordToEnter.getWindowToken(), 0);
							}
							Toast toast = new Toast(getApplicationContext());
							int dp90 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
							toast.setGravity(Gravity.CENTER_VERTICAL, 0, -dp90);
							toast.setDuration(Toast.LENGTH_LONG);
							View view = LayoutInflater.from(PlayGameActivity.this).inflate(R.layout.customtoast, null);
							((TextView) view.findViewById(R.id.text)).setText(result);
							toast.setView(view);
							toast.show();
						}
						shouldToast = true;
						wordToEnter.setOnEditorActionListener(myListener);
						submit.setEnabled(true);
					}
				};
				
			};
			sendWordTask.execute((Void) null);
		}
		else {
			Toast.makeText(PlayGameActivity.this, "Your opponent is still answering!", Toast.LENGTH_LONG).show();
		}
	}
	
	private void showForfeitedDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("You Win!");
		b.setMessage("Your opponent has forfeited the game!");
		b.setCancelable(false);
		b.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				dialog.cancel();
				finish();
			}
		});
		MediaPlayer player = MediaPlayer.create(PlayGameActivity.this, R.raw.win);
		player.start();
		b.show();
	}

//	private void scrollMyListViewToBottom() {
//		wordList.post(new Runnable() {
//	        @Override
//	        public void run() {
//	        	wordList.setSelection(wordListAdapter.getCount()-1);
//	        }
//	    });
//	}
	
	private class GetGameInfoTask extends AsyncTask<Void, String, String[]> {
		
		private String toast; //show a toast instead of dialog
		
		public GetGameInfoTask(String toast) {
			this.toast = toast;
		}
		
		@Override
		protected void onPreExecute() {
			if (toast == null) {
				return;
			}
			if (toast.equalsIgnoreCase("dialog")) {
				Util.showProgressDialog(PlayGameActivity.this,
						"Getting game info", "Please wait...");
			}
			super.onPreExecute();
		}

		@Override
		protected String[] doInBackground(Void... v) {
			String[] returnString = new String[2];
			String gameid = getIntent().getStringExtra("gameid");
			Cursor c = getContentResolver().query(WordGameProvider.GAMES_URI,
					null, 
					DBHelper.GAME_ID+"=?", 
					new String[] {gameid}, 
					null);
			c.moveToFirst();
			Game g = WordGameProvider.gameCurToGame(c);
			if (g == null) {
				return null;
			}
			else if (toast != null && toast.equalsIgnoreCase("toast")) {
				publishProgress("Your opponent has played a word!");
			}
//			Log.d("TURN", g.getCurrPlayer());
			Cursor wordListCur = getContentResolver().query(WordGameProvider.WORDS_URI,
					null,
					DBHelper.GAME_ID+"=?",
					new String[] {gameid},
					DBHelper.MY_ORDER);
			g.setWordList(WordGameProvider.wordCurToWordList(PlayGameActivity.this, wordListCur));
			returnString[0] = g.getCurrPlayer();
			returnString[1] = g.getLastWord();
			wordListCur.close();
			c.close();
			PlayGameActivity.this.thisGame = g;
			return returnString;
		}
		
		protected void onProgressUpdate(String value) {
			Toast.makeText(PlayGameActivity.this, value, Toast.LENGTH_LONG).show();
		};

		@Override
		protected void onPostExecute(String[] result) {
			if (toast != null && toast.equalsIgnoreCase("dialog")) {
				Util.cancelDialog();
			}
			if (result == null) {
				showForfeitedDialog();
				return;
			}
			ArrayList<HashMap<String, String>> tempWordList = 
					new ArrayList<HashMap<String, String>>(thisGame.getWordList().values());
			words.clear();
			playerids.clear();
			for (int i = 0; i<tempWordList.size(); i++) {
//				wordListAdapter.add((String) tempWordList.get(i).entrySet().iterator().next().getKey());	
				words.add((String) tempWordList.get(i).entrySet().iterator().next().getKey());
				playerids.add((String) tempWordList.get(i).entrySet().iterator().next().getValue());
				wordListAdapter.notifyDataSetChanged();
			}
//			scrollMyListViewToBottom();
			wordToEnter.setHint(thisGame.getType());
			if (result[0].equalsIgnoreCase(PreferenceManager.getDefaultSharedPreferences(PlayGameActivity.this).getString(WordGameActivity.USERID_PREFERENCE_KEY, null))) {
				turnNotifier.setText("It is your turn!");
				isMyTurn = true;
			}
			else {
				turnNotifier.setText("It is "+WordGameProvider.getUserName(PlayGameActivity.this, result[0])+"'s turn!");
				isMyTurn = false;
			}
			if (result[1] != null) {
				lastWord.setVisibility(View.GONE);
				lastPlayed = result[1];
			}
			else if (isMyTurn) {
				lastWord.setText("You're going first! Enter any word in the given topic: "+thisGame.getType());
			}
			else {
				lastWord.setText("You're opponent is playing the first word.");
			}
			super.onPostExecute(result);
		}
	}
	
	class NewWordObserver extends ContentObserver {

		public NewWordObserver() {
			super(null);
		}

		public void onChange(boolean selfChange) {
			if (gameInfoTask == null || gameInfoTask.getStatus() != AsyncTask.Status.RUNNING) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (shouldToast) {
							gameInfoTask = new GetGameInfoTask("toast");
							gameInfoTask.execute((Void) null);

						}
						else {
							gameInfoTask = new GetGameInfoTask(null);
							gameInfoTask.execute((Void) null);
						}
					}
				});
			}
		}
	}
	
}
