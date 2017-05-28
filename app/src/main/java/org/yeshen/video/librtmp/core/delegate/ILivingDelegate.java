package org.yeshen.video.librtmp.core.delegate;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface ILivingDelegate {
    void cameraSuccess();
    void cameraError(int error);

    void livingSuccess();
    void livingFail(int error);
}
