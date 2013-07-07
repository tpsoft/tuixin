package com.tpsoft.tuixin;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.TextView;
import android.widget.Toast;

import com.tpsoft.tuixin.R;

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
						// ���ֵڶ�����Ϣʱ��������
						mGestureDetector = new GestureDetector(
								(OnGestureListener) MessageDialog.this);
						LinearLayout popupLayout = (LinearLayout) notifyView
								.findViewById(R.id.message);
						popupLayout.setOnTouchListener(MessageDialog.this);
						popupLayout.setLongClickable(true);
					}
				}
			}
		}
	}

	private static final int POPUP_INFO_TIME = 1000 * 30;
	private static final int POPUP_ALERT_TIME = 1000 * 40;

	private View notifyView;
	private Timer autoCloseTimer;
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

		// ���ô��ڱ���͸��
		setTheme(R.style.Transparent);
		setContentView(R.layout.transparent);

		// ��ʾ��Ϣ
		showMessage();

		super.onCreate(savedInstanceState);

		// ׼�����̨����ͨ��
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

		// ׼����������
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		notifyView = inflater.inflate(R.layout.notification_popup,
				(ViewGroup) findViewById(R.id.message));
		updateNotifyView(msgBundle, true);

		// ��ʾ��Ϣ�Ի���
		showAlertDialog(msgBundle);
	}

	private void showAlertDialog(Bundle msgBundle) {
		AlertDialog.Builder builder;
		final AlertDialog alertDialog;
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

		autoCloseTimer = new Timer();
		autoCloseTimer.schedule(new TimerTask() {
			public void run() {
				closeAlertDialog(alertDialog, autoCloseTimer);
			}

		}, msgBundle.getBoolean("alert") ? POPUP_ALERT_TIME : POPUP_INFO_TIME);

		ImageButton closeButton = (ImageButton) notifyView
				.findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				closeAlertDialog(alertDialog, autoCloseTimer);
			}
		});

		alertDialog.show();
	}

	private void closeAlertDialog(AlertDialog alertDialog, Timer t) {
		// �ر���Ϣ�Ի���
		alertDialog.dismiss(); // when the task is active then close the dialog

		// ȡ����ʱ��
		t.cancel(); // also just top the timer thread, otherwise, you may
					// receive a crash report
		// ֪ͨ���÷���ǰ��ѹر�
		Intent i = new Intent();
		i.setAction(MainActivity.MESSAGE_DIALOG_CLASSNAME);
		i.putExtra("action", "popupClosed");
		sendBroadcast(i);

		// ����ԭ����
		finish();
	}

	private void updateNotifyView(Bundle msgBundle, boolean updateMessage) {
		// ��Ϣ���
		TextView msgIndex = (TextView) notifyView.findViewById(R.id.msgIndex);
		if (msgCount > 1)
			msgIndex.setText(String.format("%d/%d: ", msgNumber, msgCount));

		// ����ֻ������Ϣ���
		if (!updateMessage)
			return;

		// ���ڱ���
		if (msgBundle.getBoolean("alert"))
			notifyView.setBackgroundResource(R.drawable.alert_bg);
		else
			notifyView.setBackgroundResource(R.drawable.info_bg);
		// ��Ϣ����
		TextView msgTitle = (TextView) notifyView.findViewById(R.id.msgTitle);
		if (msgBundle.containsKey("title"))
			msgTitle.setText(msgBundle.getString("title"));
		else
			msgTitle.setText("");
		// ��Ϣ����
		TextView msgBody = (TextView) notifyView.findViewById(R.id.msgBody);
		msgBody.setText(msgBundle.getString("body"));
		// ����URL
		TextView msgUrl = (TextView) notifyView.findViewById(R.id.msgUrl);
		if (msgBundle.containsKey("url"))
			msgUrl.setText(msgBundle.getString("url"));
		else
			msgUrl.setText("");
		// ͼƬ
		final ImageView msgAttachment = (ImageView) notifyView
				.findViewById(R.id.msgAttachment);
		Bitmap bitmap = null;
		if (msgBundle.getBoolean("showPic")) {
			String imageFilepath = msgBundle.getString("imageFilepath");
			try {
				FileInputStream fis = new FileInputStream(imageFilepath);
				bitmap = BitmapFactory.decodeStream(fis);
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		msgAttachment.setImageBitmap(bitmap);
	}
}