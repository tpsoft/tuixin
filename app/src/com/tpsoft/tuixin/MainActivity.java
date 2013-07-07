package com.tpsoft.tuixin;

import java.io.FileInputStream;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
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

	private class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					"com.tpsoft.pushnotification.NotifyPushService")) {
				String action = intent.getStringExtra("action");
				if (action.equals("notify")) {
					showNotification(intent.getStringExtra("msgText"));
				} else if (action.equals("log")) {
					// 处理日志
					Toast.makeText(MainActivity.this,
							intent.getStringExtra("logText"),
							Toast.LENGTH_SHORT).show();
				} else if (action.equals("status")) {
					// 响应消息接收器状态变化
					boolean receiverStarted = intent.getBooleanExtra("started",
							false);
					MyApplicationClass.clientStarted = receiverStarted;
					//
					Toast.makeText(
							MainActivity.this,
							receiverStarted ? R.string.receiver_started
									: R.string.receiver_stopped,
							Toast.LENGTH_SHORT).show();
				} else if (action.equals("logining")) {
					// 响应登录状态变化
					boolean logining = intent
							.getBooleanExtra("logining", false);
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
			}
		}
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"HH:mm:ss", Locale.CHINESE);

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

		final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setHomeAction(new IntentAction(MainActivity.this,
				createIntent(this), R.drawable.home_logo));
		// /actionBar.setHomeLogo(R.drawable.ic_launcher);
		// actionBar.setTitle(R.string.app_name);

		// 准备通知
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 初始化消息和日志控件
		msg = (LinearLayout) findViewById(R.id.msg);

		// 恢复消息显示
		Resources res = getResources();
		for (MyMessage message : MyApplicationClass.savedMsgs) {
			// 获取图片URL
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
							message,
							(imageUrl != null ? MyApplicationClass.savedImages
									.get(imageUrl) : null), res), 0);
		}
		msgCount = MyApplicationClass.savedMsgs.size();

		if (myBroadcastReceiver == null) {
			// 准备与后台服务通信
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
			registerReceiver(myBroadcastReceiver, filter);

			// 启动消息接收器
			Handler handler = new Handler();
			Runnable runnable = new Runnable() {
				public void run() {
					startMessageReceiver();
				}
			};
			handler.postDelayed(runnable, 1000); // 启动Timer
		}
	}

	@Override
	protected void onDestroy() {
		mNM.cancel(R.id.app_notification_id);

		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			if (event.isLongPress()) {
				// 退出程序
				mNM.cancel(R.id.app_notification_id);
				//
				stopService(new Intent(MainActivity.this,
						NotifyPushService.class));
				unregisterReceiver(myBroadcastReceiver);
				//
				System.exit(0);
				return true;
			} else {
				// 提示退出
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

		// 获取最新设置
		MyApplicationClass myApp = (MyApplicationClass) getApplication();
		myApp.loadUserSettings();

		AppParams appParams = new AppParams(MyApplicationClass.APP_ID,
				MyApplicationClass.APP_PASSWORD,
				MyApplicationClass.LOGIN_PROTECT_KEY);
		LoginParams loginParams = new LoginParams(
				MyApplicationClass.userSettings.getServerHost(),
				MyApplicationClass.userSettings.getServerPort(),
				MyApplicationClass.userSettings.getClientId(),
				MyApplicationClass.userSettings.getClientPassword());
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

	@SuppressLint("HandlerLeak")
	private void showNotification(String msgText) {

		// 解析消息文本
		final MyMessage message;
		try {
			message = MyMessage.extractMessage(msgText);
		} catch (Exception e) {
			return;
		}

		// 获取图片URL及文件名
		String attachmentFilename = null;
		String attachmentUrl = null;
		if (message.getAttachments() != null) {
			for (MyMessage.Attachment attachment : message.getAttachments()) {
				if (attachment.getType().matches("image/.*")) {
					attachmentFilename = attachment.getFilename();
					attachmentUrl = attachment.getUrl();
					break;
				}
			}
		}

		// 为消息对话框准备数据
		final Bundle msgParams = new Bundle();
		msgParams.putBoolean("alert", MyApplicationClass.ALERT_MSG);
		if (message.getTitle() != null && !message.getTitle().equals(""))
			msgParams.putString("title", message.getTitle());
		msgParams.putString("body", message.getBody());
		if (message.getUrl() != null && !message.getUrl().equals(""))
			msgParams.putString("url", message.getUrl());
		//
		final Intent messageDialogIntent = (messagePopupClosed ? new Intent(
				MainActivity.this, MessageDialog.class) : new Intent());
		if (attachmentUrl != null
				&& (MyApplicationClass.mExternalStorageAvailable && MyApplicationClass.mExternalStorageWriteable)) {
			final String imageFilename = new Date().getTime() + "_"
					+ attachmentFilename;
			final String imageUrl = attachmentUrl;
			//
			String sdcardPath = Environment.getExternalStorageDirectory()
					.getPath();
			final String imageFilepath = sdcardPath + "/tmp/" + imageFilename;
			final Handler handler = new Handler() {
				@SuppressLint("HandlerLeak")
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case 0:
						if (msg.arg1 == 0) {
							msgParams.putBoolean("showPic", true);
							msgParams.putString("imageFilepath", imageFilepath);
							MyApplicationClass.savedImages.put(imageUrl,
									imageFilepath);
						} else {
							msgParams.putBoolean("showPic", false);
						}
						// 添加到消息列表
						showMsg(message, msg.arg1 == 0 ? imageFilepath : null);
						// 显示/更新消息对话框
						messageDialogIntent.putExtras(msgParams);
						if (messagePopupClosed) {
							// 声音提醒
							if (MyApplicationClass.userSettings.isPlaySound()) {
								MyApplicationClass.playSoundPool
										.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
												: MyApplicationClass.INFO_SOUND,
												0);
							}
							// 显示消息对话框
							startActivity(messageDialogIntent);
							messagePopupClosed = false;
						} else {
							// 更新消息对话框
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
					int errCode = 0;
					if (!MyApplicationClass.savedImages.containsKey(imageUrl)) {
						errCode = httpDownloader.downFile(imageUrl, "tmp",
								imageFilename, true);
					}
					Message msg = new Message();
					msg.what = 0;
					msg.arg1 = errCode;
					handler.sendMessage(msg);
				}
			}.start();
		} else {
			// 添加到消息列表
			showMsg(message, null);
			// 显示/更新消息对话框
			msgParams.putBoolean("showPic", false);
			messageDialogIntent.putExtras(msgParams);
			if (messagePopupClosed) {
				// 声音提醒
				if (MyApplicationClass.userSettings.isPlaySound()) {
					MyApplicationClass.playSoundPool
							.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
									: MyApplicationClass.INFO_SOUND, 0);
				}
				// 显示消息对话框
				startActivity(messageDialogIntent);
				messagePopupClosed = false;
			} else {
				// 更新消息对话框
				messageDialogIntent.setAction(MY_CLASSNAME);
				messageDialogIntent.putExtra("action", "update");
				sendBroadcast(messageDialogIntent);
			}
		}
	}

	private void showMsg(MyMessage message, String imageFilepath) {
		Resources res = getResources();

		// 生成消息界面
		View view = makeMessageView(message, imageFilepath, res);
		if (msgCount < MAX_MSG_COUNT) {
			msg.addView(view, 0);
			msgCount++;
		} else {
			msg.removeViewAt(msg.getChildCount() - 1);
			msg.addView(view, 0);
		}

		// 保存消息界面
		MyApplicationClass.savedMsgs.add(message);
		if (MyApplicationClass.savedMsgs.size() == MAX_MSG_COUNT) {
			MyApplicationClass.savedMsgs.remove(0);
		}

		useMsgColor1 = !useMsgColor1;
	}

	@SuppressLint("ResourceAsColor")
	private View makeMessageView(MyMessage message, String imageFilepath,
			Resources res) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View listItemView = inflater.inflate(R.layout.message_list_item,
				(ViewGroup) findViewById(R.id.message));
		ImageView msgAttachmentView = (ImageView) listItemView
				.findViewById(R.id.msgAttachment);
		Bitmap bitmap = null;
		if (imageFilepath != null) {
			try {
				FileInputStream fis = new FileInputStream(imageFilepath);
				bitmap = BitmapFactory.decodeStream(fis);
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		msgAttachmentView.setImageBitmap(bitmap);
		//
		TextView msgTitleView = (TextView) listItemView
				.findViewById(R.id.msgTitle);
		msgTitleView.setText(message.getTitle() != null ? message.getTitle()
				: "");
		//
		TextView msgBodyView = (TextView) listItemView
				.findViewById(R.id.msgBody);
		msgBodyView.setText(message.getBody());
		//
		TextView msgTimeView = (TextView) listItemView
				.findViewById(R.id.msgTime);
		msgTimeView.setText(makeTimeString(message.getGenerateTime()));
		return listItemView;
	}

	private String makeTimeString(Date time) {
		return sdf.format(time);
	}

	public static Intent createIntent(Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}
}
