package org.gitzian.neurogranny.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.gitzian.neurogranny.R;

/** Converts selected images to metadata-free square JPEG cues, preserving visual orientation. */
final class CueImageProcessor {
    private static final int IMAGE_SIZE_PX = 512;
    private static final int JPEG_QUALITY = 88;

    private final Context context;

    CueImageProcessor(Context context) {
        this.context = context;
    }

    byte[] encodeCueBitmap(Bitmap source) {
        return encodeCueBitmap(source, ExifInterface.ORIENTATION_NORMAL);
    }

    private byte[] encodeCueBitmap(Bitmap source, int orientation) {
        if (source == null) {
            return null;
        }

        int edge = Math.min(source.getWidth(), source.getHeight());
        int left = (source.getWidth() - edge) / 2;
        int top = (source.getHeight() - edge) / 2;
        Bitmap cropped = Bitmap.createBitmap(source, left, top, edge, edge);
        Bitmap upright = applyExifOrientation(cropped, orientation);
        Bitmap scaled = Bitmap.createScaledBitmap(upright, IMAGE_SIZE_PX, IMAGE_SIZE_PX, true);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                throw new IllegalStateException(context.getString(R.string.invalid_image));
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(context.getString(R.string.invalid_image), exception);
        } finally {
            if (scaled != upright) {
                scaled.recycle();
            }
            if (upright != cropped) {
                upright.recycle();
            }
            if (cropped != source) {
                cropped.recycle();
            }
        }
    }

    private static Bitmap applyExifOrientation(Bitmap image, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180.0f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1.0f, -1.0f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90.0f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90.0f);
                break;
            default:
                return image;
        }
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    private int readCueOrientation(Uri uri) throws IOException {
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IOException(context.getString(R.string.cannot_open_file));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ExifInterface exif = new ExifInterface(stream);
                return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
            }
            return JpegExifOrientation.read(stream);
        }
    }

    byte[] readCueImage(Uri uri) throws IOException {
        int orientation = readCueOrientation(uri);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IOException(context.getString(R.string.cannot_open_file));
            }
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException(context.getString(R.string.invalid_image));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.min(bounds.outWidth, bounds.outHeight) / (options.inSampleSize * 2)
                >= IMAGE_SIZE_PX) {
            options.inSampleSize *= 2;
        }

        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IOException(context.getString(R.string.cannot_open_file));
            }
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
            if (bitmap == null) {
                throw new IOException(context.getString(R.string.invalid_image));
            }
            try {
                return encodeCueBitmap(bitmap, orientation);
            } finally {
                bitmap.recycle();
            }
        }
    }

}
