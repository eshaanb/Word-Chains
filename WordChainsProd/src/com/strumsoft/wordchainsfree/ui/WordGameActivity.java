package com.strumsoft.wordchainsfree.ui;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.google.android.gcm.GCMRegistrar;
import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.ServerUtilities;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;

public class WordGameActivity extends Activity implements OnClickListener {
    
	public static final String XMPP_LOGGEDIN = "xmpp_loggedin";
	public static final String USERNAME_PREFERENCE_KEY = "loggedinusername";
	public static final String LOGGED_IN_USER_PIC = "loggedinuserpic";
	public static final String USERID_PREFERENCE_KEY = "loggedinuserid";
    private Session.StatusCallback statusCallback = new SessionStatusCallback();
//	private EditText usernameRegister;
//	private EditText usernameLogin;
//	private Button submitRegister;
//	private Button submitLogin;
	private LoginButton loginWithFacebook;
//	private UiLifecycleHelper uiHelper;
	private SharedPreferences myPrefs;
	private AsyncTask<Void, Void, Void> mRegisterTask;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);
//        uiHelper = new UiLifecycleHelper(this, statusCallback);
//        uiHelper.onCreate(savedInstanceState);
        myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals("")) {
            // Automatically registers application on startup.
            GCMRegistrar.register(this, Util.SENDER_ID);
        }
        else {
        	Editor e = myPrefs.edit();
        	e.putString("regId", regId);
        	e.commit();
        }
        final Context context = this;
        mRegisterTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
            	String prefRegId = PreferenceManager.getDefaultSharedPreferences(WordGameActivity.this).getString("regId", null);
                boolean registered =
                        ServerUtilities.register(context, prefRegId);
                // At this point all attempts to register with the app
                // server failed, so we need to unregister the device
                // from GCM - the app will try to register again when
                // it is restarted. Note that GCM will send an
                // unregistered callback upon completion, but
                // GCMIntentService.onUnregistered() will ignore it.
                if (!registered) {
                    GCMRegistrar.unregister(context);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
            	if (GCMRegistrar.isRegistered(WordGameActivity.this) && GCMRegistrar.isRegisteredOnServer(WordGameActivity.this)) {
                    mRegisterTask = null;
            	}
            }

        };
//        usernameRegister = (EditText) findViewById(R.id.username);
//        usernameLogin = (EditText) findViewById(R.id.login_username);
//        submitLogin = (Button) findViewById(R.id.login_button);
//        submitLogin.setOnClickListener(this);
        loginWithFacebook = (LoginButton) findViewById(R.id.login_with_facebook);
        loginWithFacebook.setReadPermissions(Arrays.asList("xmpp_login"));
//        loginWithFacebook.setOnClickListener(this);
//        Session session = Session.getActiveSession();
//        if (session == null) {
//            if (savedInstanceState != null) {
//                session = Session.restoreSession(this, null, statusCallback, savedInstanceState);
//            }
//            if (session == null) {
//                session = new Session(this);
//            }
//            Session.setActiveSession(session);
//            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
//                session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
//            }
//        }
//        submitRegister = (Button) findViewById(R.id.submit);
//        submitRegister.setOnClickListener(this);
//        String access_token = myPrefs.getString("access_token", null);
//        long expires = myPrefs.getLong("access_expires", 0);
//        if (access_token != null) {
//            facebook.setAccessToken(access_token);
//        }
//        if (expires != 0) {
//            facebook.setAccessExpires(expires);
//        }
    }
        
    @Override
    protected void onPause() {
//    	uiHelper.onPause();
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
//    	uiHelper.onResume();
//    	facebook.extendAccessTokenIfNeeded(this, null); recent
    	String username = PreferenceManager.getDefaultSharedPreferences(WordGameActivity.this).getString(USERNAME_PREFERENCE_KEY, null);
    	if (username != null) {
    		startActivity(new Intent(this, GameListActivity.class));
    		finish();
    	}
    	else {
    		Session session = Session.getActiveSession();
            if (session != null && (session.isOpened() || session.isClosed())) {
                onSessionStateChange(session, session.getState(), null);
            }
    	}
    	super.onResume();
    }
    
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
//			case R.id.submit:
//				String email = usernameRegister.getText().toString().trim();
//				if (email.length() > 0 && email.contains("@")) {
//					RestClient doRegister = new RestClient();
//					Map<String, String> params = new HashMap<String, String>();
//					params.put("email", email);
//					try {
//						String response = doRegister.post("/register", params);
//						if (response.length() > 0) {
//							Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
//							break;
//						}
//					} catch (NetworkException e) {
//						Toast.makeText(this, R.string.network_fail, Toast.LENGTH_LONG).show();
//						Log.e("ERROR", e.getMessage());
//						break;
//					}
//					SharedPreferences.Editor sp = myPrefs.edit();
//					sp.putString(USERNAME_PREFERENCE_KEY, email);
//					sp.commit();
//			        mRegisterTask.execute(null, null, null);
//					Intent i = new Intent(this, GameListActivity.class);
//					startActivity(i);
//					finish();
//				}
//				break;
//			case R.id.login_button:
//				String loginemail = usernameLogin.getText().toString().trim();
//				if (loginemail.length() > 0 && loginemail.contains("@")) {
//					RestClient doRegister = new RestClient();
//					Map<String, String> params = new HashMap<String, String>();
//					params.put("email", loginemail);
//					try {
//						String response = doRegister.get("/login", params);
//						if (response.length() > 0) {
//							Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
//							if (!response.startsWith("Success")) {
//								break;
//							}
//						}
//					} catch (NetworkException e) {
//						Toast.makeText(this, R.string.network_fail, Toast.LENGTH_LONG).show();
//						Log.e("ERROR", e.getMessage());
//						break;
//					}
//					Intent i = new Intent(this, GameListActivity.class);
//					SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(this).edit();
//					sp.putString(USERNAME_PREFERENCE_KEY, loginemail);
//					sp.commit();
//			        mRegisterTask.execute(null, null, null);
//					startActivity(i);
//					finish();
//				}
//				break;
			case R.id.login_with_facebook:
