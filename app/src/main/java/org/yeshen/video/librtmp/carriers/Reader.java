package org.yeshen.video.librtmp.carriers;

import android.os.Process;

import org.yeshen.video.librtmp.tools.Error;
import org.yeshen.video.librtmp.tools.Lg;
import org.yeshen.video.librtmp.tools.Options;

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

class Reader implements Runnable {
    private static final String TAG = "org.yeshen.video.librtmp.carriers.Reader";

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
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Options.getInstance().bufferSize);
            while (running) {
                int num = selector.select();//blocking
                if (num == 0) {
                    Set selectedKeys = selector.selectedKeys();
                    for (Object selectedKey : selectedKeys) {
                        SelectionKey key = (SelectionKey) selectedKey;
                        SocketChannel channel = (SocketChannel) key.channel();
                        channel.read(buffer);


                        //TODO
                    }
                }
            }
        } catch (IOException e) {
            Lg.e(Error.READER, e);
        }
    }
}
