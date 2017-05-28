package org.yeshen.video.librtmp.afix.net.sender;

/**
 * @Title: Sender
 * @Package org.yeshen.video.librtmp.afix.net.sender.rtmp
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 上午11:25
 * @Version
 */
public interface Sender {
    void start();
    void onData(byte[] data, int type);
    void stop();
}
