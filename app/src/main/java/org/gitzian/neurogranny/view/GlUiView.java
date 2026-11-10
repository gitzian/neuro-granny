package org.gitzian.neurogranny.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Presents the Android Canvas-based UI as an OpenGL ES 2.0 texture.
 *
 * <p>All UI hitboxes are produced by the same layout calculation that draws the
 * frame. Android EditText controls remain native views above this surface.
 */
public final class GlUiView extends GLSurfaceView implements GLSurfaceView.Renderer {
    public interface Listener {
        void onAction(UiAction action);

        void onScroll(int deltaDp, int maxScroll);
    }

    private static final String VERTEX_SHADER =
            "attribute vec2 aPosition;\n"
                    + "attribute vec2 aTex;\n"
                    + "varying vec2 vTex;\n"
                    + "void main() {\n"
                    + "    gl_Position = vec4(aPosition, 0.0, 1.0);\n"
                    + "    vTex = aTex;\n"
                    + "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "uniform sampler2D uTexture;\n"
                    + "varying vec2 vTex;\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(uTexture, vTex);\n"
                    + "}\n";

    private static final int MAX_BITMAP_DIMENSION_PX = 2048;
    private static final float MAX_FLING_VELOCITY_DP = 6000.0f;

    private final float density;
    private final float touchSlopDp;
    private final UiPainter painter;
    private final OverScroller flingScroller;

    private volatile UiScene scene;
    private volatile UiPainter.UiLayout displayedLayout;

    private Listener listener;
    private int width;
    private int height;
    private int program;
    private int texture;
    private int positionAttribute;
    private int textureAttribute;
    private Bitmap frame;
    private FloatBuffer vertices;
    private float touchStartX;
    private float touchStartY;
    private float lastTouchY;
    private boolean dragging;
    private boolean gestureCanceled;
    private UiAction pressedAction;
    private boolean startedInContent;
    private VelocityTracker velocityTracker;
    private int lastFlingPosition;
    private final Runnable flingFrame = new Runnable() {
        @Override
        public void run() {
            UiPainter.UiLayout layout = displayedLayout;
            if (layout == null || !flingScroller.computeScrollOffset()) {
                return;
            }
            int position = flingScroller.getCurrY();
            int delta = position - lastFlingPosition;
            lastFlingPosition = position;
            if (delta != 0 && listener != null) {
                listener.onScroll(delta, layout.maxScroll);
            }
            if (!flingScroller.isFinished()) {
                postOnAnimation(this);
            }
        }
    };

    public GlUiView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        touchSlopDp = ViewConfiguration.get(context).getScaledTouchSlop() / density;
        painter = new UiPainter(context);
        flingScroller = new OverScroller(context);

        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void show(UiScene state) {
        UiScene previous = scene;
        scene = state;
        if (previous == null || previous.page != state.page) {
            // Do not accept clicks from the previous page during navigation.
            displayedLayout = null;
            stopFling();
        }
        requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            throw new IllegalStateException("OpenGL program link failed: "
                    + GLES20.glGetProgramInfoLog(program));
        }

        positionAttribute = GLES20.glGetAttribLocation(program, "aPosition");
        textureAttribute = GLES20.glGetAttribLocation(program, "aTex");

