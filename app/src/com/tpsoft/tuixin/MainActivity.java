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
import android.app.NotificationManager;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.tpsoft.pushnotification.client.MessageTransceiverListener;
import com.tpsoft.pushnotification.client.PushNotificationClient;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.model.PublicAccount;
import com.tpsoft.pushnotification.service.NotifyPushService;
import com.tpsoft.tuixin.db.DBManager;
import com.tpsoft.tuixin.model.MyMessageSupportSave;
import com.tpsoft.tuixin.utils.HttpUtils;

@SuppressWarnings("deprecation")
@SuppressLint("UseSparseArrays")
public class MainActivity extends TabActivity implements
		MessageTransceiverListener {

	public static final String MY_CLASSNAME = "com.tpsoft.tuixin.MainActivity";
	public static final String MESSAGE_DIALOG_CLASSNAME = "com.tpsoft.tuixin.MessageDialog";
	public static final String MESSAGE_SEND_CLASSNAME = "com.tpsoft.tuixin.SendMessageActivity";

	public static final int FAVORITE_FLAG_WIDTH = 48;
	public static final int FAVORITE_FLAG_HEIGHT = 48;

	private static final String TAG_APILOG = "PushNotification-API";
	private static final String TAG_MAINLOG = "MAIN";

	private static final String SENDER_ICON_TITLE = "sender-avatar";

	private static final SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日",
			Locale.CHINESE);

	private static final int LOAD_MSG_COUNT = -1; // 程序启动时自动加载的最近消息条数(-1表示所有消息)
	private static final long LATEST_MSG_DURATION = 24 * 60 * 7; // 要保留在列表中的消息的生成时间与当前时间的最大差值(分钟，不包含已收藏的消息)

	private static final int MESSAGE_START_RECEIVER = 1;
	private static final int MESSAGE_SHOW_NOTIFICATION = 2;
	private static final int MESSAGE_UPDATE_TIME = 3;

	private class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MESSAGE_DIALOG_CLASSNAME)) {
				// 消息弹出通知
				String action = intent.getStringExtra("action");
				if (action.equals("popupClosed")) {
					// 弹出窗口已关闭
					messagePopupClosed = true;
				} else if (action.equals("favorite")
						|| action.equals("unfavorite")) {
					// 收藏/取消收藏
					boolean favorite = action.equals("favorite");
					int id = intent.getIntExtra("id", -1);
					if (id != -1 && messages.containsKey(id)) {
						MyMessageSupportSave message = messages.get(id);
						ImageView msgSenderView = (ImageView) msgListItemViews
								.get(id).findViewById(R.id.msgSenderIcon);
						//
						Bitmap senderIcon = msgSenderIcons.get(id);
						if (favorite) {
							db.favourMessage(message.getRecordId(), true);
							message.setFavorite(true);
							//
							Bitmap bitmap = Bitmap.createBitmap(senderIcon
									.copy(Config.ARGB_8888, true));
							Canvas canvas = new Canvas(bitmap);
							canvas.drawBitmap(
									favoriteFlag,
									null,
									new Rect(senderIcon.getWidth()
											- FAVORITE_FLAG_WIDTH,
											senderIcon.getHeight()
													- FAVORITE_FLAG_HEIGHT,
											senderIcon.getWidth(), senderIcon
													.getHeight()), null);
							msgSenderView.setImageBitmap(bitmap);
						} else {
							db.favourMessage(message.getRecordId(), false);
							message.setFavorite(false);
							msgSenderView.setImageBitmap(senderIcon);
						}
					}
				} else if (action.equals("remove")) {
					// 删除
					int id = intent.getIntExtra("id", -1);
					if (id != -1 && messages.containsKey(id)) {
						removeMessageFromList(messages.get(id),
								msgListItemViews.get(id));
					}
				} else if (action.equals("sendMessage")) {
					// 发送消息(回复)
					int msgId = MyApplicationClass.nextMsgId++;
					String receiver = intent.getStringExtra("receiver");
					String title = intent.getStringExtra("title");
					String body = intent.getStringExtra("body");
					boolean record = intent.getBooleanExtra("record", false);
					sendMessage(msgId, receiver, title, body);
					if (record) {
						MyMessageSupportSave msg = new MyMessageSupportSave();
						msg.setMessageId(msgId);
						msg.setSender("me");
						msg.setReceiver(receiver);
						msg.setTitle(title);
						msg.setBody(body);
						msg.setGenerateTime(new Date());
						showMsg(msg,
								BitmapFactory.decodeResource(
										MainActivity.this.getResources(),
										R.drawable.me), null, true);
					}
				}
			} else if (intent.getAction().equals(MESSAGE_SEND_CLASSNAME)) {
				// 消息发送通知
				String action = intent.getStringExtra("action");
				if (action.equals("send")) {
					if (intent.hasExtra("errmsg")) {
						Toast.makeText(MainActivity.this,
								"发送消息失败：" + intent.getStringExtra("errmsg"),
								Toast.LENGTH_LONG).show();
						return;
					}

					MyMessage msg;
					try {
						msg = new MyMessage(intent.getBundleExtra("message"));
					} catch (ParseException e) {
						Log.e(TAG_MAINLOG, e.getMessage());
						return;
					}
					//
					int msgId = MyApplicationClass.nextMsgId++;
					msg.setSender("me");
					//
					mClient.sendMessage(msgId, msg);
					//
					Bitmap msgAttachment = null;
					if (intent.hasExtra("photo")) {
						msgAttachment = intent.getParcelableExtra("photo");
					}
					MyMessageSupportSave msg2 = new MyMessageSupportSave(msg);
					msg2.setMessageId(msgId);
					showMsg(msg2, BitmapFactory.decodeResource(
							MainActivity.this.getResources(), R.drawable.me),
							msgAttachment, true);
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
			MyMessageSupportSave message;
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
				message = (MyMessageSupportSave) msg.obj;
				// 添加到消息列表
				mActivity.get().showMsg(message, senderIcon, msgAttachment,
						false);
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
				int i = 0;
				while (i < mActivity.get().msgCount) {
					message = MyApplicationClass.latestMsgs.get(i);
					long diffInMinutes = (now.getTime() - message
							.getGenerateTime().getTime()) / (1000 * 60);
					if (diffInMinutes <= LATEST_MSG_DURATION
							|| message.isFavorite()) {
						// 消息较新或已收藏
						View listItemView = mActivity.get().msg
								.getChildAt(i * 2);
						TextView msgTimeView = (TextView) listItemView
								.findViewById(R.id.msgTime);
						String newMsgTime = mActivity.get().makeTimeString(now,
								message.getGenerateTime());
						if (!msgTimeView.getText().toString()
								.equals(newMsgTime)) {
							msgTimeView.setText(newMsgTime);
						}
						i++;
					} else {
						// 消息旧了且未收藏
						mActivity.get().msg.removeViewAt(i * 2); // 删除消息视图
						if (i < mActivity.get().msgCount - 1) {
							// 不是最后一条消息:删除后面的分隔符
							mActivity.get().msg.removeViewAt(i * 2);
						} else if (i > 0) {
							// 是最后一条消息但前面还有:删除前面的分隔符
						}
						MyApplicationClass.latestMsgs.remove(i); // 从缓存删除消息
						//
						long recordId = message.getRecordId();
						mActivity.get().db.deleteMessage(recordId); // 从数据库删除消息
						//
						mActivity.get().messages.remove(message.getMessageId());
						mActivity.get().msgSenderIcons.remove(message
								.getMessageId());
						mActivity.get().msgListItemViews.remove(message
								.getMessageId());
						mActivity.get().msgCount--; // 计数器不变
					}
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	private PushNotificationClient mClient;

	private int msgCount = 0;
	private Map<Integer/* msgId */, MyMessageSupportSave> messages = new HashMap<Integer, MyMessageSupportSave>();
	private Map<Integer/* msgId */, Bitmap> msgSenderIcons = new HashMap<Integer, Bitmap>();
	private Map<Integer/* msgId */, View> msgListItemViews = new HashMap<Integer, View>();

	private ScrollView msgContainer;
	private LinearLayout msg;
	private int verticalMinDistance = 30;
	private int minVelocity = 0;

	private NotificationManager mNM;
	private MyBroadcastReceiver myBroadcastReceiver = null;

	private MessageHandler msgHandler;
	private Timer timer = new Timer(true);

	private static boolean messagePopupClosed = true;
	private PopupWindow popupWindow;

	private DBManager db;

	private Bitmap favoriteFlag;

	private TabHost tabHost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 设置外观
		setTabs();

		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		// actionBar.setHomeAction(new IntentAction(MainActivity.this,
		// createIntent(this), R.drawable.logo));
		actionBar.setTitle(R.string.app_name);
		if (!MyApplicationClass.receiveOnly) {
			Action sendMessageAction = new IntentAction(MainActivity.this,
					new Intent(MainActivity.this, SendMessageActivity.class),
					R.drawable.send_message);
			actionBar.addAction(sendMessageAction);
		}

		// 实例化客户端
		mClient = new PushNotificationClient(this);
		mClient.addListener(this);

		// 准备通知
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 初始化消息控件
		msgContainer = (ScrollView) findViewById(R.id.msgContainer);
		msgContainer.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});
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
		filter.addAction(MESSAGE_DIALOG_CLASSNAME);
		filter.addAction(MESSAGE_SEND_CLASSNAME);
		registerReceiver(myBroadcastReceiver, filter);

		// 启动消息收发器
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
				MainActivity.this.getResources(), R.drawable.favorite_message);

		// 装入已有消息
		List<MyMessageSupportSave> messages = db.queryMessages(null,
				LOAD_MSG_COUNT);
		for (MyMessageSupportSave message : messages) {
			message.setMessageId(MyApplicationClass.nextMsgId++);
		}
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
			menu.findItem(R.id.menu_start_transceiver).setVisible(false);
			menu.findItem(R.id.menu_stop_transceiver).setVisible(true);
		} else {
			menu.findItem(R.id.menu_start_transceiver).setVisible(true);
			menu.findItem(R.id.menu_stop_transceiver).setVisible(false);
		}
		if (MyApplicationClass.clientLogon) {
			menu.findItem(R.id.menu_public_accounts_settings).setVisible(true);
		} else {
			menu.findItem(R.id.menu_public_accounts_settings).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start_transceiver:
			// 启动消息收发器
			startMessageReceiver();
			break;
		case R.id.menu_stop_transceiver:
			// 停止消息收发器
			stopMessageReceiver();
			break;
		case R.id.menu_public_accounts_settings:
			// 打开公众号界面
			startActivity(new Intent(this, PublicAccountsActivity.class));
			break;
		case R.id.menu_settings:
			// 打开设置界面
			startActivity(new Intent(this, UserSettingsActivity.class));
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
				return false;
			}
		}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onTransceiverStatus(boolean started) {
		int resId = (started ? R.string.receiver_started
				: R.string.receiver_stopped);
		String log = MainActivity.this.getText(resId).toString();
		Log.i(TAG_APILOG, log);
		//
		MyApplicationClass.clientStarted = started;
		if (!started) {
			MyApplicationClass.clientLogon = false;
		}
		Toast.makeText(MainActivity.this, log, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLogining(boolean logining) {
		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setProgressBarVisibility(logining ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onLoginStatus(int code, String text) {
		if (code < 0)
			Log.w(TAG_APILOG, String.format("%s(#%d)", text, code));
		else
			Log.i(TAG_APILOG, String.format("%s(#%d)", text, code));
		//
		if (code == 0) {
			MyApplicationClass.clientLogon = true;
		} else {
			MyApplicationClass.clientLogon = false;
		}
	}

	@Override
	public void onMessageSendStatus(int msgId, int code, final String text) {
		// 处理消息发送状态
		int ok = -1;
		if (code < 0) {
			// 失败
			Log.w(TAG_APILOG,
					String.format("msg#%d: %s(#%d)", msgId, text, code));
			ok = 0;
		} else if (code == 0) {
			// 成功
			ok = 1;
		} else {
			Log.i(TAG_APILOG,
					String.format("msg#%d: %s(#%d)", msgId, text, code));
		}
		if (ok == 0 || ok == 1) {
			// 成功或失败
			View listItemView = msgListItemViews.get(msgId);
			if (listItemView != null) {
				ImageView msgStatusView = (ImageView) listItemView
						.findViewById(R.id.msgStatus);
				if (ok == 1) {
					msgStatusView.setVisibility(View.GONE);
				} else {
					msgStatusView.setImageResource(R.drawable.message_error);
					msgStatusView
							.setOnClickListener(new View.OnClickListener() {

								@Override
								public void onClick(View v) {
									Toast.makeText(MainActivity.this,
											String.format("消息%s", text),
											Toast.LENGTH_SHORT).show();
								}
							});
				}
			}
		}
	}

	@Override
	public void onNewMessageReceived(MyMessage msg) {
		showNotification(msg);
	}

	@Override
	public void onPublicAccountsReceived(PublicAccount[] accounts) {
	}

	@Override
	public void onPublicAccountFollowed(String accountName) {

	}

	@Override
	public void onPublicAccountUnfollowed(String accountName) {

	}

	@Override
	public void onFollowedAccountsReceived(PublicAccount[] accounts) {
	}

	@android.webkit.JavascriptInterface
	public void sendMessage(final int msgId, final String receiver,
			final String title, final String body) {
		msgHandler.post(new Runnable() {

			@Override
			public void run() {
				String ss = "send message \"" + title + "\" to " + receiver
						+ ": " + body;
				Log.d(TAG_MAINLOG, ss);
				MyMessage msg = new MyMessage();
				msg.setReceiver(receiver);
				msg.setTitle(title);
				msg.setBody(body);
				mClient.sendMessage(msgId, msg);
			}

		});
	}

	private void setTabs() {
		tabHost = getTabHost();

		addTab(R.string.tab_home, R.drawable.tab_home);
		addTab(R.string.tab_contacts, R.drawable.tab_contacts);
		addTab(R.string.tab_myself, R.drawable.tab_myself);

		tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

			@Override
			public void onTabChanged(String tabId) {
				// Toast.makeText(MainActivity.this, tabId, Toast.LENGTH_SHORT)
				// .show();
			}
		});
	}

	private void addTab(int labelId, int drawableId) {
		TabHost.TabSpec spec = tabHost.newTabSpec("tab" + labelId);

		View tabIndicator = LayoutInflater.from(this).inflate(
				R.layout.tab_indicator, getTabWidget(), false);

		TextView title = (TextView) tabIndicator.findViewById(R.id.title);
		title.setText(labelId);
		ImageView icon = (ImageView) tabIndicator.findViewById(R.id.icon);
		icon.setImageResource(drawableId);

		spec.setIndicator(tabIndicator);
		switch (labelId) {
		case R.string.tab_home:
			spec.setContent(R.id.home);
			break;
		case R.string.tab_contacts:
			spec.setContent(R.id.contacts);
			break;
		case R.string.tab_myself:
			spec.setContent(R.id.myself);
			break;
		}
		tabHost.addTab(spec);

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
				senderIcon = BitmapFactory.decodeResource(MainActivity.this
						.getResources(),
						(message.getSender().equals("me") ? R.drawable.me
								: R.drawable.sender_avatar));
			}
			Bitmap msgAttachment = (msgAttachmentUrl != null ? MyApplicationClass
					.loadImage(msgAttachmentUrl) : null);
			if (msgCount != 0) {
				// 加消息分隔条
				msg.addView(makeMessageSepView());
			}
			msg.addView(makeMessageView(now, message, senderIcon,
					msgAttachment, false));
			msgCount++;
		}
	}

	private void startMessageReceiver() {
		if (MyApplicationClass.clientStarted)
			return;

		// 获取最新设置
		String clientId = MyApplicationClass.userSettings.getClientId().trim();
		String clientPassword = MyApplicationClass.userSettings
				.getClientPassword();

		AppParams appParams = new AppParams(MyApplicationClass.APP_ID,
				MyApplicationClass.APP_PASSWORD,
				MyApplicationClass.LOGIN_PROTECT_KEY);
		LoginParams loginParams = new LoginParams(
				MyApplicationClass.userSettings.getServerHost(),
				MyApplicationClass.userSettings.getServerPort(), clientId,
				clientPassword);
		NetworkParams networkParams = new NetworkParams();

		Toast.makeText(this, getText(R.string.receiver_starting),
				Toast.LENGTH_SHORT).show();

		mClient.startMessageTransceiver(appParams, loginParams, networkParams);

	}

	private void stopMessageReceiver() {
		if (!MyApplicationClass.clientStarted)
			return;

		Toast.makeText(this, getText(R.string.receiver_stopping),
				Toast.LENGTH_SHORT).show();

		mClient.stopMessageTransceiver();

	}

	private void showNotification(MyMessage message) {
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
		msgParams.putInt("id", MyApplicationClass.nextMsgId);
		msgParams.putBoolean("alert", MyApplicationClass.ALERT_MSG);
		if (message.getSender() != null)
			msgParams.putString("sender", message.getSender());
		if (message.getTitle() != null && !message.getTitle().equals(""))
			msgParams.putString("title", message.getTitle());
		if (message.getType() != null && !message.getType().equals(""))
			msgParams.putString("type", message.getType());
		msgParams.putString("body", message.getBody());
		if (message.getUrl() != null && !message.getUrl().equals(""))
			msgParams.putString("url", message.getUrl());
		//
		final MyMessageSupportSave messageSupportSave = new MyMessageSupportSave(
				message);
		messageSupportSave.setMessageId(MyApplicationClass.nextMsgId++);
		//
		if (senderIconUrl != null || msgAttachmentUrl != null) {
			final String iconUrl = senderIconUrl;
			final String attachmentUrl = msgAttachmentUrl;
			//

			new Thread() {
				public void run() {
					if (iconUrl != null
							&& !MyApplicationClass.existsImage(iconUrl)) {
						InputStream imageStream = HttpUtils
								.getInputStreamFromURL(iconUrl);
						Bitmap image = null;
						if (imageStream != null) {
							image = BitmapFactory.decodeStream(imageStream);
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
						InputStream imageStream = HttpUtils
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
			showMsg(messageSupportSave, null, null, false);
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
			Bitmap msgAttachment, boolean send) {
		if (popupWindow != null) {
			popupWindow.dismiss();
		}

		Date now = new Date();

		// 生成消息界面
		View view = makeMessageView(
				now,
				message,
				senderIcon == null ? BitmapFactory.decodeResource(
						MainActivity.this.getResources(),
						R.drawable.sender_avatar) : senderIcon, msgAttachment,
				send);
		if (msgCount != 0) {
			// 加消息分隔条
			msg.addView(makeMessageSepView(), 0);
		}
		msg.addView(view, 0);
		msgCount++;

		// 保存消息
		long recordId = db.addMessage(message, false);
		message.setRecordId(recordId);
		MyApplicationClass.latestMsgs.add(0, message);

		// 更新旧消息的生成时间
		for (int i = 1; i < msgCount; i++) {
			message = MyApplicationClass.latestMsgs.get(i);
			View listItemView = msg.getChildAt(i * 2);
			TextView msgTimeView = (TextView) listItemView
					.findViewById(R.id.msgTime);
			String newMsgTime = makeTimeString(now, message.getGenerateTime());
			if (!msgTimeView.getText().toString().equals(newMsgTime)) {
				msgTimeView.setText(newMsgTime);
			}
		}
	}

	@SuppressLint({ "ResourceAsColor", "SetJavaScriptEnabled" })
	private View makeMessageView(Date now, final MyMessageSupportSave message,
			final Bitmap senderIcon, final Bitmap msgAttachment, boolean send) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View listItemView = inflater.inflate(R.layout.message_list_item,
				null);
		final ImageView msgSenderIconView = (ImageView) listItemView
				.findViewById(R.id.msgSenderIcon);
		msgSenderIconView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});
		ImageView msgStatusView = (ImageView) listItemView
				.findViewById(R.id.msgStatus);
		//
		messages.put(message.getMessageId(), message);
		msgSenderIcons.put(message.getMessageId(), senderIcon);
		msgListItemViews.put(message.getMessageId(), listItemView);
		// 给发送者图标绘制收藏标志
		Bitmap bitmap = senderIcon;
		if (message.isFavorite()) {
			// 已经收藏
			bitmap = Bitmap.createBitmap(senderIcon
					.copy(Config.ARGB_8888, true));
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(favoriteFlag, null,
					new Rect(senderIcon.getWidth() - FAVORITE_FLAG_WIDTH,
							senderIcon.getHeight() - FAVORITE_FLAG_HEIGHT,
							senderIcon.getWidth(), senderIcon.getHeight()),
					null);
		}

		msgSenderIconView.setImageBitmap(bitmap);
		//
		if (send) {
			msgStatusView.setVisibility(View.VISIBLE);
		}
		//
		TextView msgSenderNameView = (TextView) listItemView
				.findViewById(R.id.msgSenderName);
		if (message.getSender().equals("me"))
			msgSenderNameView.setText(R.string.me);
		else
			msgSenderNameView.setText(message.getSender());
		//
		TextView msgTitleView = (TextView) listItemView
				.findViewById(R.id.msgTitle);
		if (message.getTitle() != null) {
			msgTitleView.setVisibility(View.VISIBLE);
			msgTitleView.setText(message.getTitle());
		} else if (message.getSender().equals("me")) {
			msgTitleView.setVisibility(View.VISIBLE);
			msgTitleView.setText("对 " + message.getReceiver() + " 说:");
		}

		if (message.getUrl() != null) {
			final String url = message.getUrl();
			ImageView msgLinkView = (ImageView) listItemView
					.findViewById(R.id.msgLink);
			msgLinkView.setVisibility(View.VISIBLE);
			msgLinkView.setOnClickListener(new View.OnClickListener() {

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

		TextView msgBodyView = (TextView) listItemView
				.findViewById(R.id.msgBody);
		WebView msgBodyHtmlView = (WebView) listItemView
				.findViewById(R.id.msgBodyHtml);
		//
		if ("html".equals(message.getType())) {
			// HTML消息
			msgBodyView.setVisibility(View.GONE);
			//
			msgBodyHtmlView.setVisibility(View.VISIBLE);
			msgBodyHtmlView.getSettings().setJavaScriptEnabled(true);
			msgBodyHtmlView.getSettings().setLoadsImagesAutomatically(true);
			msgBodyHtmlView
					.addJavascriptInterface(MainActivity.this, "android");
			//
			msgBodyHtmlView.loadDataWithBaseURL("file:///android_asset/",
					message.getBody(), "text/html", "UTF-8", null);
		} else {
			// 文本消息
			msgBodyView.setText(message.getBody());
		}
		//
		if (!MyApplicationClass.receiveOnly) {
			ImageView msgActionsView = (ImageView) listItemView
					.findViewById(R.id.msgActions);
			msgActionsView.setVisibility(View.VISIBLE);
			msgActionsView.setClickable(true);
			msgActionsView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					listItemView.setBackgroundColor(MainActivity.this
							.getResources().getColor(
									R.color.message_list_item_selected_color));
					showMsgActionMenu(message, listItemView);
				}
			});
		}
		//
		ImageView msgAttachmentView = (ImageView) listItemView
				.findViewById(R.id.msgAttachment);
		msgAttachmentView.setImageBitmap(msgAttachment);
		//
		TextView msgTimeView = (TextView) listItemView
				.findViewById(R.id.msgTime);
		msgTimeView.setText(makeTimeString(now, message.getGenerateTime()));
		//
		final GestureDetector mGestureDetector = new GestureDetector(
				MainActivity.this, new OnGestureListener() {

					@Override
					public boolean onDown(MotionEvent e) {
						return true; // 非常重要，若是返回false，则无法触发onFling
					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						if (e1.getX() - e2.getX() > verticalMinDistance
								&& Math.abs(velocityX) > minVelocity) {
							// 收藏/取消收藏(向左滑)
							if (!message.isFavorite()) {
								// 收藏
								db.favourMessage(message.getRecordId(), true);
								message.setFavorite(true);
								//
								Bitmap bitmap = Bitmap.createBitmap(senderIcon
										.copy(Config.ARGB_8888, true));
								Canvas canvas = new Canvas(bitmap);
								canvas.drawBitmap(favoriteFlag, null,
										new Rect(senderIcon.getWidth()
												- FAVORITE_FLAG_WIDTH,
												senderIcon.getHeight()
														- FAVORITE_FLAG_HEIGHT,
												senderIcon.getWidth(),
												senderIcon.getHeight()), null);
								msgSenderIconView.setImageBitmap(bitmap);
							} else {
								// 取消收藏
								db.favourMessage(message.getRecordId(), false);
								message.setFavorite(false);
								//
								msgSenderIconView.setImageBitmap(senderIcon);
							}
							return true;
						} else if (e2.getX() - e1.getX() > verticalMinDistance
								&& Math.abs(velocityX) > minVelocity) {
							// 删除(向右滑)
							removeMessageFromList(message, listItemView);
							return true;
						}

						return false;
					}

					@Override
					public void onLongPress(MotionEvent e) {
					}

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						return false;
					}

					@Override
					public void onShowPress(MotionEvent e) {
					}

					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						return false;
					}

				});
		listItemView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		msgSenderIconView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		msgSenderNameView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		msgTitleView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		msgTimeView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		msgBodyView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		msgBodyHtmlView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		msgAttachmentView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});
		//
		return listItemView;
	}

	private View makeMessageSepView() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View sepView = inflater.inflate(R.layout.message_list_sep, null);
		return sepView;
	}

	private void showMsgActionMenu(final MyMessage message,
			final View listItemView) {
		LinearLayout layout = (LinearLayout) LayoutInflater.from(
				MainActivity.this).inflate(R.layout.message_actions, null);
		final ListView listView = (ListView) layout
				.findViewById(R.id.lv_dialog);
		final boolean sentMessage = (message.getSender().equals("me"));
		listView.setAdapter(new ArrayAdapter<String>(
				MainActivity.this,
				R.layout.message_action_text,
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
								R.color.message_background_color));
				popupWindow = null;
			}
		});
		popupWindow.showAsDropDown(listItemView
				.findViewById(R.id.msgSenderName));
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

	private void removeMessageFromList(MyMessageSupportSave message,
			View listItemView) {
		int pos = msg.indexOfChild(listItemView);
		msg.removeView(listItemView);
		if (pos < (msgCount - 1) * 2) {
			// 不是最后一条:删除后面的分隔符
			msg.removeViewAt(pos);
		} else if (pos > 0) {
			// 是最后一条消息但前面还有:删除前面的分隔符
			msg.removeViewAt(pos - 1);
		}
		MyApplicationClass.latestMsgs.remove(pos / 2);
		//
		messages.remove(message.getMessageId());
		msgSenderIcons.remove(message.getMessageId());
		msgListItemViews.remove(message.getMessageId());
		//
		msgCount--;
	}

	private String makeTimeString(Date now, Date time) {
		String str;
		long diffInSeconds = (now.getTime() - time.getTime()) / 1000;
		if (diffInSeconds < 1) {
			str = "刚刚";
		} else if (diffInSeconds < 60) {
			str = (diffInSeconds) + "秒钟前";
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

	@SuppressWarnings("unused")
	private Intent createIntent(Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}

}
