package org.yeshen.video.librtmp.packets.messages;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public class Abort extends Message {
    @Override
    int type() {
        return TYPE_ABORT;
    }
}
