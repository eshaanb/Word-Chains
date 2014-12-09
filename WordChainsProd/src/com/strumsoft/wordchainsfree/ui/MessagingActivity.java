package com.strumsoft.wordchainsfree.ui;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.adapter.MessagingAdapter;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.FacebookChatManager;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;

public class MessagingActivity extends Activity implements OnClickListener {

	private Chat thisChat;
	private Button send;
	private EditText message;
	private ListView chatList;
	private FacebookChatManager facebookChatManager;
	private String opponentId;
	private NewMessageObserver nwo;
	private MessagingAdapter myAdapter;
	private ProgressDialog connectingDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.messaginglayout);
		setVolumeControlStream(AudioManager.STREAM_MUSIC); //TODO: play a sound when a message comes
		connectingDialog = new ProgressDialog(this);
		connectingDialog.setMessage("Connecting...");
		connectingDialog.setCancelable(false);
		connectingDialog.show();
		opponentId = getIntent().getStringExtra("oppid");
		chatList = (ListView) findViewById(R.id.messageList); 
		send = (Button) findViewById(R.id.send);
		message = (EditText) findViewById(R.id.message);
		send.setOnClickListener(this);
		facebookChatManager = FacebookChatManager.getInstance(null);
		nwo = new NewMessageObserver();
		
//		Roster r = facebookChatManager.getRoster();
//		for (RosterEntry e : r.getEntries()) {
//			Log.d("USER", e.getUser());
//			Log.d("NAME", e.getName());
//		}
		super.onCreate(savedInstanceState);
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
	
	@Override
	protected void onResume() {
		initFB();
		super.onResume();
	}
	
	private void initFB() {
		new ConnectMessaging().execute((Void) null); //asynchronously connect to facebook xmpp chat
//		if (facebook.shouldExtendAccessToken()) {
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
//    				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MessagingActivity.this).edit();
//                    editor.putString("access_token", facebook.getAccessToken());
//                    editor.putLong("access_expires", facebook.getAccessExpires());
//                    editor.commit();
//    				new ConnectMessaging().execute((Void) null);
//    			}
//    		});
//		}
//		else {
//			new ConnectMessaging().execute((Void) null);
//		}
	}
	
	@Override
	protected void onPause() {
		if (connectingDialog != null) {
			connectingDialog.cancel();
			connectingDialog = null;
			finish();
		}
		super.onPause();
	}

	@Override
	public void onClick(View v) {
		final String messageToSend = message.getText().toString();
		if (messageToSend.trim().length() > 0) {
			message.setText("");
			try {
				thisChat.sendMessage(messageToSend);
			} catch (Exception e) {
				Toast.makeText(this, "Message could not be sent. - Reconnecting", Toast.LENGTH_SHORT).show();
				connectingDialog = new ProgressDialog(this);
				connectingDialog.setMessage("Connecting...");
				connectingDialog.setCancelable(false);
				connectingDialog.show();
				new ConnectMessaging().execute((Void) null);
				return;
			}
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					ContentValues cv = new ContentValues();
					cv.put(DBHelper.BODY, messageToSend);
					String userid = PreferenceManager.getDefaultSharedPreferences(MessagingActivity.this).getString(WordGameActivity.USERID_PREFERENCE_KEY, null);
					cv.put(DBHelper.USERID, userid);
					Cursor q = getContentResolver().query(WordGameProvider.MESSAGES_URI, null, DBHelper.THREAD_ID+"=?", new String[] {opponentId}, null);
					cv.put(DBHelper.MY_ORDER, q.getCount());
					q.close();
					cv.put(DBHelper.PIC_URL, PreferenceManager.getDefaultSharedPreferences(MessagingActivity.this).getString(WordGameActivity.LOGGED_IN_USER_PIC, null));
					cv.put(DBHelper.THREAD_ID, opponentId);
					getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.MESSAGES_URI, opponentId), cv);
					return null;
				}
				
				protected void onPostExecute(Void result) {
				};
				
			}.execute((Void) null);
		}
		else {
			Toast.makeText(this, "Please enter a non-empty message.", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onStop() {
		getContentResolver().unregisterContentObserver(nwo);
		super.onStop();
	}
	
	class NewMessageObserver extends ContentObserver {

		public NewMessageObserver() {
			super(null);
		}

		public void onChange(boolean selfChange) {
			myAdapter.changeCursor(getContentResolver().query(WordGameProvider.MESSAGES_URI,
				null,
				DBHelper.THREAD_ID+"=?",
				new String[] {opponentId},
				DBHelper.MY_ORDER));
			myAdapter.notifyDataSetChanged();
		}
	}
	
	class ConnectMessaging extends AsyncTask<Void, Void, String> {
		
		@Override
		protected String doInBackground(Void... params) {
			try {
				if (!facebookChatManager.isConnected()) {
					facebookChatManager.connect();
				}
				if (!facebookChatManager.isAuthenticated()) {
					 Session sess = Session.getActiveSession();
					 if (sess != null && sess.getAccessToken() != null) {
						 facebookChatManager.logIn(getString(R.string.app_id), sess.getAccessToken());
					 }
					 else {
						 runOnUiThread(new Runnable() {
							@Override
							public void run() {
								AlertDialog.Builder builder = new AlertDialog.Builder(MessagingActivity.this);
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
						});
					 }
				}
			} catch (Exception e) {
				if (!e.getMessage().startsWith("Already logged in to")) {
//					Log.e("Error", e.getMessage());
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							showNetworkDialog();
						}
					});
					e.printStackTrace();
				}
			}
			thisChat = facebookChatManager.createNewChat("-"+opponentId+"@chat.facebook.com", new MessageListener() {
				@Override
				public void processMessage(Chat c, final Message m) {
					new AsyncTask<Void, Void, Void>() {

						@Override
						protected Void doInBackground(Void... params) {
							if (m.getBody() != null) {
								ContentValues cv = new ContentValues();
								cv.put(DBHelper.BODY, m.getBody());
								cv.put(DBHelper.USERID, opponentId);
								Cursor q = getContentResolver().query(WordGameProvider.MESSAGES_URI, null, DBHelper.THREAD_ID+"=?", new String[] {opponentId}, null);
								cv.put(DBHelper.MY_ORDER, q.getCount());
								q.close();
								cv.put(DBHelper.PIC_URL, getIntent().getStringExtra("picurl"));
								cv.put(DBHelper.THREAD_ID, opponentId);
								getContentResolver().insert(Uri.withAppendedPath(WordGameProvider.MESSAGES_URI, opponentId), cv);
							}
							return null;
						}
						
					}.execute((Void) null);
				}
			});
			return WordGameProvider.getUserName(MessagingActivity.this, opponentId).split(" ")[0];
		}
		
		protected void onPostExecute(String result) {
			((TextView) findViewById(R.id.title)).setText("Conversation With "+result);
			if (connectingDialog != null && connectingDialog.isShowing()) {
				connectingDialog.cancel();
				connectingDialog = null;
			}
			myAdapter = new MessagingAdapter(MessagingActivity.this, managedQuery(WordGameProvider.MESSAGES_URI,
					null,
					DBHelper.THREAD_ID+"=?",
					new String[] {opponentId},
					DBHelper.MY_ORDER));
			getContentResolver().registerContentObserver(Uri.withAppendedPath(WordGameProvider.MESSAGES_URI, opponentId), false, nwo);
			chatList.setAdapter(myAdapter);
//			scrollMyListViewToBottom();
		};
	}
	
}
