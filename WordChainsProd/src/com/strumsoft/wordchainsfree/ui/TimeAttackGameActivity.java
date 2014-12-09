package com.strumsoft.wordchainsfree.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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

public class TimeAttackGameActivity extends Activity implements OnClickListener {

	private TextView lastWord;
	private TextView turnNotifier;
	private AdView adView;
	private EditText wordToEnter;
	private MyCountDown count;
	private long millisLeft;
	private int dp42;
	private boolean started;
	private Button forfeitButton;
	private Game thisGame;
	private Button submit;
	private AsyncTask<Void, Void, String> sendWordTask;
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
		count = new MyCountDown(60000, 1000);
		dp42 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, getResources().getDisplayMetrics());
		millisLeft = -1;
		started = false;
		messagingButton = (Button) findViewById(R.id.message_friend);
		wordList = (ListView) findViewById(R.id.wordlist);
		adView = (AdView)this.findViewById(R.id.adview);
		sendingWord = (ProgressBar) findViewById(R.id.sending_word);
		currPlayerId = PreferenceManager.getDefaultSharedPreferences(TimeAttackGameActivity.this).getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
				SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(TimeAttackGameActivity.this);
				if (myPrefs.getBoolean(WordGameActivity.XMPP_LOGGEDIN, false)) {
					Intent i = new Intent(TimeAttackGameActivity.this, MessagingActivity.class);
					i.putExtra("picurl", getIntent().getStringExtra("picurl"));
					i.putExtra("oppid", getIntent().getStringExtra("oppid"));
					startActivity(i);
				}
				else {
					AlertDialog.Builder builder = new AlertDialog.Builder(TimeAttackGameActivity.this);
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
	
	private void showForfeitDialog() {
		Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle("Forfeit");
		dialogBuilder.setMessage("Are you sure you would like to forfeit this game?");
		dialogBuilder.setNegativeButton("no", new DialogInterface.OnClickListener() {
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
						Util.showProgressDialog(TimeAttackGameActivity.this, "Processing", "Forfeiting game...");
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
							getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.TEMP_WORDS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
							getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.USERS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
						} catch (NetworkException e) {
							return false;
						}
						return true;
					}
					
					protected void onPostExecute(Boolean result) {
						Util.cancelDialog();
						if (result) {
							Toast.makeText(TimeAttackGameActivity.this, "Game forfeited!", Toast.LENGTH_LONG).show();
							finish();
						}
						else {
							Toast.makeText(TimeAttackGameActivity.this, "Request failed, please try again.", Toast.LENGTH_LONG).show();
						}
					}
					
				}.execute((Void) null);
			}
		});
		dialogBuilder.setCancelable(false);
		dialogBuilder.show();
	}
	
	@Override
	protected void onResume() {
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
		final String answer = wordToEnter.getText().toString().trim();
		if (answer.length() < 1) {
			Toast.makeText(TimeAttackGameActivity.this, "Please enter a valid word", Toast.LENGTH_LONG).show();
			return;
		}
		for (int i = 0; i < words.size(); i++) {
			if (words.get(i).equalsIgnoreCase(answer)) {
				Toast.makeText(TimeAttackGameActivity.this, "This word has already been played!", Toast.LENGTH_LONG).show();
				return;
			}
		}
		wordToEnter.setOnEditorActionListener(null);
		submit.setEnabled(false);
		sendWordTask = new AsyncTask<Void, Void, String>() {
			
			protected void onPreExecute() {
				sendingWord.setVisibility(View.VISIBLE);
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp42);
				lp.setMargins((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()), 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0);
				lp.addRule(RelativeLayout.LEFT_OF, R.id.sending_word);
				submit.setLayoutParams(lp);
			};
			
			@Override
			protected String doInBackground(Void... params) {
				if (thisGame.getType().equalsIgnoreCase("pokemon") && !Util.isValidPokemon(answer, getAssets())) {
					return "Please enter a valid pokemon!";
				}
				if (thisGame.getType().equalsIgnoreCase("all words") && !Util.isValidWord(answer, getAssets())) {
					return "Please enter a valid word!";
				}
				if (thisGame.getType().equalsIgnoreCase("countries") && !Util.isValidCountry(answer, getAssets())) {
					return "Please enter a valid country!";
				}
				if (thisGame.getType().equalsIgnoreCase("magic cards") && !Util.isValidMagicCard(answer, getAssets())) {
					return "Please enter a valid magic card!";
				}
				RestClient rc = new RestClient();
				if (thisGame.getType().equalsIgnoreCase("Musicians")) {
					try {
						boolean isValid = rc.itunesGetMusic(answer);
						if (!isValid) {
							return getString(R.string.invalid_artist);
						}
					} catch (NetworkException e) {
//						Log.e("ERROR", e.getMessage());
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
//						Log.e("ERROR", e.getMessage());
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
//						Log.e("ERROR", e.getMessage());
						return getString(R.string.network_fail);
					}
				}
				HashMap<String, String> wordToUser = new HashMap<String, String>();
				wordToUser.put(answer, currPlayerId);
				thisGame.getWordList().put(""+thisGame.getWordList().size(), wordToUser);
				ContentValues cv = new ContentValues();
				cv.put(DBHelper.WORD_USERNAME, currPlayerId);
				cv.put(DBHelper.WORD, answer);
				cv.put(DBHelper.GAME_ID, thisGame.getStrId());
				cv.put(DBHelper.MY_ORDER, thisGame.getWordList().size()-1);
				getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.TEMP_WORDS_URI, thisGame.getStrId()), cv);
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						lastWord.setVisibility(View.GONE);
						words.add(answer);
						playerids.add(currPlayerId);
						wordListAdapter.notifyDataSetChanged();
					}
				});
				return null;
			}
			
			protected void onPostExecute(String result) {
				sendingWord.setVisibility(View.GONE);
				wordToEnter.setText("");
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp42);
				lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				lp.setMargins((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()), 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0);
				submit.setLayoutParams(lp);
				if (result != null) {
					MediaPlayer player = MediaPlayer.create(TimeAttackGameActivity.this, R.raw.invalidword);
					player.start();
					Toast.makeText(TimeAttackGameActivity.this, result, Toast.LENGTH_LONG).show();
				}
				wordToEnter.setOnEditorActionListener(myListener);
				submit.setEnabled(true);
			};
			
		};
		sendWordTask.execute((Void) null);
	}
	
	private void showStartingDialog(String secs) {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Time Attack!");
		if (secs == null) {
			b.setMessage("In this game mode you will have 60 seconds to play as many words in the topic: "+thisGame.getType()+".");
		}
		else {
			b.setMessage("Press play now to resume play in the topic: "+thisGame.getType()+". You have "+secs+" seconds left!");
		}
		b.setCancelable(false);
		b.setPositiveButton("Play Now", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				dialog.cancel();
				started = true;
				count.start();
			}
		});
		b.setNegativeButton("Later", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				dialog.cancel();
				finish();
			}
		});
		b.show();
	}
	
	@Override
	protected void onPause() {
		if (started) {
			count.cancel();
			Editor e = PreferenceManager.getDefaultSharedPreferences(TimeAttackGameActivity.this).edit();
			e.putLong(thisGame.getStrId(), millisLeft);
			e.commit();
		}
		super.onPause();
	}
	
	private void showFinishedDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Round Over!");
		b.setMessage("You accumulated "+words.size()+" points!");
		b.setCancelable(false);
		b.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				dialog.cancel();
				Editor e = PreferenceManager.getDefaultSharedPreferences(TimeAttackGameActivity.this).edit();
				e.putLong(thisGame.getStrId(), -1);
				e.commit();
				Intent i = new Intent(TimeAttackGameActivity.this, ScoreScreenActivity.class);
				i.putExtra("gameid", thisGame.getStrId());
				i.putExtra("picurl", getIntent().getStringExtra("picurl"));
				i.putExtra("oppid", getIntent().getStringExtra("oppid"));
				startActivity(i);
				finish();
			}
		});
		b.show();
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
		MediaPlayer player = MediaPlayer.create(TimeAttackGameActivity.this, R.raw.win);
		player.start();
		b.show();
	}

	private class MyCountDown extends CountDownTimer {

		public MyCountDown(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {
			DoMoveTask doMoveTask = new DoMoveTask();
			doMoveTask.execute((Void) null);
		}

		@Override
		public void onTick(long millisUntilFinished) {
			millisLeft = millisUntilFinished;
			turnNotifier.setText(millisUntilFinished/1000 + " secs");
		}
		
	}

	public void showRetryDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle(R.string.network_error);
		b.setCancelable(false);
		b.setMessage(R.string.network_error_message);
		b.setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				DoMoveTask doMoveTask = new DoMoveTask();
				doMoveTask.execute((Void) null);
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
		b.create().show();
	}
	
	private class DoMoveTask extends AsyncTask<Void, Void, String> {
		
		protected void onPreExecute() {
			Util.showProgressDialog(TimeAttackGameActivity.this, null, "Sending score to server...");
		};
		
		@Override
		protected String doInBackground(Void... params) {
			Map<String, String> moveParams = new HashMap<String, String>();
			String score = Integer.toString(words.size());
			moveParams.put("userid", currPlayerId);
			moveParams.put("gameid", thisGame.getStrId());
			moveParams.put("score", score);
			RestClient rc = new RestClient();
			String response = null;
			try {
				ContentValues scoreCv = new ContentValues();
				scoreCv.put(DBHelper.USERID, currPlayerId);
				scoreCv.put(DBHelper.GAME_ID, thisGame.getStrId());
				scoreCv.put(DBHelper.SCORE, score);
				response = rc.put("/timeattackmove", moveParams);
				getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.SCORE_URI, thisGame.getStrId()), scoreCv);
				getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.TEMP_WORDS_URI, thisGame.getStrId()), DBHelper.GAME_ID+"=?", new String[] {thisGame.getStrId()});
				if (response.startsWith("Opponent has forfeited")) {
					return "forfeited";
				}
			} catch (NetworkException e) {
				e.printStackTrace();
				return e.getMessage();
			}
			return null;
		}
		
		protected void onPostExecute(String result) {
			Util.cancelDialog();
			if (result != null) {
				if (result.equalsIgnoreCase("forfeited")) {
					showForfeitedDialog();
				}
				else {
					showRetryDialog();
				}
			}
			else {
				showFinishedDialog();
			}
		};
	}
	
	private class GetGameInfoTask extends AsyncTask<Void, String, Long> {
		
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
				Util.showProgressDialog(TimeAttackGameActivity.this,
						"Getting game info", "Please wait...");
			}
			super.onPreExecute();
		}

		@Override
		protected Long doInBackground(Void... v) {
			String gameid = getIntent().getStringExtra("gameid");
			SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(TimeAttackGameActivity.this);
			Long secsLeft = (long) 60000;
			if (myPrefs.getLong(gameid, -1) != -1) {
				secsLeft = myPrefs.getLong(gameid, -1);
			}
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
			Cursor wordListCur = getContentResolver().query(WordGameProvider.TEMP_WORDS_URI,
					null,
					DBHelper.GAME_ID+"=?",
					new String[] {gameid},
					DBHelper.MY_ORDER);
			g.setWordList(WordGameProvider.wordCurToWordList(TimeAttackGameActivity.this, wordListCur));
			c.close();
			TimeAttackGameActivity.this.thisGame = g;
			return secsLeft;
		}

		@Override
		protected void onPostExecute(Long result) {
			if (toast != null && toast.equalsIgnoreCase("dialog")) {
				Util.cancelDialog();
			}
			if (result == null) {
				showForfeitedDialog();
				return;
			}
			wordToEnter.setHint(thisGame.getType());
			ArrayList<HashMap<String, String>> tempWordList = 
					new ArrayList<HashMap<String, String>>(thisGame.getWordList().values());
			words.clear();
			playerids.clear();
			for (int i = 0; i<tempWordList.size(); i++) {
				words.add((String) tempWordList.get(i).entrySet().iterator().next().getKey());
				playerids.add((String) tempWordList.get(i).entrySet().iterator().next().getValue());
			}
			wordListAdapter.notifyDataSetChanged();
			count = new MyCountDown(result, 1000);
			turnNotifier.setText(result/1000+" secs");
			if (result != 60000) {
				showStartingDialog(Long.toString(result/1000));
			}
			else {
				showStartingDialog(null);
			}
			super.onPostExecute(result);
		}
	}
	
}
