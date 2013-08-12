package com.shuron.avraham.wifi2go;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.shuron.avraham.wifi2go.LoginAsyncTask.LoginResult;

public class LoginAsyncTask extends AsyncTask<Context, String, LoginResult> {

	public enum LoginResult {
		NoContext, NoNeed, Failure, Success;
	}

	private static final String TAG = "LoginAsyncTask";
	private static final String ENCODING = "UTF-8";

	/*
	 * Used to retrieve the Host address of the Captive Portal
	 */
	private static final String REDIRECT_LOCATION_HEADER = "Location";
	private static final String F_QV_PARAMETER_IN_URL = "Qv=";
	private static final String HEADER_CONTENT_TYPE = "application/x-www-form-urlencoded";
	private static final String HEADER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	private static final String HEADER_ACCEPT_ENCODING = "gzip,deflate,sdch";
	private static final String HEADER_ACCEPT_LANGUAGE = "he-IL,he;q=0.8,en-US;q=0.6,en;q=0.4";
	private static final String HEADER_CONNECTION = "keep-alive";
	private static final String HEADER_CACHE_CONTROL = "max-age=0";

	/*
	 * Special Google address that always return 204 Code. If anything else
	 * returned - we are in a Captive Portal
	 */
	private final static String CHECK_CAPTIVE_PORTAL_URL = "http://clients3.google.com/generate_204";

	private static final int CAPTIVE_PORTAL_SOCKET_TIMEOUT_MS = 5000;

	// private static final String ISRAEL_RAILWAYS_SSID = "\"Shukron Family\"";
	// private static final String EGGED_SSID = ISRAEL_RAILWAYS_SSID;

	private static final String ISRAEL_RAILWAYS_SSID = "\"ISRAEL-RAILWAYS\"";
	private static final String EGGED_SSID = "\"Egged.co.il\"";

	/*
	 * In Israel Railways, each train has it's own HTTP server with different IP
	 * address. Because of this, we need to find out the host address before
	 * performing any login.
	 */
	private String hs_server;
	private String host;
	private String origin;
	
	/*
	 * Another parameter that is sent by the IR server before login. It changes
	 * every time so we need to figure it out when checking for Captive Portal.
	 */
	private String f_Qv;
	private String mRedirectionURL;
	
	private static int NOTIFICATION_ID = 1234;
	private Context mContext;

	/*
	 * Checks if connected to one of the supported WiFi networks and if login is
	 * required. If so, perform login.
	 */
	@Override
	protected LoginResult doInBackground(Context... params) {

		if (params == null || params.length == 0)
			return LoginResult.NoContext;

		if (isLogInRequired()) {

			Log.d(TAG, "Inside captive portal!");
			mContext = params[0];

			try {
				return performLogin();
			} catch (Exception e) {
				return LoginResult.Failure;
			}

		} else {
			Log.d(TAG, "Not in captive portal!");
			return LoginResult.NoNeed;
		}
	}

	private LoginResult performLogin() throws Exception {
		WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		String ssid = wifiManager.getConnectionInfo().getSSID();
		if (ssid.equals(ISRAEL_RAILWAYS_SSID)) {
			loginToIsraelRaylways();
		} else if (ssid.equals(EGGED_SSID)) {
			loginToEgged();
		}

		// Verify. Success means log in not required
		return isLogInRequired() ? LoginResult.Failure : LoginResult.Success;
	}

	@Override
	protected void onProgressUpdate(String... messages) {
		if (messages.length > 0) {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(
					mContext).setContentTitle(messages[0])
					.setProgress(0, 0, true).setSmallIcon(R.drawable.ic_wifi);

			Notification notification = builder.build();
			NotificationManager mngr = (NotificationManager) mContext
					.getSystemService(Context.NOTIFICATION_SERVICE);
			mngr.notify(NOTIFICATION_ID, notification);

			Toast.makeText(mContext, messages[0], Toast.LENGTH_LONG).show();

			Log.d(TAG, messages[0]);
		}
	}

	@Override
	protected void onPostExecute(LoginResult result) {
		switch (result) {
		case Failure:
		case NoContext:
			Log.d(TAG, "Failed to login");
			notifyLoginFailed();
			break;
		case NoNeed:
			Log.d(TAG, "Not in captive portal");
			// Do nothing
			break;
		case Success:
			Log.d(TAG, "Logged in successfully");
			notifyLoginSucceeded();
			break;
		default:
			break;
		}
	}

