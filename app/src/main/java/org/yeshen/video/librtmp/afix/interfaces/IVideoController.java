package org.yeshen.video.librtmp.afix.interfaces;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface IVideoController {
    void start();
    void stop();
    void pause();
    void resume();
    boolean setVideoBps(int bps);
    void setVideoEncoderListener(OnVideoEncodeListener listener);
    void setVideoConfiguration();
}
