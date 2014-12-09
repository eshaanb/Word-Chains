package com.strumsoft.wordchainsfree.helper;

import java.io.File;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;

import android.os.Build;

public class FacebookChatManager {

	private static FacebookChatManager chatManager;
	private XMPPConnection connection;
	private final String SERVER = "chat.facebook.com";
	private final int PORT = 5222;
	private final String FACEBOOK_MECHANISM = "X-FACEBOOK-PLATFORM";
//	private RosterListener rosterListner;

	private FacebookChatManager(RosterListener rosterListner) {
//		this.rosterListner = rosterListner;
		ConnectionConfiguration connFig = new ConnectionConfiguration(SERVER, PORT);
//				PORT);
		if (Build.VERSION.SDK_INT >= 14) {
			connFig.setTruststoreType("AndroidCAStore");
			connFig.setTruststorePassword(null);
			connFig.setTruststorePath(null);
		} 
		else {
			connFig.setTruststoreType("BKS");
		    String path = System.getProperty("javax.net.ssl.trustStore");
		    if (path == null)
		        path = System.getProperty("java.home") + File.separator + "etc"
		            + File.separator + "security" + File.separator
		            + "cacerts.bks";
		    connFig.setTruststorePath(path);
		}
//		connFig.setCompressionEnabled(false);
		connFig.setDebuggerEnabled(true);
		connFig.setSASLAuthenticationEnabled(true);
		connection = new XMPPConnection(connFig);
		// setup facebook authentication mechanism
		SASLAuthentication.registerSASLMechanism(FACEBOOK_MECHANISM,
				SASLXFacebookPlatformMecha.class);
		SASLAuthentication.supportSASLMechanism(FACEBOOK_MECHANISM, 0);
	}

	public static FacebookChatManager getInstance(RosterListener rosterListner) {
		if (chatManager == null) {
			chatManager = new FacebookChatManager(rosterListner);
		}
		return chatManager;
	}

	public XMPPConnection getConnection() {
		return connection;
	}
	
	public boolean connect() {
		try {
			connection.connect();
			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
			connection.disconnect();
		}
		return false;
	}

	public void disconnect() {
		connection.disconnect();
	}

	public boolean logIn(String apiKey, String accessToken) {
		try {
			connection.login(apiKey, accessToken, "Application");
			setPresenceState(Presence.Type.available, "");
//			connection.getRoster().addRosterListener(rosterListner);
			return true;
		} catch (XMPPException e) {
			connection.disconnect();
			e.printStackTrace();
		}
		return false;
	}

	public boolean isAuthenticated() {
		return connection.isAuthenticated();
	}
	
	public boolean isConnected() {
		return connection.isConnected();
	}
	
	public Roster getRoster() {
		return connection.getRoster();
	}

	public Chat createNewChat(String user, MessageListener messageListner) {
		return connection.getChatManager().createChat(user, messageListner);
	}

	public void registerNewIncomingChatListner(
			ChatManagerListener chatManagerListner) {
		connection.getChatManager().addChatListener(chatManagerListner);
	}

	public void setPresenceState(Type precenseType, String status) {
		Presence presence = new Presence(precenseType);
		presence.setStatus(status);
		connection.sendPacket(presence);
	}

	public Presence getUserPresence(String userId) {
		return connection.getRoster().getPresence(userId);
	}
}