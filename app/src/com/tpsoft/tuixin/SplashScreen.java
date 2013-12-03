package com.tpsoft.tuixin;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.tpsoft.pushnotification.client.PushNotificationClient;

public class SplashScreen extends Activity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		setContentView(R.layout.splashscreen);

		// Display the current version number
		PackageManager pm = getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo("com.tpsoft.tuixin", 0);
			TextView versionNumber = (TextView) findViewById(R.id.app_version);
			versionNumber.setText(pi.versionName + " 版");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// 加载用户设置
		MyApplicationClass myApp = (MyApplicationClass) getApplication();
		myApp.loadUserSettings();

		if (MyApplicationClass.clientLogon) {

			/* Create an Intent that will start the Login Activity. */
			Intent mainIntent = new Intent(SplashScreen.this,
					MainActivity.class);
			// mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			SplashScreen.this.startActivity(mainIntent);
		} else {

			// 实例化客户端(允许显示状态栏通知图标)
			PushNotificationClient client = new PushNotificationClient(
					this.getApplicationContext(), MainActivity.MY_CLASSNAME,
					R.drawable.ic_launcher,
					getText(R.string.notification_title).toString(), getText(
							R.string.notification_message).toString());
			client.release(false);

			/* Create an Intent that will start the Login Activity. */
			Intent loginIntent = new Intent(SplashScreen.this,
					LoginActivity.class);
			// loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			SplashScreen.this.startActivity(loginIntent);
		}
		SplashScreen.this.finish();

	}
}
