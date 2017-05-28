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

    //render status
    int NO_ERROR = 0;
    int VIDEO_TYPE_ERROR = 1;
    int AUDIO_TYPE_ERROR = 2;
    int VIDEO_CONFIGURATION_ERROR = 3;
    int AUDIO_CONFIGURATION_ERROR = 4;
    int CAMERA_ERROR = 5;
    int AUDIO_ERROR = 6;
    int AUDIO_AEC_ERROR = 7;
    int SDK_VERSION_ERROR = 8;

    void setCameraOpenListener(CameraOpenDelegate cameraOpenListener);
    void syncVideoConfig();
    void enableRecord(@NonNull MyRecorder recorder);
    void disableRecord();
    int checkStatus();
}
