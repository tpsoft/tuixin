package com.tpsoft.tuixin.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.view.ContextThemeWrapper;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.tpsoft.tuixin.R;

public class ImageUtils {

	/* ������ʶ�������๦�ܵ�activity */
	public static final int CAMERA_WITH_DATA = 3023;

	/* ������ʶ����gallery��activity */
	public static final int PHOTO_PICKED_WITH_DATA = 3021;

	//public static File mCurrentPhotoFile;// ��������յõ���ͼƬ

	private static Activity mActivity;

	public static Bitmap roundCorners(Bitmap source, final float radius) {
		int width = source.getWidth();
		int height = source.getHeight();
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(android.graphics.Color.WHITE);
		Bitmap clipped = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(clipped);
		canvas.drawRoundRect(new RectF(0, 0, width, height), radius, radius,
				paint);
		paint.setXfermode(new PorterDuffXfermode(
				android.graphics.PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(source, 0, 0, paint);
		source.recycle();
		return clipped;
	}

	public static void doPickPhotoAction(Activity activity) {
		ImageUtils.mActivity = activity;

		// Wrap our context to inflate list items using correct theme
		final Context dialogContext = new ContextThemeWrapper(activity,
				android.R.style.Theme_Light);
		String cancel = "����";
		String[] choices;
		choices = new String[2];
		choices[0] = mActivity.getString(R.string.take_photo); // ����
		choices[1] = mActivity.getString(R.string.pick_photo); // �������ѡ��
		final ListAdapter adapter = new ArrayAdapter<String>(dialogContext,
				android.R.layout.simple_list_item_1, choices);

		final AlertDialog.Builder builder = new AlertDialog.Builder(
				dialogContext);
		builder.setTitle(R.string.msg_photo);
		builder.setSingleChoiceItems(adapter, -1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						switch (which) {
						case 0: {
							//String status = Environment
							//		.getExternalStorageState();
							//if (status.equals(Environment.MEDIA_MOUNTED)) {// �ж��Ƿ���SD��
								doTakePhoto();// �û�����˴��������ȡ
							//} else {
							//	Toast.makeText(mActivity, R.string.noSDCard,
							//			Toast.LENGTH_LONG).show();
							//}
							break;

						}
						case 1:
							doPickPhotoFromGallery();// �������ȥ��ȡ
							break;
						}
					}
				});
		builder.setNegativeButton(cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}

				});
		builder.create().show();
	}

	/**
	 * ���ջ�ȡͼƬ
	 * 
	 */
	private static void doTakePhoto() {
		/* ���յ���Ƭ�洢λ�� */
		//File photoDir = new File(Environment.getExternalStorageDirectory()
		//		+ "/tuixin/photo");
		try {
			// Launch camera to take photo for selected contact
			//photoDir.mkdirs();// ������Ƭ�Ĵ洢Ŀ¼
			//mCurrentPhotoFile = new File(photoDir, getPhotoFileName());// �����յ���Ƭ�ļ�����
			Intent intent = getTakePickIntent(/*mCurrentPhotoFile*/);
			mActivity.startActivityForResult(intent, CAMERA_WITH_DATA);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(mActivity, R.string.photoPickerNotFound,
					Toast.LENGTH_LONG).show();
		}
	}

	private static Intent getTakePickIntent(/*File f*/) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
		//intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
		return intent;
	}

	/**
	 * �õ�ǰʱ���ȡ�õ�ͼƬ����
	 * 
	 */
	@SuppressLint("SimpleDateFormat")
	private static String getPhotoFileName() {
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"'IMG'_yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(date) + ".jpg";
	}

	// ����Gallery����
	private static void doPickPhotoFromGallery() {
		try {
			// Launch picker to choose photo for selected contact
			final Intent intent = getPhotoPickIntent();
			mActivity.startActivityForResult(intent, PHOTO_PICKED_WITH_DATA);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(mActivity, R.string.photoPickerNotFound,
					Toast.LENGTH_LONG).show();
		}
	}

	// ��װ����Gallery��intent
	private static Intent getPhotoPickIntent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
		intent.setType("image/*");
		intent.putExtra("crop", "true");
		//intent.putExtra("aspectX", 1);
		//intent.putExtra("aspectY", 1);
		//intent.putExtra("outputX", 200);
		//intent.putExtra("outputY", 200);
		intent.putExtra("return-data", true);
		return intent;
	}

}
