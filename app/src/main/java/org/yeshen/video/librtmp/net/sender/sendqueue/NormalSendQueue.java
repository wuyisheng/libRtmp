package org.yeshen.video.librtmp.net.sender.sendqueue;


import org.yeshen.video.librtmp.net.Frame;
import org.yeshen.video.librtmp.tools.Lg;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.yeshen.video.librtmp.net.Frame.FRAME_TYPE_AUDIO;
import static org.yeshen.video.librtmp.net.Frame.FRAME_TYPE_INTER_FRAME;
import static org.yeshen.video.librtmp.net.Frame.FRAME_TYPE_KEY_FRAME;

/**
 * @Title: NormalSendQueue
 * @Package org.yeshen.video.librtmp.net.sender.sendqueue
 * @Description:
 * @Author Jim
 * @Date 2016/11/21
 * @Time 上午10:33
 * @Version
 */

public class NormalSendQueue implements ISendQueue {
    private static final int NORMAL_FRAME_BUFFER_SIZE = 800;
    private ArrayBlockingQueue<Frame> mFrameBuffer;
    private int mFullQueueCount = NORMAL_FRAME_BUFFER_SIZE;
    private AtomicInteger mTotalFrameCount = new AtomicInteger(0);  //总个数
    private AtomicInteger mGiveUpFrameCount = new AtomicInteger(0);  //总个数

    private AtomicInteger mInFrameCount = new AtomicInteger(0);  //进入总个数
    private AtomicInteger mOutFrameCount = new AtomicInteger(0);  //输出总个数
    private volatile boolean mScanFlag;
    private SendQueueListener mSendQueueListener;

    public NormalSendQueue() {
        mFrameBuffer = new ArrayBlockingQueue<>(mFullQueueCount, true);
    }

    @Override
    public void start() {
        mScanFlag = true;
        ScanThread mScanThread = new ScanThread();
        mScanThread.start();
    }

    @Override
    public void stop() {
        mScanFlag = false;
        mInFrameCount.set(0);
        mOutFrameCount.set(0);
        mTotalFrameCount.set(0);
        mGiveUpFrameCount.set(0);
        mFrameBuffer.clear();
    }

    public void setSendQueueListener(SendQueueListener listener) {
        mSendQueueListener = listener;
    }

    @Override
    public void setBufferSize(int size) {
        mFullQueueCount = size;
    }

    @Override
    public void putFrame(Frame frame) {
        if (mFrameBuffer == null) {
            return;
        }
        abandonData();
        try {
            mFrameBuffer.put(frame);
            mInFrameCount.getAndIncrement();
            mTotalFrameCount.getAndIncrement();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Frame takeFrame() {
        if (mFrameBuffer == null) {
            return null;
        }
        Frame frame = null;
        try {
            frame = mFrameBuffer.take();
            mOutFrameCount.getAndIncrement();
            mTotalFrameCount.getAndDecrement();
        } catch (InterruptedException e) {
            //do nothing
        }
        return frame;
    }

    private void abandonData() {
        if (mTotalFrameCount.get() >= (mFullQueueCount / 2)) {
            //从队列头部开始搜索，删除最早发现的连续P帧
            boolean pFrameDelete = false;
            boolean start = false;
            for (Frame frame : mFrameBuffer) {
                if (frame.frameType == FRAME_TYPE_INTER_FRAME) {
                    start = true;
                }
                if (start) {
                    if (frame.frameType == FRAME_TYPE_INTER_FRAME) {
                        mFrameBuffer.remove(frame);
                        mTotalFrameCount.getAndDecrement();
                        mGiveUpFrameCount.getAndIncrement();
                        pFrameDelete = true;
                    } else if (frame.frameType == FRAME_TYPE_KEY_FRAME) {
                        break;
                    }
                }
            }
            boolean kFrameDelete = false;
            //从队列头部开始搜索，删除最早发现的I帧
            if (!pFrameDelete) {
                for (Frame frame : mFrameBuffer) {
                    if (frame.frameType == FRAME_TYPE_KEY_FRAME) {
                        mFrameBuffer.remove(frame);
                        mTotalFrameCount.getAndDecrement();
                        mGiveUpFrameCount.getAndIncrement();
                        kFrameDelete = true;
                        break;
                    }
                }
            }
            //从队列头部开始搜索，删除音频
            if (!pFrameDelete && !kFrameDelete) {
                for (Frame frame : mFrameBuffer) {
                    if (frame.frameType == FRAME_TYPE_AUDIO) {
                        mFrameBuffer.remove(frame);
                        mTotalFrameCount.getAndDecrement();
                        mGiveUpFrameCount.getAndIncrement();
                        break;
                    }
                }
            }
        }
    }

    private class ScanThread extends Thread {
        private static final int SCAN_MAX_TIME = 6;
        private int mCurrentScanTime = 0;
        private ArrayList<ScanSnapShot> mScanSnapShotList = new ArrayList<>();

        @Override
        public void run() {
            while (mScanFlag) {
                //达到仲裁次数了
                if (mCurrentScanTime == SCAN_MAX_TIME) {
                    int averageDif = 0;
                    int negativeCounter = 0;
                    String strLog = "";
                    for (int i = 0; i < SCAN_MAX_TIME; i++) {
                        int dif = mScanSnapShotList.get(i).outCount - mScanSnapShotList.get(i).inCount;
                        if (dif < 0) {
                            negativeCounter++;
                        }
                        averageDif += dif;
                        strLog = strLog + String.format(Locale.CHINA, "n%d:%d  ", i, dif);
                    }
                    Lg.d(strLog);
                    if (negativeCounter >= 4 || averageDif < -100) {
                        //坏
                        Lg.d("Bad Send Speed.");
                        if (mSendQueueListener != null) {
                            mSendQueueListener.bad();
                        }
                    } else {
                        //好
                        Lg.d("Good Send Speed.");
                        if (mSendQueueListener != null) {
                            mSendQueueListener.good();
                        }
                    }
                    //清空
                    mScanSnapShotList.clear();
                    mCurrentScanTime = 0;
                }
                mScanSnapShotList.add(new ScanSnapShot(mInFrameCount.get(), mOutFrameCount.get()));
                mInFrameCount.set(0);
                mOutFrameCount.set(0);
                mCurrentScanTime++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ScanSnapShot {
        int inCount;
        int outCount;

        ScanSnapShot(int inCount, int outCount) {
            this.inCount = inCount;
            this.outCount = outCount;
        }
    }
}
