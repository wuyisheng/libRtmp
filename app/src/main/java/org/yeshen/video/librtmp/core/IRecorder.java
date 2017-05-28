package org.yeshen.video.librtmp.core;

import android.support.annotation.NonNull;

import org.yeshen.video.librtmp.core.delegate.IVideoEncodeDelegate;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface IRecorder {
    void pause();
    void resume();
    void prepare(@NonNull IVideoEncodeDelegate delegate);
    boolean isFirstSetup();
    void startSwapAsync();
    void makeCurrent();
    void swapBuffers();
    void stop();

    boolean resetVideoBps(int bps);
}
