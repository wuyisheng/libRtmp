package org.yeshen.video.librtmp.internal.net.packets.messages;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public class Acknowledgement extends Message {
    @Override
    int type() {
        return TYPE_ACKNOWLEDGEMENT;
    }
}
