package com.strumsoft.wordchainsfree.helper;

import java.util.Hashtable;
import java.util.Stack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.strumsoft.wordchainsfree.R;

/*
 * Fetch friends profile pictures request via AsyncTask
 */
public class FriendsGetProfilePics {

    Hashtable<String, Bitmap> friendsImages;
    Hashtable<String, String> positionRequested;
    BaseAdapter listener;
    int runningCount = 0;
    Stack<ItemPair> queue;

    /*
     * 15 max async tasks at any given time.
     */
    final static int MAX_ALLOWED_TASKS = 15;

    public FriendsGetProfilePics() {
        friendsImages = new Hashtable<String, Bitmap>();
        positionRequested = new Hashtable<String, String>();
        queue = new Stack<ItemPair>();
    }

    /*
     * Inform the listener when the image has been downloaded. listener is
     * FriendsList here.
     */
    public void setListener(BaseAdapter listener) {
        this.listener = listener;
        reset();
    }

    public void reset() {
        positionRequested.clear();
        runningCount = 0;
        queue.clear();
    }

    /*
     * If the profile picture has already been downloaded and cached, return it
     * else execute a new async task to fetch it - if total async tasks >15,
     * queue the request.
     */
    public Bitmap getImage(String uid, String url, Context c) {
        Bitmap image = friendsImages.get(uid);
        if (image != null) {
            return image;
        }
        if (!positionRequested.containsKey(uid)) {
            positionRequested.put(uid, "");
            if (runningCount >= MAX_ALLOWED_TASKS) {
                queue.push(new ItemPair(uid, url));
            } else {
                runningCount++;
                new GetProfilePicAsyncTask().execute(uid, url);
            }
        }
        return BitmapFactory.decodeResource(c.getResources(),
                R.drawable.blankimage);
    }
    
    /*
     * If the profile picture has already been downloaded and cached, return it
     * else execute a new async task to fetch it - if total async tasks >15,
     * queue the request.
     */
    public Bitmap getImageNow(String uid, String url, Context c, ImageView view) {
        Bitmap image = friendsImages.get(uid);
        if (image != null) {
            return image;
        }
        new GetProfilePicAsyncTask().execute(uid, url, view);
        return BitmapFactory.decodeResource(c.getResources(),
                R.drawable.blankimage);
    }

    public void getNextImage() {
        if (!queue.isEmpty()) {
            ItemPair item = queue.pop();
            new GetProfilePicAsyncTask().execute(item.uid, item.url);
        }
    }

    /*
     * Start a AsyncTask to fetch the request
     */
    private class GetProfilePicAsyncTask extends AsyncTaskEx<Object, Void, Bitmap> {
        
    	String uid;
    	ImageView view;

        @Override
        protected Bitmap doInBackground(Object... params) {
            this.uid = (String) params[0];
            String url = (String) params[1];
            if (params.length > 2) {
            	view = (ImageView) params[2];
            }
            return Util.getBitmap(url);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            runningCount--;
            if (result != null) {
            	if (view != null) {
                	friendsImages.put(uid, result);
                	view.setImageBitmap(result);
            	}
            	else {
                	friendsImages.put(uid, result);
                    listener.notifyDataSetChanged();
                    getNextImage();
            	}
            }
        }
    }

    class ItemPair {
        String uid;
        String url;

        public ItemPair(String uid, String url) {
            this.uid = uid;
            this.url = url;
        }
    }

}
