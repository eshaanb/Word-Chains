package com.strumsoft.wordchainsfree.adapter;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.BaseDialogListener;
import com.strumsoft.wordchainsfree.helper.FriendsGetProfilePics;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.httplayer.NetworkException;
import com.strumsoft.wordchainsfree.httplayer.RestClient;
import com.strumsoft.wordchainsfree.ui.FriendsListActivity;

public class FriendsListAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private JSONArray jsonArray;
	private Context c;
	private Handler mHandler;
	
	public FriendsListAdapter(Context c, JSONArray jsonArray) {
		mHandler = new Handler();
		this.jsonArray = jsonArray;
		this.c = c;
		if (Util.model == null) {
			Util.model = new FriendsGetProfilePics();
		}
		Util.model.setListener(this);
		mInflater = LayoutInflater.from(c);
	}

	@Override
	public int getCount() {
		return jsonArray.length();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		JSONObject jsonObject = null;
		try {
			jsonObject = jsonArray.getJSONObject(position);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		View hView = convertView;
		if (convertView == null) {
			hView = mInflater.inflate(R.layout.frienditem, null);
			ViewHolder holder = new ViewHolder();
			holder.profile_pic = (ImageView) hView.findViewById(R.id.prof_pic);
			holder.name = (TextView) hView.findViewById(R.id.name);
			hView.setTag(holder);
		}
		try {
			final String id = Long.toString(jsonObject.getLong("uid"));
			final String name = jsonObject.getString("name");
			final String url = jsonObject.getString("pic_square").replace("\\", "");
			ViewHolder holder = (ViewHolder) hView.getTag();
			hView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					String checked = checkIfUser(id, c);
					if (checked != null && checked.equalsIgnoreCase("yes")) {
						((FriendsListActivity) c).sendBackFriend(name, id, url);
					}
					else if (checked != null && checked.equalsIgnoreCase("No")) {
						showPostToWallDialog(name, id);
					}
				}
			});
			holder.profile_pic.setImageBitmap(Util.model.getImage(id, url, c));
			holder.name.setText(name);
		} catch (JSONException e) {
			//ignore
		}
		return hView;
	}
	
	private String checkIfUser(String id, Context c) {
		ProgressDialog pd = new ProgressDialog(c);
		pd.setMessage("Inviting...");
		pd.show();
		try {
			RestClient rc = new RestClient();
			Map<String, String> userId = new HashMap<String, String>();
			userId.put("userid", id);
			String resp = rc.get("/checkuser", userId);
			pd.cancel();
			return resp;
		} catch (NetworkException e) {
			pd.cancel();
			showNetworkDialog(c);
		}
		return null;
	}
	
	private void showNetworkDialog(final Context c) {
		AlertDialog.Builder b = new Builder(c);
		b.setTitle(R.string.network_error);
		b.setCancelable(false);
		b.setMessage(R.string.network_required);
		b.setNegativeButton(c.getString(R.string.exit), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent i = new Intent(Intent.ACTION_MAIN);
				i.addCategory(Intent.CATEGORY_HOME);
		        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				c.startActivity(i);
			}
		});
		b.show();
	}
	
	private void showPostToWallDialog(String name, final String id) {
		new AlertDialog.Builder(c).setTitle("Send Request")
        .setMessage(String.format(c.getString(R.string.post_on_wall), name))
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle params = new Bundle();
                params.putString("message", "Come try out Word Chains with me! New, fun and innovative app!");
                params.putString("to", id);
                WebDialog requestsDialog = (
                    new WebDialog.RequestsDialogBuilder(c,
                        Session.getActiveSession(),
                        params))
                        .setOnCompleteListener(new OnCompleteListener() {

                            @Override
                            public void onComplete(Bundle values,
                                FacebookException error) {
                                if (error != null) {
                                    if (error instanceof FacebookOperationCanceledException) {
                                        Toast.makeText(c.getApplicationContext(), 
                                            "Request cancelled", 
                                            Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(c.getApplicationContext(), 
                                            "Network Error", 
                                            Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    final String requestId = values.getString("request");
                                    if (requestId != null) {
                                        Toast.makeText(c.getApplicationContext(), 
                                            "Request sent",  
                                            Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(c.getApplicationContext(), 
                                            "Request cancelled", 
                                            Toast.LENGTH_SHORT).show();
                                    }
                                }   
                            }

                        })
                        .build();
                requestsDialog.show();
            }

        }).setNegativeButton("No", null).show();
	}

	/*
     * Callback after the message has been posted on friend's wall.
     */
    public class PostDialogListener extends BaseDialogListener {
        @Override
        public void onComplete(Bundle values) {
            final String postId = values.getString("post_id");
            if (postId != null) {
                showToast("Message posted!");
            } else {
                showToast("Message could not be posted.");
            }
        }
    }
    
    public void showToast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(c, msg, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }
	
	class ViewHolder {
		ImageView profile_pic;
		TextView name;
	}

}