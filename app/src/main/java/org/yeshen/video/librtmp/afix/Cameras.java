package org.yeshen.video.librtmp.afix;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

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

    public abstract CameraMessage getCurrentCamera();

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

        private List<CameraMessage> mCameraMessage;
        private Camera mCameraDevice;
        private CameraMessage mCurrentCamera;
        private State mState;
        private SurfaceTexture mTexture;
        private boolean isTouchMode = false;
        private boolean isOpenBackFirst = false;

        public CameraImpl() {
            mState = State.INIT;
        }

        @Override
        public CameraMessage getCurrentCamera() {
            return mCurrentCamera;
        }

        @Override
        public boolean isLandscape() {
            return (Options.getInstance().camera.orientation != Options.Orientation.PORTRAIT);
        }

        @SuppressWarnings("deprecation")
        @Override
        public Camera openCamera()
                throws CameraHardwareException, CameraNotSupportException {
            if (mCameraMessage == null || mCameraMessage.size() == 0) {
                mCameraMessage = AndroidUntil.getAllCamerasData(isOpenBackFirst);
            }
            CameraMessage cameraData = mCameraMessage.get(0);
            if (mCameraDevice != null && mCurrentCamera == cameraData) {
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
            mCurrentCamera = cameraData;
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
            mCurrentCamera = null;
            mState = State.INIT;
        }

        @Override
        public void release() {
            mCameraMessage = null;
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
            if (mState != State.PREVIEW || mCameraDevice == null || mCurrentCamera == null) {
                return;
            }
            isTouchMode = touchMode;
            mCurrentCamera.touchFocusMode = touchMode;
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
                CameraMessage camera = mCameraMessage.remove(1);
                mCameraMessage.add(0, camera);
                openCamera();
                startPreview();
                return true;
            } catch (Exception e) {
                CameraMessage camera = mCameraMessage.remove(1);
                mCameraMessage.add(0, camera);
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
            if (mState != State.PREVIEW || mCameraDevice == null || mCurrentCamera == null) {
                return false;
            }
            if (!mCurrentCamera.hasLight) {
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

    public static class CameraMessage {

        public static final int FACING_FRONT = 1;
        public static final int FACING_BACK = 2;

        public int cameraID;            //camera的id
        public int cameraFacing;        //区分前后摄像头
        public int cameraWidth;         //camera的宽度
        public int cameraHeight;        //camera的高度
        public boolean hasLight;
        public int orientation;
        public boolean supportTouchFocus;
        public boolean touchFocusMode;

        public CameraMessage(int id, int facing) {
            cameraID = id;
            cameraFacing = facing;
        }
    }
}
