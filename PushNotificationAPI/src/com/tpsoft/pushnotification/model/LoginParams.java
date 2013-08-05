package com.tpsoft.pushnotification.model;

import android.os.Bundle;

/**
 * ��¼����
 * 
 * @author joebin.don@gmail.com
 * @since 2013-5-28
 */
public class LoginParams {

	private String serverHost; // ��������ַ
	private int serverPort; // �������˿�
	private String clientId; // �ͻ�ID
	private String clientPassword; // �ͻ�����

	public LoginParams() {
	}

	public LoginParams(String serverHost, int serverPort, String clientId,
			String clientPassword) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.clientId = clientId;
		this.clientPassword = clientPassword;
	}

	public LoginParams(Bundle bundle) {
		serverHost = bundle.getString("serverHost");
		serverPort = bundle.getInt("serverPort");
		clientId = bundle.getString("clientId");
		clientPassword = bundle.getString("clientPassword");
	}

	public Bundle getBundle() {
		Bundle bundle = new Bundle();
		bundle.putString("serverHost", serverHost);
		bundle.putInt("serverPort", serverPort);
		bundle.putString("clientId", clientId);
		bundle.putString("clientPassword", clientPassword);
		return bundle;
	}

	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
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

}
