package com.strumsoft.wordchainsfree.ui;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.FriendsGetProfilePics;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;
import com.strumsoft.wordchainsfree.model.Game;

public class ScoreScreenActivity extends Activity implements OnClickListener {

	private TextView myScore;
	private TextView oppScore;
	private ImageView myImg;
	private ImageView oppImg;
	private Button finishGame;
	private Game thisGame;
	private String myId;
	private SharedPreferences myPrefs;
	private FriendsGetProfilePics getPics;
	private Button messagingButton;
	private GetGameInfoTask gameInfoTask;
	private OpponentMove oppMoveObserver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.scoreviewlayout);
		messagingButton = (Button) findViewById(R.id.message_friend);
		messagingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(ScoreScreenActivity.this);
				if (myPrefs.getBoolean(WordGameActivity.XMPP_LOGGEDIN, false)) {
					Intent i = new Intent(ScoreScreenActivity.this, MessagingActivity.class);
					i.putExtra("picurl", getIntent().getStringExtra("picurl"));
					i.putExtra("oppid", getIntent().getStringExtra("oppid"));
					startActivityForResult(i, 0);
				}
				else {
					AlertDialog.Builder builder = new AlertDialog.Builder(ScoreScreenActivity.this);
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
		myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		oppMoveObserver = new OpponentMove();
		myId = myPrefs.getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
		finishGame = (Button) findViewById(R.id.finish_game);
		finishGame.setOnClickListener(this);
		myScore = (TextView) findViewById(R.id.myscore);
		oppScore = (TextView) findViewById(R.id.oppscore);
		myImg = (ImageView) findViewById(R.id.myPic);
		oppImg = (ImageView) findViewById(R.id.oppPic);
		getPics = new FriendsGetProfilePics();
		gameInfoTask = new GetGameInfoTask("dialog");
		gameInfoTask.execute((Void) null);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onResume() {
        getContentResolver().registerContentObserver(Uri.withAppendedPath(WordGameProvider.SCORE_URI, getIntent().getStringExtra("gameid")), true, oppMoveObserver);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		getContentResolver().unregisterContentObserver(oppMoveObserver);
		super.onPause();
	}
	
	private class GetGameInfoTask extends AsyncTask<Void, String, Game> {
		
		private String toast; //show a toast instead of dialog
		
		public GetGameInfoTask(String toast) {
			this.toast = toast;
		}
		
		@Override
		protected void onPreExecute() {
			if (toast.equalsIgnoreCase("toast")) {
				Toast.makeText(ScoreScreenActivity.this, "Your Opponent has taken his turn!", Toast.LENGTH_LONG).show();
			}
			if (toast.equalsIgnoreCase("dialog")) {
				Util.showProgressDialog(ScoreScreenActivity.this,
						"Getting game info", "Please wait...");
			}
			super.onPreExecute();
		}


		@Override
		protected Game doInBackground(Void... v) {
			String gameid = getIntent().getStringExtra("gameid");
			Cursor c = getContentResolver().query(WordGameProvider.GAMES_URI,
					null, 
					DBHelper.GAME_ID+"=?", 
					new String[] {gameid}, 
					null);
			c.moveToFirst();
			Game g = WordGameProvider.gameCurToGame(c);
			if (g == null) {
				return g;
			}
			if (toast != null && toast.equalsIgnoreCase("toast")){
				publishProgress("Your opponent has taken his turn!");
			}
			Cursor scores = getContentResolver().query(WordGameProvider.SCORE_URI,
					null,
					DBHelper.GAME_ID+"=?", 
					new String[] {gameid}, 
					null);
			scores.moveToFirst();
			while (!scores.isAfterLast()) {
				if (scores.getString(scores.getColumnIndex(DBHelper.USERID)).equalsIgnoreCase(myId)) {
					g.setMyScore(scores.getString(scores.getColumnIndex(DBHelper.SCORE)));
				}
				else {
					g.setOppScore(scores.getString(scores.getColumnIndex(DBHelper.SCORE)));
				}
				scores.moveToNext();
			}
			c.close();
			return g;
		}
		
		protected void onProgressUpdate(String value) {
			Toast.makeText(ScoreScreenActivity.this, value, Toast.LENGTH_LONG).show();
		};

		@Override
		protected void onPostExecute(Game result) {
			if (toast != null && toast.equalsIgnoreCase("dialog")) {
				Util.cancelDialog();
			}
			if (result == null) {
				showForfeitedDialog();
				return;
			}
			if (result != null) {
				ScoreScreenActivity.this.thisGame = result;
				myImg.setImageBitmap(getPics.getImageNow(myPrefs.getString(WordGameActivity.USERID_PREFERENCE_KEY, null), myPrefs.getString(WordGameActivity.LOGGED_IN_USER_PIC, null), ScoreScreenActivity.this, myImg));
				oppImg.setImageBitmap(getPics.getImageNow(getIntent().getStringExtra("oppid"), thisGame.getPicUrl(), ScoreScreenActivity.this, oppImg));
				if (thisGame.getMyScore() != null) {
					myScore.setText(thisGame.getMyScore());
				}
				else {
					myScore.setText("???");
				}
				if (thisGame.getOppScore() != null) {
					oppScore.setText(thisGame.getOppScore());
				}
				else {
					oppScore.setText("???");
				}
				if (thisGame.getMyScore() != null && thisGame.getOppScore() != null) {
					Integer myScore = Integer.parseInt(thisGame.getMyScore());
					Integer oppScore = Integer.parseInt(thisGame.getOppScore());
					if (myScore > oppScore) {
						((TextView) findViewById(R.id.header)).setText("You Won!!!");
						MediaPlayer player = MediaPlayer.create(ScoreScreenActivity.this, R.raw.win);
						player.start();
					}
					else if (myScore == oppScore) {
						((TextView) findViewById(R.id.header)).setText("You Tied!!!");
					}
					else {
						((TextView) findViewById(R.id.header)).setText(WordGameProvider.getUserName(ScoreScreenActivity.this, getIntent().getStringExtra("oppid")).split(" ")[0]+" Won!!!");
						MediaPlayer player = MediaPlayer.create(ScoreScreenActivity.this, R.raw.lose);
						player.start();

					}
					finishGame.setVisibility(View.VISIBLE);
				}
			}
			super.onPostExecute(result);
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
		b.show();
	}

	@Override
	public void onClick(View v) {
		new AsyncTask<Void, Void, String>() {

			@Override
			protected void onPreExecute() {
				Util.showProgressDialog(ScoreScreenActivity.this, "Finishing Game", "Please wait...");
				super.onPreExecute();
			}
			
			@Override
			protected String doInBackground(Void... params) {
				Map<String, String> moveParams = new HashMap<String, String>();
				moveParams.put("gameid", thisGame.getStrId());
				moveParams.put("userid", myId);
				RestClient rc = new RestClient();
				try {
					rc.post("/finished", moveParams);
				} catch (NetworkException e) {
					e.printStackTrace();
					return e.getMessage();
				}
				getContentResolver().unregisterContentObserver(oppMoveObserver);
				getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.SCORE_URI, thisGame.getStrId()), DBHelper.GAME_ID+"=?", new String[] {thisGame.getStrId()});
				return null;
			}
			
			protected void onPostExecute(String result) {
				Util.cancelDialog();
				if (result != null) {
					Toast.makeText(ScoreScreenActivity.this, result, Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(ScoreScreenActivity.this, "Game Finished!", Toast.LENGTH_LONG).show();
					finish();
				}
			};
			
		}.execute((Void) null);
	}
	
	class OpponentMove extends ContentObserver {

		public OpponentMove() {
			super(null);
		}

		public void onChange(boolean selfChange) {
			if (gameInfoTask == null || gameInfoTask.getStatus() != AsyncTask.Status.RUNNING) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						gameInfoTask = new GetGameInfoTask("toast");
						gameInfoTask.execute((Void) null);
					}
				});
			}
		}
	}
	
	
}
