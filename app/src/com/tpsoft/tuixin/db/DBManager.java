package com.tpsoft.tuixin.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.MyMessage.Attachment;
import com.tpsoft.tuixin.model.MyMessageSupportSave;

public class DBManager {

	private final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyyMMddHHmmss", Locale.getDefault());

	private DBHelper helper;
	private SQLiteDatabase db;

	public DBManager(Context context) {
		helper = new DBHelper(context);
		// 因为getWritableDatabase内部调用了mContext.openOrCreateDatabase(mName, 0,
		// mFactory);
		// 所以要确保context已初始化,我们可以把实例化DBManager的步骤放在Activity的onCreate里
		db = helper.getWritableDatabase();
	}

	/**
	 * add message
	 * 
	 * @param message
	 * @return long
	 */
	public long addMessage(MyMessage message) {
		db.beginTransaction(); // 开始事务
		try {
			ContentValues cv = new ContentValues();
			cv.put("sender", message.getSender());
			cv.put("receiver", message.getReceiver());
			cv.put("title", message.getTitle());
			cv.put("body", message.getBody());
			cv.put("type", message.getType());
			cv.put("url", message.getUrl());
			cv.put("generateTime",
					(message.getGenerateTime() != null ? dateFormat
							.format(message.getGenerateTime()) : ""));
			cv.put("expiration",
					(message.getExpiration() != null ? dateFormat
							.format(message.getExpiration()) : ""));
			long messageId = db.insert("message", null, cv);
			//
			for (int i = 0; i < message.getAttachmentCount(); i++) {
				Attachment attachment = message.getAttachment(i);
				cv.clear();
				cv.put("title", attachment.getTitle());
				cv.put("type", attachment.getType());
				cv.put("filename", attachment.getFilename());
				cv.put("url", attachment.getUrl());
				cv.put("messageId", messageId);
				db.insert("attachment", null, cv);
			}
			db.setTransactionSuccessful(); // 设置事务成功完成
			return messageId;
		} finally {
			db.endTransaction(); // 结束事务
		}
	}

	/**
	 * delete message
	 * 
	 * @param id
	 */
	public void deleteMessage(long id) {
		db.delete("attachment", "messageId = ?",
				new String[] { Long.toString(id) });
		db.delete("message", "_id = ?", new String[] { Long.toString(id) });
	}

	/**
	 * delete old messages
	 * 
	 * @param before
	 *            null for all
	 */
	public void deleteOldMessages(Date before) {
		db.beginTransaction(); // 开始事务
		try {
			if (before != null)
				db.delete("message", "generateTime <= ?",
						new String[] { dateFormat.format(before) });
			else
				db.delete("message", null, null);
			db.setTransactionSuccessful(); // 设置事务成功完成
		} finally {
			db.endTransaction(); // 结束事务
		}
	}

	/**
	 * query messages, return list
	 * 
	 * @param after
	 *            null for all
	 * @param maxRecords
	 *            -1 for all
	 * @return List<MyMessageSupportSave>
	 */
	public List<MyMessageSupportSave> queryMessages(Date after, int maxRecords) {
		ArrayList<MyMessageSupportSave> messages = new ArrayList<MyMessageSupportSave>();
		Cursor c = (after != null ? db.rawQuery(
				"SELECT * FROM message WHERE generateTime>=? ORDER BY generateTime DESC"
						+ (maxRecords > 0 ? " LIMIT 0," + maxRecords : ""),
				new String[] { dateFormat.format(after) }) : db.rawQuery(
				"SELECT * FROM message ORDER BY generateTime DESC"
						+ (maxRecords > 0 ? " LIMIT 0," + maxRecords : ""),
				null));
		while (c.moveToNext()) {
			MyMessageSupportSave message = new MyMessageSupportSave();
			message.setRecordId(c.getLong(c.getColumnIndex("_id"))); // record id, not msgid
			message.setSender(c.getString(c.getColumnIndex("sender")));
			message.setReceiver(c.getString(c.getColumnIndex("receiver")));
			message.setTitle(c.getString(c.getColumnIndex("title")));
			message.setBody(c.getString(c.getColumnIndex("body")));
			message.setType(c.getString(c.getColumnIndex("type")));
			message.setUrl(c.getString(c.getColumnIndex("url")));
			String generateTime = c.getString(c.getColumnIndex("generateTime"));
			if (generateTime != null && !generateTime.equals("")) {
				try {
					message.setGenerateTime(dateFormat.parse(generateTime));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			String expiration = c.getString(c.getColumnIndex("expiration"));
			if (expiration != null && !expiration.equals("")) {
				try {
					message.setExpiration(dateFormat.parse(expiration));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			messages.add(message);
		}
		c.close();
		//
		for (MyMessageSupportSave message : messages) {
			c = db.rawQuery("SELECT * FROM attachment WHERE messageId=?",
					new String[] { Long.toString(message.getRecordId()) });
			List<Attachment> attachments = new ArrayList<Attachment>();
			while (c.moveToNext()) {
				Attachment attachment = new Attachment();
				attachment.setTitle(c.getString(c.getColumnIndex("title")));
				attachment.setType(c.getString(c.getColumnIndex("type")));
				attachment
						.setFilename(c.getString(c.getColumnIndex("filename")));
				attachment.setUrl(c.getString(c.getColumnIndex("url")));
				attachments.add(attachment);
			}
			message.setAttachments(attachments.toArray(new Attachment[0]));
		}
		return messages;
	}

	/**
	 * save attachment content
	 * 
	 * @param url
	 * @param data
	 * @return long
	 */
	public long saveAttachmentContent(String url, byte[] data) {
		db.beginTransaction(); // 开始事务
		try {
			ContentValues cv = new ContentValues();
			cv.put("url", url);
			cv.put("data", data);
			cv.put("time", dateFormat.format(new Date()));
			long id = db.insert("attachment_content", null, cv);
			db.setTransactionSuccessful(); // 设置事务成功完成
			return id;
		} finally {
			db.endTransaction(); // 结束事务
		}
	}

	/**
	 * load attachment content
	 * 
	 * @param url
	 * @return byte[]
	 */
	public byte[] loadAttachmentContent(String url) {
		byte[] data = null;
		Cursor c = db.rawQuery("SELECT * FROM attachment_content WHERE url=?",
				new String[] { url });
		if (c.moveToNext()) {
			data = c.getBlob(c.getColumnIndex("data"));
		}
		c.close();
		return data;
	}

	/**
	 * delete attachment content
	 * 
	 * @param message
	 * @return long
	 */
	public void deleteAttachmentContent(String url) {
		db.execSQL("DELETE FROM attachment_content WHERE url=?",
				new String[] { url });
	}

	/**
	 * delete old attachments content
	 * 
	 * @param before
	 *            null for all
	 */
	public void deleteOldAttachmentsContent(Date before) {
		db.beginTransaction(); // 开始事务
		try {
			if (before != null)
				db.delete("attachment_content", "time <= ?",
						new String[] { dateFormat.format(before) });
			else
				db.delete("attachment_content", null, null);
			db.setTransactionSuccessful(); // 设置事务成功完成
		} finally {
			db.endTransaction(); // 结束事务
		}
	}

	/**
	 * close database
	 */
	public void closeDB() {
		db.close();
	}
}
