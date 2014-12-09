package com.strumsoft.wordchainsfree.helper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public final class Util {
	
	public static final String GCM_URL = "/gcm";
	public static final String GCM_UNREGISTER = "/gcmunregister";
	private static ProgressDialog pd = null;
	public static final String SENDER_ID = "429661227281";
    public static final String EXTRA_MESSAGE = "message";
    public static FriendsGetProfilePics model;
	
	public static void showProgressDialog(Context c, String title, String message) {
		cancelDialog();
		pd = new ProgressDialog(c);
		pd.setTitle(title);
		pd.setMessage(message);
		pd.setCancelable(false);
		pd.show();
	}
	
	public static boolean isShowing() {
		if (pd != null && pd.isShowing()) {
			return true;
		}
		return false;
	}
	
	public static void cancelDialog() {
		if (pd != null && pd.isShowing()) {
			pd.cancel();
			pd = null;
		}
	}
    
    public static String getNextPokemon(String startingLetter, AssetManager assets, ArrayList<String> playedWords) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("pokemons.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
	    	Random r = new Random();
	    	int startLine = r.nextInt(648);
	    	int lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount >= startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				lineCount++;
			}
			reader = new InputStreamReader(assets.open("pokemons.txt"), "UTF-8");
			br = new BufferedReader(reader);
			lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount < startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				else {
					return null;
				}
				lineCount++;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    public static String getNextCountry(String startingLetter, AssetManager assets, ArrayList<String> playedWords) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("countrylist.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
	    	Random r = new Random();
	    	int startLine = r.nextInt(197);
	    	int lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount >= startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				lineCount++;
			}
			reader = new InputStreamReader(assets.open("countrylist.txt"), "UTF-8");
			br = new BufferedReader(reader);
			lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount < startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				else {
					return null;
				}
				lineCount++;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    public static boolean containsCaseInsensitive(ArrayList<String> searchList, String searchTerm) {
        for (String item : searchList) {
            if (item.equalsIgnoreCase(searchTerm)) 
                return true;
        }
        return false;
    }
    
    public static String getNextWord(String startingLetter, AssetManager assets, ArrayList<String> playedWords) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("randomed.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
	    	Random r = new Random();
	    	int startLine = r.nextInt(178691);
	    	int lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount >= startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				lineCount++;
			}
			reader = new InputStreamReader(assets.open("randomed.txt"), "UTF-8");
			br = new BufferedReader(reader);
			lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount < startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				else {
					return null;
				}
				lineCount++;
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    public static String getNextMagicCard(String startingLetter, AssetManager assets, ArrayList<String> playedWords) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("mtgcardslist.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
	    	Random r = new Random();
	    	int startLine = r.nextInt(12843);
	    	int lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount >= startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				lineCount++;
			}
			reader = new InputStreamReader(assets.open("mtgcardslist.txt"), "UTF-8");
			br = new BufferedReader(reader);
			lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (lineCount < startLine) {
					line = line.toLowerCase();
					if (line.startsWith(startingLetter.toLowerCase()) && !containsCaseInsensitive(playedWords, line)) {
						return line;
					}
				}
				else {
					return null;
				}
				lineCount++;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    public static boolean isValidPokemon(String poke, AssetManager assets) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("pokemons.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader);
	    	String line = null;
			while ((line = br.readLine()) != null) {
				if (poke.equalsIgnoreCase(line)) {
					return true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return false;
    }
    
    public static boolean isValidWord(String word, AssetManager assets) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("randomed.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
			while ((line = br.readLine()) != null) {
				if (word.equalsIgnoreCase(line)) {
					return true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return false;
    }
    
    public static boolean isValidArtist(String artist, AssetManager assets) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("artists.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
			while ((line = br.readLine()) != null) {
				if (artist.equalsIgnoreCase(line)) {
					return true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return false;
    }
    
    public static boolean isValidCountry(String country, AssetManager assets) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("countrylist.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
			while ((line = br.readLine()) != null) {
				if (country.equalsIgnoreCase(line)) {
					return true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return false;
    }
    
    public static boolean isValidMagicCard(String card, AssetManager assets) {
    	InputStreamReader reader;
		try {
			reader = new InputStreamReader(assets.open("mtgcardslist.txt"), "UTF-8");
			BufferedReader br = new BufferedReader(reader); 
	    	String line = null;
			while ((line = br.readLine()) != null) {
				if (card.equalsIgnoreCase(line)) {
					return true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return false;
    }
    
    public static Bitmap getBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(new FlushedInputStream(is));
            bis.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }
    
    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}
