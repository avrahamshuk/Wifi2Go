package com.shuron.avraham.wifi2go;

import java.net.URL;
import java.util.LinkedHashMap;

import android.content.Context;
import android.util.Log;

public class EggedLoginAsyncTask extends LoginAsyncTask {
	
	private static final String TAG = "EggedLoginAsyncTask";

	public EggedLoginAsyncTask(Context context) {
		super(context);
	}
	
	@Override
	protected LoginResult performLogin() {
		try {
			URL url = new URL("http://egged.co.il/login");

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
		} catch (Exception e) {
			Log.d(TAG, "Egged login failed", e);
			return LoginResult.Failure;
		}
		
	}
}
