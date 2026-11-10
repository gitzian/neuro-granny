package org.gitzian.neurogranny.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.LruCache;

import java.util.Arrays;

import org.gitzian.neurogranny.model.CueMode;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.patterns.PatternGenerator;

/** Renders either a procedural cue or a stored photo cue. */
public final class CueRenderer {
    private static final Paint BITMAP_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(16);

    private CueRenderer() {
    }

    public static void render(Canvas canvas, Memory memory, float left, float top, float size) {
        if (memory == null || size <= 0.0f) {
            return;
        }

        if (memory.cueMode == CueMode.PHOTO && memory.cueImage != null && memory.cueImage.length > 0) {
            Bitmap bitmap = decode(memory);
            if (bitmap != null) {
                RectF target = new RectF(left, top, left + size, top + size);
                canvas.drawBitmap(bitmap, null, target, BITMAP_PAINT);
                return;
            }
        }

        PatternGenerator.render(canvas, memory.patternSeed, left, top, size);
    }

    private static Bitmap decode(Memory memory) {
        String key = memory.id + ":" + Arrays.hashCode(memory.cueImage);
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        Bitmap decoded = BitmapFactory.decodeByteArray(memory.cueImage, 0, memory.cueImage.length);
        if (decoded != null) {
            CACHE.put(key, decoded);
        }
        return decoded;
    }
}
