package org.yeshen.video.librtmp.internal.net.carriers;

import org.yeshen.video.librtmp.internal.encodes.AudioEncoder;
import org.yeshen.video.librtmp.unstable.tools.Lg;

import java.util.Arrays;

/*********************************************************************
 * Created by yeshen on 2017/05/20.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class Streamer {
    private AudioEncoder encoder;
    private Connector connector;

    public void start(final Connector.ConnectionResult callback) {
        if (connector == null) {
            connector = new Connector();
        } else {
            connector.stop();
        }
        encoder = new AudioEncoder();
        connector.create();
        connector.connect();
    }

    public void stop() {
        if (connector != null) connector.stop();
    }

    public void stream(byte[] raw) {
        if (encoder == null || connector == null) return;
        byte[] data = encoder.encode(raw);
        connector.write(data);

        Lg.e("writing stream " + Arrays.hashCode(data));
    }
}
