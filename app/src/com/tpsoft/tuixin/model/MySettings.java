package com.tpsoft.tuixin.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * �ҵ�����
 * 
 * @author joebin.don@gmail.com
 * @since 2013-5-28
 */
public class MySettings {

	public static final String DEFAULT_SERVER_HOST = "isajia.com"; // Ĭ�Ϸ����ַ
	public static final int DEFAULT_SERVER_PORT = 1234; // Ĭ�Ϸ���˿�
	public static final String DEFAULT_UPLOAD_SERVER = DEFAULT_SERVER_HOST; // Ĭ���ϴ�������
	public static final int DEFAULT_UPLOAD_PORT = 2345; // Ĭ���ϴ��˿�
	private static final String DEFAULT_CLIENT_ID = "00000000000"; // Ĭ���˺�����
	private static final String DEFAULT_CLIENT_PASSWORD = ""; // Ĭ�ϵ�¼����
	private static final boolean DEFAULT_AUTO_LOGIN = false; // Ĭ���Զ���¼

	private static final boolean DEFAULT_POPUP_MSG = true; // Ĭ�ϵ�����Ϣ״̬
	private static final boolean DEFAULT_PLAY_SOUND = true; // Ĭ����������״̬��������������Ϣʱ��Ч��

	private String serverHost; // ��������ַ
	private int serverPort; // �������˿�
	private String uploadServer; // �ϴ�������
	private int uploadPort; // �ϴ��˿�
	private String clientId; // �ͻ�ID
	private String clientPassword; // �ͻ�����
	private boolean autoLogin; // �Զ���¼
	private boolean popupMsg; // ������Ϣ
	private boolean playSound; // ��������

	public MySettings(Context context) {
		readFromPreferences(context);
	}

	public String getServerHost() {
		return serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public String getUploadServer() {
		return uploadServer;
	}

	public int getUploadPort() {
		return uploadPort;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientPassword() {
		return clientPassword;
	}

	public void setClientPassword(String clientPassword) {
		this.clientPassword = clientPassword;
	}

	public boolean isAutoLogin() {
		return autoLogin;
	}

	public void setAutoLogin(boolean autoLogin) {
		this.autoLogin = autoLogin;
	}

	public boolean isPopupMsg() {
		return popupMsg;
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
		uploadServer = prefs.getString("uploadServer", DEFAULT_UPLOAD_SERVER);
		uploadPort = Integer.parseInt(prefs.getString("uploadPort",
				Integer.toString(DEFAULT_UPLOAD_PORT)));
		clientId = prefs.getString("clientId", DEFAULT_CLIENT_ID);
		if (clientId.equals(DEFAULT_CLIENT_ID)) {
			clientId = readClientId(context);
		}
		clientPassword = prefs.getString("clientPassword", DEFAULT_CLIENT_PASSWORD);
		autoLogin = prefs.getBoolean("autoLogin", DEFAULT_AUTO_LOGIN);

		popupMsg = prefs.getBoolean("popupMsg", DEFAULT_POPUP_MSG);
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
			if (phoneNumber.startsWith("+86")) {
				// ȥ���й���½����ǰ׺
				phoneNumber = phoneNumber.substring(3);
			}
			return phoneNumber;
		}
	}
}
