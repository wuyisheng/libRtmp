package org.yeshen.video.librtmp.internal.net.carriers;

import android.os.Process;

import org.yeshen.video.librtmp.unstable.tools.Error;
import org.yeshen.video.librtmp.unstable.tools.Lg;
import org.yeshen.video.librtmp.unstable.tools.Options;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

class NReader implements Runnable {
    private static final String TAG = "org.yeshen.video.librtmp.internal.net.carriers.NReader";

    private Selector selector = null;
    private Thread thread = null;
    private volatile boolean running = false;

    void start(SocketChannel channel) throws IOException {
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        if (!running) {
            running = true;
            thread = new Thread(this, TAG);
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
        } else {
            if (selector != null) {
                selector.wakeup();
            }
        }
    }

    void stop() {
        if (running) {
            running = false;
            if (!Thread.currentThread().equals(thread)) {
                selector.wakeup();
            }
        }
        try {
            if (selector != null) selector.close();
        } catch (IOException e) {
            Lg.e(e);
        }
        selector = null;
        thread = null;
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(Options.getInstance().bufferSize);
        while (running) {
            try {
                selector.select();
                Set selectedKeys = selector.selectedKeys();
                for (Object selectedKey : selectedKeys) {
                    SelectionKey key = (SelectionKey) selectedKey;
                    SocketChannel channel = (SocketChannel) key.channel();
                    channel.read(buffer);


                    //TODO
                }
            } catch (IOException e) {
                Lg.e(Error.READER, e);
            }
        }
    }
}
