package org.yeshen.video.librtmp.internal.net.carriers;

import org.yeshen.video.librtmp.core.IRtmpConnection;
import org.yeshen.video.librtmp.internal.net.NHandshake;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.io.RtmpConnectListener;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.io.SessionInfo;
import org.yeshen.video.librtmp.unstable.net.sender.sendqueue.ISendQueue;
import org.yeshen.video.librtmp.unstable.tools.Error;
import org.yeshen.video.librtmp.unstable.tools.Lg;
import org.yeshen.video.librtmp.unstable.tools.Options;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/*********************************************************************
 * Created by yeshen on 2017/05/18.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class NRtmpConnection implements IRtmpConnection {

    private NReader NReader = new NReader();
    private NWriter NWriter = new NWriter();
    private SocketChannel channel;

    private State state = State.INIT;
    private SessionInfo sessionInfo;
    private RtmpConnectListener listener;
    private ISendQueue mSendQueue;

    @Override
    public void connect(String url) {
        state = State.INIT;
        try {
            //start tcp connect
            InetSocketAddress inetSocketAddress = new InetSocketAddress(
                    Options.getInstance().service,
                    Options.getInstance().port);
            channel = SocketChannel.open();
            channel.connect(inetSocketAddress);
            channel.configureBlocking(false);
        } catch (IOException e) {
            Lg.e(Error.NET + state, e);
            if (listener != null) listener.onSocketConnectFail();
            return;
        }

        //handshake rtmp
        state = State.HANDSHAKE;
        boolean success = handshake(channel);
        if (!success) {
            if (listener != null) listener.onHandshakeFail();
            state = State.INIT;
            return;
        }

        try {
            //start thread to read and write
            NReader.start(channel);
            NWriter.start(channel);
        } catch (IOException e) {
            Lg.e(Error.CONNECTION, e);
        }
    }

    @Override
    public void publishAudioData(byte[] data, int type) {

    }

    @Override
    public void publishVideoData(byte[] data, int type) {

    }

    @Override
    public void stop() {
        if (NReader != null) NReader.stop();
        if (NWriter != null) NWriter.stop();
    }

    @Override
    public void setConnectListener(RtmpConnectListener listener) {
        this.listener = listener;
    }

    @Override
    public void setSendQueue(ISendQueue sendQueue) {
        this.mSendQueue = sendQueue;
    }

    @Override
    public void setVideoParams(int width, int height) {

    }

    @Override
    public void setAudioParams(int sampleRate, int sampleSize, boolean isStereo) {

    }

    private boolean handshake(SocketChannel channel) {
        NHandshake handshake = null;
        try {
            handshake = new NHandshake(channel);
            handshake.writeC0C1();
            handshake.readS0S1();
            handshake.writeC2();
            handshake.readS2();
            handshake.done();
        } catch (Exception e) {
            if (handshake != null) handshake.done();
            Lg.e(Error.HANDSHAKE + state, e);
            return false;
        }
        return true;
    }

}
