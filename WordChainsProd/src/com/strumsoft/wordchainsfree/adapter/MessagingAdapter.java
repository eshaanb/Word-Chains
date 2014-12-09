package com.strumsoft.wordchainsfree.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.FriendsGetProfilePics;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.model.FBMessage;

public class MessagingAdapter extends CursorAdapter {

	public MessagingAdapter(Context context, Cursor c) {
		super(context, c);
		if (Util.model == null) {
			Util.model = new FriendsGetProfilePics();
		}
		Util.model.setListener(this);
	}
	
	public static class MessageItemHolder {
		ImageView icon;
		TextView message;
	}

	@Override
	public void bindView(View v, final Context context, Cursor cursor) {
		MessageItemHolder mih = (MessageItemHolder) v.getTag();
		final FBMessage msg = WordGameProvider.messageCurToMessage(cursor);
		mih.icon.setImageBitmap(Util.model.getImage(msg.getUserid(), msg.getPicurl(), context));
		mih.message.setText(msg.getBody());
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = vi.inflate(R.layout.frienditem, null);
		MessageItemHolder mih = new MessageItemHolder();
		mih.icon = (ImageView) v.findViewById(R.id.prof_pic);
		mih.message = (TextView) v.findViewById(R.id.name);
		v.setTag(mih);
		return v;
	}

}
