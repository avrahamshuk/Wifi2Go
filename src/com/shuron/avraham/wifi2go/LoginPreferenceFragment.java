package com.shuron.avraham.wifi2go;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class LoginPreferenceFragment extends PreferenceFragment {

	SwitchPreference mServiceEnabledPreference;
	SwitchPreference mEggedEnabledPreference;
	SwitchPreference mIREnabledPreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.login_preferences);

		wireUp();
	}

	private void wireUp() {
		mServiceEnabledPreference = (SwitchPreference) findPreference(getText(
				R.string.pref_key_enable_auto_login));

		mServiceEnabledPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					/*
					 * Enable/Disable the WifiConnectedReceiver according to the
					 * new value of the preference 
					 */
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						setServiceEnabled((Boolean)newValue);
						return true;
					}
				});

		mEggedEnabledPreference = (SwitchPreference) findPreference(getText(
				R.string.pref_key_egged_auto_login));

		mIREnabledPreference = (SwitchPreference) findPreference(getText(
				R.string.pref_key_israel_railways_auto_login));
	}
	
	private void setServiceEnabled(boolean enabled) {
		PackageManager mngr = getActivity().getPackageManager();
		ComponentName name = new ComponentName(getActivity(),
				WifiConnectedReceiver.class);
		int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
				: PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		mngr.setComponentEnabledSetting(name, newState,
				PackageManager.DONT_KILL_APP);
	}
}
