package org.yeshen.video.librtmp.unstable.net.sender.sendqueue;

/**
 * @Title: SendQueueListener
 * @Package org.yeshen.video.librtmp.afix.net.sender.sendqueue
 * @Description:
 * @Author Jim
 * @Date 2016/11/21
 * @Time 下午3:19
 * @Version
 */

public interface SendQueueListener {
    void good();
    void bad();
}
