package org.yeshen.video.librtmp.afix.interfaces;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface OnAudioEncodeListener {
    void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi);
}
