package org.yeshen.video.librtmp.internal.net.packets.messages;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public class SetPeerBandwidth extends Message {

    public static final short LIMIT_HARD = 0;
    public static final short LIMIT_SOFT = 1;
    public static final short LIMIT_DYNAMIC = 2;

    @Override
    int type() {
        return TYPE_SET_PEER_BANDWIDTH;
    }
}
