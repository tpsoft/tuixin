package com.tpsoft.tuixin.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.util.Log;

import com.tpsoft.tuixin.MyApplicationClass;

public class HttpUtils {

	public static final String TAG_LOG = "HttpUtils";
	public static final String UPLOAD_FILE_ACTION_URL_FORMAT = "http://%s:%d/uploadFiles/";

	/**
	 * 根据URL地址下载文本
	 * 
	 * @param urlStr
	 *            URL地址
	 * @return 文本
	 */
	public static String download(String urlStr) {
		StringBuffer sb = new StringBuffer();
		String line = null;
		BufferedReader buffer = null;
		URL url;
		try {
			url = new URL(urlStr);
			HttpURLConnection urlConn = (HttpURLConnection) url
					.openConnection();
			buffer = new BufferedReader(new InputStreamReader(
					urlConn.getInputStream()));
			while ((line = buffer.readLine()) != null) {
				sb.append(line);
			}

		} catch (MalformedURLException e) {
			Log.w(TAG_LOG, "Malformed url: " + urlStr);
		} catch (IOException e) {
			Log.w(TAG_LOG, "IO error: " + e.getMessage());
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
	 * 根据URL地址下载文件并写入SD卡
	 * 
	 * @param urlStr
	 *            URL地址
	 * @param path
	 *            保存路径
	 * @param fileName
	 *            保存文件名
	 * @param overwrite
	 *            是否覆盖同名文件
	 * @return 0:操作成功, 1:文件已经存在, 2:下载失败, 3:保存失败
	 */
	public static int downFile(String urlStr, String path, String fileName,
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
			return 4;
		}
		return 0;
	}

	/**
	 * 根据URL地址得到输入流
	 * 
	 * @param urlStr
	 *            url地址
	 * @return 输入流
	 */
	public static InputStream getInputStreamFromURL(String urlStr) {
		HttpURLConnection urlConn = null;
		InputStream inputStream = null;
		URL url;
		try {
			url = new URL(urlStr);
			urlConn = (HttpURLConnection) url.openConnection();
			inputStream = urlConn.getInputStream();
		} catch (MalformedURLException e) {
			Log.w(TAG_LOG, "Malformed url: " + urlStr);
		} catch (IOException e) {
			Log.w(TAG_LOG, "IO error: " + e.getMessage());
		}

		return inputStream;
	}

	/**
	 * 上传文件到服务器
	 * 
	 * @param contentType
	 *            文件内容类型(MIME)
	 * @param inputStream
	 *            要上传的内容(输入流)
	 * @param uploadedName
	 *            上传后的名字
	 * @param uploadedFilename
	 *            上传后的文件名
	 * @return 非空时为下载URL，否则代表上传失败
	 */
	@SuppressLint("DefaultLocale")
	public static String uploadFile(String contentType,
			InputStream inputStream, String uploadedName,
			String uploadedFilename) throws Exception {
		String uploadFileActionUrl = String.format(
				UPLOAD_FILE_ACTION_URL_FORMAT,
				MyApplicationClass.userSettings.getServerHost(),
				MyApplicationClass.userSettings.getUploadPort());

		final String end = "\r\n";
		final String twoHyphens = "--";
		final String boundary = "*****";

		URL url = new URL(uploadFileActionUrl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		/* 设置传送的method=POST */
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setRequestMethod("POST");
		/* setRequestProperty */
		con.setRequestProperty("Connection", "Keep-Alive");
		con.setRequestProperty("Charset", "UTF-8");
		con.setRequestProperty("Content-Type", "multipart/form-data; boundary="
				+ boundary);
		/* 设置DataOutputStream */
		DataOutputStream ds = new DataOutputStream(con.getOutputStream());
		ds.writeBytes(twoHyphens + boundary + end);
		ds.writeBytes("Content-Disposition: form-data; " + "name=\""
				+ uploadedName + "\"; filename=\"" + uploadedFilename + "\""
				+ end + "Content-Type: " + contentType + end);
		ds.writeBytes(end);
		/* 设置每次写入1024bytes */
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		int length = -1;
		/* 从文件读取数据至缓冲区 */
		while ((length = inputStream.read(buffer)) != -1) {
			/* 将资料写入DataOutputStream中 */
			ds.write(buffer, 0, length);
		}
		ds.writeBytes(end);
		ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
		ds.flush();
		/* 取得Response内容 */
		InputStream is = con.getInputStream();
		int ch;
		StringBuffer b = new StringBuffer();
		while ((ch = is.read()) != -1) {
			b.append((char) ch);
		}
		/* 将Response显示于Dialog */
		String responseText = b.toString();
		Log.d(TAG_LOG, responseText);
		String downloadUrl = parseUploadResult(responseText);
		/* 关闭DataOutputStream */
		ds.close();
		return downloadUrl;
	}

	private static String parseUploadResult(String responseText)
			throws JSONException {
		boolean success = false;
		JSONObject jsonObject = new JSONObject(responseText);
		if (jsonObject.has("success")) {
			success = "true".equals(jsonObject.getString("success"));
		}
		if (!success) {
			int errcode = jsonObject.getInt("errcode");
			String errmsg = jsonObject.getString("errmsg");
			Log.w(TAG_LOG, String.format("Error #%d: %s", errcode, errmsg));
			return null;
		}
		// 解析URL
		if (jsonObject.has("urls")) {
			JSONArray jsonArray = jsonObject.getJSONArray("urls");
			if (jsonArray.length() != 0) {
				String url = jsonArray.getString(0);
				return url;
			}
		}

		return null;
	}
}
