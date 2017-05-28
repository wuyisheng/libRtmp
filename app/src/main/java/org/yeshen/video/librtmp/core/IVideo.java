package org.yeshen.video.librtmp.core;

import org.yeshen.video.librtmp.core.delegate.IVideoEncodeDelegate;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface IVideo {
    void start();
    void stop();
    void pause();
    void resume();
    boolean setVideoBps(int bps);
    void setVideoEncoderListener(IVideoEncodeDelegate listener);
    void setVideoConfiguration();
}
