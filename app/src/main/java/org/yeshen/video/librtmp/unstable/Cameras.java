package org.yeshen.video.librtmp.unstable;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import org.yeshen.video.librtmp.core.ICamera;
import org.yeshen.video.librtmp.unstable.exception.CameraHardwareException;
import org.yeshen.video.librtmp.unstable.exception.CameraNotSupportException;
import org.yeshen.video.librtmp.unstable.tools.AndroidUntil;
import org.yeshen.video.librtmp.unstable.tools.Options;

import java.io.IOException;
import java.util.List;

/*********************************************************************
 * Created by yeshen on 2017/05/26.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public abstract class Cameras implements ICamera {

    private static final Cameras sInstance =new CameraImpl();
    public static Cameras instance() {
        return sInstance;
    }

    public enum State {
        INIT,
        OPENED,
        PREVIEW
    }

    private static class CameraImpl extends Cameras {
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

        @Override
        public void openCamera()
                throws CameraHardwareException, CameraNotSupportException {
            if (mCameraMessage == null || mCameraMessage.size() == 0) {
                mCameraMessage = AndroidUntil.getAllCamerasData(isOpenBackFirst);
            }
            CameraMessage cameraData = mCameraMessage.get(0);
            if (mCameraDevice != null && mCurrentCamera == cameraData) {
                return;
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
        }

        @Override
        public void placePreview(SurfaceTexture texture) {
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
        public void syncConfig() {
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
