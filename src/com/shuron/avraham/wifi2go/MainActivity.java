package com.shuron.avraham.wifi2go;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;

public class MainActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager
				.setDefaultValues(this, R.xml.login_preferences, false);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			// Load the preferences directly
			addPreferencesFromResource(R.xml.login_preferences);
		} else {
			// Display the fragment as the main content.
			getFragmentManager()
					.beginTransaction()
					.replace(android.R.id.content,
							new LoginPreferenceFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
