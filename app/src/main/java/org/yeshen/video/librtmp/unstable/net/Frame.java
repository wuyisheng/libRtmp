package org.yeshen.video.librtmp.unstable.net;

import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Chunk;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class Frame {
    public static final int FRAME_TYPE_AUDIO = 1;
    public static final int FRAME_TYPE_KEY_FRAME = 2;
    public static final int FRAME_TYPE_INTER_FRAME = 3;
    public static final int FRAME_TYPE_CONFIGURATION = 4;

    public Chunk data;
    public int packetType;
    public int frameType;

    public Frame(Chunk data, int packetType, int frameType) {
        this.data = data;
        this.packetType = packetType;
        this.frameType = frameType;
    }
}