//		        Session session = Session.getActiveSession();
//		        session.requestNewReadPermissions(new NewPermissionsRequest(this, Arrays.asList("xmpp_login")));
//		        if (!session.isOpened() && !session.isClosed()) {
//		            session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
//		        } else {
		        Session.openActiveSession(this, true, statusCallback);
//		        }
//				if(!facebook.isSessionValid() || !myPrefs.getBoolean(XMPP_LOGGEDIN, false)) {
//					facebook.authorize(this, new String[] {"email", "xmpp_login"}, new DialogListener() {
//			            @Override
//			            public void onComplete(Bundle values) {
//		    				FacebookChatManager facebookChatManager = FacebookChatManager.getInstance(null);
//		    				if (facebookChatManager.connect() && facebookChatManager.logIn(Util.FACEBOOK_APP_ID, Util.facebook.getAccessToken())) {
//		    					SharedPreferences.Editor editor = myPrefs.edit();
//			                    editor.putString("access_token", facebook.getAccessToken());
//			                    editor.putLong("access_expires", facebook.getAccessExpires());
//			                    editor.putBoolean(XMPP_LOGGEDIN, true);
//			                    editor.commit();
//		    					requestMe();
//		    				}
//		    				else {
//		    					Toast.makeText(WordGameActivity.this, "Login failed, please try again", Toast.LENGTH_LONG).show();
//		    					
//		    				}
//		                    
//			            }
//
//			            @Override
//			            public void onFacebookError(FacebookError error) {
//			            	Log.e("Error", error.getMessage());
//			            	Toast.makeText(WordGameActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
//			            }
//
//			            @Override
//			            public void onError(DialogError e) {
//			            	Toast.makeText(WordGameActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
//			            }
//
//			            @Override
//			            public void onCancel() {
//			            	
//			            }
//			        });
//
//				}
//				else {
//					requestMe();
//				}
		}
	}
	
