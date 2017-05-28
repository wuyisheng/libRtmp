package org.yeshen.video.librtmp.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import org.yeshen.video.librtmp.afix.Cameras;
import org.yeshen.video.librtmp.tools.Lg;

/**
 * @Title: RenderSurfaceView
 * @Package com.laifeng.sopcastsdk.ui
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午5:12
 * @Version
 */

public class RenderSurfaceView extends GLSurfaceView {
    private MyRenderer mRenderer;

    public RenderSurfaceView(Context context) {
        super(context);
        init();
    }

    public RenderSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mRenderer = new MyRenderer(this);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(mSurfaceHolderCallback);
    }

    public MyRenderer getRenderer() {
        return mRenderer;
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Lg.d("SurfaceView destroy");
            Cameras.instance().stopPreview();
            Cameras.instance().releaseCamera();
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Lg.d("SurfaceView created");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Lg.d("SurfaceView width:" + width + " height:" + height);
        }
    };
}
