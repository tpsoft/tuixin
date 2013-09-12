package com.tpsoft.tuixin.db;

import java.util.Date;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "tuixin.db";
	private static final int DATABASE_VERSION = 1;

	public DBHelper(Context context) {
		// CursorFactory����Ϊnull,ʹ��Ĭ��ֵ
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// ���ݿ��һ�α�����ʱonCreate�ᱻ����
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS msg" // ��Ϣ��:
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " // ��¼ID
				+ "sender TEXT NOT NULL, " // ����������
				+ "title TEXT, " // ��Ϣ����(��ѡ)
				+ "body TEXT NOT NULL, " // ��Ϣ����
				+ "type TEXT NOT NULL DEFAULT text, " // ��Ϣ����([text],html,xml,...)
				+ "url TEXT, " // ��Ϣ����URL(��ѡ)
				+ "generateTime TEXT NOT NULL, " // ��Ϣ����ʱ��
				+ "expiration TEXT)"); //  ��Ϣ����ʱ��(��ѡ)
	}

	// ���DATABASE_VERSIONֵ����Ϊ2,ϵͳ�����������ݿ�汾��ͬ,�������onUpgrade
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}
