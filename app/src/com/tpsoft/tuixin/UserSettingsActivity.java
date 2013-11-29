package com.tpsoft.tuixin;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class UserSettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.user_settings);

		EditTextPreference prefUploadServer = (EditTextPreference) findPreference("uploadServer");
		EditTextPreference prefUploadPort = (EditTextPreference) findPreference("uploadPort");
		EditTextPreference prefMsgReserve = (EditTextPreference) findPreference("msgReserve");
		CheckBoxPreference prefPopupMsg = (CheckBoxPreference) findPreference("popupMsg");
		CheckBoxPreference prefPlaySound = (CheckBoxPreference) findPreference("playSound");

		// 允许显示现有值
		prefUploadServer.setSummary(prefUploadServer.getText());
		prefUploadPort.setSummary(prefUploadPort.getText());
		prefMsgReserve.setSummary(prefMsgReserve.getText()+"天");
		updateSummary(prefPopupMsg, prefPopupMsg.isChecked());
		updateSummary(prefPlaySound, prefPlaySound.isChecked());

		// 允许监听值改变事件
		prefUploadServer.setOnPreferenceChangeListener(this);
		prefUploadPort.setOnPreferenceChangeListener(this);
		prefMsgReserve.setOnPreferenceChangeListener(this);
		prefPopupMsg.setOnPreferenceChangeListener(this);
		prefPlaySound.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		updateSummary(preference, newValue);
		return true;
	}

	private void updateSummary(Preference preference, Object newValue) {
		if (preference.getKey().equals("popupMsg")
				|| preference.getKey().equals("playSound")) {
			preference.setSummary((Boolean) newValue ? "允许" : "禁止");
		} else if (preference.getKey().equals("popupMsg")) {
			preference.setSummary(newValue + "天");
		} else {
			preference.setSummary(newValue.toString());
		}
	}

	@Override
	public void finish() {
		MyApplicationClass myApp = (MyApplicationClass) getApplication();
		myApp.loadUserSettings();
		super.finish();
	}

}
