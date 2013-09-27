package com.tpsoft.tuixin.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "tuixin.db";
	private static final int DATABASE_VERSION = 2;

	public DBHelper(Context context) {
		// CursorFactory设置为null,使用默认值
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// 数据库第一次被创建时onCreate会被调用
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS message" // 消息表:
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // 记录ID
				+ "sender TEXT NOT NULL, " // 发送者名字
				+ "receiver TEXT, " // 接收者名字
				+ "title TEXT, " // 消息标题(可选)
				+ "body TEXT NOT NULL, " // 消息内容
				+ "type TEXT NOT NULL, " // 消息类型(text,html,xml,...)
				+ "url TEXT, " // 消息详情URL(可选)
				+ "generateTime TEXT NOT NULL, " // 消息生成时间
				+ "expiration TEXT)"); //  消息过期时间(可选)
		db.execSQL("CREATE TABLE IF NOT EXISTS attachment" // 附件表:
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // 记录ID
				+ "title TEXT NOT NULL, " // 附件标题
				+ "type TEXT NOT NULL, " // 附件类型
				+ "filename TEXT NOT NULL, " // 附件文件名
				+ "url TEXT NOT NULL, " // 附件下载URL
				+ "messageId INTEGER, " // 外键
				+ "FOREIGN KEY (messageId) REFERENCES message (_id))");
		db.execSQL("CREATE TABLE IF NOT EXISTS attachment_content" // 附件内容:
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // 记录ID
				+ "url TEXT NOT NULL, " // 附件URL
				+ "data BLOB NOT NULL," // 数据
				+ "time TEXT NOT NULL," // 时间
				+ "UNIQUE (url) ON CONFLICT REPLACE)");
	}

	// 如果DATABASE_VERSION值被改为2,系统发现现有数据库版本不同,即会调用onUpgrade
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion==1 && newVersion==2) {
			db.execSQL("CREATE TABLE IF NOT EXISTS attachment_content" // 附件内容:
					+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // 记录ID
					+ "url TEXT NOT NULL, " // 附件URL
					+ "data BLOB NOT NULL," // 数据
					+ "time TEXT NOT NULL," // 时间
					+ "UNIQUE (url) ON CONFLICT REPLACE)");
		}
	}
}
