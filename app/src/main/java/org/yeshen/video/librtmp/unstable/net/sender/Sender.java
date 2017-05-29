package org.yeshen.video.librtmp.unstable.net.sender;

public interface Sender {
    void start();
    void onData(byte[] data, int type);
    void stop();
}
