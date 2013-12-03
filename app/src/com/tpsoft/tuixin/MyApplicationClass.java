package com.tpsoft.tuixin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.tpsoft.tuixin.db.DBManager;
import com.tpsoft.tuixin.model.MyMessageSupportSave;
import com.tpsoft.tuixin.model.MySettings;
import com.tpsoft.tuixin.utils.PlaySoundPool;

public class MyApplicationClass extends Application {

	public static final String APP_ID = "4083AD3D-0F41-B78E-4F5D-F41A515F2667"; //"5A6D032A-DB6C-43BF-98EE-A699FBCAA628"
	public static final String APP_PASSWORD = "@0Vd*4Ak"; //"THHgux8k"
	public static final String LOGIN_PROTECT_KEY = "n9SfmcRs"; //"cpuHCK9V"

	public static final boolean ALERT_MSG = false;
	public static final int INFO_SOUND = 1;
	public static final int ALERT_SOUND = 2;
	
	public static final boolean receiveOnly = false;
	// //////////////////////////////////////

	public static PlaySoundPool playSoundPool;
	public static MySettings userSettings;

	public static boolean clientStarted = false;
	public static boolean clientLogon = false;
	public static List<MyMessageSupportSave> latestMsgs = new ArrayList<MyMessageSupportSave>(); // 已有消息(按生成时间降序)
	public static int nextMsgId = 0;

	public static DBManager db;

	public static void saveImage(String url, Bitmap image) {
		if (image != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			image.compress(Bitmap.CompressFormat.PNG, 100, baos);
			db.saveAttachmentContent(url, baos.toByteArray());
		}
		//
		savedImages.put(url, image);
	}

	public static boolean existsImage(String url) {
		if (savedImages.containsKey(url))
			return true;
		byte[] bytes = db.loadAttachmentContent(url);
		if (bytes != null) {
			savedImages.put(url,
					BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
			return true;
		}
		return false;
	}

	public static Bitmap loadImage(String url) {
		if (savedImages.containsKey(url))
			return savedImages.get(url);
		byte[] bytes = db.loadAttachmentContent(url);
		if (bytes != null) {
			Bitmap image = BitmapFactory
					.decodeByteArray(bytes, 0, bytes.length);
			savedImages.put(url, image);
			return image;
		}
		return null;
	}

	private static Map<String, Bitmap> savedImages = new HashMap<String, Bitmap>();

	@Override
	public void onCreate() {
		super.onCreate();

		// 准备音效
		playSoundPool = new PlaySoundPool(this);
		playSoundPool.loadSfx(R.raw.info, INFO_SOUND);
		playSoundPool.loadSfx(R.raw.alert, ALERT_SOUND);

		// 装入用户设置
		loadUserSettings();
	}

	public void loadUserSettings() {
		userSettings = new MySettings(this);
	}

}
