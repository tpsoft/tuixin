package com.tpsoft.pushnotification.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import com.tpsoft.pushnotification.R;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.utils.Crypt;

@SuppressLint("DefaultLocale")
public class NotifyPushService extends Service {

	private static final String INPUT_RETURN = "\r\n";

	private static final int ONGOING_NOTIFICATION = 10086;

	private MyBroadcastReceiver myBroadcastReceiver;
	private Thread mServiceThread = null;
	private boolean exitNow = false;

	private AppParams appParams;
	private LoginParams loginParams;
	private NetworkParams networkParams;

	private boolean receiverStarted = false;

	@Override
	public void onCreate() {
		super.onCreate();

		// ��ʼ���չ㲥
		myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.ServiceController");
		registerReceiver(myBroadcastReceiver, filter);
	}

	@Override
	public void onDestroy() {
		// ֹͣ���չ㲥
		unregisterReceiver(myBroadcastReceiver);

		// ȡ��ǰ̨����
		stopForeground(true);

		// ���������߳�
		if (receiverStarted) {
			exitNow = true;
			try {
				mServiceThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// �÷���ǰ̨����
		Notification notification = new Notification(intent.getIntExtra(
				"notification_logo", R.drawable.ic_launcher),
				intent.getStringExtra("ticker_text"),
				System.currentTimeMillis());
		Intent notificationIntent;
		try {
			notificationIntent = new Intent(this, Class.forName(intent
					.getStringExtra("ActivityClassName")));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this,
				intent.getStringExtra("notification_title"),
				intent.getStringExtra("notification_message"), pendingIntent);
		startForeground(ONGOING_NOTIFICATION, notification);

		return Service.START_STICKY;
	}

	private void showNotification(String msgText) {
		// �㲥����Ϣ֪ͨ
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "notify");
		activityIntent.putExtra("msgText", msgText);
		sendBroadcast(activityIntent);
	}

	private void showLog(String logText) {
		// �㲥��־
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "log");
		activityIntent.putExtra("logText", logText);
		sendBroadcast(activityIntent);
	}

