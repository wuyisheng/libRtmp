package org.yeshen.video.librtmp.packets.messages;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public abstract class Message {

    public final int TYPE_DEFAULT = 0;
    public final int TYPE_SET_CHUNK_SIZE = 1;
    public final int TYPE_ABORT = 2;
    public final int TYPE_ACKNOWLEDGEMENT = 3;
    public final int TYPE_WINDOW_ACKNOWLEDGEMENT_SIZE = 5;
    public final int TYPE_SET_PEER_BANDWIDTH = 6;

    abstract int type();

}
