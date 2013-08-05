package com.tpsoft.tuixin.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * 用户设置
 * 
 * @author joebin.don@gmail.com
 * @since 2013-5-28
 */
public class UserSettings {

	public static final String DEFAULT_SERVER_HOST = "118.244.9.191"; // 默认服务地址
	public static final int DEFAULT_SERVER_PORT = 3456; // 默认服务端口
	private static final String DEFAULT_CLIENT_ID = "00000000000"; // 默认客户ID

	private static final boolean DEFAULT_PLAY_SOUND = true; // 默认声音提醒状态

	private String serverHost; // 服务器地址
	private int serverPort; // 服务器端口
	private String clientId; // 客户ID
	private String clientPassword; // 客户密码
	private boolean playSound; // 声音提醒

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
		String phoneNumber = telMgr.getLine1Number(); // 电话号码
		if (phoneNumber == null || phoneNumber.equals("")) {
			String imsi = telMgr.getSubscriberId(); // 国际移动用户识别码(总长度最多15位)
			if (imsi == null || imsi.equals("")) {
				String imei = telMgr.getSimSerialNumber(); // 国际移动设备身份码(15位电子串号)
				if (imei == null || imei.equals("")) {
					String deviceId = telMgr.getDeviceId(); // 设备序列号
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
