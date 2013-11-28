package com.tpsoft.tuixin;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MessageDialog extends Activity implements OnTouchListener,
		OnGestureListener {

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MainActivity.MY_CLASSNAME)) {
				if (intent.getStringExtra("action").equals("update")) {
					autoCloseTimer.cancel();

					Bundle msgBundle = intent.getExtras();
					msgBundles.add(msgBundle);
					msgCount++;

					updateNotifyView(msgBundle, false);
				}
			}
		}
	}

	private static final int POPUP_INFO_TIME = 1000 * 50 * 3;
	private static final int POPUP_ALERT_TIME = 1000 * 60 * 3;

	private View notifyView;
	private Timer autoCloseTimer;
	private AlertDialog alertDialog;
	private MyBroadcastReceiver myBroadcastReceiver = null;

	private List<Bundle> msgBundles = new ArrayList<Bundle>();
	private int msgCount = 1;
	private int msgNumber = 1;

	private GestureDetector mGestureDetector;
	private int verticalMinDistance = 30;
	private int horizontalMinDistance = 30;
	private int minVelocity = 0;

	private Bitmap favoriteFlag;

	private static final int MESSAGE_REMOVE_MESSAGE = 1;
	private MessageHandler msgHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Remove notification bar
		// this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// 设置窗口背景透明
		setTheme(R.style.Transparent);
		setContentView(R.layout.transparent);

		// 初始化收藏标志
		favoriteFlag = BitmapFactory.decodeResource(
				MessageDialog.this.getResources(), R.drawable.favorite_message);

		// 创建 Handler 对象
		msgHandler = new MessageHandler(this);

		// 显示消息
		showMessage();

		super.onCreate(savedInstanceState);

		// 允许手势
		mGestureDetector = new GestureDetector(MessageDialog.this,
				(OnGestureListener) MessageDialog.this);
		LinearLayout msgContainer = (LinearLayout) notifyView
				.findViewById(R.id.msgContainer);
		msgContainer.setOnTouchListener(MessageDialog.this);

		// 准备与后台服务通信
		myBroadcastReceiver = new MyBroadcastReceiver();
		//
		IntentFilter filter = new IntentFilter();
		filter.addAction(MainActivity.MY_CLASSNAME);
		registerReceiver(myBroadcastReceiver, filter);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// 重新显示消息列表
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onDestroy() {
		// this is very important here ;)
		unregisterReceiver(myBroadcastReceiver);
		//
		super.onDestroy();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		autoCloseTimer.cancel();
		return mGestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		if (e1.getY() - e2.getY() > horizontalMinDistance
				&& Math.abs(velocityY) > minVelocity) {
			// 上一条(向上滑)
			if (msgCount < 2)
				return false;
			if (msgNumber > 1) {
				msgNumber--;
				updateNotifyView(msgBundles.get(msgNumber - 1), true);
			} else {
				Toast.makeText(this, R.string.msg_first, Toast.LENGTH_SHORT)
						.show();
			}
			return true;
		} else if (e2.getY() - e1.getY() > horizontalMinDistance
				&& Math.abs(velocityY) > minVelocity) {
			// 下一条(向下滑)
			if (msgCount < 2)
				return false;
			if (msgNumber < msgBundles.size()) {
				msgNumber++;
				updateNotifyView(msgBundles.get(msgNumber - 1), true);
			} else {
				Toast.makeText(this, R.string.msg_last, Toast.LENGTH_SHORT)
						.show();
			}
			return true;
		} else if (e1.getX() - e2.getX() > verticalMinDistance
				&& Math.abs(velocityX) > minVelocity) {
			// 收藏/取消收藏(向左滑)
			Bundle msgBundle = msgBundles.get(msgNumber - 1);
			//
			boolean favorite = msgBundle.getBoolean("favorite", false);
			ImageView msgSenderIcon = (ImageView) notifyView
					.findViewById(R.id.msgSenderIcon);
			Bitmap senderIcon;
			if (msgBundle.getBoolean("showIcon")) {
				senderIcon = MyApplicationClass.loadImage(msgBundle
						.getString("iconUrl"));
				showSenderIcon(msgSenderIcon, senderIcon, favorite);
			}
			//
			Intent i = new Intent();
			i.setAction(MainActivity.MESSAGE_DIALOG_CLASSNAME);
			i.putExtra("action", favorite ? "unfavorite" : "favorite");
			i.putExtra("id", msgBundle.getInt("id"));
			sendBroadcast(i);
			//
			msgBundle.putBoolean("favorite", !favorite);
			return true;
		} else if (e2.getX() - e1.getX() > verticalMinDistance
				&& Math.abs(velocityX) > minVelocity) {
			// 删除(向右滑)
			Bundle msgBundle = msgBundles.get(msgNumber - 1);
			//
			Intent i = new Intent();
			i.setAction(MainActivity.MESSAGE_DIALOG_CLASSNAME);
			i.putExtra("action", "remove");
			i.putExtra("id", msgBundle.getInt("id"));
			sendBroadcast(i);
			//
			Message message = new Message();
			message.what = MESSAGE_REMOVE_MESSAGE;
			msgHandler.sendMessage(message);
			return true;
		}

		return false;
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		return true;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		return false;
	}

	@SuppressLint("HandlerLeak")
	public void showMessage() {
		Bundle msgBundle = getIntent().getExtras();
		msgBundles.add(msgBundle);

		// 准备弹出界面
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		notifyView = inflater.inflate(R.layout.notification_popup,
				(ViewGroup) findViewById(R.id.message));
		updateNotifyView(msgBundle, true);

		// 显示消息对话框
		showAlertDialog(msgBundle);
	}

	@android.webkit.JavascriptInterface
	public void sendMessage(final String receiver, final String title,
			final String body) {
		msgHandler.post(new Runnable() {

			@Override
			public void run() {
				Intent i = new Intent();
				i.setAction(MainActivity.MESSAGE_DIALOG_CLASSNAME);
				i.putExtra("action", "sendMessage");
				i.putExtra("receiver", receiver);
				i.putExtra("title", title);
				i.putExtra("body", body);
				sendBroadcast(i);
			}

		});
	}

	private void showAlertDialog(Bundle msgBundle) {
		AlertDialog.Builder builder;
		builder = new AlertDialog.Builder(this);
		builder.setView(notifyView);
		alertDialog = builder.create();
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams alertDialogLayoutParams = alertDialog
				.getWindow().getAttributes();
		alertDialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));
		if (msgBundle.getBoolean("alert")) {
			alertDialogLayoutParams.gravity = Gravity.CENTER_VERTICAL;
		} else {
			alertDialogLayoutParams.gravity = Gravity.BOTTOM;
		}
		alertDialog
				.setOnDismissListener(new DialogInterface.OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						// 取消定时器
						autoCloseTimer.cancel();
						// 关闭窗口
						closeSelf();
					}
				});

		autoCloseTimer = new Timer();
		autoCloseTimer.schedule(new TimerTask() {
			public void run() {
				// 关闭消息对话框
				alertDialog.dismiss();
				// 取消定时器
				autoCloseTimer.cancel();
				// 关闭窗口
				closeSelf();
			}

		}, msgBundle.getBoolean("alert") ? POPUP_ALERT_TIME : POPUP_INFO_TIME);

		ImageButton closeButton = (ImageButton) notifyView
				.findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 关闭消息对话框
				alertDialog.dismiss();
				// 取消定时器
				autoCloseTimer.cancel();
				// 关闭窗口
				closeSelf();
			}
		});

		alertDialog.show();
	}

	private void closeSelf() {
		// 通知调用方当前活动已关闭
		Intent i = new Intent();
		i.setAction(MainActivity.MESSAGE_DIALOG_CLASSNAME);
		i.putExtra("action", "popupClosed");
		sendBroadcast(i);

		// 返回原界面
		finish();
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void updateNotifyView(final Bundle msgBundle, boolean updateMessage) {
		// 消息编号
		TextView msgIndex = (TextView) notifyView.findViewById(R.id.msgIndex);
		if (msgCount > 1) {
			msgIndex.setText(String.format("%d/%d: ", msgNumber, msgCount));
		} else {
			msgIndex.setText("");
		}

		// 允许只更新消息编号
		if (!updateMessage)
			return;

		// 发送者
		final ImageView msgSenderIcon = (ImageView) notifyView
				.findViewById(R.id.msgSenderIcon);
		final Bitmap senderIcon;
		if (msgBundle.getBoolean("showIcon")) {
			senderIcon = MyApplicationClass.loadImage(msgBundle
					.getString("iconUrl"));
			boolean favorite = msgBundle.getBoolean("favorite", false);
			showSenderIcon(msgSenderIcon, senderIcon, favorite);
		} else {
			senderIcon = null;
			msgSenderIcon.setImageBitmap(null);
		}
		TextView msgSenderName = (TextView) notifyView
				.findViewById(R.id.msgSenderName);
		if (msgBundle.getString("sender").equals("me")) {
			msgSenderName.setText(R.string.me);
		} else {
			msgSenderName.setText(msgBundle.getString("sender"));
		}
		msgSenderName.setClickable(true);
		msgSenderName.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				final EditText editor = new EditText(MessageDialog.this);
				new AlertDialog.Builder(MessageDialog.this)
						.setTitle("回复" + msgBundle.getString("sender") + ":")
						.setIcon(R.drawable.reply_message)
						.setView(editor)
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String msgText = editor.getText()
												.toString();
										Intent i = new Intent();
										i.setAction(MainActivity.MESSAGE_DIALOG_CLASSNAME);
										i.putExtra("action", "sendMessage");
										i.putExtra("receiver",
												msgBundle.getString("sender"));
										i.putExtra(
												"title",
												"回复"
														+ msgBundle
																.getString("sender")
														+ ":");
										i.putExtra("body", msgText);
										i.putExtra("record", true);
										sendBroadcast(i);
									}
								}).setNegativeButton("取消", null).show();
			}
		});
		// 消息标题
		TextView msgTitle = (TextView) notifyView.findViewById(R.id.msgTitle);
		if (msgBundle.containsKey("title")) {
			msgTitle.setVisibility(View.VISIBLE);
			msgTitle.setText(msgBundle.getString("title"));
		} else {
			msgTitle.setVisibility(View.GONE);
		}
		// 消息正文
		if (msgBundle.containsKey("type")
				&& "text".equals(msgBundle.getString("type"))) {

			TextView msgBody = (TextView) notifyView.findViewById(R.id.msgBody);
			msgBody.setVisibility(View.VISIBLE);
			// INVISIBLE
			WebView msgBodyHtml = (WebView) notifyView
					.findViewById(R.id.msgBodyHtml);
			msgBodyHtml.setVisibility(View.GONE);
			//
			msgBody.setText(msgBundle.getString("body"));
		} else {

			TextView msgBody = (TextView) notifyView.findViewById(R.id.msgBody);
			msgBody.setVisibility(View.GONE);
			//
			WebView msgBodyHtml = (WebView) notifyView
					.findViewById(R.id.msgBodyHtml);
			msgBodyHtml.setVisibility(View.VISIBLE);
			msgBodyHtml.getSettings().setJavaScriptEnabled(true);
			msgBodyHtml.getSettings().setLoadsImagesAutomatically(true);
			msgBodyHtml.addJavascriptInterface(MessageDialog.this, "android");
			//
			msgBodyHtml.loadDataWithBaseURL("file:///android_asset/",
					msgBundle.getString("body"), "text/html", "UTF-8", null);
		}
		// 图片
		ImageView msgAttachment = (ImageView) notifyView
				.findViewById(R.id.msgAttachment);
		Bitmap msgAttachmentImage = null;
		if (msgBundle.getBoolean("showAttachment")) {
			msgAttachmentImage = MyApplicationClass.loadImage(msgBundle
					.getString("attachmentUrl"));
		}
		msgAttachment.setImageBitmap(msgAttachmentImage);
	}

	private void showSenderIcon(ImageView msgSender, Bitmap senderIcon,
			boolean favorite) {
		if (favorite) {
			// 已收藏
			Bitmap bitmap = Bitmap.createBitmap(senderIcon.copy(
					Config.ARGB_8888, true));
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(
					favoriteFlag,
					null,
					new Rect(senderIcon.getWidth()
							- MainActivity.FAVORITE_FLAG_WIDTH, senderIcon
							.getHeight() - MainActivity.FAVORITE_FLAG_HEIGHT,
							senderIcon.getWidth(), senderIcon.getHeight()),
					null);
			msgSender.setImageBitmap(bitmap);
		} else {
			// 未收藏
			msgSender.setImageBitmap(senderIcon);
		}
	}

	private static class MessageHandler extends Handler {
		private WeakReference<MessageDialog> mActivity;

		public MessageHandler(MessageDialog activity) {
			mActivity = new WeakReference<MessageDialog>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_REMOVE_MESSAGE:
				if (mActivity.get().msgCount == 1) {
					// 删除唯一的一条消息:关闭弹出窗口
					mActivity.get().alertDialog.dismiss(); // 关闭消息对话框
					mActivity.get().autoCloseTimer.cancel(); // 取消定时器
					mActivity.get().closeSelf(); // 关闭窗口
				} else {
					// 删除一条后还有
					mActivity.get().msgBundles
							.remove(mActivity.get().msgNumber - 1);
					if (mActivity.get().msgNumber < mActivity.get().msgCount) {
						// 要删除的不是最后一条消息:显示后面的消息(当前消息编号不变)
					} else {
						// 要删除的是最后一条消息:显示前面的消息(当前消息编号减1)
						mActivity.get().msgNumber--;
					}
					mActivity.get().msgCount--;
					mActivity.get()
							.updateNotifyView(
									mActivity.get().msgBundles.get(mActivity
											.get().msgNumber - 1), true);
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
}