package com.strumsoft.wordchainsfree.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.strumsoft.wordchainsfree.model.Game;

public class BotGameActivity extends Activity implements OnClickListener {

	private TextView lastWord;
	private TextView turnNotifier;
	private AdView adView;
	private EditText wordToEnter;
	private Button forfeitButton;
	private Game thisGame;
	private Button submit;
	private ProgressDialog computerLoading;
	private AsyncTask<Void, Void, String[]> sendWordTask;
	private String lastPlayed;
	private int dp42;
	private ListView wordList;
	private GetGameInfoTask gameInfoTask;
	private PlayedWordlistAdapter wordListAdapter;
	private ProgressBar sendingWord;
	private String currPlayerId;
	private ArrayList<String> words;
	private ArrayList<String> playerids;
	private long millisLeft;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.playgamelayout);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		millisLeft = 0;
		dp42 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, getResources().getDisplayMetrics());
		((Button) findViewById(R.id.message_friend)).setVisibility(View.GONE);
		wordList = (ListView) findViewById(R.id.wordlist);
		adView = (AdView)this.findViewById(R.id.adview);
		sendingWord = (ProgressBar) findViewById(R.id.sending_word);
		computerLoading = new ProgressDialog(this);
		computerLoading.setMessage("Computer is thinking of a word...");
		computerLoading.setCancelable(false);
		currPlayerId = PreferenceManager.getDefaultSharedPreferences(BotGameActivity.this).getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
		String currPlayerName = PreferenceManager.getDefaultSharedPreferences(BotGameActivity.this).getString(WordGameActivity.USERNAME_PREFERENCE_KEY, null);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		words = new ArrayList<String>();
		playerids = new ArrayList<String>();
        wordListAdapter = new PlayedWordlistAdapter(this, words, playerids, currPlayerId, currPlayerName);
        wordList.setAdapter(wordListAdapter);
		turnNotifier = (TextView) findViewById(R.id.turn_notifier);
		lastWord = (TextView) findViewById(R.id.lastword);
		wordToEnter = (EditText) findViewById(R.id.word_to_enter);
		TextView.OnEditorActionListener answerListener = new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) { 
					onClick(null);
				}
				return true;
			}
		};
		wordToEnter.setOnEditorActionListener(answerListener);
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
		dialogBuilder.setMessage("Are you sure you would like to end this game?");
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
				new AsyncTask<Void, Void, Void>() {
					
					@Override
					protected void onPreExecute() {
//						getContentResolver().unregisterContentObserver(nwo);
						Util.showProgressDialog(BotGameActivity.this, "Processing", "Ending game...");
					}
					
					@Override
					protected Void doInBackground(Void... params) {
						String gameid = getIntent().getStringExtra("gameid");
						getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.GAMES_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
						getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.BOT_WORDS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
						return null;
					}
					
					protected void onPostExecute(Void result) {
						Util.cancelDialog();
						Toast.makeText(BotGameActivity.this, "Game Ended!", Toast.LENGTH_LONG).show();
						finish();
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
				Toast.makeText(BotGameActivity.this, "Please enter a valid word", Toast.LENGTH_LONG).show();
				return;
			}
			String testString = answer.replaceAll("[^A-Za-z0-9]", "");
			if (lastPlayed != null) {
				String lastWordCorrected = lastPlayed.replaceAll("[^A-Za-z0-9]", "");
				if (testString.length() < 1 || (!lastWordCorrected.substring(lastWordCorrected.length() - 1).equalsIgnoreCase(testString.substring(0, 1)))) {
					showCustomToast("Your word must start with "+lastWordCorrected.substring(lastWordCorrected.length() - 1).toUpperCase()+"!");
					MediaPlayer player = MediaPlayer.create(BotGameActivity.this, R.raw.invalidword);
					player.start();
					return;
				}
			}
			for (int i = 0; i<words.size(); i++) {
				if (words.get(i).equalsIgnoreCase(answer)) {
					showCustomToast("This word has already been played!");
					MediaPlayer player = MediaPlayer.create(BotGameActivity.this, R.raw.invalidword);
					player.start();
					return;
				}
			}
			if (thisGame.getType().equalsIgnoreCase("pokemon") && !Util.isValidPokemon(answer, getAssets())) {
				showCustomToast("Please enter a valid pokemon!");
				MediaPlayer player = MediaPlayer.create(BotGameActivity.this, R.raw.invalidword);
				player.start();
				return;
			}
			if (thisGame.getType().equalsIgnoreCase("all words") && !Util.isValidWord(answer, getAssets())) {
				showCustomToast("Please enter a valid word!");
				MediaPlayer player = MediaPlayer.create(BotGameActivity.this, R.raw.invalidword);
				player.start();
				return;
			}
			if (thisGame.getType().equalsIgnoreCase("countries") && !Util.isValidCountry(answer, getAssets())) {
				showCustomToast("Please enter a valid country!");
				MediaPlayer player = MediaPlayer.create(BotGameActivity.this, R.raw.invalidword);
				player.start();
				return;
			}
			if (thisGame.getType().equalsIgnoreCase("magic cards") && !Util.isValidMagicCard(answer, getAssets())) {
				showCustomToast("Please enter a valid magic card!");
				MediaPlayer player = MediaPlayer.create(BotGameActivity.this, R.raw.invalidword);
				player.start();
				return;
			}
			submit.setEnabled(false);
			sendWordTask = new AsyncTask<Void, Void, String[]>() {
				
				MyCountDown timer;
				
				protected void onPreExecute() {
					timer = new MyCountDown(1000, 100);
					timer.start();
					sendingWord.setVisibility(View.VISIBLE);
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp42);
					lp.setMargins((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()), 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0);
					lp.addRule(RelativeLayout.LEFT_OF, R.id.sending_word);
					submit.setLayoutParams(lp);
					wordToEnter.setText("");
				};
				
				@Override
				protected String[] doInBackground(Void... params) {
					String gameid = getIntent().getStringExtra("gameid");
					HashMap<String, String> wordToUser = new HashMap<String, String>();
					wordToUser.put(answer, currPlayerId);
					thisGame.getWordList().put(""+thisGame.getWordList().size(), wordToUser);
					String computerResp = null;
					ContentValues cv = new ContentValues();
					cv.put(DBHelper.WORD_USERNAME, currPlayerId);
					cv.put(DBHelper.WORD, answer);
					cv.put(DBHelper.GAME_ID, gameid);
					cv.put(DBHelper.MY_ORDER, thisGame.getWordList().size()-1);
					getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.BOT_WORDS_URI, thisGame.getStrId()), cv);
					wordListAdapter.addItem(answer, currPlayerId);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							wordListAdapter.notifyDataSetChanged();
						}
					});
					String testString = answer.replaceAll("[^A-Za-z0-9]", "");
					if (thisGame.getType().equalsIgnoreCase("pokemon")) {
						computerResp = Util.getNextPokemon(testString.substring(testString.length() - 1), getAssets(), words);
					}
					if (thisGame.getType().equalsIgnoreCase("magic cards")) {
						computerResp = Util.getNextMagicCard(testString.substring(testString.length() - 1), getAssets(), words);
					}
					if (thisGame.getType().equalsIgnoreCase("all words")) {
						computerResp = Util.getNextWord(testString.substring(testString.length() - 1), getAssets(), words);
					}
					if (thisGame.getType().equalsIgnoreCase("countries")) {
						computerResp = Util.getNextCountry(testString.substring(testString.length() - 1), getAssets(), words);
					}
					if (computerResp != null) {
						HashMap<String, String> wordToBot = new HashMap<String, String>();
						wordToBot.put(computerResp, "Bot");
						thisGame.getWordList().put(""+thisGame.getWordList().size(), wordToBot);
						cv.put(DBHelper.WORD_USERNAME, "Bot");
						cv.put(DBHelper.WORD, computerResp);
						cv.put(DBHelper.GAME_ID, gameid);
						cv.put(DBHelper.MY_ORDER, thisGame.getWordList().size()-1);
						getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.BOT_WORDS_URI, thisGame.getStrId()), cv);
						wordListAdapter.addItem(computerResp, "Bot");
						String[] retString = new String[2];
						retString[0] = "Computer played "+computerResp;
						retString[1] = computerResp;
						if (timer.isFinished()) {
							return retString;
						}
						try {
							Thread.sleep(millisLeft);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						return retString;
					}
					else {
						getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.GAMES_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
						getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.BOT_WORDS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
						String[] retString = new String[1];
						retString[0] = "Computer fails";
						return retString;
					}
				}
				
				protected void onPostExecute(String[] result) {
					sendingWord.setVisibility(View.GONE);
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp42);
					lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
					lp.setMargins((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()), 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0);
					submit.setLayoutParams(lp);
					if (result[0].startsWith("Computer fails")) {
						showForfeitedDialog();
					}
					else {
						lastWord.setVisibility(View.GONE);
						if (result[0] != null) {
							showCustomToast(result[0]);
						}
						lastPlayed = result[1];
						wordListAdapter.notifyDataSetChanged();
						submit.setEnabled(true);
					}
				};
				
			};
			sendWordTask.execute((Void) null);
		
	}
	
	private void showForfeitedDialog() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("You Win!");
		b.setMessage("You have stumped the computer!");
		b.setCancelable(false);
		b.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				dialog.cancel();
				finish();
			}
		});
		MediaPlayer player = MediaPlayer.create(BotGameActivity.this, R.raw.win);
		player.start();
		b.show();
	}

	private void showCustomToast(String text) {
		Toast toast = new Toast(getApplicationContext());
		int dp90 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, -dp90);
		toast.setDuration(Toast.LENGTH_LONG);
		View view = LayoutInflater.from(this).inflate(R.layout.customtoast, null);
		((TextView) view.findViewById(R.id.text)).setText(text);
		toast.setView(view);
		toast.show();
	}
	
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
				Util.showProgressDialog(BotGameActivity.this,
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
			Cursor wordListCur = getContentResolver().query(WordGameProvider.BOT_WORDS_URI,
					null,
					DBHelper.GAME_ID+"=?",
					new String[] {gameid},
					DBHelper.MY_ORDER);
			g.setWordList(WordGameProvider.wordCurToWordList(BotGameActivity.this, wordListCur));
			returnString[0] = g.getLastWord();
			wordListCur.close();
			c.close();
			BotGameActivity.this.thisGame = g;
			return returnString;
		}
		
		protected void onProgressUpdate(String value) {
			Toast.makeText(BotGameActivity.this, value, Toast.LENGTH_LONG).show();
		};

		@Override
		protected void onPostExecute(String[] result) {
			Util.cancelDialog();
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
			turnNotifier.setText("It is your turn!");
			wordToEnter.setHint(thisGame.getType());
			if (result != null) {
				if (result[0] == null) {
					lastWord.setText("You're going first! Enter any word in the given topic: "+thisGame.getType());
				}
				else {
					lastWord.setVisibility(View.GONE);
				}
				lastPlayed = result[0];
			}
			super.onPostExecute(result);
		}
	}
	
	private class MyCountDown extends CountDownTimer {

		private boolean isFinished;
		
		public MyCountDown(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			isFinished = false;
		}

		@Override
		public void onFinish() {
			isFinished = true;
		}

		@Override
		public void onTick(long millisUntilFinished) {
			millisLeft = millisUntilFinished;
		}
		
		public boolean isFinished() {
			return isFinished;
		}
		
	}
	
}
