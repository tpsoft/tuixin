package com.tpsoft.tuixin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
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
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
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
import com.tpsoft.tuixin.db.DBManager;
import com.tpsoft.tuixin.model.MyMessageSupportSave;
import com.tpsoft.tuixin.utils.HttpDownloader;
import com.tpsoft.tuixin.utils.ImageUtils;

@SuppressLint("UseSparseArrays")
public class MainActivity extends Activity {

	public static final String MY_CLASSNAME = "com.tpsoft.tuixin.MainActivity";
	public static final String MESSAGE_DIALOG_CLASSNAME = "com.tpsoft.tuixin.MessageDialog";
	public static final String MESSAGE_SEND_CLASSNAME = "com.tpsoft.tuixin.SendMessageActivity";

	public static final String TAG_APILOG = "PushNotification-API";
	public static final String TAG_MAINLOG = "MAIN";

	public static final String SENDER_ICON_TITLE = "sender-avatar";
	public static final int ICON_CORNER_SIZE = 60;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日",
			Locale.CHINESE);

	private static final int LOAD_MSG_COUNT = 20; // 程序启动时自动加载的最近消息条数
	private static final long LATEST_MSG_DURATION = 4 * 60; // 要保留在列表中的消息的生成时间与当前时间的最大差值(分钟，不包含已收藏的消息)

	private static final int MESSAGE_START_RECEIVER = 1;
	private static final int MESSAGE_SHOW_NOTIFICATION = 2;
	private static final int MESSAGE_UPDATE_TIME = 3;

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
							MyApplicationClass.clientLogon = true;
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
							MyApplicationClass.clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // 连接已中断
							Log.w(TAG_APILOG, "连接已中断！");
							Toast.makeText(MainActivity.this, "连接已中断",
									Toast.LENGTH_SHORT).show();
							MyApplicationClass.clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // 服务器不可用
							Log.w(TAG_APILOG, "服务器不可用！");
							Toast.makeText(MainActivity.this, "服务器不可用",
									Toast.LENGTH_SHORT).show();
							MyApplicationClass.clientLogon = false;
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
							MyApplicationClass.clientLogon = false;
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
							MyApplicationClass.clientLogon = false;
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
					if (!receiverStarted) {
						MyApplicationClass.clientLogon = false;
					}
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
					// 弹出窗口已关闭
					messagePopupClosed = true;
				} else if (action.equals("favorite")
						|| action.equals("unfavorite")) {
					// 收藏/取消收藏
					boolean favorite = action.equals("favorite");
					int id = intent.getIntExtra("id", -1);
					if (id != -1) {
						MyMessageSupportSave message = messages.get(id);
						ImageView msgSenderView = msgSenderViews.get(id);
						//
						Bitmap senderIcon = msgSenderIcons.get(id);
						if (favorite) {
							long recordId = db.addMessage(message);
							message.setRecordId(recordId);
							//
							Bitmap bitmap = Bitmap.createBitmap(senderIcon
									.copy(Config.ARGB_8888, true));
							Canvas canvas = new Canvas(bitmap);
							canvas.drawBitmap(favoriteFlag, null,
									new Rect(0, 0, senderIcon.getWidth(),
											senderIcon.getHeight()), null);
							msgSenderView.setImageBitmap(bitmap);
						} else {
							long recordId = message.getRecordId();
							db.deleteMessage(recordId);
							message.setRecordId(null);
							msgSenderView.setImageBitmap(senderIcon);
						}
					}
				} else {
					;
				}
			} else if (intent.getAction().equals(MESSAGE_SEND_CLASSNAME)) {
				String action = intent.getStringExtra("action");
				if (action.equals("sent")) {
					try {
						showMsg(new MyMessageSupportSave(new MyMessage(
								intent.getBundleExtra("message"))),
								BitmapFactory.decodeResource(
										MainActivity.this.getResources(),
										R.drawable.sent_message), null);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else {
					;
				}
			}
		}
	}

	private static class MessageHandler extends Handler {
		private WeakReference<MainActivity> mActivity;

		public MessageHandler(MainActivity activity) {
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_START_RECEIVER:
				mActivity.get().startMessageReceiver();
				break;
			case MESSAGE_SHOW_NOTIFICATION:
				Bundle msgParams = msg.getData();
				Bitmap senderIcon = msgParams.get("iconUrl") != null ? MyApplicationClass
						.loadImage(msgParams.getString("iconUrl")) : null;
				if (senderIcon != null) {
					msgParams.putBoolean("showIcon", true);
				} else {
					msgParams.putBoolean("showIcon", false);
				}
				Bitmap msgAttachment = msgParams.get("attachmentUrl") != null ? MyApplicationClass
						.loadImage(msgParams.getString("attachmentUrl")) : null;
				if (msgAttachment != null) {
					msgParams.putBoolean("showAttachment", true);
				} else {
					msgParams.putBoolean("showAttachment", false);
				}
				// 添加到消息列表
				mActivity.get().showMsg((MyMessageSupportSave) msg.obj,
						senderIcon, msgAttachment);
				//
				if (MyApplicationClass.userSettings.isPopupMsg()) {
					// 显示/更新消息对话框
					Intent messageDialogIntent = (messagePopupClosed ? new Intent(
							mActivity.get(), MessageDialog.class)
							: new Intent());
					messageDialogIntent.putExtras(msgParams);
					if (messagePopupClosed) {
						// 声音提醒
						if (MyApplicationClass.userSettings.isPlaySound()) {
							MyApplicationClass.playSoundPool
									.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
											: MyApplicationClass.INFO_SOUND, 0);
						}
						// 显示消息对话框
						mActivity.get().startActivity(messageDialogIntent);
						messagePopupClosed = false;
					} else {
						// 更新消息对话框
						messageDialogIntent.setAction(MY_CLASSNAME);
						messageDialogIntent.putExtra("action", "update");
						mActivity.get().sendBroadcast(messageDialogIntent);
					}
				}
				break;
			case MESSAGE_UPDATE_TIME:
				// 更新旧消息的生成时间，同时清除已旧的未收藏消息
				Date now = new Date();
				int msgCount = mActivity.get().msgCount;
				int i = 0;
				while (i < msgCount) {
					MyMessageSupportSave message = MyApplicationClass.latestMsgs
							.get(i);
					long diffInMinutes = (now.getTime() - message
							.getGenerateTime().getTime()) / (1000 * 60);
					if (diffInMinutes <= LATEST_MSG_DURATION
							|| message.getRecordId() != null) {
						// 消息较新或已收藏
						View listItemView = mActivity.get().msg
								.getChildAt(i * 2);
						TextView msgTimeView = (TextView) listItemView
								.findViewById(R.id.msgTime);
						msgTimeView.setText(mActivity.get().makeTimeString(now,
								message.getGenerateTime()));
						i++;
					} else {
						// 消息旧了且未收藏
						mActivity.get().msg.removeViewAt(i); // 删除消息视图
						if (i < msgCount - 1) {
							// 不是最后一条消息
							mActivity.get().msg.removeViewAt(i); // 删除分隔符视图
						}
						MyApplicationClass.latestMsgs.remove(i); // 删除消息
						msgCount--; // 计数器不变
					}
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	private int msgCount = 0;
	private int nextMsgId = 0;
	private Map<Integer/* msgId */, MyMessageSupportSave> messages = new HashMap<Integer, MyMessageSupportSave>();
	private Map<Integer/* msgId */, Bitmap> msgSenderIcons = new HashMap<Integer, Bitmap>();
	private Map<Integer/* msgId */, ImageView> msgSenderViews = new HashMap<Integer, ImageView>();

	private LinearLayout msg;

	private NotificationManager mNM;
	private MyBroadcastReceiver myBroadcastReceiver = null;

	private HttpDownloader httpDownloader = new HttpDownloader();
	private MessageHandler msgHandler;
	private Timer timer = new Timer(true);

	private static boolean messagePopupClosed = true;
	private PopupWindow popupWindow;

	private DBManager db;

	private Bitmap favoriteFlag;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		MyApplicationClass myApp = (MyApplicationClass) getApplication();
		myApp.loadUserSettings();

		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setHomeAction(new IntentAction(MainActivity.this,
				createIntent(this), R.drawable.clock));
		// actionBar.setHomeLogo(R.drawable.ic_launcher);
		actionBar.setTitle(R.string.msg_list);
		if (!MyApplicationClass.receiveOnly) {
			Action sendMessageAction = new IntentAction(this, new Intent(this,
					SendMessageActivity.class), R.drawable.send_message);
			actionBar.addAction(sendMessageAction);
		}

		// 准备通知
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 初始化消息和日志控件
		msg = (LinearLayout) findViewById(R.id.msg);

		// 创建 Handler 对象
		msgHandler = new MessageHandler(this);

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
		Message msg = new Message();
		msg.what = MESSAGE_START_RECEIVER;
		msgHandler.sendMessageDelayed(msg, 1000);

		// 设置定时更新消息列表中的时间
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Message message = new Message();
				message.what = MESSAGE_UPDATE_TIME;
				msgHandler.sendMessage(message);
			}
		}, 1000 * 60, 1000 * 60);

		// 打开数据库
		db = new DBManager(this);
		MyApplicationClass.db = db;

		// 初始化收藏标志
		favoriteFlag = BitmapFactory.decodeResource(
				MainActivity.this.getResources(), R.drawable.favorite);

		// 装入已有消息
		List<MyMessageSupportSave> messages = db.queryMessages(null,
				LOAD_MSG_COUNT);
		MyApplicationClass.latestMsgs.addAll(messages);
		showMessages(messages);
	}

	@Override
	protected void onDestroy() {
		mNM.cancel(R.id.app_notification_id);

		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		msg.removeAllViews();
		msgCount = 0;

		showMessages(MyApplicationClass.latestMsgs);
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
			menu.findItem(R.id.menu_start).setVisible(false);
			menu.findItem(R.id.menu_stop).setVisible(true);
		} else {
			menu.findItem(R.id.menu_start).setVisible(true);
			menu.findItem(R.id.menu_stop).setVisible(false);
		}
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

	private void showMessages(List<MyMessageSupportSave> messages) {
		Date now = new Date();

		for (MyMessageSupportSave message : messages) {
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
			Bitmap senderIcon = (senderIconUrl != null ? MyApplicationClass
					.loadImage(senderIconUrl) : null);
			if (senderIcon == null) {
				senderIcon = BitmapFactory
						.decodeResource(
								MainActivity.this.getResources(),
								(message.getSender().equals("me") ? R.drawable.sent_message
										: R.drawable.avatar));
			}
			Bitmap msgAttachment = (msgAttachmentUrl != null ? MyApplicationClass
					.loadImage(msgAttachmentUrl) : null);
			if (msgCount != 0) {
				// 加消息分隔条
				msg.addView(makeMessageSepView());
			}
			msg.addView(makeMessageView(now, message, senderIcon, msgAttachment));
			msgCount++;
		}
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

	private void showNotification(String msgText) {

		// 解析消息文本
		final MyMessage message;
		try {
			message = MyMessage.extractMessage(msgText);
		} catch (Exception e) {
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
		msgParams.putInt("id", nextMsgId);
		msgParams.putBoolean("alert", MyApplicationClass.ALERT_MSG);
		if (message.getSender() != null)
			msgParams.putString("sender", message.getSender());
		if (message.getTitle() != null && !message.getTitle().equals(""))
			msgParams.putString("title", message.getTitle());
		msgParams.putString("body", message.getBody());
		if (message.getUrl() != null && !message.getUrl().equals(""))
			msgParams.putString("url", message.getUrl());
		//
		final MyMessageSupportSave messageSupportSave = new MyMessageSupportSave(
				message);
		messageSupportSave.setMessageId(nextMsgId++);
		//
		if (senderIconUrl != null || msgAttachmentUrl != null) {
			final String iconUrl = senderIconUrl;
			final String attachmentUrl = msgAttachmentUrl;
			//

			new Thread() {
				public void run() {
					if (iconUrl != null
							&& !MyApplicationClass.existsImage(iconUrl)) {
						InputStream imageStream = httpDownloader
								.getInputStreamFromURL(iconUrl);
						Bitmap image = null;
						if (imageStream != null) {
							image = ImageUtils.roundCorners(
									BitmapFactory.decodeStream(imageStream),
									ICON_CORNER_SIZE);
							MyApplicationClass.saveImage(iconUrl, image);
							try {
								imageStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							MyApplicationClass.saveImage(iconUrl, null);
						}
					}
					if (attachmentUrl != null
							&& !MyApplicationClass.existsImage(attachmentUrl)) {
						InputStream imageStream = httpDownloader
								.getInputStreamFromURL(attachmentUrl);
						Bitmap image = null;
						if (imageStream != null) {
							image = BitmapFactory.decodeStream(imageStream);
							MyApplicationClass.saveImage(attachmentUrl, image);
							try {
								imageStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							MyApplicationClass.saveImage(attachmentUrl, null);
						}
					}
					if (iconUrl != null)
						msgParams.putString("iconUrl", iconUrl);
					if (attachmentUrl != null)
						msgParams.putString("attachmentUrl", attachmentUrl);
					Message msg = new Message();
					msg.what = MESSAGE_SHOW_NOTIFICATION;
					msg.obj = messageSupportSave;
					msg.setData(msgParams);
					msgHandler.sendMessage(msg);
				}
			}.start();
		} else {
			// 添加到消息列表
			showMsg(messageSupportSave, null, null);
			//
			if (MyApplicationClass.userSettings.isPopupMsg()) {
				// 显示/更新消息对话框
				msgParams.putBoolean("showIcon", false);
				msgParams.putBoolean("showAttachment", false);
				//
				Intent messageDialogIntent = (messagePopupClosed ? new Intent(
						MainActivity.this, MessageDialog.class) : new Intent());
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

	private void showMsg(MyMessageSupportSave message, Bitmap senderIcon,
			Bitmap msgAttachment) {
		if (popupWindow != null) {
			popupWindow.dismiss();
		}

		Date now = new Date();

		// 生成消息界面
		View view = makeMessageView(
				now,
				message,
				senderIcon == null ? BitmapFactory.decodeResource(
						MainActivity.this.getResources(), R.drawable.avatar)
						: senderIcon, msgAttachment);
		if (msgCount != 0) {
			// 加消息分隔条
			msg.addView(makeMessageSepView(), 0);
		}
		msg.addView(view, 0);
		msgCount++;

		// 保存消息
		MyApplicationClass.latestMsgs.add(0, message);

		// 更新旧消息的生成时间
		for (int i = 1; i < msgCount; i++) {
			message = MyApplicationClass.latestMsgs.get(i);
			View listItemView = msg.getChildAt(i * 2);
			TextView msgTimeView = (TextView) listItemView
					.findViewById(R.id.msgTime);
			msgTimeView.setText(makeTimeString(now, message.getGenerateTime()));
		}
	}

	@SuppressLint("ResourceAsColor")
	private View makeMessageView(Date now, final MyMessageSupportSave message,
			final Bitmap senderIcon, final Bitmap msgAttachment) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View listItemView = inflater.inflate(R.layout.message_list_item,
				null);
		final ImageView msgSenderView = (ImageView) listItemView
				.findViewById(R.id.msgSender);
		//
		messages.put(message.getMessageId(), message);
		msgSenderIcons.put(message.getMessageId(), senderIcon);
		msgSenderViews.put(message.getMessageId(), msgSenderView);
		//
		msgSenderView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				if (message.getRecordId() == null) {
					// 收藏
					long recordId = db.addMessage(message);
					message.setRecordId(recordId);
					//
					Bitmap bitmap = Bitmap.createBitmap(senderIcon.copy(
							Config.ARGB_8888, true));
					Canvas canvas = new Canvas(bitmap);
					canvas.drawBitmap(favoriteFlag, null, new Rect(0, 0,
							senderIcon.getWidth(), senderIcon.getHeight()),
							null);
					msgSenderView.setImageBitmap(bitmap);
				} else {
					// 取消收藏
					long recordId = message.getRecordId();
					db.deleteMessage(recordId);
					message.setRecordId(null);
					//
					msgSenderView.setImageBitmap(senderIcon);
				}
			}
		});
		// 给发送者图标绘制收藏标志
		Bitmap bitmap = senderIcon;
		if (message.getRecordId() != null) {
			// 已经收藏
			bitmap = Bitmap.createBitmap(senderIcon
					.copy(Config.ARGB_8888, true));
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(
					favoriteFlag,
					null,
					new Rect(0, 0, senderIcon.getWidth(), senderIcon
							.getHeight()), null);
		}

		msgSenderView.setImageBitmap(bitmap);
		//
		TextView msgTitleView = (TextView) listItemView
				.findViewById(R.id.msgTitle);
		if (message.getTitle() != null)
			msgTitleView.setText(message.getTitle());
		else if (message.getSender() != null) {
			if (message.getSender().equals("me"))
				msgTitleView.setText("致 " + message.getReceiver() + ": ");
			else
				msgTitleView.setText("自 " + message.getSender() + ": ");
		} else
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
		ImageView msgActions = (ImageView) listItemView
				.findViewById(R.id.msgActions);
		if (MyApplicationClass.receiveOnly) {
			msgActions.setVisibility(View.INVISIBLE);
		} else {
			msgActions.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					listItemView.setBackgroundColor(MainActivity.this
							.getResources().getColor(
									R.color.message_list_item_selected));
					showMsgActionMenu(message, listItemView);
				}
			});
		}
		return listItemView;
	}

	private View makeMessageSepView() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View sepView = inflater.inflate(R.layout.message_list_sep, null);
		return sepView;
	}

	@SuppressWarnings("deprecation")
	private void showMsgActionMenu(final MyMessage message,
			final View listItemView) {
		LinearLayout layout = (LinearLayout) LayoutInflater.from(
				MainActivity.this).inflate(R.layout.dialog, null);
		final ListView listView = (ListView) layout
				.findViewById(R.id.lv_dialog);
		final boolean sentMessage = (message.getSender().equals("me"));
		listView.setAdapter(new ArrayAdapter<String>(
				MainActivity.this,
				R.layout.text,
				R.id.tv_text,
				(sentMessage ? new String[] { MainActivity.this.getResources()
						.getText(R.string.msg_sendagain).toString() }
						: new String[] {
								MainActivity.this.getResources()
										.getText(R.string.msg_reply).toString(),
								MainActivity.this.getResources()
										.getText(R.string.msg_forward)
										.toString() })));

		popupWindow = new PopupWindow(MainActivity.this);
		popupWindow.setBackgroundDrawable(new BitmapDrawable());
		popupWindow.setWidth(LayoutParams.WRAP_CONTENT);
		popupWindow.setHeight(LayoutParams.WRAP_CONTENT);
		popupWindow.setOutsideTouchable(true);
		popupWindow.setFocusable(true);
		popupWindow.setContentView(layout);
		popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

			@Override
			public void onDismiss() {
				listItemView.setBackgroundColor(MainActivity.this
						.getResources().getColor(
								R.color.message_list_item_unselected));
				popupWindow = null;
			}
		});
		// showAsDropDown会把里面的view作为参照物，所以要那满屏幕parent
		// popupWindow.showAsDropDown(findViewById(R.id.tv_title), x, 10);
		popupWindow.showAsDropDown(listItemView.findViewById(R.id.msgTitle));

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				popupWindow.dismiss();
				//
				Intent i = new Intent(MainActivity.this,
						SendMessageActivity.class);
				switch (position) {
				case 0:
					// 回复/再次发送
					if (sentMessage) {
						// 再次发送
						i.putExtra("receiver", message.getReceiver());
						i.putExtra("message", message.getBody());
					} else {
						i.putExtra("receiver", message.getSender());
					}
					break;
				case 1:
					// 转发
					i.putExtra("message", message.getBody());
					break;
				}
				startActivity(i);
			}
		});
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
