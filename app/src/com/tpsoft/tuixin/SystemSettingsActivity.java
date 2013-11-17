package com.tpsoft.tuixin;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class SystemSettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.system_settings);

		EditTextPreference prefServerHost = (EditTextPreference) findPreference("serverHost");
		EditTextPreference prefServerPort = (EditTextPreference) findPreference("serverPort");

		// 允许显示现有值
		prefServerHost.setSummary(prefServerHost.getText());
		prefServerPort.setSummary(prefServerPort.getText());

		// 允许监听值改变事件
		prefServerHost.setOnPreferenceChangeListener(this);
		prefServerPort.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		updateSummary(preference, newValue);
		return true;
	}

	private void updateSummary(Preference preference, Object newValue) {
		preference.setSummary(newValue.toString());
	}

	@Override
	public void finish() {
		MyApplicationClass myApp = (MyApplicationClass) getApplication();
		myApp.loadUserSettings();
		super.finish();
	}

}
