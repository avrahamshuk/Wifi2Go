package com.shuron.avraham.wifi2go;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class WifiConnectedReceiver extends BroadcastReceiver {

	@SuppressWarnings("unused")
	private static final String TAG = "WifiConnectedReceiver";

	private static final String ISRAEL_RAILWAYS_SSID = "\"ISRAEL-RAILWAYS\"";
	private static final String EGGED_SSID = "\"Egged.co.il\"";

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {

		mContext = context.getApplicationContext();
		NetworkInfo inf = (NetworkInfo) intent.getExtras().get(
				WifiManager.EXTRA_NETWORK_INFO);

		if (inf.isConnected()) {
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(mContext);
			Boolean eggedEnabled = sp.getBoolean(
					mContext.getString(R.string.pref_key_egged_auto_login), true);
			Boolean israelRailwaysEnabled = sp.getBoolean(mContext.getString(
					R.string.pref_key_israel_railways_auto_login), true);

			WifiManager mngr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
			
			String ssid = mngr.getConnectionInfo().getSSID();
			
			if (isEggedSSID(ssid) && eggedEnabled) {
				new EggedLoginAsyncTask(mContext).execute();
			} else if (isIsraelRailwaysSSID(ssid) && israelRailwaysEnabled) {
				new IsraelRailwaysLoginAsyncTask(mContext).execute();
			}
		}
		else {
			removeNotification();
		}
	}
	
	private void removeNotification() {
		NotificationManager mngr = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mngr.cancel(LoginAsyncTask.NOTIFICATION_ID);
	}
	
	private boolean isEggedSSID(String ssid) {
		return (ssid != null && ssid.equalsIgnoreCase(EGGED_SSID));
	}
	
	private boolean isIsraelRailwaysSSID(String ssid) {
		return (ssid != null && ssid.contains(ISRAEL_RAILWAYS_SSID));
	}
}
