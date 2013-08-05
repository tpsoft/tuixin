package com.tpsoft.tuixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.service.NotifyPushService;
import com.tpsoft.tuixin.model.UserSettings;
import com.tpsoft.tuixin.utils.PlaySoundPool;

public class MyApplicationClass extends Application {

	public static final String APP_ID = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
	public static final String APP_PASSWORD = "@0Vd*4Ak";
	public static final String LOGIN_PROTECT_KEY = "n9SfmcRs";

	public static final boolean ALERT_MSG = false;
	public static final int INFO_SOUND = 1;
	public static final int ALERT_SOUND = 2;
	// //////////////////////////////////////

	public static PlaySoundPool playSoundPool;
	public static UserSettings userSettings;

	public static boolean mExternalStorageAvailable = false;
	public static boolean mExternalStorageWriteable = false;

	public static boolean clientStarted = false;
	public static List<MyMessage> savedMsgs = new ArrayList<MyMessage>();
	public static Map<String, String> savedImages = new HashMap<String, String>();

	private BroadcastReceiver mExternalStorageReceiver;

	@Override
	public void onCreate() {
		super.onCreate();

		// 准备音效
		playSoundPool = new PlaySoundPool(this);
		playSoundPool.loadSfx(R.raw.info, INFO_SOUND);
		playSoundPool.loadSfx(R.raw.alert, ALERT_SOUND);

		// 装入用户设置
		loadUserSettings();

		// 开始监视外部存储器状态
		startWatchingExternalStorage();

		// 启动后台服务
		Intent intent = new Intent(this, NotifyPushService.class);
		intent.putExtra("ActivityClassName", MainActivity.MY_CLASSNAME);
		intent.putExtra("notification_logo", R.drawable.ic_launcher);
		intent.putExtra("notification_title",
				getText(R.string.notification_title).toString());
		intent.putExtra("notification_message",
				getText(R.string.notification_message).toString());
		startService(intent);

	}

	@Override
	public void onTerminate() {
		stopWatchingExternalStorage();

		super.onTerminate();
	}

	public void loadUserSettings() {
		userSettings = new UserSettings(this);
	}

	private void startWatchingExternalStorage() {
		mExternalStorageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateExternalStorageState();
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		registerReceiver(mExternalStorageReceiver, filter);
		updateExternalStorageState();
	}

	private void stopWatchingExternalStorage() {
		unregisterReceiver(mExternalStorageReceiver);
	}

	private void updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}
}
