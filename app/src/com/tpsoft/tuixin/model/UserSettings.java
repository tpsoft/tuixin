package com.tpsoft.tuixin.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * �û�����
 * 
 * @author joebin.don@gmail.com
 * @since 2013-5-28
 */
public class UserSettings {

	public static final String DEFAULT_SERVER_HOST = "118.244.9.191"; // Ĭ�Ϸ����ַ
	public static final int DEFAULT_SERVER_PORT = 3456; // Ĭ�Ϸ���˿�
	private static final String DEFAULT_CLIENT_ID = "00000000000"; // Ĭ�Ͽͻ�ID

	private static final boolean DEFAULT_PLAY_SOUND = true; // Ĭ����������״̬

	private String serverHost; // ��������ַ
	private int serverPort; // �������˿�
	private String clientId; // �ͻ�ID
	private String clientPassword; // �ͻ�����
	private boolean playSound; // ��������

	public UserSettings(Context context) {
		readFromPreferences(context);
	}

	public String getServerHost() {
		return serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public String getClientId() {
		return clientId;
	}

	public String getClientPassword() {
		return clientPassword;
	}

	public boolean isPlaySound() {
		return playSound;
	}

	private void readFromPreferences(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		serverHost = prefs.getString("serverHost", DEFAULT_SERVER_HOST);
		serverPort = Integer.parseInt(prefs.getString("serverPort",
				Integer.toString(DEFAULT_SERVER_PORT)));
		clientId = prefs.getString("clientId", DEFAULT_CLIENT_ID);
		if (clientId.equals(DEFAULT_CLIENT_ID)) {
			clientId = readClientId(context);
		}
		clientPassword = clientId;

		playSound = prefs.getBoolean("playSound", DEFAULT_PLAY_SOUND);
	}

	public static String readClientId(Context context) {
		TelephonyManager telMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber = telMgr.getLine1Number(); // �绰����
		if (phoneNumber == null || phoneNumber.equals("")) {
			String imsi = telMgr.getSubscriberId(); // �����ƶ��û�ʶ����(�ܳ������15λ)
			if (imsi == null || imsi.equals("")) {
				String imei = telMgr.getSimSerialNumber(); // �����ƶ��豸�����(15λ���Ӵ���)
				if (imei == null || imei.equals("")) {
					String deviceId = telMgr.getDeviceId(); // �豸���к�
					if (deviceId == null || deviceId.equals("")) {
						return DEFAULT_CLIENT_ID;
					} else {
						return "DEV_" + deviceId;
					}
				} else {
					return "IMEI_" + imei;
				}
			} else {
				return "IMSI_" + imsi;
			}
		} else {
			return phoneNumber;
		}
	}
}
