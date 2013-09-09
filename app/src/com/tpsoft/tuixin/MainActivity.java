package com.tpsoft.tuixin;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.service.NotifyPushService;
import com.tpsoft.tuixin.utils.HttpDownloader;

public class MainActivity extends Activity {

	public static final String MY_CLASSNAME = "com.tpsoft.tuixin.MainActivity";
	public static final String MESSAGE_DIALOG_CLASSNAME = "com.tpsoft.tuixin.MessageDialog";
	public static final String MESSAGE_SEND_CLASSNAME = "com.tpsoft.tuixin.SendMessageActivity";

	public static final String TAG_APILOG = "PushNotification-API";
	public static final String TAG_MAINLOG = "MAIN";

	private class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					"com.tpsoft.pushnotification.NotifyPushService")) {
				String action = intent.getStringExtra("action");
				if (action.equals("notify")) {
					showNotification(intent.getStringExtra("msgText"));
				} else if (action.equals("log")) {
					// ������־
					int type = intent.getIntExtra("type", 0);
					int code = intent.getIntExtra("code", 0);
					String params = intent.getStringExtra("params");
					switch (type) {
					case NotifyPushService.LOG_CONNECT: // ����:
						switch (code) {
						case NotifyPushService.STATUS_CONNECT_CONNECTING: // ���ӷ�����...
							Log.i(TAG_APILOG, "���ӷ����� " + params + "...");
							break;
						case NotifyPushService.STATUS_CONNECT_CONNECTED: // �Ѿ����ӵ�������
							Log.i(TAG_APILOG, "�����ӵ���������");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATING: // Ӧ����֤...
							Log.i(TAG_APILOG, "У��Ӧ��ID�ͽ�������...");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATED: // Ӧ����֤ͨ��
							Log.i(TAG_APILOG, "Ӧ����֤ͨ����");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATING: // �û���֤...
							Log.i(TAG_APILOG, "У���û���������...");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATED: // �û���֤ͨ��
							Log.i(TAG_APILOG, "�û���֤ͨ����");
							break;
						case NotifyPushService.STATUS_CONNECT_MSGKEY_RECEIVED: // �յ���Ϣ��Կ
							Log.i(TAG_APILOG, "�յ���Ϣ��Կ��");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED: // �յ���������
							Log.d(TAG_APILOG,
									"�յ���������: " + Integer.parseInt(params)
											/ 1000 + "�롣");
							break;
						case NotifyPushService.STATUS_CONNECT_LOGON: // ��¼�ɹ�
							Log.i(TAG_APILOG, "��¼�ɹ���");
							Toast.makeText(MainActivity.this, "��¼�ɹ�",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE: // ���������ź�
							Log.d(TAG_APILOG, "���������ź�...");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE_REPLIED: // �յ������ظ��ź�
							// showLog("�յ������ظ��źš�");
							Log.d(TAG_APILOG, "�յ������ظ��ź�");
							break;
						case NotifyPushService.ERROR_CONNECT_NETWORK_UNAVAILABLE: // ���粻����
							Log.w(TAG_APILOG, "���粻���ã�");
							Toast.makeText(MainActivity.this, "���粻����",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // �������ж�
							Log.w(TAG_APILOG, "�������жϣ�");
							Toast.makeText(MainActivity.this, "�������ж�",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // ������������
							Log.w(TAG_APILOG, "�����������ã�");
							Toast.makeText(MainActivity.this, "������������",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_LOGIN_TIMEOUT: // ��¼��ʱ
							Log.w(TAG_APILOG, "��¼��ʱ��");
							Toast.makeText(MainActivity.this, "��¼��ʱ",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_IO_FAULT: // ����IO����
							Log.w(TAG_APILOG, "����IO���ϣ�");
							Toast.makeText(MainActivity.this, "����IO����",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_APP_CERTIFICATE: // Ӧ����֤ʧ��
							Log.e(TAG_APILOG, "Ӧ����֤ʧ�ܣ�");
							Toast.makeText(MainActivity.this, "Ӧ����֤ʧ��",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_USER_CERTIFICATE: // �û���֤ʧ��
							Log.e(TAG_APILOG, "�û���֤ʧ�ܣ�");
							Toast.makeText(MainActivity.this, "�û���֤ʧ��",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER: // ����������
							Log.e(TAG_APILOG, "����������");
							Toast.makeText(MainActivity.this, "����������",
									Toast.LENGTH_SHORT).show();
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_SENDMSG: // ������Ϣ:
						switch (code) {
						case NotifyPushService.STATUS_SENDMSG_SUBMIT: // �ύ��Ϣ
							Log.i(TAG_APILOG, "�ύ #" + params + " ��Ϣ...");
							Toast.makeText(MainActivity.this, "�ύ��Ϣ...",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_SENDMSG_SUBMITTED: // ���ύ��Ϣ
							Log.i(TAG_APILOG, "#" + params + " ��Ϣ���ύ��");
							Toast.makeText(MainActivity.this, "��Ϣ���ύ",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_SENDMSG_OK: // �յ���Ϣȷ��
							Log.i(TAG_APILOG, "#" + params + " ��Ϣ��ȷ�ϡ�");
							Toast.makeText(MainActivity.this, "��Ϣ��ȷ��",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_NOT_LOGON: // ��δ��¼�ɹ�
							Toast.makeText(MainActivity.this, "��δ�ɹ���¼",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_DATA: // ��Ϣ���ݴ���
							Log.e(TAG_APILOG, "#" + params + " ��Ϣ���ݴ���");
							break;
						case NotifyPushService.ERROR_SENDMSG_SUBMIT: // �ύ��Ϣʧ��
							Log.w(TAG_APILOG, "#" + params + " ��Ϣ�ύʧ�ܡ�");
							Toast.makeText(MainActivity.this, "��Ϣ�ύʧ��",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_FAILED: // ������Ϣʧ��
							int pos = params.indexOf(":");
							String msgId = params.substring(0, pos);
							String err = params.substring(pos + 1);
							String errmsg = err.substring(err.indexOf(",") + 1);
							Log.w(TAG_APILOG, "#" + msgId + " ��Ϣ����ʧ��(" + errmsg
									+ ")��");
							Toast.makeText(MainActivity.this, "��Ϣ����ʧ��",
									Toast.LENGTH_SHORT).show();
							break;
						default:
							break;
						}
						break;
					default:
						break;
					}
				} else if (action.equals("status")) {
					// ��Ӧ��Ϣ������״̬�仯
					boolean receiverStarted = intent.getBooleanExtra("started",
							false);
					MyApplicationClass.clientStarted = receiverStarted;
					//
					int resId = (receiverStarted ? R.string.receiver_started
							: R.string.receiver_stopped);
					String log = MainActivity.this.getText(resId).toString();
					Log.i(TAG_APILOG, log);
					Toast.makeText(MainActivity.this, log, Toast.LENGTH_SHORT)
							.show();
				} else if (action.equals("logining")) {
					// ��Ӧ��¼״̬�仯
					boolean logining = intent
							.getBooleanExtra("logining", false);
					if (logining) {
						Toast.makeText(MainActivity.this, "���ڵ�¼...",
								Toast.LENGTH_SHORT).show();
					}
					ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
					actionBar.setProgressBarVisibility(logining ? View.VISIBLE
							: View.GONE);
				} else {
					;
				}
			} else if (intent.getAction().equals(MESSAGE_DIALOG_CLASSNAME)) {
				String action = intent.getStringExtra("action");
				if (action.equals("popupClosed")) {
					messagePopupClosed = true;
				} else {
					;
				}
			} else if (intent.getAction().equals(MESSAGE_SEND_CLASSNAME)) {
				String action = intent.getStringExtra("action");
				if (action.equals("sent")) {
					try {
						showMsg(new MyMessage(intent.getBundleExtra("message")),
								null);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else {
					;
				}
			}
		}
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy��MM��dd�� HH:mm:ss", Locale.CHINESE);

	private static final int MAX_MSG_COUNT = 20;
	private LinearLayout msg;
	private int msgCount = 0;
	private boolean useMsgColor1 = true;

	private NotificationManager mNM;
	private MyBroadcastReceiver myBroadcastReceiver = null;
	private boolean messagePopupClosed = true;

	private HttpDownloader httpDownloader = new HttpDownloader();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		MyApplicationClass myApp = (MyApplicationClass) getApplication();
		myApp.loadUserSettings();

		final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setHomeAction(new IntentAction(MainActivity.this,
				createIntent(this), R.drawable.home_logo));
		// actionBar.setHomeLogo(R.drawable.ic_launcher);
		actionBar.setTitle(R.string.app_name);
		final Action sendMessageAction = new IntentAction(this, new Intent(
				this, SendMessageActivity.class), R.drawable.send_message);
		actionBar.addAction(sendMessageAction);

		// ׼��֪ͨ
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// ��ʼ����Ϣ����־�ؼ�
		msg = (LinearLayout) findViewById(R.id.msg);

		if (myBroadcastReceiver == null) {
			// ׼�����̨����ͨ��
			myBroadcastReceiver = new MyBroadcastReceiver();
			try {
				unregisterReceiver(myBroadcastReceiver);
			} catch (Exception e) {
				;
			}
			//
			IntentFilter filter = new IntentFilter();
			filter.addAction("com.tpsoft.pushnotification.NotifyPushService");
			filter.addAction(MESSAGE_DIALOG_CLASSNAME);
			filter.addAction(MESSAGE_SEND_CLASSNAME);
			registerReceiver(myBroadcastReceiver, filter);

			// ������Ϣ������
			Handler handler = new Handler();
			Runnable runnable = new Runnable() {
				public void run() {
					startMessageReceiver();
				}
			};
			handler.postDelayed(runnable, 1000); // ����Timer
		}
	}

	@Override
	protected void onDestroy() {
		mNM.cancel(R.id.app_notification_id);

		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		msg.removeAllViews();

		Date now = new Date();
		Resources res = getResources();

		for (MyMessage message : MyApplicationClass.savedMsgs) {
			// ��ȡͼƬURL
			String imageUrl = null;
			if (message.getAttachments() != null) {
				for (MyMessage.Attachment attachment : message.getAttachments()) {
					if (attachment.getType().matches("image/.*")) {
						imageUrl = attachment.getUrl();
						break;
					}
				}
			}
			msg.addView(
					makeMessageView(
							now,
							message,
							(imageUrl != null ? MyApplicationClass.savedImages
									.get(imageUrl) : null), res), 0);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (MyApplicationClass.clientStarted) {
			menu.findItem(R.id.menu_start).setEnabled(false);
			menu.findItem(R.id.menu_stop).setEnabled(true);
		} else {
			menu.findItem(R.id.menu_start).setEnabled(true);
			menu.findItem(R.id.menu_stop).setEnabled(false);
		}
		menu.findItem(R.id.menu_settings).setEnabled(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			// ������Ϣ������
			startMessageReceiver();
			break;
		case R.id.menu_stop:
			// ֹͣ��Ϣ������
			stopMessageReceiver();
			break;
		case R.id.menu_settings:
			// �����ý���
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		}
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			if (event.isLongPress()) {
				// �˳�����
				mNM.cancel(R.id.app_notification_id);
				//
				stopService(new Intent(MainActivity.this,
						NotifyPushService.class));
				unregisterReceiver(myBroadcastReceiver);
				//
				System.exit(0);
				return true;
			} else {
				// ��ʾ�˳�
				Toast.makeText(this, getText(R.string.app_exit),
						Toast.LENGTH_SHORT).show();
				boolean flag = false;
				return flag;
			}
		}
		}
		return super.dispatchKeyEvent(event);
	}

	private void startMessageReceiver() {
		if (MyApplicationClass.clientStarted)
			return;

		Toast.makeText(this, getText(R.string.receiver_starting),
				Toast.LENGTH_SHORT).show();

		// ��ȡ��������
		String clientPassword = MyApplicationClass.userSettings
				.getClientPassword().trim();
		if (clientPassword.equals("")) {
			clientPassword = MyApplicationClass.userSettings.getClientId();
		}

		AppParams appParams = new AppParams(MyApplicationClass.APP_ID,
				MyApplicationClass.APP_PASSWORD,
				MyApplicationClass.LOGIN_PROTECT_KEY);
		LoginParams loginParams = new LoginParams(
				MyApplicationClass.userSettings.getServerHost(),
				MyApplicationClass.userSettings.getServerPort(),
				MyApplicationClass.userSettings.getClientId(), clientPassword);
		NetworkParams networkParams = new NetworkParams();

		Intent serviceIntent = new Intent();
		serviceIntent
				.setAction("com.tpsoft.pushnotification.ServiceController");
		serviceIntent.putExtra("command", "start");
		serviceIntent.putExtra("com.tpsoft.pushnotification.AppParams",
				appParams.getBundle());
		serviceIntent.putExtra("com.tpsoft.pushnotification.LoginParams",
				loginParams.getBundle());
		serviceIntent.putExtra("com.tpsoft.pushnotification.NetworkParams",
				networkParams.getBundle());
		sendBroadcast(serviceIntent);
	}

	private void stopMessageReceiver() {
		if (!MyApplicationClass.clientStarted)
			return;

		Toast.makeText(this, getText(R.string.receiver_stopping),
				Toast.LENGTH_SHORT).show();
		Intent serviceIntent = new Intent();
		serviceIntent
				.setAction("com.tpsoft.pushnotification.ServiceController");
		serviceIntent.putExtra("command", "stop");
		sendBroadcast(serviceIntent);
	}

	@SuppressLint("HandlerLeak")
	private void showNotification(String msgText) {

		// ������Ϣ�ı�
		final MyMessage message;
		try {
			message = MyMessage.extractMessage(msgText);
		} catch (Exception e) {
			return;
		}

		// ��ȡͼƬURL���ļ���
		// String attachmentFilename = null;
		String attachmentUrl = null;
		if (message.getAttachments() != null) {
			for (MyMessage.Attachment attachment : message.getAttachments()) {
				if (attachment.getType().matches("image/.*")) {
					// attachmentFilename = attachment.getFilename();
					attachmentUrl = attachment.getUrl();
					break;
				}
			}
		}

		// Ϊ��Ϣ�Ի���׼������
		final Bundle msgParams = new Bundle();
		msgParams.putBoolean("alert", MyApplicationClass.ALERT_MSG);
		if (message.getSender() != null)
			msgParams.putString("sender", message.getSender());
		if (message.getTitle() != null && !message.getTitle().equals(""))
			msgParams.putString("title", message.getTitle());
		msgParams.putString("body", message.getBody());
		if (message.getUrl() != null && !message.getUrl().equals(""))
			msgParams.putString("url", message.getUrl());
		//
		final Intent messageDialogIntent = (messagePopupClosed ? new Intent(
				MainActivity.this, MessageDialog.class) : new Intent());
		if (attachmentUrl != null) {
			final String imageUrl = attachmentUrl;
			//
			final Handler handler = new Handler() {
				@SuppressLint("HandlerLeak")
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case 0:
						Bitmap image = MyApplicationClass.savedImages
								.get(imageUrl);
						if (image != null) {
							msgParams.putBoolean("showPic", true);
							msgParams.putString("picUrl", imageUrl);
						} else {
							msgParams.putBoolean("showPic", false);
						}
						// ��ӵ���Ϣ�б�
						showMsg(message, image);
						// ��ʾ/������Ϣ�Ի���
						messageDialogIntent.putExtras(msgParams);
						if (messagePopupClosed) {
							// ��������
							if (MyApplicationClass.userSettings.isPlaySound()) {
								MyApplicationClass.playSoundPool
										.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
												: MyApplicationClass.INFO_SOUND,
												0);
							}
							// ��ʾ��Ϣ�Ի���
							startActivity(messageDialogIntent);
							messagePopupClosed = false;
						} else {
							// ������Ϣ�Ի���
							messageDialogIntent.setAction(MY_CLASSNAME);
							messageDialogIntent.putExtra("action", "update");
							sendBroadcast(messageDialogIntent);
						}
						break;
					default:
						super.handleMessage(msg);
					}
				}
			};

			new Thread() {
				public void run() {
					if (!MyApplicationClass.savedImages.containsKey(imageUrl)) {
						InputStream imageStream = httpDownloader
								.getInputStreamFromURL(imageUrl);
						Bitmap image = null;
						if (imageStream != null) {
							image = BitmapFactory.decodeStream(imageStream);
							MyApplicationClass.savedImages.put(imageUrl, image);
							try {
								imageStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							MyApplicationClass.savedImages.put(imageUrl, null);
						}
					}
					Message msg = new Message();
					msg.what = 0;
					handler.sendMessage(msg);
				}
			}.start();
		} else {
			// ��ӵ���Ϣ�б�
			showMsg(message, null);
			// ��ʾ/������Ϣ�Ի���
			msgParams.putBoolean("showPic", false);
			messageDialogIntent.putExtras(msgParams);
			if (messagePopupClosed) {
				// ��������
				if (MyApplicationClass.userSettings.isPlaySound()) {
					MyApplicationClass.playSoundPool
							.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
									: MyApplicationClass.INFO_SOUND, 0);
				}
				// ��ʾ��Ϣ�Ի���
				startActivity(messageDialogIntent);
				messagePopupClosed = false;
			} else {
				// ������Ϣ�Ի���
				messageDialogIntent.setAction(MY_CLASSNAME);
				messageDialogIntent.putExtra("action", "update");
				sendBroadcast(messageDialogIntent);
			}
		}
	}

	private void showMsg(MyMessage message, Bitmap image) {
		Date now = new Date();
		Resources res = getResources();

		// ������Ϣ����
		View view = makeMessageView(now, message, image, res);
		if (msgCount < MAX_MSG_COUNT) {
			msg.addView(view, 0);
			msgCount++;
		} else {
			msg.removeViewAt(msg.getChildCount() - 1);
			msg.addView(view, 0);
		}

		// ������Ϣ
		MyApplicationClass.savedMsgs.add(message);
		if (MyApplicationClass.savedMsgs.size() == MAX_MSG_COUNT) {
			MyApplicationClass.savedMsgs.remove(0);
		}

		if (msgCount > 1) {
			// ���¾���Ϣ������ʱ��
			for (int i = 1; i < msgCount; i++) {
				message = MyApplicationClass.savedMsgs.get(msgCount - i - 1);
				View listItemView = msg.getChildAt(i);
				TextView msgTimeView = (TextView) listItemView
						.findViewById(R.id.msgTime);
				msgTimeView.setText(makeTimeString(now,
						message.getGenerateTime()));
			}
		}

		useMsgColor1 = !useMsgColor1;
	}

	@SuppressLint("ResourceAsColor")
	private View makeMessageView(Date now, MyMessage message, Bitmap image,
			Resources res) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View listItemView = inflater.inflate(R.layout.message_list_item,
				(ViewGroup) findViewById(R.id.message));
		ImageView msgAttachmentView = (ImageView) listItemView
				.findViewById(R.id.msgAttachment);
		msgAttachmentView.setImageBitmap(image);
		//
		TextView msgTitleView = (TextView) listItemView
				.findViewById(R.id.msgTitle);
		if (message.getTitle() != null)
			msgTitleView.setText(message.getTitle());
		else if (message.getSender() != null)
			msgTitleView.setText(message.getSender() + ": ");
		else
			msgTitleView.setText(R.string.msg_notitle);
		if (message.getUrl() != null) {
			final String url = message.getUrl();
			msgTitleView.setClickable(true);
			msgTitleView.setTextColor(Color.BLUE);
			msgTitleView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					try {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW,
								Uri.parse(url));
						startActivity(browserIntent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		//
		TextView msgBodyView = (TextView) listItemView
				.findViewById(R.id.msgBody);
		msgBodyView.setText(message.getBody());
		//
		TextView msgTimeView = (TextView) listItemView
				.findViewById(R.id.msgTime);
		msgTimeView.setText(makeTimeString(now, message.getGenerateTime()));
		return listItemView;
	}

	private String makeTimeString(Date now, Date time) {
		String str;
		long diffInSeconds = (now.getTime() - time.getTime()) / 1000;
		if (diffInSeconds < 60) {
			str = "�ո�";
		} else if (diffInSeconds < 60 * 60) {
			str = (diffInSeconds / 60) + "����ǰ";
		} else if (diffInSeconds < 60 * 60 * 24) {
			str = (diffInSeconds / (60 * 60)) + "Сʱǰ";
		} else if (diffInSeconds < 60 * 60 * 24 * 30) {
			str = (diffInSeconds / (60 * 60 * 24)) + "��ǰ";
		} else {
			str = sdf.format(time);
		}
		return str;
	}

	public static Intent createIntent(Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}
}
