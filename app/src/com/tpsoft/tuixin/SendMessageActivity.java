package com.tpsoft.tuixin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.tuixin.utils.HttpUtils;
import com.tpsoft.tuixin.utils.ImageUtils;

public class SendMessageActivity extends Activity {

	/* 用来标识请求联系人的activity */
	public static final int CONTACTS_PICKED_WITH_DATA = 1;

	private static List<String> latestReceivers = new ArrayList<String>();

	private ActionBar actionBar;
	private EditText receiverView, msgBodyView;
	private ImageView receiversView;
	private ImageView photoView;
	private Bitmap msgPhoto;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_message);

		actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setTitle(R.string.msg_send);
		actionBar.setHomeAction(new ReturnAction());
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.addAction(new SendAction());

		receiverView = (EditText) findViewById(R.id.msgReceiver);
		receiversView = (ImageView) findViewById(R.id.msgReceivers);
		msgBodyView = (EditText) findViewById(R.id.msgContent);
		photoView = (ImageView) findViewById(R.id.msgPhoto);

		if (getIntent().hasExtra("receiver")) {
			receiverView.setText(getIntent().getStringExtra("receiver"));
			msgBodyView.requestFocus();
		}
		if (getIntent().hasExtra("message")) {
			msgBodyView.setText(getIntent().getStringExtra("message"));
		}

		receiverView.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {

				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				// BoD con't: CONTENT_TYPE instead of CONTENT_ITEM_TYPE
				intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
				startActivityForResult(intent, CONTACTS_PICKED_WITH_DATA);
				return false;
			}
		});
		receiversView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (latestReceivers.size() == 0) {
					Toast.makeText(SendMessageActivity.this,
							R.string.no_receivers, Toast.LENGTH_SHORT).show();
					return;
				}

				new AlertDialog.Builder(SendMessageActivity.this)
						.setTitle(R.string.msg_receivers)
						.setSingleChoiceItems(
								latestReceivers.toArray(new String[0]), -1,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										receiverView.setText(latestReceivers
												.get(which));
										dialog.dismiss();
									}

								}).show();
			}
		});
		photoView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ImageUtils.doPickPhotoAction(SendMessageActivity.this);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ImageUtils.PHOTO_PICKED_WITH_DATA: {// 调用Gallery返回的
			if (resultCode == Activity.RESULT_OK) {
				msgPhoto = data.getParcelableExtra("data");
				photoView.setImageBitmap(msgPhoto);
			}
			break;
		}
		case ImageUtils.CAMERA_WITH_DATA: // 照相机程序返回的,再次调用图片剪辑程序去修剪图片
			if (resultCode == Activity.RESULT_OK) {
				msgPhoto = data.getParcelableExtra("data");
				photoView.setImageBitmap(msgPhoto);
			}
			break;
		case CONTACTS_PICKED_WITH_DATA:
			if (data == null)
				return;

			Uri uri = data.getData();

			if (uri != null) {
				Cursor c = null;
				try {
					c = getContentResolver()
							.query(uri,
									new String[] {
											ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
											ContactsContract.CommonDataKinds.Phone.NUMBER },
									null, null, null);

					if (c != null && c.moveToFirst()) {
						String name = c.getString(0);
						String number = c.getString(1);
						receiverView.setText(name + "<" + number + ">");
					}
				} finally {
					if (c != null) {
						c.close();
					}
				}
			}
			break;
		}
	}

	@SuppressWarnings("unused")
	private void doCropPhoto(File f) {
		try {
			// 启动gallery去剪辑这个照片
			Intent intent = getCropImageIntent(Uri.fromFile(f));
			startActivityForResult(intent, ImageUtils.PHOTO_PICKED_WITH_DATA);
		} catch (Exception e) {
			Toast.makeText(this, R.string.photoPickerNotFound,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Constructs an intent for image cropping. 调用图片剪辑程序
	 */
	private static Intent getCropImageIntent(Uri photoUri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(photoUri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", 80);
		intent.putExtra("outputY", 80);
		intent.putExtra("return-data", true);
		return intent;
	}

	private class SendAction extends AbstractAction {

		public SendAction() {
			super(R.drawable.send_message);
		}

		@Override
		public void performAction(View view) {
			String receiver = receiverView.getText().toString().trim();
			if (receiver.matches("\\w+<.+?>")) {
				receiver = receiver.substring(receiver.indexOf("<") + 1,
						receiver.indexOf(">"));
			}
			if ("".equals(receiver)) {
				Toast.makeText(SendMessageActivity.this,
						R.string.error_noreceiver, Toast.LENGTH_SHORT).show();
				return;
			}
			String content = msgBodyView.getText().toString().trim();
			if ("".equals(content)) {
				Toast.makeText(SendMessageActivity.this,
						R.string.error_nocontent, Toast.LENGTH_SHORT).show();
				return;
			}

			for (int i = 0; i < latestReceivers.size(); i++) {
				if (latestReceivers.get(i).equals(receiver)) {
					latestReceivers.remove(i);
					break;
				}
			}
			latestReceivers.add(receiver);

			if (!MyApplicationClass.clientLogon) {
				Dialog alertDialog = new AlertDialog.Builder(
						SendMessageActivity.this)
						.setTitle("提示")
						.setMessage("因尚未登录，无法发送消息！")
						.setIcon(R.drawable.ic_launcher)
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								})
						.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										finish();
									}
								}).create();
				alertDialog.show();
				return;
			}

			final MyMessage msg = new MyMessage();
			msg.setSender("me");
			msg.setReceiver(receiver);
			msg.setBody(content);
			msg.setGenerateTime(new Date());

			final int msgId = MyApplicationClass.nextMsgId++;
			final Intent i = new Intent();
			i.setAction(MainActivity.MESSAGE_SEND_CLASSNAME);
			i.putExtra("action", "send");
			i.putExtra("msgId", msgId);

			if (msgPhoto == null) {
				i.putExtra("message", msg.getBundle());
				sendBroadcast(i);

				finish();
				return;
			}

			// 上传图片(不能在主线程中调用)
			Toast.makeText(SendMessageActivity.this, R.string.photo_uploading,
					Toast.LENGTH_LONG).show();
			new Thread() {
				public void run() {
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						msgPhoto.compress(Bitmap.CompressFormat.PNG, 100, baos);
						ByteArrayInputStream bais = new ByteArrayInputStream(
								baos.toByteArray());
						baos.close();

						String downloadUrl = HttpUtils.uploadFile("image/png",
								bais, "message_photo", "message_photo.png");
						if (downloadUrl != null) {
							msg.setAttachments(new MyMessage.Attachment[] { new MyMessage.Attachment(
									"图片", "image/png", "图片.png", downloadUrl) });
						}
						bais.close();

						MyApplicationClass.saveImage(downloadUrl, msgPhoto);
						i.putExtra("photo", msgPhoto);
						i.putExtra("message", msg.getBundle());
					} catch (Exception e) {
						i.putExtra(
								"errmsg",
								e.getMessage() == null ? "IOException" : e
										.getMessage());
					}

					sendBroadcast(i);

					finish();
				}
			}.start();
		}
	}

	private class ReturnAction extends AbstractAction {

		public ReturnAction() {
			super(R.drawable.app_logo);
		}

		@Override
		public void performAction(View view) {
			finish();
		}
	}
}
