package com.strumsoft.wordchainsfree.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.strumsoft.wordchainsfree.R;
import com.strumsoft.wordchainsfree.helper.WordGameProvider;

public class PlayedWordlistAdapter extends BaseAdapter {

	private Context c;
	private ArrayList<String> words;
	private ArrayList<String> ids;
	private String currPlayerName;
	private String currPlayerId;
	
	public PlayedWordlistAdapter(Context c, ArrayList<String> words, ArrayList<String> ids, String currPlayerId, String currPlayerName) {
		this.c = c;
		this.currPlayerName = currPlayerName;
		this.words = words;
		this.ids = ids;
		this.currPlayerId = currPlayerId;
	}
	
	@Override
	public int getCount() {
		return words.size();
	}

	public void addItem(String word, String id) {
		words.add(word);
		ids.add(id);
	}
	
	@Override
	public Object getItem(int index) {
		return words.get(index);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String word = words.get(words.size()-(1+position));
		final String id = ids.get(words.size()-(1+position));
		View hView = convertView;
		if (convertView == null) {
			LayoutInflater mInflater = LayoutInflater.from(c);
			hView = mInflater.inflate(R.layout.worditem, null);
			ViewHolder holder = new ViewHolder();
			holder.player = (TextView) hView.findViewById(R.id.name);
			holder.word = (TextView) hView.findViewById(R.id.word);
			hView.setTag(holder);
		}
		
		final ViewHolder holder = (ViewHolder) hView.getTag();
		if (!currPlayerId.equalsIgnoreCase(id)) {
			holder.word.setBackgroundResource(R.drawable.opponentword);
			holder.player.setBackgroundResource(R.drawable.opponentname);
		}
		else {
			holder.word.setBackgroundResource(R.drawable.selfword);
			holder.player.setBackgroundResource(R.drawable.selfname);
		}
		holder.word.setText(word);
//		Log.d("ID", id);
		if (!id.equalsIgnoreCase("bot")) {
			if (currPlayerName != null) {
				holder.player.setText(currPlayerName.split(" ")[0]);
			}
			else {
				new AsyncTask<Void, Void, String>() {
					
					@Override
					protected String doInBackground(Void... params) {
						return WordGameProvider.getUserName(c, id);
					}
					
					@Override
					protected void onPostExecute(String result) {
						holder.player.setText(result.split(" ")[0]);
						super.onPostExecute(result);
					}
					
				}.execute((Void) null);
			}
		}
		else {
			holder.player.setText("Bot");
		}
		
		return hView;
	}
	
	class ViewHolder {
		TextView word;
		TextView player;
	}

}
