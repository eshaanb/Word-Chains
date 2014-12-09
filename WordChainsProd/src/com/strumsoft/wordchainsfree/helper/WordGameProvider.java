package com.strumsoft.wordchainsfree.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.strumsoft.wordchainsfree.model.Artists;
import com.strumsoft.wordchainsfree.model.FBMessage;
import com.strumsoft.wordchainsfree.model.Game;
import com.strumsoft.wordchainsfree.model.Games;
import com.strumsoft.wordchainsfree.model.Movies;
import com.strumsoft.wordchainsfree.model.TVShows;

public class WordGameProvider extends ContentProvider {

	private DBHelper dbHelper;
	
    public static final String GAMES_TABLE_NAME = "games";
    
    private static final String DATABASE_NAME = "wordgames.db";
    
    public static final String WORDS_TABLE_NAME = "words";

    public static final String TEMP_WORDS_TABLE_NAME = "tempwords";

    public static final String BOTWORDS_TABLE_NAME = "botwords";

    public static final String FRIENDS_TABLE_NAME = "friends";

    public static final String REGISTERED_FRIENDS_TABLE_NAME = "regfriends";

    public static final String USERS_TABLE_NAME = "users";

    public static final String MESSAGES_TABLE_NAME = "messages";

    public static final String SCORE_TABLE_NAME = "score";

    public static final String AUTHORITY = "com.strumsoft.wordgamefree.provider.WordGameProvider";
    
    public static final Uri GAMES_URI = Uri.parse("content://"+AUTHORITY+"/"+GAMES_TABLE_NAME);
    
    public static final Uri USERS_URI = Uri.parse("content://"+AUTHORITY+"/"+USERS_TABLE_NAME);
    
    public static final Uri WORDS_URI = Uri.parse("content://"+AUTHORITY+"/"+WORDS_TABLE_NAME);

    public static final Uri TEMP_WORDS_URI = Uri.parse("content://"+AUTHORITY+"/"+TEMP_WORDS_TABLE_NAME);

    public static final Uri BOT_WORDS_URI = Uri.parse("content://"+AUTHORITY+"/"+BOTWORDS_TABLE_NAME);

    public static final Uri FRIENDS_URI = Uri.parse("content://"+AUTHORITY+"/"+FRIENDS_TABLE_NAME);

    public static final Uri REGISTERED_FRIENDS_URI = Uri.parse("content://"+AUTHORITY+"/"+REGISTERED_FRIENDS_TABLE_NAME);

    public static final Uri ALL_URI = Uri.parse("content://"+AUTHORITY+"/all");
    
    public static final Uri ALL_GAMES_URI = Uri.parse("content://"+AUTHORITY+"/allgames");
    
    public static final Uri MESSAGES_URI = Uri.parse("content://"+AUTHORITY+"/messages");
    
    public static final Uri SCORE_URI = Uri.parse("content://"+AUTHORITY+"/score");

    private static final UriMatcher uriMatcher;

