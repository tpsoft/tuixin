package com.tpsoft.tuixin.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Environment;

public class FileUtils {
	private String SDPATH;

	private int FILESIZE = 4 * 1024;

	public FileUtils() {
		// �õ���ǰ�ⲿ�洢�豸��Ŀ¼( /SDCARD )
		SDPATH = Environment.getExternalStorageDirectory().getPath();
	}

	public String getSDPATH() {
		return SDPATH;
	}

	/**
	 * ��SD���ϴ���Ŀ¼
	 * 
	 * @param dirName
	 * @return
	 */
	public File createSDDir(String dirName) {
		File dir = new File(SDPATH + "/" + dirName);
		if (!dir.exists() && !dir.mkdir())
			return null;
		return dir;
	}

	/**
	 * ��SD���ϴ����ļ�
	 * 
	 * @param fileName
	 * @param autoOverwrite
	 * @return
	 * @throws IOException
	 */
	public File createSDFile(String fileName, boolean autoOverwrite)
			throws IOException {
		File file = new File(SDPATH + "/" + fileName);
		if (file.exists()) {
			if (!file.delete())
				return null;
		}
		if (!file.createNewFile())
			return null;
		return file;
	}

	/**
	 * �ж�SD���ϵ��ļ��Ƿ����
	 * 
	 * @param fileName
	 * @return
	 */
	public boolean isFileExist(String fileName) {
		File file = new File(SDPATH + "/" + fileName);
		return file.exists();
	}

	/**
	 * ��һ��InputStream���������д�뵽SD����
	 * 
	 * @param path
	 * @param fileName
	 * @param input
	 * @param autoOverwrite
	 * @return
	 */
	public File write2SDFromInput(String path, String fileName,
			InputStream input, boolean autoOverwrite) {
		File file = null;
		OutputStream output = null;
		try {
			if (createSDDir(path) == null)
				return null;
			file = createSDFile(path + "/" + fileName, autoOverwrite);
			if (file == null)
				return null;

			output = new FileOutputStream(file);
			byte[] buffer = new byte[FILESIZE];
			int length;
			while ((length = (input.read(buffer))) > 0) {
				output.write(buffer, 0, length);
			}
			output.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
}
