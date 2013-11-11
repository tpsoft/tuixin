package com.tpsoft.tuixin;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.provider.Settings;

import com.tpsoft.tuixin.model.UserSettings;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		EditTextPreference prefServerHost = (EditTextPreference) findPreference("serverHost");
		EditTextPreference prefServerPort = (EditTextPreference) findPreference("serverPort");
		EditTextPreference prefUploadPort = (EditTextPreference) findPreference("uploadPort");
		EditTextPreference prefClientId = (EditTextPreference) findPreference("clientId");
		CheckBoxPreference prefPopupMsg = (CheckBoxPreference) findPreference("popupMsg");
		CheckBoxPreference prefPlaySound = (CheckBoxPreference) findPreference("playSound");

		// 生成客户端标识(电话号码)
		if (prefClientId.getText().equals("")) {
			String clientId = UserSettings.readClientId(this);
			prefClientId.setText(clientId);
			Settings.System.putString(getContentResolver(), "clientId",
					clientId);
		}

		// 允许显示现有值
		prefServerHost.setSummary(prefServerHost.getText());
		prefServerPort.setSummary(prefServerPort.getText());
		prefUploadPort.setSummary(prefUploadPort.getText());
		prefClientId.setSummary(prefClientId.getText());
		updateSummary(prefPopupMsg, prefPopupMsg.isChecked());
		updateSummary(prefPlaySound, prefPlaySound.isChecked());

		// 允许监听值改变事件
		prefServerHost.setOnPreferenceChangeListener(this);
		prefServerPort.setOnPreferenceChangeListener(this);
		prefUploadPort.setOnPreferenceChangeListener(this);
		prefClientId.setOnPreferenceChangeListener(this);
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
