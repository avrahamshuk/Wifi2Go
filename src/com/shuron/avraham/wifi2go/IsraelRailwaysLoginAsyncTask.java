package com.shuron.avraham.wifi2go;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;

import android.content.Context;
import android.util.Log;

public class IsraelRailwaysLoginAsyncTask extends LoginAsyncTask {
	
	private static final String TAG = "IRLoginAsyncTask";
	private static final String F_QV_PARAMETER_IN_URL = "Qv=";
	
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

	public IsraelRailwaysLoginAsyncTask(Context context) {
		super(context);
	}
	
	@Override
	protected LoginResult performLogin() {
		try {
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
			headers.put("Referer", mCaptivePortalURL);

			LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
			params.put("f_flex", "");
			params.put("f_flex_type", "log");
			params.put("f_hs_server", hs_server);
			params.put("f_Qv", f_Qv);
			params.put("submit", "התחבר");

			return performRequest(url, headers, params) == 200 ? LoginResult.Success
					: LoginResult.Failure;
			
		} catch (Exception e) {
			Log.d(TAG, "Egged login failed", e);
			return LoginResult.Failure;
		}
	}
	
	private void parseRedirectionParameters() throws MalformedURLException {
		URL url;
		if (mCaptivePortalURL != null) {
			url = new URL(mCaptivePortalURL);

			Log.d(TAG, "Redirection URL: " + mCaptivePortalURL);
			// Extract the host from the URL.
			hs_server = url.getHost();

			if (url.getPort() != -1) {
				host = hs_server + ":" + String.valueOf(url.getPort());
			}
			origin = "http://" + host;

			Log.d(TAG, "Origin: " + origin);
			// Extract the f_Qv parameter from the URL.
			int location = mCaptivePortalURL.indexOf(F_QV_PARAMETER_IN_URL);
			if (location != -1) {
				f_Qv = mCaptivePortalURL.substring(location
						+ F_QV_PARAMETER_IN_URL.length());
				Log.d(TAG, "f_Qv: " + f_Qv);
			}
		}
	}

}
