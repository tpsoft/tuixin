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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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

	public static final String SENDER_ICON_TITLE = "sender-avatar";
	private static final int MESSAGE_REPLY = Menu.FIRST;

	private int msgCount = 0;

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
					int type = intent.getIntExtra("type", 0);
					int code = intent.getIntExtra("code", 0);
					String params = intent.getStringExtra("params");
					switch (type) {
					case NotifyPushService.LOG_CONNECT: // 连接:
						switch (code) {
						case NotifyPushService.STATUS_CONNECT_CONNECTING: // 连接服务器...
							Log.i(TAG_APILOG, "连接服务器 " + params + "...");
							break;
						case NotifyPushService.STATUS_CONNECT_CONNECTED: // 已经连接到服务器
							Log.i(TAG_APILOG, "已连接到服务器。");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATING: // 应用认证...
							Log.i(TAG_APILOG, "校验应用ID和接入密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATED: // 应用认证通过
							Log.i(TAG_APILOG, "应用认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATING: // 用户认证...
							Log.i(TAG_APILOG, "校验用户名和密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATED: // 用户认证通过
							Log.i(TAG_APILOG, "用户认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_MSGKEY_RECEIVED: // 收到消息密钥
							Log.i(TAG_APILOG, "收到消息密钥。");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED: // 收到心跳周期
							Log.i(TAG_APILOG,
									"收到心跳周期: " + Integer.parseInt(params)
											/ 1000 + "秒。");
							break;
						case NotifyPushService.STATUS_CONNECT_LOGON: // 登录成功
							Log.i(TAG_APILOG, "登录成功。");
							Toast.makeText(MainActivity.this, "登录成功",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE: // 发送心跳信号
							Log.d(TAG_APILOG, "发送心跳信号...");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE_REPLIED: // 收到心跳回复信号
							// showLog("收到心跳回复信号。");
							Log.d(TAG_APILOG, "收到心跳回复信号");
							break;
						case NotifyPushService.ERROR_CONNECT_NETWORK_UNAVAILABLE: // 网络不可用
							Log.w(TAG_APILOG, "网络不可用！");
							Toast.makeText(MainActivity.this, "网络不可用",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // 连接已中断
							Log.w(TAG_APILOG, "连接已中断！");
							Toast.makeText(MainActivity.this, "连接已中断",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // 服务器不可用
							Log.w(TAG_APILOG, "服务器不可用！");
							Toast.makeText(MainActivity.this, "服务器不可用",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_LOGIN_TIMEOUT: // 登录超时
							Log.w(TAG_APILOG, "登录超时！");
							Toast.makeText(MainActivity.this, "登录超时",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_IO_FAULT: // 网络IO故障
							Log.w(TAG_APILOG, "网络IO故障！");
							Toast.makeText(MainActivity.this, "网络IO故障",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_APP_CERTIFICATE: // 应用认证失败
							Log.e(TAG_APILOG, "应用认证失败！");
							Toast.makeText(MainActivity.this, "应用认证失败",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_USER_CERTIFICATE: // 用户认证失败
							Log.e(TAG_APILOG, "用户认证失败！");
							Toast.makeText(MainActivity.this, "用户认证失败",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER: // 服务器错误
							Log.e(TAG_APILOG, "服务器错误！");
							Toast.makeText(MainActivity.this, "服务器错误",
									Toast.LENGTH_SHORT).show();
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_SENDMSG: // 发送消息:
						switch (code) {
						case NotifyPushService.STATUS_SENDMSG_SUBMIT: // 提交消息
							Log.i(TAG_APILOG, "提交 #" + params + " 消息...");
							Toast.makeText(MainActivity.this, "提交消息...",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_SENDMSG_SUBMITTED: // 已提交消息
							Log.i(TAG_APILOG, "#" + params + " 消息已提交。");
							Toast.makeText(MainActivity.this, "消息已提交",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_SENDMSG_OK: // 收到消息确认
							Log.i(TAG_APILOG, "#" + params + " 消息已确认。");
							Toast.makeText(MainActivity.this, "消息已确认",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_NOT_LOGON: // 尚未登录成功
							Toast.makeText(MainActivity.this, "尚未成功登录",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_DATA: // 消息数据错误
							Log.e(TAG_APILOG, "#" + params + " 消息数据错误！");
							break;
						case NotifyPushService.ERROR_SENDMSG_SUBMIT: // 提交消息失败
							Log.w(TAG_APILOG, "#" + params + " 消息提交失败。");
							Toast.makeText(MainActivity.this, "消息提交失败",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_FAILED: // 发送消息失败
							int pos = params.indexOf(":");
							String msgId = params.substring(0, pos);
							String err = params.substring(pos + 1);
							String errmsg = err.substring(err.indexOf(",") + 1);
							Log.w(TAG_APILOG, "#" + msgId + " 消息发送失败(" + errmsg
									+ ")！");
							Toast.makeText(MainActivity.this, "消息发送失败",
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
					// 响应消息接收器状态变化
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
					// 响应登录状态变化
					boolean logining = intent
							.getBooleanExtra("logining", false);
					if (logining) {
						Toast.makeText(MainActivity.this, "正在登录...",
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
								BitmapFactory.decodeResource(
										MainActivity.this.getResources(),
										R.drawable.send), null);
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
			"yyyy年MM月dd日", Locale.CHINESE);

	private static final int MAX_MSG_COUNT = 20;
	private LinearLayout msg;

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

		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setHomeAction(new IntentAction(MainActivity.this,
				createIntent(this), R.drawable.home_logo));
		// actionBar.setHomeLogo(R.drawable.ic_launcher);
		actionBar.setTitle(R.string.latest_msgs);
		Action sendMessageAction = new IntentAction(this, new Intent(this,
				SendMessageActivity.class), R.drawable.send_message);
		actionBar.addAction(sendMessageAction);

		// 准备通知
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 初始化消息和日志控件
		msg = (LinearLayout) findViewById(R.id.msg);

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
			filter.addAction(MESSAGE_SEND_CLASSNAME);
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
		msg.removeAllViews();

		Date now = new Date();
		Resources res = getResources();

		for (MyMessage message : MyApplicationClass.savedMsgs) {
			// 获取图片URL
			String senderIconUrl = null;
			String msgAttachmentUrl = null;
			if (message.getAttachments() != null) {
				for (MyMessage.Attachment attachment : message.getAttachments()) {
					if (attachment.getType().matches("image/.*")) {
						if (attachment.getTitle().equals(SENDER_ICON_TITLE))
							senderIconUrl = attachment.getUrl();
						else
							msgAttachmentUrl = attachment.getUrl();
					}
				}
			}
			Bitmap senderIcon = (senderIconUrl != null ? MyApplicationClass.savedImages
					.get(senderIconUrl) : null);
			if (senderIcon == null) {
				senderIcon = BitmapFactory.decodeResource(MainActivity.this
						.getResources(),
						(message.getSender().equals("我") ? R.drawable.send
								: R.drawable.avatar));
			}
			Bitmap msgAttachment = (msgAttachmentUrl != null ? MyApplicationClass.savedImages
					.get(msgAttachmentUrl) : null);
			if (msgCount != 0) {
				// 加消息分隔条
				msg.addView(makeMessageSepView(), 0);
			}
			msg.addView(
					makeMessageView(now, message, senderIcon, msgAttachment,
							res), 0);
			msgCount++;
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
			// 启动消息接收器
			startMessageReceiver();
			break;
		case R.id.menu_stop:
			// 停止消息接收器
			stopMessageReceiver();
			break;
		case R.id.menu_settings:
			// 打开设置界面
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		Animation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(500);
		anim.setStartOffset(0);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setRepeatCount(0);
		v.startAnimation(anim);
		//
		menu.add(0, MESSAGE_REPLY, 0, "回复");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MESSAGE_REPLY:
			break;
		}
		return true;
	}

	private void startMessageReceiver() {
		if (MyApplicationClass.clientStarted)
			return;

		Toast.makeText(this, getText(R.string.receiver_starting),
				Toast.LENGTH_SHORT).show();

		// 获取最新设置
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

		// 解析消息文本
		final MyMessage message;
		try {
			message = MyMessage.extractMessage(msgText);
		} catch (Exception e) {
			return;
		}

		if (message.getGenerateTime() == null) {
			Log.w(TAG_MAINLOG, "接收到没有生成时间的消息：" + msgText);
			return;
		}

		// 获取图片URL及文件名
		String senderIconUrl = null;
		String msgAttachmentUrl = null;
		if (message.getAttachments() != null) {
			for (MyMessage.Attachment attachment : message.getAttachments()) {
				if (attachment.getType().matches("image/.*")) {
					if (attachment.getTitle().equals("sender-avatar"))
						senderIconUrl = attachment.getUrl();
					else
						msgAttachmentUrl = attachment.getUrl();
				}
			}
		}

		// 为消息对话框准备数据
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
		if (senderIconUrl != null || msgAttachmentUrl != null) {
			final String iconUrl = senderIconUrl;
			final String attachmentUrl = msgAttachmentUrl;
			//
			final Handler handler = new Handler() {
				@SuppressLint("HandlerLeak")
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case 0:
						Bitmap senderIcon = MyApplicationClass.savedImages
								.get(iconUrl);
						if (senderIcon != null) {
							msgParams.putBoolean("showIcon", true);
							msgParams.putString("iconUrl", iconUrl);
						} else {
							msgParams.putBoolean("showIcon", false);
						}
						Bitmap msgAttachment = MyApplicationClass.savedImages
								.get(attachmentUrl);
						if (msgAttachment != null) {
							msgParams.putBoolean("showAttachment", true);
							msgParams.putString("attachmentUrl", attachmentUrl);
						} else {
							msgParams.putBoolean("showAttachment", false);
						}
						// 添加到消息列表
						showMsg(message, senderIcon, msgAttachment);
						if (MyApplicationClass.userSettings.isPopupMsg()) {
							// 显示/更新消息对话框
							messageDialogIntent.putExtras(msgParams);
							if (messagePopupClosed) {
								// 声音提醒
								if (MyApplicationClass.userSettings
										.isPlaySound()) {
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
								messageDialogIntent
										.putExtra("action", "update");
								sendBroadcast(messageDialogIntent);
							}
						}
						break;
					default:
						super.handleMessage(msg);
					}
				}
			};

			new Thread() {
				public void run() {
					if (!MyApplicationClass.savedImages.containsKey(iconUrl)) {
						InputStream imageStream = httpDownloader
								.getInputStreamFromURL(iconUrl);
						Bitmap image = null;
						if (imageStream != null) {
							image = BitmapFactory.decodeStream(imageStream);
							MyApplicationClass.savedImages.put(iconUrl, image);
							try {
								imageStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							MyApplicationClass.savedImages.put(iconUrl, null);
						}
					}
					if (!MyApplicationClass.savedImages
							.containsKey(attachmentUrl)) {
						InputStream imageStream = httpDownloader
								.getInputStreamFromURL(attachmentUrl);
						Bitmap image = null;
						if (imageStream != null) {
							image = BitmapFactory.decodeStream(imageStream);
							MyApplicationClass.savedImages.put(attachmentUrl,
									image);
							try {
								imageStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							MyApplicationClass.savedImages.put(attachmentUrl,
									null);
						}
					}
					Message msg = new Message();
					msg.what = 0;
					handler.sendMessage(msg);
				}
			}.start();
		} else {
			// 添加到消息列表
			showMsg(message, null, null);
			if (MyApplicationClass.userSettings.isPopupMsg()) {
				// 显示/更新消息对话框
				msgParams.putBoolean("showIcon", false);
				msgParams.putBoolean("showAttachment", false);
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
	}

	private void showMsg(MyMessage message, Bitmap senderIcon,
			Bitmap msgAttachment) {
		Date now = new Date();
		Resources res = getResources();

		// 生成消息界面
		View view = makeMessageView(
				now,
				message,
				senderIcon == null ? BitmapFactory.decodeResource(
						MainActivity.this.getResources(), R.drawable.avatar)
						: senderIcon, msgAttachment, res);
		if (msgCount != 0) {
			// 加消息分隔条
			msg.addView(makeMessageSepView(), 0);
		}
		msg.addView(view, 0);
		if (msgCount == MAX_MSG_COUNT) {
			msg.removeViewAt(MAX_MSG_COUNT * 2 - 1);
			msg.removeViewAt(MAX_MSG_COUNT * 2 - 1);
		} else {
			msgCount++;
		}

		// 保存消息
		if (MyApplicationClass.savedMsgs.size() == MAX_MSG_COUNT) {
			MyApplicationClass.savedMsgs.remove(0);
		}
		MyApplicationClass.savedMsgs.add(message);

		// 更新旧消息的生成时间
		for (int i = 1; i < msgCount; i++) {
			message = MyApplicationClass.savedMsgs.get(msgCount - i - 1);
			View listItemView = msg.getChildAt(i * 2);
			TextView msgTimeView = (TextView) listItemView
					.findViewById(R.id.msgTime);
			msgTimeView.setText(makeTimeString(now, message.getGenerateTime()));
		}
	}

	@SuppressLint("ResourceAsColor")
	private View makeMessageView(Date now, MyMessage message,
			Bitmap senderIcon, Bitmap msgAttachment, Resources res) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View listItemView = inflater.inflate(R.layout.message_list_item,
				null);
		ImageView msgSenderView = (ImageView) listItemView
				.findViewById(R.id.msgSender);
		msgSenderView.setImageBitmap(senderIcon);
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
		ImageView msgAttachmentView = (ImageView) listItemView
				.findViewById(R.id.msgAttachment);
		msgAttachmentView.setImageBitmap(msgAttachment);
		//
		TextView msgTimeView = (TextView) listItemView
				.findViewById(R.id.msgTime);
		msgTimeView.setText(makeTimeString(now, message.getGenerateTime()));
		//
		if (message.getSender() != null && !message.getSender().equals("我")) {
			registerForContextMenu(listItemView);
		}
		return listItemView;
	}

	private View makeMessageSepView() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View sepView = inflater.inflate(R.layout.message_list_sep, null);
		return sepView;
	}

	private String makeTimeString(Date now, Date time) {
		String str;
		long diffInSeconds = (now.getTime() - time.getTime()) / 1000;
		if (diffInSeconds < 60) {
			str = "刚刚";
		} else if (diffInSeconds < 60 * 60) {
			str = (diffInSeconds / 60) + "分钟前";
		} else if (diffInSeconds < 60 * 60 * 24) {
			str = (diffInSeconds / (60 * 60)) + "小时前";
		} else if (diffInSeconds < 60 * 60 * 24 * 30) {
			long days = diffInSeconds / (60 * 60 * 24);
			switch ((int) days) {
			case 1:
				str = "昨天";
				break;
			case 2:
				str = "前天";
				break;
			default:
				str = (days) + "天前";
				break;
			}
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
