package com.strumsoft.wordchainsfree.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.DBHelper;
import com.strumsoft.wordchainsfree.helper.FriendsGetProfilePics;
import com.strumsoft.wordchainsfree.model.Friend;
import com.strumsoft.wordchainsfree.ui.FriendsListActivity;

public class RegisteredFriendsListAdapter extends CursorAdapter {

	ArrayList<Friend> allFriends;
	FriendsGetProfilePics myGetter;
	Context c;
	
	public static class UserItemHolder {
		ImageView icon;
		TextView name;
	}
	
	public RegisteredFriendsListAdapter(Context context, Cursor c, ArrayList<Friend> list) {
		super(context, c);
		this.c = context;
		allFriends = list;
		myGetter = new FriendsGetProfilePics();
		myGetter.setListener(this);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		UserItemHolder holder = (UserItemHolder) view.getTag();
		String userid = cursor.getString(cursor.getColumnIndex(DBHelper.USERID));
		for (int i = 0; i<allFriends.size(); i++) {
			if (allFriends.get(i).getId().equalsIgnoreCase(userid)) {
				final String name = allFriends.get(i).getName();
				final String id = allFriends.get(i).getId();
				final String url = allFriends.get(i).getUrl();
				holder.icon.setImageBitmap(myGetter.getImage(id, url, context));
				holder.name.setText(name);
				view.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						((FriendsListActivity) c).sendBackFriend(name, id, url);
					}
				});
				break;
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = vi.inflate(R.layout.frienditem, null);
		UserItemHolder uih = new UserItemHolder();
		uih.icon = (ImageView) v.findViewById(R.id.prof_pic);
		uih.name = (TextView) v.findViewById(R.id.name);
		v.setTag(uih);
		return v;
	}
	
	

}