    // Mapped to content://wordgame/games
    private static final int GAMES = 1;
    // Mapped to content://wordgame/messages
    private static final int MESSAGES = 12;
    // Mapped to content://wordgame/player
    private static final int PLAYERS = 2;
    // Mapped to content://wordgame/words
    private static final int WORDS = 3;
    private static final int BOT_WORDS = 9;
    // Mapped to content://wordgame/games/*
    private static final int GAME_ITEM = 4;
    // Mapped to content://wordgame/users/*
    private static final int PLAYER_ITEM = 5;
    // Mapped to content://wordgame/words/*
    private static final int WORD_ITEM = 6;
    // Mapped to content://wordgame/botwords/*
    private static final int BOT_WORD_ITEM = 11;
    // Mapped to content://wordgame/messages/*
    private static final int MESSAGE_ITEM = 13;
    // Mapped to content://wordgame/all
    private static final int ALL = 7;
    private static final int REGISTERED_FRIENDS = 14;
    private static final int SCORE_ITEM = 15;
    private static final int SCORES = 16;
    private static final int TEMP_WORDS = 17;
    private static final int TEMP_WORD_ITEM = 18;

    
    private static final HashMap<String, String> gamesProjection;
    private static final HashMap<String, String> playersProjection;
    private static final HashMap<String, String> wordsProjection;
    
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, GAMES_TABLE_NAME, GAMES);
        uriMatcher.addURI(AUTHORITY, USERS_TABLE_NAME, PLAYERS);
        uriMatcher.addURI(AUTHORITY, WORDS_TABLE_NAME, WORDS);
        uriMatcher.addURI(AUTHORITY, BOTWORDS_TABLE_NAME, BOT_WORDS);
        uriMatcher.addURI(AUTHORITY, GAMES_TABLE_NAME+"/*", GAME_ITEM);
        uriMatcher.addURI(AUTHORITY, USERS_TABLE_NAME+"/*", PLAYER_ITEM);
        uriMatcher.addURI(AUTHORITY, WORDS_TABLE_NAME+"/*", WORD_ITEM);
        uriMatcher.addURI(AUTHORITY, BOTWORDS_TABLE_NAME+"/*", BOT_WORD_ITEM);
        uriMatcher.addURI(AUTHORITY, "all", ALL);
        uriMatcher.addURI(AUTHORITY, MESSAGES_TABLE_NAME, MESSAGES);
        uriMatcher.addURI(AUTHORITY, MESSAGES_TABLE_NAME+"/*", MESSAGE_ITEM);
        uriMatcher.addURI(AUTHORITY, REGISTERED_FRIENDS_TABLE_NAME, REGISTERED_FRIENDS);
        uriMatcher.addURI(AUTHORITY, SCORE_TABLE_NAME, SCORES);
        uriMatcher.addURI(AUTHORITY, SCORE_TABLE_NAME+"/*", SCORE_ITEM);
        uriMatcher.addURI(AUTHORITY, TEMP_WORDS_TABLE_NAME, TEMP_WORDS);
        uriMatcher.addURI(AUTHORITY, TEMP_WORDS_TABLE_NAME+"/*", TEMP_WORD_ITEM);
        

        gamesProjection = new HashMap<String, String>();
        gamesProjection.put(DBHelper.GAME_ID, DBHelper.GAME_ID);
        gamesProjection.put(DBHelper.CREATOR, DBHelper.CREATOR);
        gamesProjection.put(DBHelper.TURNCOUNT, DBHelper.TURNCOUNT);
        gamesProjection.put(DBHelper.TYPE, DBHelper.TYPE);
        playersProjection = new HashMap<String, String>();
        playersProjection.put(DBHelper.USER_USERNAME, DBHelper.USER_USERNAME);
        playersProjection.put(DBHelper.GAME_ID, DBHelper.GAME_ID);
        wordsProjection = new HashMap<String, String>();
        wordsProjection.put(DBHelper.GAME_ID, DBHelper.GAME_ID);
        wordsProjection.put(DBHelper.MY_ORDER, DBHelper.MY_ORDER);
        wordsProjection.put(DBHelper.GAME_ID, DBHelper.GAME_ID);
        wordsProjection.put(DBHelper.WORD_USERNAME, DBHelper.WORD_USERNAME);
        wordsProjection.put(DBHelper.WORD, DBHelper.WORD);
    }
    
    public static ContentValues gameToContentValues(Game g) {
        ContentValues gameTableCv = new ContentValues();
        gameTableCv.put(DBHelper.GAME_ID, g.getStrId());
        gameTableCv.put(DBHelper.TYPE, g.getType());
        gameTableCv.put(DBHelper.PIC_URL, g.getPicUrl());
        gameTableCv.put(DBHelper.CURRENT_PLAYER, g.getCurrPlayer());
        gameTableCv.put(DBHelper.CREATOR, g.getGameCreator());
        gameTableCv.put(DBHelper.MODE, g.getMode());
        return gameTableCv;
    }
    
    public static FBMessage messageCurToMessage(Cursor c) {
    	if (c.getCount() < 1) {
    		return null;
    	}
    	return new FBMessage(c.getString(c.getColumnIndex(DBHelper.BODY)), 
    			c.getInt(c.getColumnIndex(DBHelper.MY_ORDER)),
    			c.getString(c.getColumnIndex(DBHelper.USERID)),
    			c.getString(c.getColumnIndex(DBHelper.PIC_URL)));
    }
    
    public static Game gameCurToGame(Cursor c) {
    	if (c.getCount() < 1) {
    		return null;
    	}
    	if (c.getColumnIndex(DBHelper.CURRENT_PLAYER) != -1) {
    		String id = c.getString(c.getColumnIndex(DBHelper.GAME_ID));
            String type  = c.getString(c.getColumnIndex(DBHelper.TYPE));
            String currPlayer = c.getString(c.getColumnIndex(DBHelper.CURRENT_PLAYER));
            String creator = c.getString(c.getColumnIndex(DBHelper.CREATOR));
            String picUrl = c.getString(c.getColumnIndex(DBHelper.PIC_URL));
            String mode = c.getString(c.getColumnIndex(DBHelper.MODE));
            int index;
            ArrayList<String> ids = null;
            ArrayList<String> names = null;
            if ((index = c.getColumnIndex(DBHelper.USERLIST)) != -1) {
            	String userids = c.getString(c.getColumnIndex(DBHelper.USERIDS));
            	String userList = c.getString(index);
            	if (userList != null) {
                	names = new ArrayList<String>(Arrays.asList(userList.split(",")));
            	}
            	if (userids != null) {
            		ids = new ArrayList<String>(Arrays.asList(userids.split(",")));
            	}
            }
            return new Game(ids, names, creator, id, null, currPlayer, type, picUrl, mode);
    	}
    	else {
    		String id = c.getString(c.getColumnIndex(DBHelper.GAME_ID));
            String type  = c.getString(c.getColumnIndex(DBHelper.TYPE));
            return new Game(null, null, null, id, null, null, type, null, null);
    	}
    }
        
    public static String getUserName(Context c, String userid) {
    	Cursor myCur = c.getContentResolver().query(USERS_URI,
    			new String[] {DBHelper.USER_USERNAME},
    			DBHelper.USERID+"=?",
    			new String[] {userid},
    			null);
    	myCur.moveToFirst();
    	if (myCur.getCount() > 0) {
        	String retString = myCur.getString(myCur.getColumnIndex(DBHelper.USER_USERNAME));
        	myCur.close();
        	return retString;
    	}
    	else {
    		return userid;
    	}
    }
    
    public static LinkedHashMap<String, HashMap<String, String>> wordCurToWordList(Context con, Cursor c) {
    	c.moveToFirst();
    	LinkedHashMap<String, HashMap<String, String>> wordList = new LinkedHashMap<String, HashMap<String, String>>();
    	while (!c.isAfterLast()) {
    		String word = c.getString(c.getColumnIndex(DBHelper.WORD));
    		String name = c.getString(c.getColumnIndex(DBHelper.WORD_USERNAME));
    		int order = c.getInt(c.getColumnIndex(DBHelper.MY_ORDER));
    		HashMap<String, String> wordToPlayer = new HashMap<String, String>();
    		wordToPlayer.put(word, name);
    		wordList.put(""+order, wordToPlayer);
            c.moveToNext();
    	}
        return wordList;
    }
    
    public static Games jsonStringToGamesList(String json) {
   		Gson gson2 = new Gson();
       	Games games = gson2.fromJson(json, Games.class);
       	return games;
    }
    
    public static Artists jsonStringToArtists(String json) {
   		Gson gson2 = new Gson();
       	Artists artists = gson2.fromJson(json, Artists.class);
       	return artists;
    }
    
    public static Movies jsonStringToMovies(String json) {
   		Gson gson2 = new Gson();
       	Movies movies = gson2.fromJson(json, Movies.class);
       	return movies;
    }
    
    public static TVShows jsonStringToTVShows(String json) {
   		Gson gson2 = new Gson();
   		TVShows shows = gson2.fromJson(json, TVShows.class);
       	return shows;
    }

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		Uri gameUri = null;
        switch (uriMatcher.match(uri)) {
        case GAME_ITEM:
        	gameUri = Uri.withAppendedPath(GAMES_URI, whereArgs[0]);
        	count = db.delete(GAMES_TABLE_NAME, where, whereArgs);
            break;
        case PLAYER_ITEM:
        	gameUri = Uri.withAppendedPath(USERS_URI, whereArgs[0]);
        	count = db.delete(USERS_TABLE_NAME, where, whereArgs);
            break;
        case WORD_ITEM:
        	gameUri = Uri.withAppendedPath(WORDS_URI, whereArgs[0]);
        	count = db.delete(WORDS_TABLE_NAME, where, whereArgs);
            break;
        case BOT_WORD_ITEM:
        	gameUri = Uri.withAppendedPath(BOT_WORDS_URI, whereArgs[0]);
        	count = db.delete(BOTWORDS_TABLE_NAME, where, whereArgs);
            break;
        case MESSAGE_ITEM:
        	gameUri = Uri.withAppendedPath(MESSAGES_URI, whereArgs[0]);
        	count = db.delete(MESSAGES_TABLE_NAME, where, whereArgs);
            break;
        case REGISTERED_FRIENDS:
        	count = db.delete(REGISTERED_FRIENDS_TABLE_NAME, where, whereArgs);
            break;
        case SCORE_ITEM:
        	gameUri = Uri.withAppendedPath(SCORE_URI, whereArgs[0]);
        	count = db.delete(SCORE_TABLE_NAME, where, whereArgs);
            break;
        case TEMP_WORD_ITEM:
        	count = db.delete(TEMP_WORDS_TABLE_NAME, where, whereArgs);
            break;
        case ALL:
        	count += db.delete(GAMES_TABLE_NAME, where, whereArgs);
        	count += db.delete(USERS_TABLE_NAME, null, null);
        	count += db.delete(WORDS_TABLE_NAME, null, null);
        	count += db.delete(SCORE_TABLE_NAME, null, null);
            break;
        default:
            throw new IllegalArgumentException("Unsupported Operation for Uri=" + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        if (null != gameUri) {
        	getContext().getContentResolver().notifyChange(gameUri, null);
        }
        return count;
	}

	@Override
	public String getType(Uri arg0) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long row = 0L;
        Uri notifyUri = null;
        switch (uriMatcher.match(uri)) {
        // games/*
        case GAME_ITEM:
        	if (values.containsKey("gameid")) {
        		notifyUri = Uri.withAppendedPath(GAMES_URI, values.getAsString("gameid"));
            }
        	row = db.insert(GAMES_TABLE_NAME, "", values);
            break;
        // users/*
        case PLAYER_ITEM:
        	if (values.containsKey("gameid")) {
        		notifyUri = Uri.withAppendedPath(USERS_URI, values.getAsString("gameid"));
            }
            row = db.insert(USERS_TABLE_NAME, "", values);
            break;
        // words/*
        case WORD_ITEM:
        	if (values.containsKey("gameid")) {
        		notifyUri = Uri.withAppendedPath(WORDS_URI, values.getAsString("gameid"));
            }
            row = db.insert(WORDS_TABLE_NAME, "", values);
            break;
        case BOT_WORD_ITEM:
            row = db.insert(BOTWORDS_TABLE_NAME, "", values);
            break;
        case REGISTERED_FRIENDS:
            row = db.insert(REGISTERED_FRIENDS_TABLE_NAME, "", values);
            break;
        case SCORE_ITEM:
        	if (values.containsKey("gameid")) {
        		notifyUri = Uri.withAppendedPath(SCORE_URI, values.getAsString("gameid"));
            }
            row = db.insert(SCORE_TABLE_NAME, "", values);
            break;
        case TEMP_WORD_ITEM:
            row = db.insert(TEMP_WORDS_TABLE_NAME, "", values);
            break;
        case MESSAGE_ITEM:
        	if (values.containsKey(DBHelper.THREAD_ID)) {
        		notifyUri = Uri.withAppendedPath(MESSAGES_URI, values.getAsString(DBHelper.THREAD_ID));
            }
            row = db.insert(MESSAGES_TABLE_NAME, "", values);
            Cursor q = db.query(MESSAGES_TABLE_NAME, null, DBHelper.THREAD_ID+"=?", new String[] {values.getAsString(DBHelper.THREAD_ID)}, null, null, null);
            if (q.getCount() > 19) {
            	db.delete(MESSAGES_TABLE_NAME, DBHelper.MY_ORDER+"=?", new String[] {String.valueOf(values.getAsInteger(DBHelper.MY_ORDER)-20)});
            }
            q.close();
            break;
        default:
            throw new IllegalArgumentException("Unsupported Operation for Uri=" + uri);
        }
        // if successful
        if (row > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            if (null != notifyUri) {
                getContext().getContentResolver().notifyChange(notifyUri, null);
            }
            return uri;
        }
        return null;
	}

	@Override
	public boolean onCreate() {
        dbHelper = DBHelper.getInstance(getContext(), DATABASE_NAME);
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        switch (uriMatcher.match(uri)) {
        case GAMES:
            cursor = db.query(GAMES_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        case PLAYERS:
            cursor = db.query(USERS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        case WORDS:
            cursor = db.query(WORDS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        case BOT_WORDS:
            cursor = db.query(BOTWORDS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        case MESSAGES:
            cursor = db.query(MESSAGES_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        case REGISTERED_FRIENDS:
            cursor = db.query(REGISTERED_FRIENDS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        case SCORES:
            cursor = db.query(SCORE_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        case TEMP_WORDS:
            cursor = db.query(TEMP_WORDS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            break;
        default:
            throw new IllegalArgumentException("Unsupported Operation for Uri=" + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) { 
            case GAME_ITEM:
                count = db.update(GAMES_TABLE_NAME, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	
}
