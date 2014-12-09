package com.strumsoft.wordchainsfree.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.FriendsGetProfilePics;
import com.strumsoft.wordchainsfree.helper.Util;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;
import com.strumsoft.wordchainsfree.model.Game;
import com.strumsoft.wordchainsfree.ui.BotGameActivity;
import com.strumsoft.wordchainsfree.ui.GameListActivity;
import com.strumsoft.wordchainsfree.ui.PlayGameActivity;
import com.strumsoft.wordchainsfree.ui.ScoreScreenActivity;
import com.strumsoft.wordchainsfree.ui.TimeAttackGameActivity;

public class GameListAdapter extends CursorAdapter {
	
	private String currUserId;

	public static class GameViewHolder {
		ImageView icon;
		TextView gameName;
		TextView currUserTurn;
	}

	public GameListAdapter(Context context, Cursor c, String currUserId) {
		super(context, c);
		this.currUserId = currUserId;
		if (Util.model == null) {
			Util.model = new FriendsGetProfilePics();
		}
		Util.model.setListener(this);
	}
	
	@Override
	public void bindView(View v, final Context context, Cursor cursor) {
		GameViewHolder gvh = (GameViewHolder) v.getTag();
		final Game g = WordGameProvider.gameCurToGame(cursor);
		gvh.icon.setImageResource(R.drawable.blankimage);
//		Log.d("Game", "id="+g.getStrId()+", currPlayer="+g.getCurrPlayer()+", creator="+g.getGameCreator()+", picUrl="+g.getPicUrl()+", type="+g.getType());
		if (g.getCurrPlayer() != null) {
			if (g.getCurrPlayer().equalsIgnoreCase("bot")) {
				gvh.gameName.setText("Game of "+g.getType()+" versus Bot -- (Word Chainer)");
				gvh.currUserTurn.setText("Your Turn!");
				v.setBackgroundColor(0xFFFFEC8B);
				v.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent i = new Intent(context, BotGameActivity.class);
						i.putExtra("gameid", g.getStrId());
						context.startActivity(i);
					}
				});
			}
			else {
				String opponentId = null;
				if (currUserId.equalsIgnoreCase(g.getGameCreator())) {
					gvh.gameName.setText("Game of "+g.getType()+" created by you -- ("+g.getMode()+")");
				}
				else {
					for (int i = 0; i<g.getUserIds().size(); i++) {
						if (!currUserId.equalsIgnoreCase(g.getUserIds().get(i))) {
							opponentId = g.getUserIds().get(i);
							gvh.gameName.setText("Game of "+g.getType()+" created by "+g.getUserStringList().get(i)+" -- ("+g.getMode()+")");
							
						}
					}
					v.setBackgroundColor(Color.WHITE);
				}
				if (currUserId.equalsIgnoreCase(g.getCurrPlayer())) {
					gvh.currUserTurn.setText("Your Turn!");
					v.setBackgroundColor(0xFFFFEC8B);
				}
				else {
					for (int i = 0; i<g.getUserIds().size(); i++) {
						if (!currUserId.equalsIgnoreCase(g.getUserIds().get(i))) {
							opponentId = g.getUserIds().get(i);
							gvh.currUserTurn.setText(g.getUserStringList().get(i)+"'s Turn.");
							
						}
					}
					v.setBackgroundColor(Color.WHITE);
				}
				if (opponentId == null) {
					for (int i = 0; i<g.getUserIds().size(); i++) {
						if (!currUserId.equalsIgnoreCase(g.getUserIds().get(i))) {
							opponentId = g.getUserIds().get(i);
						}
					}
				}
				if (g.getPicUrl() != null) {
					gvh.icon.setImageBitmap(Util.model.getImage(opponentId, g.getPicUrl(), context));
				}
				final String oppId = opponentId;
				v.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (g.getMode().equalsIgnoreCase("time attack")) {
							if (g.getCurrPlayer().equalsIgnoreCase(currUserId)) {
								//is my turn - take turn
								Intent i = new Intent(context, TimeAttackGameActivity.class);
								i.putExtra("gameid", g.getStrId());
								i.putExtra("picurl", g.getPicUrl());
								i.putExtra("oppid", oppId);
								((GameListActivity) context).startActivityForResult(i, 0);
							}
							else {
								Intent i = new Intent(context, ScoreScreenActivity.class);
								i.putExtra("gameid", g.getStrId());
								i.putExtra("picurl", g.getPicUrl());
								i.putExtra("oppid", oppId);
								((GameListActivity) context).startActivityForResult(i, 0);
							}
						}
						else {
							Intent i = new Intent(context, PlayGameActivity.class);
							i.putExtra("gameid", g.getStrId());
							i.putExtra("picurl", g.getPicUrl());
							i.putExtra("oppid", oppId);
							((GameListActivity) context).startActivityForResult(i, 0);
						}
					}
				});
			}
		}
		else {
			v.setBackgroundColor(Color.WHITE);
			String opponentId = null;
			if (currUserId.equalsIgnoreCase(g.getGameCreator())) {
				gvh.gameName.setText("Game of "+g.getType()+" created by you -- ("+g.getMode()+")");
			}
			else {
				for (int i = 0; i<g.getUserIds().size(); i++) {
					if (!currUserId.equalsIgnoreCase(g.getUserIds().get(i))) {
						opponentId = g.getUserIds().get(i);
						gvh.gameName.setText("Game of "+g.getType()+" created by "+g.getUserStringList().get(i)+" -- ("+g.getMode()+")");
					}
				}
			}
			if (opponentId == null) {
				for (int i = 0; i<g.getUserIds().size(); i++) {
					if (!currUserId.equalsIgnoreCase(g.getUserIds().get(i))) {
						opponentId = g.getUserIds().get(i);
					}
				}
			}
			final String oppId = opponentId;
			if (g.getPicUrl() != null) {
				gvh.icon.setImageBitmap(Util.model.getImage(oppId, g.getPicUrl(), context));
			}
			gvh.currUserTurn.setText("Game Over!");
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(context, ScoreScreenActivity.class);
					i.putExtra("gameid", g.getStrId());
					i.putExtra("picurl", g.getPicUrl());
					i.putExtra("oppid", oppId);
					((GameListActivity) context).startActivityForResult(i, 0);
				}
			});
		}
	}
	
	
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = vi.inflate(R.layout.gamelistitem, null);
		GameViewHolder gvh = new GameViewHolder();
		gvh.icon = (ImageView) v.findViewById(R.id.player_icon);
		gvh.gameName = (TextView) v.findViewById(R.id.gamename);
		gvh.currUserTurn = (TextView) v.findViewById(R.id.whosTurn);
		v.setTag(gvh);
		return v;
	}

}