//	private void requestMe() {
//		Util.showProgressDialog(this, "Please Wait", "Logging in...");
//        Bundle params = new Bundle();
//        params.putString("fields", "name, picture");
//		mRunner.request("me", params, new RequestListener() {
//			@Override
//			public void onMalformedURLException(MalformedURLException e, Object state) {
//				Log.e("ERROR", e.getMessage());
//			}
//			
//			@Override
//			public void onIOException(IOException e, Object state) {
//				Log.e("ERROR", e.getMessage());
//			}
//			
//			@Override
//			public void onFileNotFoundException(FileNotFoundException e, Object state) {
//				Log.e("ERROR", e.getMessage());
//			}
//			
//			@Override
//			public void onFacebookError(FacebookError e, Object state) {
//				Log.e("ERROR", e.getMessage());
//			}
//			
//			@Override
//			public void onComplete(String response, Object state) {
//				Log.d("me", response);
//				try {
//					JSONObject json = new JSONObject(response);
//					String myName = Html.fromHtml(json.getString("name")).toString();
//					String myId = Html.fromHtml(json.getString("id")).toString();
//					String myPic = json.getJSONObject("picture").getJSONObject("data").getString("url").replace("\\", "");
//					Editor edit = myPrefs.edit();
//					edit.putString(LOGGED_IN_USER_PIC, myPic);
//					edit.commit();
//					RestClient doRegister = new RestClient();
//					Map<String, String> params = new HashMap<String, String>();
//					params.put("userid", myId);
//					params.put("name", myName);
//					params.put("picurl", myPic);
//					try {
//						doRegister.post("/register", params);
//					} catch (NetworkException e) {
//						Log.e("ERROR", e.getMessage());
//						showToastOnUiThread(getString(R.string.network_fail));
//						runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								Util.cancelDialog();
//							}
//						});
//						return;
//					}
//					SharedPreferences.Editor sp = myPrefs.edit();
//					sp.putString(USERNAME_PREFERENCE_KEY, myName);
//					sp.putString(USERID_PREFERENCE_KEY, myId);
//					sp.commit();
//			        mRegisterTask.execute(null, null, null);
//					Intent i = new Intent(WordGameActivity.this, GameListActivity.class);
//					runOnUiThread(new Runnable() {
//						@Override
//						public void run() {
//							Util.cancelDialog();
//						}
//					});
//					startActivity(i);
//					finish();
//				} catch (JSONException e) {
//					//ignore since fb shouldnt return invalid jsons
//				}
//			}
//		});
//	}
	
	private void showToastOnUiThread(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(WordGameActivity.this, message, Toast.LENGTH_LONG).show();	
			}
		});
	}
	
	@Override
	protected void onDestroy() {
//    	uiHelper.onDestroy();
		if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
		GCMRegistrar.onDestroy(getApplicationContext());
		super.onDestroy();
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        uiHelper.onActivityResult(requestCode, resultCode, data);
        if (Session.getActiveSession() != null) {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
//        facebook.authorizeCallback(requestCode, resultCode, data);
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
//		uiHelper.onSaveInstanceState(outState);
		Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
	}
    
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened() && !Util.isShowing()) {
        	session.requestNewReadPermissions(new NewPermissionsRequest(WordGameActivity.this, Arrays.asList("xmpp_login")));
//        	session.requestNewReadPermissions(new NewPermissionsRequest(this, Arrays.asList("xmpp_login", "picture")));
        	Request r = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                	Log.d("me", response.getGraphObject().getInnerJSONObject().toString());
    				try {
    					SharedPreferences.Editor editor = myPrefs.edit();
//	                    editor.putString("access_token", facebook.getAccessToken());
//	                    editor.putLong("access_expires", facebook.getAccessExpires());
	                    editor.putBoolean(XMPP_LOGGEDIN, true);
	                    editor.commit();
    					JSONObject json = response.getGraphObject().getInnerJSONObject();
    					String myName = Html.fromHtml(json.getString("name")).toString();
    					String myId = Html.fromHtml(json.getString("id")).toString();
    					String myPic = json.getJSONObject("picture").getJSONObject("data").getString("url").replace("\\", "");
    					Editor edit = myPrefs.edit();
    					edit.putString(LOGGED_IN_USER_PIC, myPic);
    					edit.commit();
    					RestClient doRegister = new RestClient();
    					Map<String, String> params = new HashMap<String, String>();
    					params.put("userid", myId);
    					params.put("name", myName);
    					params.put("picurl", myPic);
    					try {
    						doRegister.post("/register", params);
    					} catch (NetworkException e) {
    						Log.e("ERROR", e.getMessage());
    						showToastOnUiThread(getString(R.string.network_fail));
//    						runOnUiThread(new Runnable() {
//    							@Override
//    							public void run() {
    								Util.cancelDialog();
//    							}
//    						});
    						return;
    					}
    					SharedPreferences.Editor sp = myPrefs.edit();
    					sp.putString(USERNAME_PREFERENCE_KEY, myName);
    					sp.putString(USERID_PREFERENCE_KEY, myId);
    					sp.commit();
    					if (mRegisterTask != null && (mRegisterTask.getStatus() != AsyncTask.Status.PENDING || mRegisterTask.getStatus() != AsyncTask.Status.RUNNING)) {
        			        mRegisterTask.execute(null, null, null);
    					}
    					Intent i = new Intent(WordGameActivity.this, GameListActivity.class);
//    					runOnUiThread(new Runnable() {
//    						@Override
//    						public void run() {
    							Util.cancelDialog();
//    						}
//    					});
    					startActivity(i);
    					finish();
    				} catch (JSONException e) {
    					//ignore since fb shouldnt return invalid jsons
    				}
                }
            });
        	Bundle params = new Bundle();
            params.putString("fields", "name, picture");
        	r.setParameters(params);
        	Request.executeBatchAsync(r);
        }
    }
    
    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	onSessionStateChange(session, state, exception);
        }
    }
	
}