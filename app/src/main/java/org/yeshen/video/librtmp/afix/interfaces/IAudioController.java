package org.yeshen.video.librtmp.afix.interfaces;

import org.yeshen.video.librtmp.android.AudioConfiguration;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface IAudioController {
    void start();
    void stop();
    void pause();
    void resume();
    void mute(boolean mute);
    int getSessionId();
    void setAudioConfiguration(AudioConfiguration audioConfiguration);
    void setAudioEncodeListener(OnAudioEncodeListener listener);
}
