package org.yeshen.video.librtmp.internal.net.carriers;

import org.yeshen.video.librtmp.internal.net.HandshakeHelper;
import org.yeshen.video.librtmp.internal.net.packets.Chunkd;
import org.yeshen.video.librtmp.unstable.net.sender.Sender;
import org.yeshen.video.librtmp.unstable.tools.Error;
import org.yeshen.video.librtmp.unstable.tools.GlobalAsyncThread;
import org.yeshen.video.librtmp.unstable.tools.Lg;
import org.yeshen.video.librtmp.unstable.tools.Options;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/*********************************************************************
 * Created by yeshen on 2017/05/18.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class Connector implements Sender {

    private Reader reader = new Reader();
    private Writer writer = new Writer();
    private SocketChannel channel;

    public void create() {
    }

    public void connect() {
        GlobalAsyncThread.post(new Runnable() {
            @Override
            public void run() {
                try {
                    //start tcp connect
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(
                            Options.getInstance().service,
                            Options.getInstance().port);
                    channel = SocketChannel.open();
                    channel.connect(inetSocketAddress);
                    channel.configureBlocking(false);
                } catch (IOException e) {
                    Lg.e(Error.NET, e);
                }

                try {
                    //handshake rtmp
                    new HandshakeHelper().shakeInSequence(channel);
                } catch (IOException e) {
                    Lg.e(Error.HANDSHAKE, e);
                }

                try {
                    //start thread to read and write
                    reader.start(channel);
                    writer.start(channel);
                } catch (IOException e) {
                    Lg.e(Error.CONNECTION, e);
                }
            }
        });
    }

    public void write(byte[] data) {
        Chunkd chunkd = new Chunkd();

        writer.post(chunkd);
    }

    public void stop() {
        if (reader != null) reader.stop();
        if (writer != null) writer.stop();
    }

    @Override
    public void start() {

    }

    @Override
    public void onData(byte[] data, int type) {

    }

}
