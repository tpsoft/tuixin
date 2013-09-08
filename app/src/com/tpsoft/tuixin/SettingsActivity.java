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
		EditTextPreference prefClientId = (EditTextPreference) findPreference("clientId");
		CheckBoxPreference prefPlaySound = (CheckBoxPreference) findPreference("playSound");

		// ���ɿͻ��˱�ʶ(�绰����)
		if (prefClientId.getText().equals("")) {
			String clientId = UserSettings.readClientId(this);
			prefClientId.setText(clientId);
			Settings.System.putString(getContentResolver(), "clientId",
					clientId);
		}

		// ������ʾ����ֵ
		prefServerHost.setSummary(prefServerHost.getText());
		prefServerPort.setSummary(prefServerPort.getText());
		prefClientId.setSummary(prefClientId.getText());
		updateSummary(prefPlaySound, prefPlaySound.isChecked());

		// �������ֵ�ı��¼�
		prefServerHost.setOnPreferenceChangeListener(this);
		prefServerPort.setOnPreferenceChangeListener(this);
		prefClientId.setOnPreferenceChangeListener(this);
		prefPlaySound.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		updateSummary(preference, newValue);
		return true;
	}

	private void updateSummary(Preference preference, Object newValue) {
		if (preference.getKey().equals("playSound")) {
			preference.setSummary((Boolean) newValue ? "����" : "��ֹ");
		} else {
			preference.setSummary(newValue.toString());
		}
	}

}
