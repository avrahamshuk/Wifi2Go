package com.shuron.avraham.wifi2go;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.shuron.avraham.wifi2go.LoginAsyncTask.LoginResult;

public abstract class LoginAsyncTask extends AsyncTask<Void, String, LoginResult> {

	public enum LoginResult {
		NoNeed, Failure, Success;
	}

	private static final String TAG = "LoginAsyncTask";
	private static final String ENCODING = "UTF-8";

	/*
	 * Used to retrieve the Host address of the Captive Portal
	 */
	protected static final String REDIRECT_LOCATION_HEADER = "Location";
	protected static final String HEADER_CONTENT_TYPE = "application/x-www-form-urlencoded";
	protected static final String HEADER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	protected static final String HEADER_ACCEPT_ENCODING = "gzip,deflate,sdch";
	protected static final String HEADER_ACCEPT_LANGUAGE = "he-IL,he;q=0.8,en-US;q=0.6,en;q=0.4";
	protected static final String HEADER_CONNECTION = "keep-alive";
	protected static final String HEADER_CACHE_CONTROL = "max-age=0";

	/*
	 * Special Google address that always return 204 Code. If anything else
	 * returned - we are in a Captive Portal
	 */
	private final static String CHECK_CAPTIVE_PORTAL_URL = "http://clients3.google.com/generate_204";

	private static final int CAPTIVE_PORTAL_SOCKET_TIMEOUT_MS = 5000;

	public static int NOTIFICATION_ID = 1234;
	private Context mContext;
	protected String mCaptivePortalURL;

	public LoginAsyncTask(Context context) {
		super();
		mContext = context;
	}

	/*
	 * Checks if connected to one of the supported WiFi networks and if login is
	 * required. If so, perform login.
	 */
	@Override
	protected LoginResult doInBackground(Void... params) {
		LoginResult result = LoginResult.NoNeed;
		
		// Check if login needed
		if (isActiveNetworkCaptivePortal()) {
			
			// Log in
			result = performLogin();
			
			if (result == LoginResult.Success) {
				result = isActiveNetworkCaptivePortal() ? LoginResult.Failure
						: LoginResult.Success;
			}
		}
		return result;
	}

	protected abstract LoginResult performLogin();

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

	private Boolean isActiveNetworkCaptivePortal() {

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

			mCaptivePortalURL = conn.getHeaderField(REDIRECT_LOCATION_HEADER);

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

	protected int performRequest(URL url,
			LinkedHashMap<String, String> headers,
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
