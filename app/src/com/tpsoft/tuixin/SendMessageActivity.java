package com.tpsoft.tuixin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar.AbstractAction;

import com.markupartist.android.widget.ActionBar;
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
		msgBodyView = (EditText) findViewById(R.id.msgContent);
	}

    private class SendAction extends AbstractAction {

        public SendAction() {
            super(R.drawable.send);
        }

        @Override
        public void performAction(View view) {
        	String receiver = receiverView.getText().toString().trim();
        	if ("".equals(receiver)) {
        		Toast.makeText(SendMessageActivity.this, R.string.error_noreceiver, Toast.LENGTH_SHORT).show();
        		return;
        	}
        	String content = msgBodyView.getText().toString().trim();
        	if ("".equals(content)) {
        		Toast.makeText(SendMessageActivity.this, R.string.error_nocontent, Toast.LENGTH_SHORT).show();
        		return;
        	}

        	MyMessage msg = new MyMessage();
    		msg.setBody(content);

    		Intent i = new Intent();
    		i.setAction("com.tpsoft.pushnotification.ServiceController");
    		i.putExtra("command", "send");
    		i.putExtra("msgId", Integer.toString(nextMsgId++));
    		i.putExtra("receiver", receiver);
    		// i.putExtra("secure", false);
    		i.putExtra("com.tpsoft.pushnotification.MyMessage", msg.getBundle());
    		sendBroadcast(i);

    		finish();
        }

    }
}