	private void showLogining(boolean logining) {
		// �㲥��¼״̬
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "logining");
		activityIntent.putExtra("logining", logining);
		sendBroadcast(activityIntent);
	}

	private void showStatus(boolean started) {
		// �㲥������״̬
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "status");
		activityIntent.putExtra("started", started);
		sendBroadcast(activityIntent);
	}

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String command = intent.getStringExtra("command");
			if (command.equals("start")) {
				// ������Ϣ��������
				// �����ظ�����
				if (receiverStarted) {
					showStatus(true);
					return;
				}

				// ��ȡ��¼����
				appParams = new AppParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.AppParams"));
				loginParams = new LoginParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.LoginParams"));
				networkParams = new NetworkParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.NetworkParams"));

				// ���������߳�
				exitNow = false;
				mServiceThread = new SocketClientThread();
				mServiceThread.start();

				// ������������־
				receiverStarted = true;
				showStatus(true);
			} else if (command.equals("stop")) {
				// ֹͣ��Ϣ��������
				// ��ֹͣ��ֱ�ӷ���
				if (!receiverStarted) {
					showStatus(false);
					return;
				}

				// ֹͣ�����߳�
				exitNow = true;
				try {
					mServiceThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// ������ֹͣ��־
				receiverStarted = false;
			}
		}

	}

	private class SocketClientThread extends Thread {

		public static final int MAX_SOCKET_BUF = 8192;

		// ͷ���ֶ�������
		public static final String FIELD_BODY_BYTE_LENGTH = "ByteLength";
		public static final String FIELD_BODY_LENGTH = "Length";
		public static final String FIELD_ACTION_SUCCESS = "Success";
		public static final String FIELD_LOGIN_SECURE = "Secure";
		public static final String FIELD_LOGIN_PASSWORD = "Password";
		public static final String FIELD_MSG_RECEIPT = "Receipt";

		public static final String CLOSE_CONN_RES = "CLOSE CONN\r\nLength: %d\r\n\r\n%s"; // �岿:
																							// ��������(�Ѱ���)
		public static final String GET_APPID_RES = "SET APPID\r\nLength: %d\r\n\r\n%s,%s"; // 0-�岿����,
																							// 1-Ӧ��ID,
																							// 2-Ӧ������
		public static final String GET_USERNAME_RES = "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\nLength: %d\r\n\r\n%s"; // 0-�岿�Ƿ����,
																															// 1-�岿�Ƿ��������,
																															// 2-�岿����,
																															// 3-�û���(������)
		public static final String SET_MSGKEY_ACK = "SET MSGKEY\r\n\r\n"; // ����Ҫ�岿
		public static final String SET_ALIVEINT_ACK = "SET ALIVEINT\r\n\r\n"; // ����Ҫ�岿
		public static final String PUSH_MSG_ACK = "PUSH MSG\r\n\r\n"; // ����Ҫ�岿
		public static final String SET_ALIVE_REQ = "SET ALIVE\r\n\r\n"; // ����Ҫ�岿

		// ������Ϣ
		public static final String INVALID_ACTION_LINE = "Invalid aciton line";
		public static final String INVALID_FIELD_LINE = "Invalid field line";
		public static final String INVALID_LENGTH_VALUE_MSG = "Invalid length value";

		private boolean waitForHead = true; // �ȴ�ͷ��(false��ʾ�ȴ��岿����Ҫ�ٵȴ�)
		private String headInput = ""; // ͷ������
		private boolean actionLineFound = false; // �Է����ҵ�������

		private String action = ""; // ����
		private String target = ""; // Ŀ��
		private Map<String, String> fields = new HashMap<String, String>(); // �ֶα�
		private boolean bodyByteLength = false; // �岿���ȵ�λ�Ƿ�Ϊ�ֽ�(Ĭ��Ϊ��--���ַ�Ϊ��λ)
		private int bodyLength = 0; // �岿����(Ĭ��Ϊ����Ҫ�岿)
		private String body = ""; // �岿����
		private byte[] unhandledInput = new byte[0]; // ��δ���������

		private String msgKey;
		private int keepAliveInterval;

		private boolean clientLogon = false;
		private boolean invalidAppInfo = false;
		private boolean invalidClientInfo = false;

		private Calendar connectedTime;
		private Calendar lastActiveTime;
		private Calendar serverActiveTime;

		private SocketClientThread() {

		}

		@Override
		public void run() {
			Socket socket = null;
			byte buffer[] = new byte[MAX_SOCKET_BUF];

			boolean networkOk = true;
			socket = null;
			InputStream in = null;
			OutputStream out = null;
			reconnect: while (!exitNow) {
				headInput = "";
				resetPacketStaus();
				clientLogon = false;
				// �ر����е��׽���
				try {
					if (socket != null) {
						socket.close();
					}
				} catch (IOException ee) {
					ee.printStackTrace();
				}
				socket = null;
				if (!isNetworkAvailable()) {
					if (networkOk) {
						showLog("���粻����");
						networkOk = false;
					}
					showLogining(false);
					waitForReconnect();
					continue reconnect;
				}
				networkOk = true;
				showLog("���ӷ����� " + loginParams.getServerHost() + "["
						+ loginParams.getServerPort() + "]...");
				while (!exitNow) {
					// �����µ��׽���
					socket = new Socket();
					// ��������
					showLogining(true);
					try {
						socket.connect(
								new InetSocketAddress(loginParams
										.getServerHost(), loginParams
										.getServerPort()), networkParams
										.getConnectTimeout());
						socket.setSoTimeout(networkParams.getReadTimeout()); // ���ö���ʱ(ms)
						// socket.setKeepAlive(true);
						//
						in = socket.getInputStream();
						out = socket.getOutputStream();
						break;
					} catch (IOException e) {
						Log.w("Network",
								String.format("����ʧ��: %s", e.getMessage()));
						if (socket.isConnected()) {
							try {
								socket.close();
							} catch (IOException ee) {
								ee.printStackTrace();
							}
							socket = null;
						}
						showLogining(false);
						waitForReconnect();
					}
				}
				if (exitNow) {
					showLogining(false);
					break;
				}
				connectedTime = Calendar.getInstance();
				showLog("�����ӵ�������");

				waitData: while (!exitNow) {
					// �ȴ����Է���������Ϣ
					int byteCount;
					try {
						byteCount = in.read(buffer);
						if (byteCount == -1) {
							// ����EOF
							showLog("�������ж�");
							showLogining(false);
							waitForReconnect();
							continue reconnect;
						} else if (byteCount == 0) {
							throw new SocketTimeoutException("������");
						} else {
							// ��ȡ������
							serverActiveTime = Calendar.getInstance();
						}
					} catch (SocketTimeoutException e) {
						// ��ȡ��ʱ
						Calendar now = Calendar.getInstance();
						if (clientLogon) {
							// �ѵ�¼����Ҫʱ���������ź�
							long diff1 = now.getTimeInMillis()
									- lastActiveTime.getTimeInMillis();
							if (diff1 >= keepAliveInterval / 2) {
								// showLog("���������ź�...");
								try {
									socket.getOutputStream().write(
											SET_ALIVE_REQ.getBytes("UTF-8"));
								} catch (UnsupportedEncodingException ee) {
									// impossible!
									ee.printStackTrace();
								} catch (IOException ee) {
									showLog("���������źŲ��ɹ�: " + ee.getMessage());
									waitForReconnect();
									continue reconnect;
								}
								lastActiveTime = Calendar.getInstance();
							}

							//
							long diff2 = now.getTimeInMillis()
									- serverActiveTime.getTimeInMillis();
							if (diff2 >= keepAliveInterval) {
								showLog("������������");
								waitForReconnect();
								continue reconnect;
							}
						} else {
							// ���ڵ�¼������¼��ʱ
							long diff = now.getTimeInMillis()
									- connectedTime.getTimeInMillis();
							if (diff >= networkParams.getLoginTimeout()) {
								showLog("��¼��ʱ");
								showLogining(false);
								waitForReconnect();
								continue reconnect;
							}
						}
						continue waitData;
					} catch (IOException e) {
						// �������
						showLog("����ʧ��: " + e.getMessage());
						showLogining(false);
						waitForReconnect();
						continue reconnect;
					}

					// �������Է�����������
					byte[] data = new byte[byteCount];
					for (int i = 0; i < byteCount; i++) {
						data[i] = buffer[i];
					}
					try {
						handleServerData(data, out);
					} catch (Exception e) {
						showLog("�����쳣: " + e.getMessage());
						showLogining(false);
						waitForReconnect();
						continue reconnect;
					}
					if (clientLogon) {
						// �ѵ�¼
						showLogining(false);
						lastActiveTime = Calendar.getInstance();
					} else {
						// ����¼
						if (invalidAppInfo || invalidClientInfo) {
							// Ӧ�û�ͻ���Ϣ��Ч
							showLogining(false);
							break reconnect;
						}
					}
				}
			}

			// �ر����е��׽���
			try {
				if (socket != null) {
					socket.close();
					socket = null;
				}
			} catch (IOException ee) {
				ee.printStackTrace();
			}

			receiverStarted = false;
			showLogining(false);
			showStatus(false);
		}

		private void handleServerData(byte data[], OutputStream out)
				throws Exception {
			// ���µ������ֽ�׷�ӵ�ԭ�ֽڻ�������
			if (unhandledInput.length != 0) {
				byte[] tmpUnhandledInput = new byte[unhandledInput.length
						+ data.length];
				for (int i = 0; i < unhandledInput.length; i++)
					tmpUnhandledInput[i] = unhandledInput[i];
				for (int i = 0; i < data.length; i++)
					tmpUnhandledInput[unhandledInput.length + i] = data[i];
				unhandledInput = tmpUnhandledInput;
			} else {
				unhandledInput = data;
			}
			// ���Խ��ֽڻ�����ת�����ַ���
			String newInput = new String(unhandledInput, "UTF-8");
			if (newInput.getBytes("UTF-8").length != unhandledInput.length) {
				// δ���յ��������ַ���
				return;
			}
			unhandledInput = new byte[0];
			// �����½��յ����ַ���
			if (waitForHead) {
				// ��ȡͷ��
				headInput += newInput;

				// ����ͷ��
				boolean emptyLineFound = false;
				do {
					// ��ʼ��
					String line = "";

					// Ѱ���н�����
					int pos = headInput.indexOf(INPUT_RETURN);
					if (pos == -1) {
						// δ�ҵ��н�����
						return;
					}

					// �ҵ��н�����
					// ��¼������(�������н�����)
					line = headInput.substring(0, pos);

					// ����ͷ����
					if (!actionLineFound) {
						// ������
						String[] starr = line.split("\\s+");
						if (starr.length != 2) {
							// ��ʽ����
							String ss = INVALID_ACTION_LINE + ":" + line;
							out.write(String.format(CLOSE_CONN_RES,
									ss.length(), ss).getBytes("UTF-8"));
							throw new Exception("�����и�ʽ����: " + line);
						}

						action = starr[0].trim().toUpperCase(); // ����
						target = starr[1].trim().toUpperCase(); // Ŀ��

						actionLineFound = true;
					} else if (!line.equals("")) {
						// ������
						String[] starr = line.split("\\s*:\\s*");
						if (starr.length != 2) {
							// ��ʽ����
							String ss = INVALID_FIELD_LINE + ":" + line;
							out.write(String.format(CLOSE_CONN_RES,
									ss.length(), ss).getBytes("UTF-8"));
							throw new Exception("�����и�ʽ����: " + line);
						}

						String name = starr[0].trim().toUpperCase(); // ����
						String value = starr[1].trim().toUpperCase(); // ֵ
						fields.put(name, value);
					} else {
						// �ҵ�����
						emptyLineFound = true;
					}

					// ΪѰ����һ���н�����׼��
					headInput = headInput
							.substring(pos + INPUT_RETURN.length());
				} while (!emptyLineFound); // ֱ����������

				// ͷ����ȡ���
				waitForHead = false;

				// �ж��岿���������Ƿ����ֽ�(Ĭ��Ϊ�ַ�)
				String bodyByteLengthFieldName = FIELD_BODY_BYTE_LENGTH
						.toUpperCase();
				if (fields.containsKey(bodyByteLengthFieldName)
						&& fields.get(bodyByteLengthFieldName.toUpperCase())
								.toUpperCase().equals("TRUE")) {
					bodyByteLength = true;
				}

				// ȷ���岿����
				String bodyLengthFieldName = FIELD_BODY_LENGTH.toUpperCase();
				if (fields.containsKey(bodyLengthFieldName)) {
					int tmpBodyLength = -1;
					try {
						tmpBodyLength = Integer.parseInt(fields
								.get(bodyLengthFieldName));
					} catch (NumberFormatException e) {
						throw new Exception("�岿���ȸ�ʽ����: "
								+ fields.get(bodyLengthFieldName));
					}
					if (tmpBodyLength < 0) {
						out.write(String.format(CLOSE_CONN_RES,
								INVALID_LENGTH_VALUE_MSG.length(),
								INVALID_LENGTH_VALUE_MSG).getBytes("UTF-8"));
						throw new Exception("�岿����ֵ��Ч: "
								+ fields.get(bodyLengthFieldName));
					}
					bodyLength = tmpBodyLength;
				}

				// ����������������Ϊ�岿
				body = headInput;
			} else {
				// ��ȡ�岿
				body += newInput;
			}

			// ����岿����
			int bodyBytes = body.getBytes("UTF-8").length;
			if ((!bodyByteLength && body.length() < bodyLength)
					|| (bodyByteLength && bodyBytes < bodyLength)) {
				// ��δ��ȡ���
				return;
			} else if ((!bodyByteLength && body.length() > bodyLength)
					|| (bodyByteLength && bodyBytes > bodyLength)) {
				// �������������Ϊ��һ�����ĵ�ͷ������
				headInput = body.substring(bodyLength);
				body = body.substring(0, bodyLength);
			} else {
				// û�ж��������
				headInput = "";
			}

			// �����µı���
			try {
				handlePacket(out, action, target, fields, body);
			} catch (Exception e) {
				throw e;
			} finally {
				resetPacketStaus();
			}
		}

		private void handlePacket(OutputStream out, String aciton,
				String target, Map<String, String> field, String body)
				throws Exception {
			if (action.equals("GET") && target.equals("APPID")) {
				// Ӧ����֤���󣬷��͵�ǰӦ��ID������
				showLog("���յ�Ӧ����֤���󣬷���Ӧ����Ϣ...");
				String password = Crypt.md5(appParams.getAppPassword());
				out.write(String.format(GET_APPID_RES,
						appParams.getAppId().length() + 1 + password.length(),
						appParams.getAppId(), password).getBytes("UTF-8"));
			} else if (action.equals("SET") && target.equals("APPID")) {
				// Ӧ����֤�ظ�
				boolean appOk = (fields.get(FIELD_ACTION_SUCCESS.toUpperCase())
						.toUpperCase().equals("TRUE"));
				if (appOk) {
					showLog("Ӧ����֤ͨ��");
				} else {
					showLog("Ӧ����֤ʧ��: " + body);
					invalidAppInfo = true;
				}
			} else if (action.equals("GET") && target.equals("USERNAME")) {
				// �û���֤���󣬷����û���������
				showLog("���յ��û���֤���󣬷����û���Ϣ...");
				if (fields.get(FIELD_LOGIN_SECURE.toUpperCase()).toUpperCase()
						.equals("TRUE")) {
					// ��ȫ��¼
					if (fields.get(FIELD_LOGIN_PASSWORD.toUpperCase())
							.toUpperCase().equals("TRUE")) {
						// ��Ҫ��¼����
						String password = Crypt.md5(loginParams
								.getClientPassword());
						String ss = Crypt
								.rsaEncrypt(
										appParams.getLoginProtectKey(),
										(loginParams.getClientPassword() + "," + password));
						out.write(String.format(GET_USERNAME_RES, "true",
								"true", ss.length(), ss).getBytes("UTF-8"));
					} else {
						// ����Ҫ��¼����
						String ss = Crypt.rsaEncrypt(
								appParams.getLoginProtectKey(),
								loginParams.getClientId());
						out.write(String.format(GET_USERNAME_RES, "true",
								"false", ss.length(), ss).getBytes("UTF-8"));
					}
				} else {
					// �ǰ�ȫ��¼
					if (fields.get(FIELD_LOGIN_PASSWORD.toUpperCase())
							.toUpperCase().equals("TRUE")) {
						// ��Ҫ��¼����
						String password = Crypt.md5(loginParams
								.getClientPassword());
						String ss = loginParams.getClientId() + "," + password;
						out.write(String.format(GET_USERNAME_RES, "false",
								"true", ss.length(), ss).getBytes("UTF-8"));
					} else {
						// ����Ҫ��¼����
						String ss = loginParams.getClientId();
						out.write(String.format(GET_USERNAME_RES, "false",
								"false", ss.length(), ss).getBytes("UTF-8"));
					}
				}
			} else if (action.equals("SET") && target.equals("USERNAME")) {
				// �û���֤�ظ�
				boolean usernameOk = (fields.get(
						FIELD_ACTION_SUCCESS.toUpperCase()).toUpperCase()
						.equals("TRUE"));
				if (usernameOk) {
					showLog("�û���֤ͨ��");
				} else {
					showLog("�û���֤ʧ��: " + body);
					invalidClientInfo = true;
				}
			} else if (action.equals("SET") && target.equals("MSGKEY")) {
				// �յ���Ϣ��Կ
				msgKey = body;
				if (fields.get(FIELD_LOGIN_SECURE.toUpperCase()).toUpperCase()
						.equals("TRUE")) {
					// ������Ϣ��Կ
					msgKey = Crypt.rsaDecrypt(appParams.getLoginProtectKey(),
							body);
				}
				out.write(String.format(SET_MSGKEY_ACK).getBytes("UTF-8"));
				showLog("���յ���Ϣ��Կ");
			} else if (action.equals("SET") && target.equals("ALIVEINT")) {
				// ������������
				keepAliveInterval = Integer.parseInt(body);
				out.write(String.format(SET_ALIVEINT_ACK).getBytes("UTF-8"));
				// showLog("���յ���������(ms): " + keepAliveInterval);

				clientLogon = true;
				showLog("��¼�ɹ�");
			} else if (action.equals("SET") && target.equals("ALIVE")) {
				// �յ������ظ��ź�
				// showLog("���յ������ظ���");
			} else if (action.equals("PUSH") && target.equals("MSG")) {
				// �յ���Ϣ
				boolean secure = fields.get(FIELD_LOGIN_SECURE.toUpperCase())
						.toUpperCase().equals("TRUE");
				boolean receipt = fields.get(FIELD_MSG_RECEIPT.toUpperCase())
						.toUpperCase().equals("TRUE");
				String msg = body;
				if (receipt) {
					// ȷ�����յ���Ϣ
					out.write(PUSH_MSG_ACK.getBytes("UTF-8"));
				}
				if (secure) {
					// ��Ϣ����
					msg = Crypt.desDecrypt(msgKey, msg);
				}
				showNotification(msg);
			} else if (action.equals("CLOSE") && target.equals("CONN")) {
				// �����������Ͽ�����
				throw new Exception("�����������Ͽ�����: " + body);
			} else {
				// �޷����Ķ���
				throw new Exception("δ֪����: " + action + " " + target);
			}
		}

		private void resetPacketStaus() {
			waitForHead = true;
			actionLineFound = false;
			//
			action = target = "";
			fields.clear();
			bodyByteLength = false;
			bodyLength = 0;
			body = "";
			unhandledInput = new byte[0];
		}

		private void waitForReconnect() {
			try {
				Thread.sleep(networkParams.getReconnectDelay());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
}
