package com.tpsoft.tuixin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.tpsoft.pushnotification.model.MyMessage;

public class SendMessageActivity extends Activity {

	private static List<String> latestReceivers = new ArrayList<String>();

	private EditText receiverView, msgBodyView;
	private ImageView receiversView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_message);

		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setTitle(R.string.msg_send);
		actionBar.setHomeAction(new IntentAction(this, MainActivity
				.createIntent(this), R.drawable.send_message));
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.addAction(new SendAction());

		receiverView = (EditText) findViewById(R.id.msgReceiver);
		receiversView = (ImageView) findViewById(R.id.msgReceivers);
		msgBodyView = (EditText) findViewById(R.id.msgContent);

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
				startActivityForResult(intent, 1);
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
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
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
		}
	}

	private class SendAction extends AbstractAction {

		public SendAction() {
			super(R.drawable.send);
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

			MyMessage msg = new MyMessage();
			msg.setSender("me");
			msg.setReceiver(receiver);
			msg.setBody(content);
			msg.setGenerateTime(new Date());

			int msgId = MyApplicationClass.nextMsgId++;

			Intent i = new Intent();
			i.setAction(MainActivity.MESSAGE_SEND_CLASSNAME);
			i.putExtra("action", "send");
			i.putExtra("msgId", msgId);
			i.putExtra("message", msg.getBundle());
			sendBroadcast(i);

			finish();
		}
	}
}


