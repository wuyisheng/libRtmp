package org.yeshen.video.librtmp.core;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public interface IEffect {

    void prepare();
    void setTextureId(int textureId);
    int getEffectedTextureId();
    void drawFromCameraPreview(final float[] tex_mtx);

}
