package org.yeshen.video.librtmp.core;

import android.graphics.SurfaceTexture;

import org.yeshen.video.librtmp.afix.Cameras;
import org.yeshen.video.librtmp.exception.CameraHardwareException;
import org.yeshen.video.librtmp.exception.CameraNotSupportException;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface ICamera {
    void openCamera() throws CameraHardwareException, CameraNotSupportException;
    void placePreview(SurfaceTexture texture);
    void startPreview();
    void stopPreview();
    void releaseCamera();
    void release();

    Cameras.CameraMessage getCurrentCamera();
    boolean isLandscape();
    Cameras.State getState();
    void syncConfig();
}
