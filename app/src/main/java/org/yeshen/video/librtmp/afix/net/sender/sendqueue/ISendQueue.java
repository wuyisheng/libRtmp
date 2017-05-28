package org.yeshen.video.librtmp.afix.net.sender.sendqueue;


import org.yeshen.video.librtmp.afix.net.Frame;

/**
 * @Title: ISendQueue
 * @Package org.yeshen.video.librtmp.afix.net.sender.sendqueue
 * @Description:
 * @Author Jim
 * @Date 2016/11/21
 * @Time 上午10:15
 * @Version
 */

public interface ISendQueue {
    void start();
    void stop();
    void setBufferSize(int size);
    void putFrame(Frame frame);
    Frame takeFrame();
    void setSendQueueListener(SendQueueListener listener);
}
