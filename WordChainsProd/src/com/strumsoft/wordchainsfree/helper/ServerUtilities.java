package com.strumsoft.wordchainsfree.helper;

import static com.strumsoft.wordchainsfree.helper.Util.GCM_UNREGISTER;
import static com.strumsoft.wordchainsfree.helper.Util.GCM_URL;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.android.gcm.GCMRegistrar;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;
import com.strumsoft.wordchainsfree.ui.WordGameActivity;

/**
 * Helper class used to communicate with the demo server.
 */
public final class ServerUtilities {

	private static final String TAG = "GCM";
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random = new Random();

    /**
     * Register this account/device pair within the server.
     *
     * @return whether the registration succeeded or not.
     */
    public static boolean register(final Context context, final String regId) {
//        Log.i(TAG, "registering device (regId = " + regId + ")");
        String gcmUrl = GCM_URL;
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);
        params.put("userid", PreferenceManager.getDefaultSharedPreferences(context).getString(WordGameActivity.USERID_PREFERENCE_KEY, null));
        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
//            Log.d(TAG, "Attempt #" + i + " to register");
            try {
                RestClient rc = new RestClient();
                String successOrFail = rc.post(gcmUrl, params);
                if (successOrFail.length() > 1) {
                	throw new NetworkException(successOrFail);
                }
                GCMRegistrar.setRegisteredOnServer(context, true);
                return true;
            } catch (NetworkException e) {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
//                Log.e(TAG, "Failed to register on attempt " + i, e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
//                    Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
//                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     */
    public static void unregister(final Context context) {
//        Log.i(TAG, "unregistering device");
        String gcmUrl = GCM_UNREGISTER;
        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", PreferenceManager.getDefaultSharedPreferences(context).getString(WordGameActivity.USERID_PREFERENCE_KEY, null));
        RestClient rc = new RestClient();
        try {
        	String successOrFail = rc.post(gcmUrl, params);
            if (successOrFail.length() > 1) {
            	throw new NetworkException(successOrFail);
            }
            GCMRegistrar.setRegisteredOnServer(context, false);
        } catch (NetworkException e) {
        	// At this point the device is unregistered from GCM, but still
            // registered in the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
        }
    }
}
