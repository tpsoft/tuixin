package com.tpsoft.tuixin.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

public class ImageUtils {

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
}
