package com.strumsoft.wordchainsfree.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;


public class DBHelper extends SQLiteOpenHelper {
 
	private static DBHelper instance;
    
    private static final int VERSION = 29;
    
	public static final String GAME_ID = "gameid";
	public static final String FBID = "id";
	public static final String WORD = "word";
	public static final String SCORE = "score";
	public static final String BODY = "body";
	public static final String MODE = "mode";
	public static final String SENDER = "sender";
	public static final String PIC_URL = "picurl";
	public static final String THREAD_ID = "thread";
	public static final String USERLIST = "userlist";
	public static final String USERIDS = "userids";
	public static final String TYPE = "type";
	public static final String CURRENT_PLAYER = "currentplayer";	
	public static final String CREATOR = "creator";	
	public static final String TURNCOUNT = "turncount";	
	public static final String MY_ORDER = "myorder";	
	public static final String USER_USERNAME = "user";
	public static final String USERID = "userid";
	public static final String WORD_USERNAME = "username";
	
    public DBHelper(Context context, String name, CursorFactory factory, int version) {
    	super(context, name, factory, version);
    }
 
    public static synchronized DBHelper getInstance(Context context, String name) {
        if (instance == null) {
            instance = new DBHelper(context, name, null, VERSION);
        }
        return instance;
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "+WordGameProvider.SCORE_TABLE_NAME+" ("+GAME_ID+" VARCHAR, "+USERID+" VARCHAR, "+SCORE+" INTEGER, _id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE "+WordGameProvider.GAMES_TABLE_NAME+" ("+MODE+" VARCHAR, "+PIC_URL+" VARCHAR, "+CREATOR+" VARCHAR, "+TYPE+" VARCHAR, "+GAME_ID+" VARCHAR, "+CURRENT_PLAYER+" VARCHAR, _id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE "+WordGameProvider.USERS_TABLE_NAME+" ("+USERID+" VARCHAR, "+GAME_ID+" VARCHAR, "+USER_USERNAME+" VARCHAR NOT NULL, _id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE "+WordGameProvider.WORDS_TABLE_NAME+" ("+MY_ORDER+" INTEGER NOT NULL, "+WORD+" VARCHAR NOT NULL , "+WORD_USERNAME+" VARCHAR NOT NULL , "+GAME_ID+" VARCHAR NOT NULL, _id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE "+WordGameProvider.TEMP_WORDS_TABLE_NAME+" ("+MY_ORDER+" INTEGER NOT NULL, "+WORD+" VARCHAR NOT NULL , "+WORD_USERNAME+" VARCHAR NOT NULL , "+GAME_ID+" VARCHAR NOT NULL, _id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE "+WordGameProvider.BOTWORDS_TABLE_NAME+" ("+MY_ORDER+" INTEGER NOT NULL, "+WORD+" VARCHAR NOT NULL , "+WORD_USERNAME+" VARCHAR NOT NULL , "+GAME_ID+" VARCHAR NOT NULL, _id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE "+WordGameProvider.MESSAGES_TABLE_NAME+" ("+MY_ORDER+" INTEGER NOT NULL, "+PIC_URL+" VARCHAR NOT NULL,"+BODY+" VARCHAR NOT NULL, "+THREAD_ID+" VARCHAR NOT NULL, "+USERID+" VARCHAR NOT NULL, _id INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE "+WordGameProvider.REGISTERED_FRIENDS_TABLE_NAME+" ("+USERID+" VARCHAR NOT NULL, _id INTEGER PRIMARY KEY AUTOINCREMENT)");

	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.GAMES_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.SCORE_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.BOTWORDS_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.TEMP_WORDS_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.USERS_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.WORDS_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.MESSAGES_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + WordGameProvider.REGISTERED_FRIENDS_TABLE_NAME);
        onCreate(db);
	}

}