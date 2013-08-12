package com.shuron.avraham.wifi2go;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class WifiConnectedReceiver extends BroadcastReceiver {

	@SuppressWarnings("unused")
	private static final String TAG = "WifiConnectedReceiver";

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {

		mContext = context.getApplicationContext();
		NetworkInfo inf = (NetworkInfo) intent.getExtras().get(
				WifiManager.EXTRA_NETWORK_INFO);

		if (inf.isConnected()) {

			Context[] params = {mContext};
			new LoginAsyncTask().execute(params);

		}
	}
}
