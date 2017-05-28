package org.yeshen.video.librtmp.core;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;

import org.yeshen.video.librtmp.afix.MyRecorder;
import org.yeshen.video.librtmp.core.delegate.CameraOpenDelegate;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface IRenderer extends GLSurfaceView.Renderer
        , SurfaceTexture.OnFrameAvailableListener {

    void setCameraOpenListener(CameraOpenDelegate cameraOpenListener);
    void syncVideoConfig();
    void enableRecord(@NonNull MyRecorder recorder);
    void disableRecord();
    boolean isCameraOpen();
}
