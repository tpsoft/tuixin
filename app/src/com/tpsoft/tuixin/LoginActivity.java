package com.tpsoft.tuixin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.tpsoft.pushnotification.client.MessageTransceiverListener;
import com.tpsoft.pushnotification.client.PushNotificationClient;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.model.PublicAccount;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity implements
		MessageTransceiverListener {

	public static final String TAG_LOGINLOG = "Login";

	// Values for user id and password at the time of the login attempt.
	private String mUserId;
	private String mPassword;

	// UI references.
	private EditText mUserIdView;
	private EditText mPasswordView;
	private CheckBox mAutoLoginView;
	private Button mLoginButton;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	private TextView mLoginErrorMessageView;

	private PushNotificationClient mClient;

	private InputMethodManager imm;
	private SharedPreferences preferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		// 实例化客户端
		mClient = new PushNotificationClient(this);
		mClient.addListener(this);

		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		preferences = PreferenceManager
				.getDefaultSharedPreferences(LoginActivity.this);

		// Set up the login form.
		mUserId = MyApplicationClass.userSettings.getClientId();
		mUserIdView = (EditText) findViewById(R.id.user_id);
		mUserIdView.setText(mUserId);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});
		if (!"".equals(mUserId)) {
			mPasswordView.requestFocus();
		}

		mAutoLoginView = (CheckBox) findViewById(R.id.autoLogin);
		mAutoLoginView
				.setChecked(MyApplicationClass.userSettings.isAutoLogin());
		if (mAutoLoginView.isChecked()) {
			mPasswordView.setText(MyApplicationClass.userSettings
					.getClientPassword());
		}

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
		mLoginErrorMessageView = (TextView) findViewById(R.id.login_error_message);

		mLoginButton = (Button) findViewById(R.id.sign_in_button);
		mLoginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				imm.hideSoftInputFromWindow(mUserIdView.getWindowToken(), 0);
				imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
				mLoginErrorMessageView.setVisibility(View.GONE);
				attemptLogin();
			}
		});

		if (mAutoLoginView.isChecked()) {
			mLoginButton.performClick();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (MyApplicationClass.clientStarted) {
			menu.findItem(R.id.action_system_settings).setVisible(false);
		} else {
			menu.findItem(R.id.action_system_settings).setVisible(true);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_system_settings:
			// 打开系统设置界面
			startActivity(new Intent(this, SystemSettingsActivity.class));
			break;
		}
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			// 取消登录
			if (mLoginStatusView.getVisibility() == View.VISIBLE) {
				mClient.stopMessageTransceiver();
				return true;
			}
			break;
		}
		}
		return super.dispatchKeyEvent(event);
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid user id, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (MyApplicationClass.clientStarted) {
			return;
		}

		// Reset errors.
		mUserIdView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mUserId = mUserIdView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 1) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid user id.
		if (TextUtils.isEmpty(mUserId)) {
			mUserIdView.setError(getString(R.string.error_field_required));
			focusView = mUserIdView;
			cancel = true;
		}/*
		 * else if (!mUserId.contains("@")) {
		 * mUserIdView.setError(getString(R.string.error_invalid_id)); focusView
		 * = mUserIdView; cancel = true; }
		 */

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);

			// 获取最新设置
			AppParams appParams = new AppParams(MyApplicationClass.APP_ID,
					MyApplicationClass.APP_PASSWORD,
					MyApplicationClass.LOGIN_PROTECT_KEY);
			LoginParams loginParams = new LoginParams(
					MyApplicationClass.userSettings.getServerHost(),
					MyApplicationClass.userSettings.getServerPort(), mUserId,
					mPassword);
			NetworkParams networkParams = new NetworkParams();

			// 启动消息收发器(并登录)
			mClient.startMessageTransceiver(appParams, loginParams,
					networkParams);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	@Override
	public void onTransceiverStatus(boolean started) {
		MyApplicationClass.clientStarted = started;
		if (!started) {
			MyApplicationClass.clientLogon = false;
			showProgress(false);
		}
	}

	@Override
	public void onLogining(boolean logining) {
		showProgress(logining || !MyApplicationClass.clientLogon);
	}

	@Override
	public void onLoginStatus(int code, String text) {
		if (code < 0)
			Log.w(TAG_LOGINLOG, String.format("%s(#%d)", text, code));
		else
			Log.i(TAG_LOGINLOG, String.format("%s(#%d)", text, code));
		//
		if (code == 0) {
			MyApplicationClass.clientLogon = true;
			MyApplicationClass.userSettings.setClientId(mUserId);
			MyApplicationClass.userSettings.setClientPassword(mPassword);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("clientId", mUserId)
					.putString("clientPassword",
							mAutoLoginView.isChecked() ? mPassword : "")
					.putBoolean("autoLogin", mAutoLoginView.isChecked())
					.commit();

			/* Create an Intent that will start the Main Activity. */
			Intent mainIntent = new Intent(LoginActivity.this,
					MainActivity.class);
			// mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(mainIntent);
			finish();
		} else if (code < 0) {
			if (code == -7) { // -7代表认证失败(密码错误)
				mPasswordView
						.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
				imm.showSoftInputFromInputMethod(
						mPasswordView.getWindowToken(), 0);
			} else {
				mLoginErrorMessageView.setText(text);
				mLoginErrorMessageView.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onMessageSendStatus(int msgId, int code, String text) {
	}

	@Override
	public void onNewMessageReceived(MyMessage msg) {
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
}
