package org.yeshen.video.librtmp.unstable;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.yeshen.video.librtmp.R;
import org.yeshen.video.librtmp.core.ILivingView;
import org.yeshen.video.librtmp.core.IRenderer;
import org.yeshen.video.librtmp.core.delegate.ICameraOpenDelegate;
import org.yeshen.video.librtmp.unstable.tools.Lg;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class LivingView extends FrameLayout implements ILivingView {

    private ICameraOpenDelegate mLivingDelegate;

    private boolean isRenderSurfaceViewShowing = true;
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

    protected GLSurfaceView mGLSurfaceView;
    private RendererImpl mRenderer;

    public LivingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public LivingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public LivingView(Context context) {
        super(context);
        initView(context);
    }

    @Override
    public void setVisibility(int visibility) {
        int currentVisibility = getVisibility();
        if (visibility == currentVisibility) {
            return;
        }
        switch (visibility) {
            case VISIBLE:
                addRenderSurfaceView();
                break;
            case GONE:
                removeRenderSurfaceView();
                break;
            case INVISIBLE:
                removeRenderSurfaceView();
                break;
        }
        super.setVisibility(visibility);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        float mAspectRatio = 9.0f / 16;
        if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.AT_MOST) {
            heightSpecSize = (int) (widthSpecSize / mAspectRatio);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSpecSize,
                    MeasureSpec.EXACTLY);
        } else if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.EXACTLY) {
            widthSpecSize = (int) (heightSpecSize * mAspectRatio);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSpecSize,
                    MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setDelegate(@NonNull ICameraOpenDelegate delegate) {
        mLivingDelegate = delegate;
    }

    @Override
    public IRenderer getRenderer() {
        return mRenderer;
    }

    @Override
    public void syncConfig() {
        Cameras.instance().syncConfig();
    }

    private void initView(Context context) {
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.layout_camera_view, this, true);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.surface_view);
        mGLSurfaceView.setZOrderMediaOverlay(false);
        initRenderer();
        mRenderer.setCameraOpenListener(mCameraOpenListener);
    }

    @SuppressWarnings("deprecation")
    private void initRenderer() {
        mRenderer = new RendererImpl(mGLSurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        SurfaceHolder surfaceHolder = mGLSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(mSurfaceHolderCallback);
    }

    private void addRenderSurfaceView() {
        if (!isRenderSurfaceViewShowing) {
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            addView(mGLSurfaceView, 0, layoutParams);
            isRenderSurfaceViewShowing = true;
        }
    }

    private void removeRenderSurfaceView() {
        if (isRenderSurfaceViewShowing) {
            removeView(mGLSurfaceView);
            isRenderSurfaceViewShowing = false;
        }
    }

    private ICameraOpenDelegate mCameraOpenListener = new ICameraOpenDelegate() {
        @Override
        public void onOpenSuccess() {
            if (mLivingDelegate != null) mLivingDelegate.onOpenSuccess();
        }

        @Override
        public void onOpenFail(int error) {
            if (mLivingDelegate != null) mLivingDelegate.onOpenFail(error);
        }
    };

}
