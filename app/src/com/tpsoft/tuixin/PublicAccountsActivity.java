package com.tpsoft.tuixin;

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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.tpsoft.pushnotification.client.MessageTransceiverListener;
import com.tpsoft.pushnotification.client.PushNotificationClient;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.PublicAccount;

public class PublicAccountsActivity extends Activity implements
		MessageTransceiverListener {

	private static final int ALL_PUBLIC_ACCOUNTS_QUERY_ID = 10000; // 查询所有公众号请求ID
	private static final String ALL_PUBLIC_ACCOUNTS_QUERY_CONDITION = ""; // 查询所有公众号条件

	private PushNotificationClient mClient;
	private LinearLayout accountsLayout;
	private Bitmap followFlag;

	private Map<String, View> listItemViews = new HashMap<String, View>();
	private Map<String, Bitmap> accountAvatars = new HashMap<String, Bitmap>();
	private Set<String> followedAccounts = new HashSet<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.public_accounts);

		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setTitle(R.string.public_accounts_settings);
		actionBar.setHomeAction(new IntentAction(this, MainActivity
				.createIntent(this), R.drawable.public_account));
		actionBar.setDisplayHomeAsUpEnabled(true);

		// 初始化控件
		accountsLayout = (LinearLayout) findViewById(R.id.accounts);

		// 实例化客户端
		mClient = new PushNotificationClient(this);
		mClient.addListener(this);

		// 初始化关注标志
		followFlag = BitmapFactory
				.decodeResource(PublicAccountsActivity.this.getResources(),
						R.drawable.favorite);

		// 发送(所有)公众号查询请求
		TextView tipText = new TextView(this);
		tipText.setText(R.string.public_accounts_loading);
		accountsLayout.addView(tipText, 0); // 提示信息
		mClient.queryPublic(ALL_PUBLIC_ACCOUNTS_QUERY_ID,
				ALL_PUBLIC_ACCOUNTS_QUERY_CONDITION);
	}

	private void showPublicAccounts(List<PublicAccount> accounts) {
		accountsLayout.removeAllViews(); // 删除原来的列表
		for (int i = 0; i < accounts.size(); i++) {
			PublicAccount account = accounts.get(i);
			//
			Bitmap accountAvatar = MyApplicationClass.loadImage(account
					.getAvatar());
			if (accountAvatar == null) {
				accountAvatar = BitmapFactory.decodeResource(
						PublicAccountsActivity.this.getResources(),
						R.drawable.avatar);
			}
			if (i != accounts.size() - 1) {
				// 加消息分隔条
				accountsLayout.addView(makeAccountSepView());
			}
			View listItemView = makeAccountView(account, accountAvatar);
			accountsLayout.addView(listItemView);
			//
			listItemViews.put(account.getName(), listItemView);
		}
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
				// 关注/取消关注
				String accountName = account.getName();
				//
				if (followedAccounts.contains(accountName)) {
					// 取消关注
					mClient.unfollowPublic(accountName);
				} else {
					// 关注
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
		accountDescView.setText("".equals(account.getDesc()) ? "(无简介)"
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
		canvas.drawBitmap(
				followFlag,
				null,
				new Rect(0, 0, accountAvatar.getWidth(), accountAvatar
						.getHeight()), null);
		//
		ImageView accountAvatarView = (ImageView) listItemView
				.findViewById(R.id.accountAvatar);
		accountAvatarView.setImageBitmap(bitmap);
	}

	private String makeTypeString(int accountType) {
		switch (accountType) {
		case 1:
			return "资讯";
		case 2:
			return "教育";
		case 3:
			return "娱乐";
		case 4:
			return "游戏";
		default:
			return "其它";
		}
	}

	@Override
	public void onTransceiverStatus(boolean started) {
	}

	@Override
	public void onLoginStatus(boolean logining, int code, String text) {
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
				// TODO 有问题:已关注的公众号无相关资料
			} else {
				// 给公众号图标绘制关注标志
				drawFollowFlag(account.getName(), listItemView);
			}
		}
	}
}
