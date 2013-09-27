package com.tpsoft.tuixin.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "tuixin.db";
	private static final int DATABASE_VERSION = 2;

	public DBHelper(Context context) {
		// CursorFactory����Ϊnull,ʹ��Ĭ��ֵ
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// ���ݿ��һ�α�����ʱonCreate�ᱻ����
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS message" // ��Ϣ��:
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // ��¼ID
				+ "sender TEXT NOT NULL, " // ����������
				+ "receiver TEXT, " // ����������
				+ "title TEXT, " // ��Ϣ����(��ѡ)
				+ "body TEXT NOT NULL, " // ��Ϣ����
				+ "type TEXT NOT NULL, " // ��Ϣ����(text,html,xml,...)
				+ "url TEXT, " // ��Ϣ����URL(��ѡ)
				+ "generateTime TEXT NOT NULL, " // ��Ϣ����ʱ��
				+ "expiration TEXT)"); //  ��Ϣ����ʱ��(��ѡ)
		db.execSQL("CREATE TABLE IF NOT EXISTS attachment" // ������:
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // ��¼ID
				+ "title TEXT NOT NULL, " // ��������
				+ "type TEXT NOT NULL, " // ��������
				+ "filename TEXT NOT NULL, " // �����ļ���
				+ "url TEXT NOT NULL, " // ��������URL
				+ "messageId INTEGER, " // ���
				+ "FOREIGN KEY (messageId) REFERENCES message (_id))");
		db.execSQL("CREATE TABLE IF NOT EXISTS attachment_content" // ��������:
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // ��¼ID
				+ "url TEXT NOT NULL, " // ����URL
				+ "data BLOB NOT NULL," // ����
				+ "time TEXT NOT NULL," // ʱ��
				+ "UNIQUE (url) ON CONFLICT REPLACE)");
	}

	// ���DATABASE_VERSIONֵ����Ϊ2,ϵͳ�����������ݿ�汾��ͬ,�������onUpgrade
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion==1 && newVersion==2) {
			db.execSQL("CREATE TABLE IF NOT EXISTS attachment_content" // ��������:
					+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // ��¼ID
					+ "url TEXT NOT NULL, " // ����URL
					+ "data BLOB NOT NULL," // ����
					+ "time TEXT NOT NULL," // ʱ��
					+ "UNIQUE (url) ON CONFLICT REPLACE)");
		}
	}
}