	private void notifyLoginSucceeded() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				mContext)
				.setContentTitle(mContext.getText(R.string.login_succeeded))
				.setContentText(
						mContext.getText(R.string.login_succeeded_message))
				.setProgress(0, 0, false).setSmallIcon(R.drawable.ic_wifi);

		Notification notification = builder.build();
		NotificationManager mngr = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mngr.notify(NOTIFICATION_ID, notification);
	}

	private Boolean isLogInRequired() {

		Log.d(TAG, "Checking if network is Captive Portal...");
		HttpURLConnection conn = null;
		try {
			URL url = new URL(CHECK_CAPTIVE_PORTAL_URL);
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(CAPTIVE_PORTAL_SOCKET_TIMEOUT_MS);
			conn.setReadTimeout(CAPTIVE_PORTAL_SOCKET_TIMEOUT_MS);
			conn.setUseCaches(false);
			conn.getInputStream();

			mRedirectionURL = conn.getHeaderField(REDIRECT_LOCATION_HEADER);

			// We got a valid response, but not from the real google.
			return conn.getResponseCode() != 204;
		} catch (IOException e) {
			Log.d(TAG,
					"Captive Portal Check - probably not a portal: exception "
							+ e);
			return false;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private void parseRedirectionParameters() throws MalformedURLException {
		URL url;
		if (mRedirectionURL != null) {
			url = new URL(mRedirectionURL);

			Log.d(TAG, "Redirection URL: " + mRedirectionURL);
			// Extract the host from the URL.
			hs_server = url.getHost();

			if (url.getPort() != -1) {
				host = hs_server + ":" + String.valueOf(url.getPort());
			}
			origin = "http://" + host;

			Log.d(TAG, "Origin: " + origin);
			// Extract the f_Qv parameter from the URL.
			int location = mRedirectionURL.indexOf(F_QV_PARAMETER_IN_URL);
			if (location != -1) {
				f_Qv = mRedirectionURL.substring(location
						+ F_QV_PARAMETER_IN_URL.length());
				Log.d(TAG, "f_Qv: " + f_Qv);
			}
		}
	}

	private LoginResult loginToEgged() throws Exception {
		URL url;
		url = new URL("http://egged.co.il/login");
		
		LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
		headers.put("Accept", HEADER_ACCEPT);
		headers.put("Accept-Encoding", HEADER_ACCEPT_ENCODING);
		headers.put("Accept-Language", HEADER_ACCEPT_LANGUAGE);
		headers.put("Cache-Control", HEADER_CACHE_CONTROL);
		headers.put("Connection", HEADER_CONNECTION);
		headers.put("Content-Type", HEADER_CONTENT_TYPE);
		
		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
		params.put("username", "ronen");
		params.put("dst", "http://google.co.il");

		return performRequest(url, headers, params) == 200 ? LoginResult.Success
				: LoginResult.Failure;
	}

	private LoginResult loginToIsraelRaylways() throws Exception {
		parseRedirectionParameters();
		String urlString = "http://" + host + "/cgi-bin/hslogin.cgi";
		URL url = new URL(urlString);
		
		LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
		headers.put("Accept", HEADER_ACCEPT);
		headers.put("Accept-Encoding", HEADER_ACCEPT_ENCODING);
		headers.put("Accept-Language", HEADER_ACCEPT_LANGUAGE);
		headers.put("Cache-Control", HEADER_CACHE_CONTROL);
		headers.put("Connection", HEADER_CONNECTION);
		headers.put("Content-Type", HEADER_CONTENT_TYPE);
		headers.put("Host", host);
		headers.put("Origin", origin);
		headers.put("Referer", mRedirectionURL);

		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
		params.put("f_flex", "");
		params.put("f_flex_type", "log");
		params.put("f_hs_server", hs_server);
		params.put("f_Qv", f_Qv);
		params.put("submit", "התחבר");

		return performRequest(url, headers, params) == 200 ? LoginResult.Success
				: LoginResult.Failure;
	}

	private void notifyLoginFailed() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				mContext)
				.setContentTitle(mContext.getText(R.string.login_failed))
				.setContentText(mContext.getText(R.string.login_failed_message))
				.setProgress(0, 0, false).setSmallIcon(R.drawable.ic_wifi);

		Notification notification = builder.build();
		NotificationManager mngr = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mngr.notify(NOTIFICATION_ID, notification);
	}

	private int performRequest(URL url, LinkedHashMap<String, String> headers,
			LinkedHashMap<String, String> params) throws Exception {

		HttpURLConnection conn = null;
		try {

			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setUseCaches(false);

			for (Map.Entry<String, String> entry : headers.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}

			if (params != null) {

				byte[] body = RequestUtils.encodeParameters(params, ENCODING);
				OutputStream os = conn.getOutputStream();
				Log.d(TAG, "Body: " + new String(body, ENCODING));
				os.write(body);
				os.close();
			}

			InputStream is = conn.getInputStream();
			String response = RequestUtils.inputStreamToString(is);
			Log.d(TAG, "Response: " + response);
			return conn.getResponseCode();

		} catch (IOException e) {
			if (conn != null) {
				conn.disconnect();
			}
			throw e;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
}
