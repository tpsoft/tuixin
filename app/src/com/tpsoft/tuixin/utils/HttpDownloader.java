package com.tpsoft.tuixin.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpDownloader {

	private URL url = null;

	/**
	 * ����URL�����ļ�,ǰ��������ļ����е��������ı�,�����ķ���ֵ�����ı����е����� 1.����һ��URL����
	 * 2.ͨ��URL����,����һ��HttpURLConnection���� 3.�õ�InputStream 4.��InputStream���ж�ȡ����
	 * 
	 * @param urlStr
	 * @return
	 */
	public String download(String urlStr) {
		StringBuffer sb = new StringBuffer();
		String line = null;
		BufferedReader buffer = null;
		try {
			url = new URL(urlStr);
			HttpURLConnection urlConn = (HttpURLConnection) url
					.openConnection();
			buffer = new BufferedReader(new InputStreamReader(
					urlConn.getInputStream()));
			while ((line = buffer.readLine()) != null) {
				sb.append(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				buffer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param urlStr
	 * @param path
	 * @param fileName
	 * @param overwrite
	 * @return 0:�����ɹ�, 1:�ļ��Ѿ�����, 2:����ʧ��, 3:����ʧ��
	 */
	public int downFile(String urlStr, String path, String fileName,
			boolean overwrite) {
		FileUtils fileUtils = new FileUtils();
		if (fileUtils.isFileExist(path + fileName)) {
			if (!overwrite) {
				return 1;
			}

		}
		//
		InputStream inputStream = getInputStreamFromURL(urlStr);
		if (inputStream == null)
			return 2;
		//
		File resultFile = fileUtils.write2SDFromInput(path, fileName,
				inputStream, overwrite);
		if (resultFile == null)
			return 3;
		try {
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return 3;
		}
		return 0;
	}

	/**
	 * ����URL�õ�������
	 * 
	 * @param urlStr
	 * @return
	 */
	public InputStream getInputStreamFromURL(String urlStr) {
		HttpURLConnection urlConn = null;
		InputStream inputStream = null;
		try {
			url = new URL(urlStr);
			urlConn = (HttpURLConnection) url.openConnection();
			inputStream = urlConn.getInputStream();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return inputStream;
	}
}
