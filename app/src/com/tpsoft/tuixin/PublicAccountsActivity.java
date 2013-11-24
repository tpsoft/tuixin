package com.tpsoft.tuixin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
import com.tpsoft.pushnotification.client.MessageTransceiverListener;
import com.tpsoft.pushnotification.client.PushNotificationClient;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.PublicAccount;
import com.tpsoft.tuixin.utils.HttpUtils;

public class PublicAccountsActivity extends Activity implements
		MessageTransceiverListener {

	private static final int ALL_PUBLIC_ACCOUNTS_QUERY_ID = 10000; // ��ѯ���й��ں�����ID
	private static final String ALL_PUBLIC_ACCOUNTS_QUERY_CONDITION = ""; // ��ѯ���й��ں�����

	private PushNotificationClient mClient;
	private LinearLayout accountsLayout;
	private Bitmap followFlag;

	private ActionBar actionBar;

	private Map<String, View> listItemViews = new HashMap<String, View>();
	private Map<String, Bitmap> accountAvatars = new HashMap<String, Bitmap>();
	private Set<String> followedAccounts = new HashSet<String>();

	private class MyTask extends AsyncTask<List<PublicAccount>, Integer, Void> {
		private List<PublicAccount> accounts;

		// onPreExecute����������ִ�к�̨����ǰ��һЩUI����
		@Override
		protected void onPreExecute() {
			accountsLayout.removeAllViews(); // ɾ��ԭ�����б�
		}

		// doInBackground�����ڲ�ִ�к�̨����,�����ڴ˷������޸�UI
		@Override
		protected Void doInBackground(List<PublicAccount>... params) {
			accounts = params[0];
			for (int i = 0; i < accounts.size(); i++) {
				PublicAccount account = accounts.get(i);
				//
				if (!MyApplicationClass.existsImage(account.getAvatar())) {
					InputStream imageStream = HttpUtils
							.getInputStreamFromURL(account.getAvatar());
					Bitmap image = null;
					if (imageStream != null) {
						image = BitmapFactory.decodeStream(imageStream);
						MyApplicationClass
								.saveImage(account.getAvatar(), image);
						try {
							imageStream.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			for (int i = 0; i < accounts.size(); i++) {
				PublicAccount account = accounts.get(i);
				//
				Bitmap accountAvatar = MyApplicationClass.loadImage(account
						.getAvatar());
				if (accountAvatar == null) {
					accountAvatar = BitmapFactory.decodeResource(
							PublicAccountsActivity.this.getResources(),
							R.drawable.sender_avatar);
				}
				if (i != accounts.size() - 1) {
					// ����Ϣ�ָ���
					accountsLayout.addView(makeAccountSepView());
				}
				View listItemView = makeAccountView(account, accountAvatar);
				accountsLayout.addView(listItemView);
				//
				listItemViews.put(account.getName(), listItemView);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.public_accounts);

		actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setTitle(R.string.public_accounts_settings);
		actionBar.setHomeAction(new ReturnAction());
		actionBar.setDisplayHomeAsUpEnabled(true);

		// ��ʼ���ؼ�
		accountsLayout = (LinearLayout) findViewById(R.id.accounts);

		// ʵ�����ͻ���
		mClient = new PushNotificationClient(this);
		mClient.addListener(this);

		// ��ʼ����ע��־
		followFlag = BitmapFactory.decodeResource(
				PublicAccountsActivity.this.getResources(),
				R.drawable.favorite_message);

		// ����(����)���ںŲ�ѯ����
		TextView tipText = new TextView(this);
		tipText.setText(R.string.public_accounts_loading);
		accountsLayout.addView(tipText, 0); // ��ʾ��Ϣ
		mClient.queryPublic(ALL_PUBLIC_ACCOUNTS_QUERY_ID,
				ALL_PUBLIC_ACCOUNTS_QUERY_CONDITION);
	}

	@SuppressWarnings("unchecked")
	private void showPublicAccounts(List<PublicAccount> accounts) {
		MyTask mTask = new MyTask();
		mTask.execute(accounts);
	}

	@SuppressLint("ResourceAsColor")
	private View makeAccountView(final PublicAccount account,
			final Bitmap accountAvatar) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View listItemView = inflater.inflate(
				R.layout.public_accounts_list_item, null);
		ImageView accountAvatarView = (ImageView) listItemView
				.findViewById(R.id.accountAvatar);
		accountAvatarView.setImageBitmap(accountAvatar);
		accountAvatars.put(account.getName(), accountAvatar);
		//
		accountAvatarView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				// ��ע/ȡ����ע
				String accountName = account.getName();
				//
				if (followedAccounts.contains(accountName)) {
					// ȡ����ע
					mClient.unfollowPublic(accountName);
				} else {
					// ��ע
					mClient.followPublic(accountName);
				}
			}
		});
		//
		TextView accountNameView = (TextView) listItemView
				.findViewById(R.id.accountName);
		accountNameView.setText(account.getName());
		//
		TextView accountDescView = (TextView) listItemView
				.findViewById(R.id.accountDesc);
		accountDescView.setText("".equals(account.getDesc()) ? "(�޼��)"
				: account.getDesc());
		//
		TextView accountTypeView = (TextView) listItemView
				.findViewById(R.id.accountType);
		accountTypeView.setText(makeTypeString(account.getType()));
		return listItemView;
	}

	private View makeAccountSepView() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View sepView = inflater
				.inflate(R.layout.public_accounts_list_sep, null);
		return sepView;
	}

	private void drawFollowFlag(String accountName, View listItemView) {
		Bitmap accountAvatar = accountAvatars.get(accountName);
		Bitmap bitmap = Bitmap.createBitmap(accountAvatar.copy(
				Config.ARGB_8888, true));
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(followFlag, null, new Rect(accountAvatar.getWidth()
				- MainActivity.FAVORITE_FLAG_WIDTH, accountAvatar.getHeight()
				- MainActivity.FAVORITE_FLAG_HEIGHT, accountAvatar.getWidth(),
				accountAvatar.getHeight()), null);
		//
		ImageView accountAvatarView = (ImageView) listItemView
				.findViewById(R.id.accountAvatar);
		accountAvatarView.setImageBitmap(bitmap);
	}

	private String makeTypeString(int accountType) {
		switch (accountType) {
		case 1:
			return "��Ѷ";
		case 2:
			return "����";
		case 3:
			return "����";
		case 4:
			return "��Ϸ";
		default:
			return "����";
		}
	}

	@Override
	public void onTransceiverStatus(boolean started) {
	}

	@Override
	public void onLogining(boolean logining) {
	}

	@Override
	public void onLoginStatus(int code, String text) {
	}

	@Override
	public void onMessageSendStatus(int msgId, int code, String text) {
	}

	@Override
	public void onNewMessageReceived(MyMessage msg) {
	}

	@Override
	public void onPublicAccountsReceived(PublicAccount[] accounts) {
		showPublicAccounts(Arrays.asList(accounts));
		mClient.getFollowed();
	}

	@Override
	public void onPublicAccountFollowed(String accountName) {
		drawFollowFlag(accountName, listItemViews.get(accountName));
		followedAccounts.add(accountName);
	}

	@Override
	public void onPublicAccountUnfollowed(String accountName) {
		View listItemView = listItemViews.get(accountName);
		ImageView accountAvatarView = (ImageView) listItemView
				.findViewById(R.id.accountAvatar);
		accountAvatarView.setImageBitmap(accountAvatars.get(accountName));
		followedAccounts.remove(accountName);
	}

	@Override
	public void onFollowedAccountsReceived(PublicAccount[] accounts) {
		for (PublicAccount account : accounts) {
			followedAccounts.add(account.getName());

			View listItemView = listItemViews.get(account.getName());
			if (listItemView == null) {
				// TODO ������:�ѹ�ע�Ĺ��ں����������
			} else {
				// �����ں�ͼ����ƹ�ע��־
				drawFollowFlag(account.getName(), listItemView);
			}
		}
	}

	private class ReturnAction extends AbstractAction {

		public ReturnAction() {
			super(R.drawable.home);
		}

		@Override
		public void performAction(View view) {
			finish();
		}
	}
}
