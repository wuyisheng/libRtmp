package org.yeshen.video.librtmp.net.carriers;

import android.os.AsyncTask;

import org.yeshen.video.librtmp.afix.net.sender.Sender;
import org.yeshen.video.librtmp.afix.net.sender.rtmp.RtmpSender;
import org.yeshen.video.librtmp.afix.net.sender.sendqueue.SendQueueListener;
import org.yeshen.video.librtmp.net.packets.Chunkd;
import org.yeshen.video.librtmp.tools.Error;
import org.yeshen.video.librtmp.net.HandshakeHelper;
import org.yeshen.video.librtmp.tools.Lg;
import org.yeshen.video.librtmp.tools.Options;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/*********************************************************************
 * Created by yeshen on 2017/05/18.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class Connector implements Sender, SendQueueListener {

    public static Connector createConnector() {
        return new Connector();
    }

    private Reader reader = new Reader();
    private Writer writer = new Writer();
    private SocketChannel channel;
    private volatile boolean running = false;
    private RtmpSender.OnSenderListener listener;

    public void create() {
    }

    public void connect() {
        new AsyncTask<Boolean, Boolean, Boolean>() {
            @Override
            protected Boolean doInBackground(Boolean... params) {
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
                    return false;
                }

                try {
                    //handshake rtmp
                    new HandshakeHelper().shakeInSequence(channel);
                } catch (IOException e) {
                    Lg.e(Error.HANDSHAKE, e);
                    return false;
                }

                try {
                    //start thread to read and write
                    reader.start(channel);
                    writer.start(channel);
                } catch (IOException e) {
                    Lg.e(Error.CONNECTION, e);
                    return false;
                }
                running = true;
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (listener != null) {
                    if (result) {
                        listener.onConnected();
                    } else {
                        listener.onPublishFail();
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void write(byte[] data) {
        Chunkd chunkd = new Chunkd();

        writer.post(chunkd);
    }

    public void stop() {
        if (reader != null) reader.stop();
        if (writer != null) writer.stop();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void setSenderListener(RtmpSender.OnSenderListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {

    }

    @Override
    public void onData(byte[] data, int type) {

    }

    @Override
    public void good() {

    }

    @Override
    public void bad() {

    }

    public interface ConnectionResult {

        void success();

        void fail(int code, String msg);

    }

}