        int[] textureNames = new int[1];
        GLES20.glGenTextures(1, textureNames, 0);
        texture = textureNames[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // x, y, textureU, textureV for each vertex of a triangle strip.
        float[] quadVertices = {
                -1.0f, -1.0f, 0.0f, 1.0f,
                1.0f, -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 1.0f, 0.0f
        };
        vertices = ByteBuffer.allocateDirect(quadVertices.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertices.put(quadVertices).position(0);
    }

    private static int compileShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            throw new IllegalStateException("OpenGL shader compilation failed: "
                    + GLES20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        displayedLayout = null;
        stopFling();
        GLES20.glViewport(0, 0, width, height);

        if (frame != null) {
            frame.recycle();
            frame = null;
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClearColor(0.031f, 0.031f, 0.043f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        UiScene currentScene = scene;
        if (currentScene == null || width == 0 || height == 0) {
            return;
        }

        float bitmapScale = Math.min(
                1.0f,
                MAX_BITMAP_DIMENSION_PX / (float) Math.max(width, height));
        int bitmapWidth = Math.max(1, Math.round(width * bitmapScale));
        int bitmapHeight = Math.max(1, Math.round(height * bitmapScale));
        ensureBitmapSize(bitmapWidth, bitmapHeight);

        Canvas canvas = new Canvas(frame);
        canvas.scale(bitmapWidth / (width / density), bitmapHeight / (height / density));
        UiPainter.UiLayout layout = painter.paint(
                canvas, width / density, height / density, currentScene);
        displayedLayout = layout;
        if (currentScene.listScroll > layout.maxScroll) {
            // A rotation may leave an obsolete offset; reconcile on the UI thread.
            post(() -> {
                UiScene latestScene = scene;
                if (latestScene != null && latestScene.page == currentScene.page
                        && latestScene.listScroll > layout.maxScroll && listener != null) {
                    listener.onScroll(layout.maxScroll - latestScene.listScroll,
                            layout.maxScroll);
                }
            });
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, frame, 0);
        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0);

        vertices.position(0);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glVertexAttribPointer(
                positionAttribute, 2, GLES20.GL_FLOAT, false, 16, vertices);
        vertices.position(2);
        GLES20.glEnableVertexAttribArray(textureAttribute);
        GLES20.glVertexAttribPointer(
                textureAttribute, 2, GLES20.GL_FLOAT, false, 16, vertices);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(textureAttribute);
    }

    private void ensureBitmapSize(int bitmapWidth, int bitmapHeight) {
        if (frame == null
                || frame.getWidth() != bitmapWidth
                || frame.getHeight() != bitmapHeight) {
            if (frame != null) {
                frame.recycle();
            }
            frame = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        }
    }

    /**
     * Scrolls the middle viewport only; native-like dragging and fling do not move
     * the fixed actions. Touch and draw use the same immutable layout snapshot.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        UiPainter.UiLayout layout = displayedLayout;
        if (layout == null) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    || event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                resetTouch();
            }
            return true;
        }

        float xDp = event.getX() / density;
        float yDp = event.getY() / density;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopFling();
                resetTouch();
                touchStartX = xDp;
                touchStartY = yDp;
                lastTouchY = yDp;
                startedInContent = layout.contentViewport.contains(xDp, yDp);
                pressedAction = layout.actionAt(xDp, yDp);
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                // A multi-touch gesture is never eligible to activate a button.
                gestureCanceled = true;
                pressedAction = null;
                releaseVelocityTracker();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (gestureCanceled) {
                    return true;
                }
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                }
                if (movedBeyondTouchSlop(xDp, yDp)) {
                    pressedAction = null;
                }
                if (!startedInContent || layout.maxScroll <= 0) {
                    return true;
                }
                if (Math.abs(yDp - touchStartY) > touchSlopDp) {
                    dragging = true;
                }
                if (dragging && listener != null) {
                    int delta = Math.round(lastTouchY - yDp);
                    if (delta != 0) {
                        listener.onScroll(delta, layout.maxScroll);
                    }
                }
                lastTouchY = yDp;
                return true;

            case MotionEvent.ACTION_UP:
                if (!gestureCanceled && velocityTracker != null) {
                    velocityTracker.addMovement(event);
                }
                if (movedBeyondTouchSlop(xDp, yDp)) {
                    pressedAction = null;
                }
                if (!gestureCanceled && dragging) {
                    startFling(layout);
                } else if (!gestureCanceled && pressedAction != null && listener != null) {
                    UiAction releasedAction = layout.actionAt(xDp, yDp);
                    if (sameAction(pressedAction, releasedAction)) {
                        UiAction action = pressedAction;
                        resetTouch();
                        listener.onAction(action);
                        return true;
                    }
                }
                resetTouch();
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                resetTouch();
                return true;

            default:
                return true;
        }
    }

    private boolean movedBeyondTouchSlop(float xDp, float yDp) {
        float dx = xDp - touchStartX;
        float dy = yDp - touchStartY;
        return dx * dx + dy * dy > touchSlopDp * touchSlopDp;
    }

    private static boolean sameAction(UiAction first, UiAction second) {
        if (second == null || first.type() != second.type()) {
            return false;
        }
        return first.type() != UiAction.Type.SELECT_MEMORY
                || first.memoryId() == second.memoryId();
    }

    private void resetTouch() {
        pressedAction = null;
        dragging = false;
        gestureCanceled = false;
        startedInContent = false;
        releaseVelocityTracker();
    }

    private void startFling(UiPainter.UiLayout layout) {
        if (velocityTracker == null || scene == null || layout.maxScroll <= 0) {
            return;
        }
        velocityTracker.computeCurrentVelocity(1000);
        float velocity = -velocityTracker.getYVelocity() / density;
        float minimum = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity() / density;
        if (Math.abs(velocity) < minimum) {
            return;
        }
        int initialScroll = Math.max(0, Math.min(scene.listScroll, layout.maxScroll));
        int speed = Math.round(Math.max(-MAX_FLING_VELOCITY_DP,
                Math.min(MAX_FLING_VELOCITY_DP, velocity)));
        lastFlingPosition = initialScroll;
        flingScroller.fling(0, initialScroll, 0, speed,
                0, 0, 0, layout.maxScroll);
        postOnAnimation(flingFrame);
    }

    private void stopFling() {
        flingScroller.forceFinished(true);
        removeCallbacks(flingFrame);
    }

    private void releaseVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }
}
