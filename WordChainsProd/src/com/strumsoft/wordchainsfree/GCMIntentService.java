package com.strumsoft.wordchainsfree;

import static com.strumsoft.wordchainsfree.helper.Util.SENDER_ID;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.ServerUtilities;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.model.Game;
import com.strumsoft.wordchainsfree.ui.GameListActivity;
import com.strumsoft.wordchainsfree.ui.WordGameActivity;


public class GCMIntentService extends GCMBaseIntentService {
	
    public GCMIntentService() {
        super(SENDER_ID);
//        Log.d(TAG, "[GCMIntentService] start");
    }
    
	@Override
    protected void onRegistered(Context context, String registrationId) {
//        Log.i(TAG, "Device registered: regId = " + registrationId);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("regId", registrationId);
        editor.commit();
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
//        Log.i(TAG, "Device unregistered");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove("regId");
        editor.commit();
        if (GCMRegistrar.isRegisteredOnServer(context)) {
            ServerUtilities.unregister(context);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
//            Log.i(TAG, "Ignoring unregister callback");
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) { //receieved gcm message
//        Log.i("Received message", intent.getExtras().toString());
        String word = intent.getStringExtra("word");
        String gameid = intent.getStringExtra("gameid");
        String gameJson = intent.getStringExtra("game");
        String forfeited = intent.getStringExtra("forfeit");
        String score = intent.getStringExtra("score");
        if (score != null) { //someone took their turn in a Time Attack Game
        	String playerid = intent.getStringExtra("playerid");
        	String name = intent.getStringExtra("playername");
        	String message = null;
        	String gameover = intent.getStringExtra("gameover");
        	if (gameover.equalsIgnoreCase("true")) {
        		if (score.equalsIgnoreCase("1")) {
            		message = name+" just took his turn and earned  "+score+" point! Game has ended!";
            	}
            	else {
            		message = name+" just took his turn and earned  "+score+" points! Game has ended!";
            	}
        		ContentValues newPlayer = new ContentValues();
            	newPlayer.putNull(DBHelper.CURRENT_PLAYER);
            	getContentResolver().update(Uri.withAppendedPath(WordGameProvider.GAMES_URI, gameid), newPlayer, DBHelper.GAME_ID+"=?", new String[] {gameid});
            	Bundle b = new Bundle();
                b.putString("gameid", gameid);
                b.putBoolean("timeattack", true);
                b.putBoolean("finished", true);
                b.putString("oppid", playerid);
                generateNotification(context, message, GameListActivity.class, b);
        	}
        	else { 
        		if (score.equalsIgnoreCase("1")) {
            		message = name+" just took his turn and earned  "+score+" point! It's your turn!";
            	}
            	else {
            		message = name+" just took his turn and earned  "+score+" points! It's your turn!";
            	}
        		ContentValues newPlayer = new ContentValues();
            	newPlayer.put(DBHelper.CURRENT_PLAYER, PreferenceManager.getDefaultSharedPreferences(context).getString(WordGameActivity.USERID_PREFERENCE_KEY, null));
            	getContentResolver().update(Uri.withAppendedPath(WordGameProvider.GAMES_URI, gameid), newPlayer, DBHelper.GAME_ID+"=?", new String[] {gameid});
            	Bundle b = new Bundle();
                b.putString("gameid", gameid);
                b.putBoolean("timeattack", true);
                b.putBoolean("finished", false);
                b.putString("oppid", playerid);
                generateNotification(context, message, GameListActivity.class, b);
        	}
        	
        	ContentValues scoreEntry = new ContentValues();
        	scoreEntry.put(DBHelper.GAME_ID, gameid);
        	scoreEntry.put(DBHelper.USERID, playerid);
        	scoreEntry.put(DBHelper.SCORE, score);
        	getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.SCORE_URI, gameid), scoreEntry);
            
        }
        else if (forfeited != null) {
        	String opponentName = intent.getStringExtra("playername");
        	String type = intent.getStringExtra("type");
        	getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.GAMES_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
        	getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.WORDS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
        	getContentResolver().delete(Uri.withAppendedPath(WordGameProvider.USERS_URI, gameid), DBHelper.GAME_ID+"=?", new String[] {gameid});
        	generateNotification(context, opponentName+" has forfeited! You have won a game of "+type+"!", GameListActivity.class, null);
        }
        else if (word != null) {
        	String playerid = intent.getStringExtra("playerid");
        	String name = intent.getStringExtra("playername");
            int index = Integer.parseInt(intent.getStringExtra("wordindex"));
//            if (index == -1) {
//            	throw new IllegalArgumentException("SERVER DID NOT RETURN AN INDEX VALUE FOR WORD "+word);
//            }
        	String message = name+" just played "+word+". It's your turn!";
        	ContentValues newPlayer = new ContentValues();
        	newPlayer.put(DBHelper.CURRENT_PLAYER, PreferenceManager.getDefaultSharedPreferences(context).getString(WordGameActivity.USERID_PREFERENCE_KEY, null));
        	getContentResolver().update(Uri.withAppendedPath(WordGameProvider.GAMES_URI, gameid), newPlayer, DBHelper.GAME_ID+"=?", new String[] {gameid});
            Bundle b = new Bundle();
            b.putString("gameid", gameid);
            b.putBoolean("timeattack", false);
            b.putString("oppid", playerid);
            ContentValues wordEntry = new ContentValues();
        	wordEntry.put(DBHelper.GAME_ID, gameid);
        	wordEntry.put(DBHelper.WORD_USERNAME, playerid);
        	wordEntry.put(DBHelper.WORD, word);
        	wordEntry.put(DBHelper.MY_ORDER, index);
        	getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.WORDS_URI, gameid), wordEntry);
            generateNotification(context, message, GameListActivity.class, b);
        }
        else if (gameJson != null) {
        	Gson gson = new Gson();
           	Game game = gson.fromJson(gameJson, Game.class);
           	String oppId = null;
           	for (int i = 0; i < game.getUserStringList().size(); i++) {
        		ContentValues tempValues = new ContentValues();
        		if (!game.getUserIds().get(i).equalsIgnoreCase(PreferenceManager.getDefaultSharedPreferences(context).getString(WordGameActivity.USERID_PREFERENCE_KEY, null))) {
        			oppId = game.getUserIds().get(i);
        		}
        		tempValues.put(DBHelper.USER_USERNAME, game.getUserStringList().get(i));
        		tempValues.put(DBHelper.USERID, game.getUserIds().get(i));
        		tempValues.put(DBHelper.GAME_ID, game.getStrId());
        		getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.USERS_URI, game.getStrId()), tempValues);
        	}
           	ContentValues cv = WordGameProvider.gameToContentValues(game);
        	getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.GAMES_URI, game.getStrId()), cv);
        	String message = WordGameProvider.getUserName(context, WordGameProvider.getUserName(context, game.getCurrPlayer())) +" has invited you to a game!";
            Bundle b = new Bundle();
            b.putString("gameid", game.getStrId());
            b.putString("oppid", oppId);
            generateNotification(context, message, GameListActivity.class, b);
        }
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
//        Log.i(TAG, "Received deleted messages notification");
//        String message = getString(R.string.gcm_deleted, total);
//        displayMessage(context, message);
//        // notifies user
//        generateNotification(context, message);
    }

    @Override
    public void onError(Context context, String errorId) {
//        Log.i(TAG, "Received error: " + errorId);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
//        Log.i(TAG, "Received recoverable error: " + errorId);
        return super.onRecoverableError(context, errorId);
    }
	
	/**
     * Issues a notification to inform the user that server has sent a message.
     */
    @SuppressWarnings("rawtypes")
	private static void generateNotification(Context context, String message, Class actToGo, Bundle extras) {
        int icon = R.drawable.ic_stat_example;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        notification.defaults |= Notification.DEFAULT_SOUND;
        String title = context.getString(R.string.app_name);
        Intent notificationIntent = new Intent(context, GameListActivity.class);
        if (extras != null) {
        	notificationIntent.putExtras(extras);
        }
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }

}
