package org.yeshen.video.librtmp.core;

import android.support.annotation.NonNull;

import org.yeshen.video.librtmp.core.delegate.ICameraOpenDelegate;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface ILivingView {

    void syncConfig();

    void setDelegate(@NonNull ICameraOpenDelegate delegate);

    IRenderer getRenderer();
}
