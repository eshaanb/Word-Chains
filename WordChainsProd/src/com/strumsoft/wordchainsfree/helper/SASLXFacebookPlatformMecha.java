package com.strumsoft.wordchainsfree.helper;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.sasl.Sasl;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

public class SASLXFacebookPlatformMecha extends SASLMechanism {

	private static final String NAME = "X-FACEBOOK-PLATFORM";

	private String apiKey = "";
	private String access_token = "";

	/**
	 * Constructor.
	 */
	public SASLXFacebookPlatformMecha(SASLAuthentication saslAuthentication) {
		super(saslAuthentication);
	}

	@Override
	protected void authenticate() throws IOException, XMPPException {
		getSASLAuthentication().send(new AuthMechanism(NAME, ""));
	}

	@Override
	public void authenticate(String apiKey, String host, String acces_token)
			throws IOException, XMPPException {
		if (apiKey == null || acces_token == null) {
			throw new IllegalArgumentException("Invalid parameters");
		}

		this.access_token = acces_token;
		this.apiKey = apiKey;
		this.hostname = host;

		String[] mechanisms = { NAME };
		Map<String, String> props = new HashMap<String, String>();
		this.sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props,
				this);
		authenticate();
	}

	@Override
	public void authenticate(String username, String host, CallbackHandler cbh)
			throws IOException, XMPPException {
		String[] mechanisms = { NAME };
		Map<String, String> props = new HashMap<String, String>();
		this.sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props,
				cbh);
		authenticate();
	}

	@Override
	protected String getName() {
		return NAME;
	}

	@Override
	public void challengeReceived(String challenge) throws IOException {
		byte[] response = null;

		if (challenge != null) {
			String decodedChallenge = new String(Base64.decode(challenge));
			Map<String, String> parameters = getQueryMap(decodedChallenge);

			String version = "1.0";
			String nonce = parameters.get("nonce");
			String method = parameters.get("method");

			long callId = new GregorianCalendar().getTimeInMillis();

			String composedResponse = "api_key="
					+ URLEncoder.encode(apiKey, "utf-8") + "&call_id=" + callId
					+ "&method=" + URLEncoder.encode(method, "utf-8")
					+ "&nonce=" + URLEncoder.encode(nonce, "utf-8")
					+ "&access_token="
					+ URLEncoder.encode(access_token, "utf-8") + "&v="
					+ URLEncoder.encode(version, "utf-8");

			response = composedResponse.getBytes("utf-8");
		}

		String authenticationText = "";

		if (response != null) {
			authenticationText = Base64.encodeBytes(response,
					Base64.DONT_BREAK_LINES);
		}

		// Send the authentication to the server
		getSASLAuthentication().send(new Response(authenticationText));
	}

	private Map<String, String> getQueryMap(String query) {
		Map<String, String> map = new HashMap<String, String>();
		String[] params = query.split("\\&");

		for (String param : params) {
			String[] fields = param.split("=", 2);
			map.put(fields[0], (fields.length > 1 ? fields[1] : null));
		}

		return map;
	}
}