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
	public long addMessage(MyMessage message, boolean favorite) {
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
			cv.put("favorite", favorite ? 1 : 0);
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
	 * favour message
	 * 
	 * @param id
	 * @param favorite
	 * @return boolean
	 */
	public boolean favourMessage(long id, boolean favorite) {
		int updatedRows = 0;
		db.beginTransaction(); // 开始事务
		try {
			ContentValues cv = new ContentValues();
			cv.put("favorite", favorite ? 1 : 0);
			updatedRows = db.update("message", cv, "_id = ?",
					new String[] { Long.toString(id) });
			db.setTransactionSuccessful(); // 设置事务成功完成
		} finally {
			db.endTransaction(); // 结束事务
		}
		return (updatedRows > 0);
	}

	/**
	 * hide message
	 * 
	 * @param id
	 * @param hidden
	 * @return boolean
	 */
	public boolean hideMessage(long id, boolean hidden) {
		int updatedRows = 0;
		db.beginTransaction(); // 开始事务
		try {
			ContentValues cv = new ContentValues();
			cv.put("hidden", hidden ? 1 : 0);
			updatedRows = db.update("message", cv, "_id = ?",
					new String[] { Long.toString(id) });
			db.setTransactionSuccessful(); // 设置事务成功完成
		} finally {
			db.endTransaction(); // 结束事务
		}
		return (updatedRows > 0);
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

		// TOOD 清理附件
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
			// TOOD 清理附件
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
		Cursor c = (after != null ? db
				.rawQuery(
						"SELECT * FROM message WHERE (hidden is null or hidden=0) and generateTime>=? ORDER BY _id DESC"
								+ (maxRecords > 0 ? " LIMIT 0," + maxRecords
										: ""),
						new String[] { dateFormat.format(after) })
				: db.rawQuery(
						"SELECT * FROM message WHERE (hidden is null or hidden=0) ORDER BY _id DESC"
								+ (maxRecords > 0 ? " LIMIT 0," + maxRecords
										: ""), null));
		fetchMessages(messages, c);
		c.close();
		return messages;
	}

	/**
	 * query messages, return list
	 * 
	 * @param maxRecordId
	 * @param maxRecords
	 *            -1 for all
	 * @return List<MyMessageSupportSave>
	 */
	public List<MyMessageSupportSave> queryMessages(long maxRecordId,
			int maxRecords) {
		ArrayList<MyMessageSupportSave> messages = new ArrayList<MyMessageSupportSave>();
		Cursor c = db
				.rawQuery(
						"SELECT * FROM message WHERE (hidden is null or hidden=0) and _id<=? ORDER BY _id DESC"
								+ (maxRecords > 0 ? " LIMIT 0," + maxRecords
										: ""),
						new String[] { Long.toString(maxRecordId) });
		fetchMessages(messages, c);
		c.close();
		return messages;
	}

	/**
	 * count messages by date, return integer
	 * 
	 * @param after
	 *            null for all
	 * @return int
	 */
	public int countMessages(Date after) {
		Cursor c = (after != null ? db
				.rawQuery(
						"SELECT count(*) FROM message WHERE (hidden is null or hidden=0) and generateTime>=?",
						new String[] { dateFormat.format(after) })
				: db.rawQuery(
						"SELECT count(*) FROM message WHERE (hidden is null or hidden=0)",
						null));
		c.moveToNext();
		int result = c.getInt(0);
		c.close();
		//
		return result;
	}

	/**
	 * count messages by id, return integer
	 * 
	 * @param maxRecordId
	 * @return int
	 */
	public int countMessages(long maxRecordId) {
		Cursor c = db
				.rawQuery(
						"SELECT count(*) FROM message WHERE (hidden is null or hidden=0) and _id<=?",
						new String[] { Long.toString(maxRecordId) });
		c.moveToNext();
		int result = c.getInt(0);
		c.close();
		//
		return result;
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

	private void fetchMessages(ArrayList<MyMessageSupportSave> messages,
			Cursor c) {
		while (c.moveToNext()) {
			MyMessageSupportSave message = new MyMessageSupportSave();
			message.setRecordId(c.getLong(c.getColumnIndex("_id"))); // record
																		// id,
																		// not
																		// msgid
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
			if (c.getColumnIndex("favorite") == -1)
				message.setFavorite(false);
			else
				message.setFavorite(c.getLong(c.getColumnIndex("favorite")) != 0);
			messages.add(message);
		}
		//
		for (MyMessageSupportSave message : messages) {
			Cursor c2 = db.rawQuery(
					"SELECT * FROM attachment WHERE messageId=?",
					new String[] { Long.toString(message.getRecordId()) });
			List<Attachment> attachments = new ArrayList<Attachment>();
			while (c2.moveToNext()) {
				Attachment attachment = new Attachment();
				attachment.setTitle(c2.getString(c2.getColumnIndex("title")));
				attachment.setType(c2.getString(c2.getColumnIndex("type")));
				attachment.setFilename(c2.getString(c2
						.getColumnIndex("filename")));
				attachment.setUrl(c2.getString(c2.getColumnIndex("url")));
				attachments.add(attachment);
			}
			c2.close();
			message.setAttachments(attachments.toArray(new Attachment[0]));
		}
	}
}
