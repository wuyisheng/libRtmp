package org.yeshen.video.librtmp.afix;

import org.yeshen.video.librtmp.core.IRenderer;
import org.yeshen.video.librtmp.core.IVideo;
import org.yeshen.video.librtmp.core.delegate.IVideoEncodeDelegate;
import org.yeshen.video.librtmp.tools.Lg;

public class ControllerCameraVideo implements IVideo {
    private MyRecorder mRecorder;
    private IRenderer mRenderer;
    private IVideoEncodeDelegate mListener;

    public ControllerCameraVideo(IRenderer renderer) {
        mRenderer = renderer;
        mRenderer.syncVideoConfig();
    }

    public void setVideoConfiguration() {
        mRenderer.syncVideoConfig();
    }

    public void setVideoEncoderListener(IVideoEncodeDelegate listener) {
        mListener = listener;
    }

    public void start() {
        if(mListener == null) {
            return;
        }
        Lg.d( "Start video recording");
        mRecorder = new MyRecorder();
        mRecorder.prepare(mListener);
        mRenderer.enableRecord(mRecorder);
    }

    public void stop() {
        Lg.d( "Stop video recording");
        mRenderer.disableRecord();
        if(mRecorder != null) {
            mRecorder.stop();
            mRecorder = null;
        }
    }

    public void pause() {
        Lg.d( "Pause video recording");
        if(mRecorder != null) {
            mRecorder.pause();
        }
    }

    public void resume() {
        Lg.d( "Resume video recording");
        if(mRecorder != null) {
            mRecorder.resume();
        }
    }

    public boolean setVideoBps(int bps) {
        return mRecorder != null && mRecorder.resetVideoBps(bps);
    }
}
