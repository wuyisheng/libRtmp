package org.yeshen.video.librtmp.core;

import android.support.annotation.NonNull;

import org.yeshen.video.librtmp.afix.net.packer.Packer;
import org.yeshen.video.librtmp.afix.net.sender.Sender;
import org.yeshen.video.librtmp.core.delegate.ILivingDelegate;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface ILivingView {

    void start();
    void stop();
    void pause();
    void resume();
    void destroy();

    void setDelegate(@NonNull ILivingDelegate delegate);
    void setPacker(@NonNull Packer packer);
    void setSender(@NonNull Sender sender);

    void syncConfig();
    boolean setVideoBps(int bps);
}
