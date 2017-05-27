package org.yeshen.video.librtmp.ui;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import org.yeshen.video.librtmp.afix.LivingCameras;
import org.yeshen.video.librtmp.carriers.Connector;
import org.yeshen.video.librtmp.carriers.Streamer;
import org.yeshen.video.librtmp.tools.Toasts;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class VideoView extends GLSurfaceView {

    private LivingCameras camera;
    private Streamer streamer;

    public VideoView(Context context) {
        super(context);
    }

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init() {
        streamer = new Streamer();
        camera = new LivingCameras();
        camera.delegate = streamer;
        camera.attach(this);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.stop();
        if (camera != null) camera.destroy();
    }

    public void pull() {
        if (streamer != null) streamer.stop();
        if (camera != null) camera.stop();
    }

    public void push() {
        //if (camera != null) camera.record();
        if (streamer != null) streamer.start(new Connector.ConnectionResult() {
            @Override
            public void success() {
                Toasts.str("connection success!");
            }

            @Override
            public void fail(int code, String msg) {
                Toasts.str("connection fail!");
            }
        });
    }

    public void stop() {
        if (streamer != null) streamer.stop();
        if (camera != null) camera.stop();
    }

    public interface StreamCallback {
        void stream(byte[] data);
    }
}
