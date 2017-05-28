package org.yeshen.video.librtmp.core;

import org.yeshen.video.librtmp.core.delegate.IAudioEncodeDelegate;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface IAudio {
    void start();
    void stop();
    void pause();
    void resume();
    void mute(boolean mute);
    int getSessionId();
    void setAudioEncodeListener(IAudioEncodeDelegate listener);
}
