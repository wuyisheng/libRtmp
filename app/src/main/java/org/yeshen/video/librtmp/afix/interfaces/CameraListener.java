package org.yeshen.video.librtmp.afix.interfaces;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public interface CameraListener {
    int CAMERA_NOT_SUPPORT = 1;
    int NO_CAMERA = 2;
    int CAMERA_DISABLED = 3;
    int CAMERA_OPEN_FAILED = 4;

    void onOpenSuccess();
    void onOpenFail(int error);
    void onCameraChange();
}
