package org.yeshen.video.librtmp.unstable.net.sender.rtmp.io;

import org.yeshen.video.librtmp.unstable.net.Frame;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Chunk;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Command;
import org.yeshen.video.librtmp.unstable.net.sender.sendqueue.ISendQueue;

import java.io.IOException;
import java.io.OutputStream;

/**
 * RTMPConnection's write thread
 * 
 * @author francois, leo
 */
class WriteThread extends Thread {

    private OutputStream out;
    private SessionInfo sessionInfo;
    private OnWriteListener listener;
    private ISendQueue mSendQueue;
    private volatile boolean startFlag;

    WriteThread(OutputStream out, SessionInfo sessionInfo) {
        this.out = out;
        this.sessionInfo = sessionInfo;
        this.startFlag = true;
    }

    void setWriteListener(OnWriteListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        while (startFlag) {
            try {
                Frame<Chunk> frame = mSendQueue.takeFrame();
                if(frame != null) {
                    Chunk chunk = frame.data;
                    chunk.writeTo(out, sessionInfo);
                    if (chunk instanceof Command) {
                        Command command = (Command) chunk;
                        sessionInfo.addInvokedCommand(command.getTransactionId(), command.getCommandName());
                    }
                    out.flush();
                }
            } catch (IOException e) {
                startFlag = false;
                if(listener != null) {
                    listener.onDisconnect();
                }
            }
        }
    }

    void setSendQueue(ISendQueue sendQueue) {
        mSendQueue = sendQueue;
    }

    void shutdown() {
        listener = null;
        startFlag = false;
        this.interrupt();
    }
}
