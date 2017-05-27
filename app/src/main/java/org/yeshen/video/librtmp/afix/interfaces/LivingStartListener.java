package org.yeshen.video.librtmp.afix.interfaces;

/*********************************************************************
 * Created by yeshen on 2017/05/26.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface LivingStartListener {
    void startError(int error);
    void startSuccess();
}
