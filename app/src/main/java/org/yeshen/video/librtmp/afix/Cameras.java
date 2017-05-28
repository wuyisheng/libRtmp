package org.yeshen.video.librtmp.afix;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import org.yeshen.video.librtmp.android.CameraData;
import org.yeshen.video.librtmp.exception.CameraHardwareException;
import org.yeshen.video.librtmp.exception.CameraNotSupportException;
import org.yeshen.video.librtmp.tools.Lg;
import org.yeshen.video.librtmp.tools.Options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*********************************************************************
 * Created by yeshen on 2017/05/26.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public abstract class Cameras {

    private static final Cameras sInstance =new CameraImpl();
    public static Cameras instance() {
        return sInstance;
    }

    public abstract CameraData getCameraData();

    public abstract boolean isLandscape();

    @SuppressWarnings("deprecation")
    public abstract Camera openCamera() throws CameraHardwareException, CameraNotSupportException;

    public abstract void setSurfaceTexture(SurfaceTexture texture);

    public abstract State getState();

    public abstract void setConfiguration();

    public abstract void startPreview();

    public abstract void stopPreview();

    public abstract void releaseCamera();

    public abstract void release();

    public abstract void setFocusPoint(int x, int y);

    public abstract boolean autoFocus(Camera.AutoFocusCallback focusCallback);

    public abstract void switchFocusMode();

    public abstract boolean switchCamera();

    public abstract boolean switchLight();

    public enum State {
        INIT,
        OPENED,
        PREVIEW
    }

    private static class CameraImpl extends Cameras {
        private static final String TAG = "CameraHolder";

        private List<CameraData> mCameraDatas;
        private Camera mCameraDevice;
        private CameraData mCameraData;
        private State mState;
        private SurfaceTexture mTexture;
        private boolean isTouchMode = false;
        private boolean isOpenBackFirst = false;

        public CameraImpl() {
            mState = State.INIT;
        }

        @Override
        public CameraData getCameraData() {
            return mCameraData;
        }

        @Override
        public boolean isLandscape() {
            return (Options.getInstance().camera.orientation != Options.Orientation.PORTRAIT);
        }

        @SuppressWarnings("deprecation")
        @Override
        public Camera openCamera()
                throws CameraHardwareException, CameraNotSupportException {
            if (mCameraDatas == null || mCameraDatas.size() == 0) {
                mCameraDatas = AndroidUntil.getAllCamerasData(isOpenBackFirst);
            }
            CameraData cameraData = mCameraDatas.get(0);
            if (mCameraDevice != null && mCameraData == cameraData) {
                return mCameraDevice;
            }
            if (mCameraDevice != null) {
                releaseCamera();
            }
            try {
                mCameraDevice = Camera.open(cameraData.cameraID);
            } catch (RuntimeException e) {
                throw new CameraHardwareException(e);
            }
            if (mCameraDevice == null) {
                throw new CameraNotSupportException();
            }
            try {
                AndroidUntil.initCameraParams(mCameraDevice, cameraData, isTouchMode);
            } catch (Exception e) {
                e.printStackTrace();
                mCameraDevice.release();
                mCameraDevice = null;
                throw new CameraNotSupportException();
            }
            mCameraData = cameraData;
            mState = State.OPENED;
            return mCameraDevice;
        }

        @Override
        public void setSurfaceTexture(SurfaceTexture texture) {
            mTexture = texture;
            if (mState == State.PREVIEW && mCameraDevice != null && mTexture != null) {
                try {
                    mCameraDevice.setPreviewTexture(mTexture);
                } catch (IOException e) {
                    releaseCamera();
                }
            }
        }

        @Override
        public State getState() {
            return mState;
        }

        @Override
        public void setConfiguration() {
            isTouchMode = (Options.getInstance().camera.focusMode != Options.FocusMode.AUTO);
            isOpenBackFirst = (Options.getInstance().camera.facing != Options.Facing.FRONT);
        }

        @Override
        public void startPreview() {
            if (mState != State.OPENED) {
                return;
            }
            if (mCameraDevice == null) {
                return;
            }
            if (mTexture == null) {
                return;
            }
            try {
                mCameraDevice.setPreviewTexture(mTexture);
                mCameraDevice.startPreview();
                mState = State.PREVIEW;
            } catch (Exception e) {
                releaseCamera();
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void stopPreview() {
            if (mState != State.PREVIEW) {
                return;
            }
            if (mCameraDevice == null) {
                return;
            }
            mCameraDevice.setPreviewCallback(null);
            Camera.Parameters cameraParameters = mCameraDevice.getParameters();
            if (cameraParameters != null && cameraParameters.getFlashMode() != null
                    && !cameraParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCameraDevice.setParameters(cameraParameters);
            mCameraDevice.stopPreview();
            mState = State.OPENED;
        }

        @Override
        public void releaseCamera() {
            if (mState == State.PREVIEW) {
                stopPreview();
            }
            if (mState != State.OPENED) {
                return;
            }
            if (mCameraDevice == null) {
                return;
            }
            mCameraDevice.release();
            mCameraDevice = null;
            mCameraData = null;
            mState = State.INIT;
        }

        @Override
        public void release() {
            mCameraDatas = null;
            mTexture = null;
            isTouchMode = false;
            isOpenBackFirst = false;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void setFocusPoint(int x, int y) {
            if (mState != State.PREVIEW || mCameraDevice == null) {
                return;
            }
            if (x < -1000 || x > 1000 || y < -1000 || y > 1000) {
                Lg.d(TAG, "setFocusPoint: values are not ideal " + "x= " + x + " y= " + y);
                return;
            }
            Camera.Parameters params = mCameraDevice.getParameters();

            if (params != null && params.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusArea = new ArrayList<>();
                focusArea.add(new Camera.Area(new Rect(x, y, x + AndroidUntil.FOCUS_WIDTH, y + AndroidUntil.FOCUS_HEIGHT), 1000));

                params.setFocusAreas(focusArea);

                try {
                    mCameraDevice.setParameters(params);
                } catch (Exception e) {
                    // Ignore, we might be setting it too
                    // fast since previous attempt
                }
            } else {
                Lg.d(TAG, "Not support Touch focus mode");
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean autoFocus(Camera.AutoFocusCallback focusCallback) {
            if (mState != State.PREVIEW || mCameraDevice == null) {
                return false;
            }
            // Make sure our auto settings aren't locked
            Camera.Parameters params = mCameraDevice.getParameters();
            if (params.isAutoExposureLockSupported()) {
                params.setAutoExposureLock(false);
            }

            if (params.isAutoWhiteBalanceLockSupported()) {
                params.setAutoWhiteBalanceLock(false);
            }

            mCameraDevice.setParameters(params);
            mCameraDevice.cancelAutoFocus();
            mCameraDevice.autoFocus(focusCallback);
            return true;
        }

        private void changeFocusMode(boolean touchMode) {
            if (mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
                return;
            }
            isTouchMode = touchMode;
            mCameraData.touchFocusMode = touchMode;
            if (touchMode) {
                AndroidUntil.setTouchFocusMode(mCameraDevice);
            } else {
                AndroidUntil.setAutoFocusMode(mCameraDevice);
            }
        }

        @Override
        public void switchFocusMode() {
            changeFocusMode(!isTouchMode);
        }

        @Override
        public boolean switchCamera() {
            if (mState != State.PREVIEW) {
                return false;
            }
            try {
                CameraData camera = mCameraDatas.remove(1);
                mCameraDatas.add(0, camera);
                openCamera();
                startPreview();
                return true;
            } catch (Exception e) {
                CameraData camera = mCameraDatas.remove(1);
                mCameraDatas.add(0, camera);
                try {
                    openCamera();
                    startPreview();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
                return false;
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean switchLight() {
            if (mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
                return false;
            }
            if (!mCameraData.hasLight) {
                return false;
            }
            Camera.Parameters cameraParameters = mCameraDevice.getParameters();
            if (cameraParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            try {
                mCameraDevice.setParameters(cameraParameters);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

}
