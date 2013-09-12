package com.tpsoft.tuixin;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.tpsoft.pushnotification.model.MyMessage;

public class SendMessageActivity extends Activity {

	private static int nextMsgId = 1;

	private EditText receiverView, msgBodyView;

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
		if (getIntent().hasExtra("receiver")) {
			receiverView.setText(getIntent().getStringExtra("receiver"));
		}
		msgBodyView = (EditText) findViewById(R.id.msgContent);

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

			MyMessage msg = new MyMessage();
			msg.setBody(content);
			msg.setGenerateTime(new Date());

			Intent i = new Intent();
			i.setAction("com.tpsoft.pushnotification.ServiceController");
			i.putExtra("command", "send");
			i.putExtra("msgId", Integer.toString(nextMsgId++));
			i.putExtra("receiver", receiver);
			// i.putExtra("secure", false);
			i.putExtra("com.tpsoft.pushnotification.MyMessage", msg.getBundle());
			sendBroadcast(i);

			Intent i2 = new Intent();
			i2.setAction(MainActivity.MESSAGE_SEND_CLASSNAME);
			i2.putExtra("action", "sent");
			msg.setSender("��");
			i2.putExtra("message", msg.getBundle());
			sendBroadcast(i2);

			finish();
		}
	}
}