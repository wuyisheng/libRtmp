package org.yeshen.video.librtmp.unstable;

import org.yeshen.video.librtmp.core.IRenderer;
import org.yeshen.video.librtmp.core.IVideo;
import org.yeshen.video.librtmp.core.delegate.IVideoEncodeDelegate;
import org.yeshen.video.librtmp.unstable.tools.Lg;

public class ControllerCameraVideo implements IVideo {
    private RecorderImpl mRecorderImpl;
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
        mRecorderImpl = new RecorderImpl();
        mRecorderImpl.prepare(mListener);
        mRenderer.enableRecord(mRecorderImpl);
    }

    public void stop() {
        Lg.d( "Stop video recording");
        mRenderer.disableRecord();
        if(mRecorderImpl != null) {
            mRecorderImpl.stop();
            mRecorderImpl = null;
        }
    }

    public void pause() {
        Lg.d( "Pause video recording");
        if(mRecorderImpl != null) {
            mRecorderImpl.pause();
        }
    }

    public void resume() {
        Lg.d( "Resume video recording");
        if(mRecorderImpl != null) {
            mRecorderImpl.resume();
        }
    }

    public boolean setVideoBps(int bps) {
        return mRecorderImpl != null && mRecorderImpl.resetVideoBps(bps);
    }
}
