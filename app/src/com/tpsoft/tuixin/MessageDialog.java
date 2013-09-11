package com.tpsoft.tuixin;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MessageDialog extends Activity implements OnTouchListener,
		OnGestureListener {

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MainActivity.MY_CLASSNAME)) {
				if (intent.getStringExtra("action").equals("update")) {
					autoCloseTimer.cancel();

					Bundle msgBundle = intent.getExtras();
					msgBundles.add(msgBundle);
					msgCount++;

					updateNotifyView(msgBundle, false);

					if (msgCount == 2) {
						// 出现第二条消息时允许手势
						mGestureDetector = new GestureDetector(
								(OnGestureListener) MessageDialog.this);
						LinearLayout popupLayout = (LinearLayout) notifyView
								.findViewById(R.id.message);
						popupLayout.setOnTouchListener(MessageDialog.this);
						popupLayout.setLongClickable(true);
						//
						LinearLayout msgContainer = (LinearLayout) notifyView
								.findViewById(R.id.msgContainer);
						msgContainer.setOnTouchListener(MessageDialog.this);
						//
						RelativeLayout msgTitleBar = (RelativeLayout) notifyView
								.findViewById(R.id.msgTitleBar);
						msgTitleBar.setOnTouchListener(MessageDialog.this);
						//
						TextView msgIndex = (TextView) notifyView
								.findViewById(R.id.msgIndex);
						msgIndex.setOnTouchListener(MessageDialog.this);
						//
						TextView msgTitle = (TextView) notifyView
								.findViewById(R.id.msgTitle);
						msgTitle.setOnTouchListener(MessageDialog.this);
						//
						TextView msgBody = (TextView) notifyView
								.findViewById(R.id.msgBody);
						msgBody.setOnTouchListener(MessageDialog.this);
						//
						ImageView msgAttachment = (ImageView) notifyView
								.findViewById(R.id.msgAttachment);
						msgAttachment.setOnTouchListener(MessageDialog.this);
					}
				}
			}
		}
	}

	private static final int POPUP_INFO_TIME = 1000 * 50;
	private static final int POPUP_ALERT_TIME = 1000 * 60;

	private View notifyView;
	private Timer autoCloseTimer;
	private AlertDialog alertDialog;
	private MyBroadcastReceiver myBroadcastReceiver = null;

	private List<Bundle> msgBundles = new ArrayList<Bundle>();
	private int msgCount = 1;
	private int msgNumber = 1;

	private GestureDetector mGestureDetector;
	private int verticalMinDistance = 20;
	private int minVelocity = 0;

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

		// 显示消息
		showMessage();

		super.onCreate(savedInstanceState);

		// 准备与后台服务通信
		myBroadcastReceiver = new MyBroadcastReceiver();
		try {
			unregisterReceiver(myBroadcastReceiver);
		} catch (Exception e) {
			;
		}
		//
		IntentFilter filter = new IntentFilter();
		filter.addAction(MainActivity.MY_CLASSNAME);
		registerReceiver(myBroadcastReceiver, filter);
	}

	@Override
	protected void onDestroy() {
		// this is very important here ;)
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

		if (e1.getX() - e2.getX() > verticalMinDistance
				&& Math.abs(velocityX) > minVelocity) {
			if (msgNumber > 1) {
				msgNumber--;
				updateNotifyView(msgBundles.get(msgNumber - 1), true);
			} else {
				Toast.makeText(this, R.string.msg_first, Toast.LENGTH_SHORT)
						.show();
			}
		} else if (e2.getX() - e1.getX() > verticalMinDistance
				&& Math.abs(velocityX) > minVelocity) {
			if (msgNumber < msgBundles.size()) {
				msgNumber++;
				updateNotifyView(msgBundles.get(msgNumber - 1), true);
			} else {
				Toast.makeText(this, R.string.msg_last, Toast.LENGTH_SHORT)
						.show();
			}
		}

		return false;
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		return false;
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

	private void updateNotifyView(Bundle msgBundle, boolean updateMessage) {
		// 消息编号
		TextView msgIndex = (TextView) notifyView.findViewById(R.id.msgIndex);
		if (msgCount > 1)
			msgIndex.setText(String.format("%d/%d: ", msgNumber, msgCount));

		// 允许只更新消息编号
		if (!updateMessage)
			return;

		// 窗口背景
		if (msgBundle.getBoolean("alert"))
			notifyView.setBackgroundResource(R.drawable.alert_bg);
		else
			notifyView.setBackgroundResource(R.drawable.info_bg);
		// 消息标题
		TextView msgTitle = (TextView) notifyView.findViewById(R.id.msgTitle);
		if (msgBundle.containsKey("title"))
			msgTitle.setText(msgBundle.getString("title"));
		else if (msgBundle.containsKey("sender"))
			msgTitle.setText(msgBundle.getString("sender")+": ");
		else
			msgTitle.setText(R.string.msg_notitle);
		if (msgBundle.containsKey("url")) {
			final String url = msgBundle.getString("url");
			if (url!=null && !url.equals("")) {
				msgTitle.setClickable(true);
				msgTitle.setTextColor(Color.BLUE);
				msgTitle.setOnClickListener(new View.OnClickListener() {
	
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
		} else {
			msgTitle.setClickable(false);
			msgTitle.setTextColor(Color.BLACK); // TODO 定义消息标题正常颜色常量
		}
		// 消息正文
		TextView msgBody = (TextView) notifyView.findViewById(R.id.msgBody);
		msgBody.setText(msgBundle.getString("body"));
		// 图片
		final ImageView msgAttachment = (ImageView) notifyView
				.findViewById(R.id.msgAttachment);
		Bitmap bitmap;
		if (msgBundle.getBoolean("showPic")) {
			bitmap = MyApplicationClass.savedImages.get(msgBundle
					.getString("picUrl"));
		} else {
			bitmap = BitmapFactory.decodeResource(MessageDialog.this.getResources(), R.drawable.avatar);
		}
		msgAttachment.setImageBitmap(bitmap);
	}
}