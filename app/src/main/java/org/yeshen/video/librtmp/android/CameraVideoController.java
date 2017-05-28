package org.yeshen.video.librtmp.android;

import android.os.Build;

import org.yeshen.video.librtmp.afix.interfaces.IVideoController;
import org.yeshen.video.librtmp.afix.interfaces.OnVideoEncodeListener;
import org.yeshen.video.librtmp.tools.Lg;

/**
 * @Title: CameraVideoController
 * @Package com.laifeng.sopcastsdk.controller.video
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午12:54
 * @Version
 */

public class CameraVideoController implements IVideoController {
    private MyRecorder mRecorder;
    private MyRenderer mRenderer;
    private OnVideoEncodeListener mListener;

    public CameraVideoController(MyRenderer renderer) {
        mRenderer = renderer;
        mRenderer.setVideoConfiguration();
    }

    public void setVideoConfiguration() {
        mRenderer.setVideoConfiguration();
    }

    public void setVideoEncoderListener(OnVideoEncodeListener listener) {
        mListener = listener;
    }

    public void start() {
        if(mListener == null) {
            return;
        }
        Lg.d( "Start video recording");
        mRecorder = new MyRecorder();
        mRecorder.setVideoEncodeListener(mListener);
        mRecorder.prepareEncoder();
        mRenderer.setRecorder(mRecorder);
    }

    public void stop() {
        Lg.d( "Stop video recording");
        mRenderer.setRecorder(null);
        if(mRecorder != null) {
            mRecorder.setVideoEncodeListener(null);
            mRecorder.stop();
            mRecorder = null;
        }
    }

    public void pause() {
        Lg.d( "Pause video recording");
        if(mRecorder != null) {
            mRecorder.setPause(true);
        }
    }

    public void resume() {
        Lg.d( "Resume video recording");
        if(mRecorder != null) {
            mRecorder.setPause(false);
        }
    }

    public boolean setVideoBps(int bps) {
        //重新设置硬编bps，在低于19的版本需要重启编码器
        boolean result = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //由于重启硬编编码器效果不好，此次不做处理
            Lg.d( "Bps need change, but MediaCodec do not support.");
        }else {
            if (mRecorder != null) {
                Lg.d( "Bps change, current bps: " + bps);
                mRecorder.setRecorderBps(bps);
                result = true;
            }
        }
        return result;
    }
}
