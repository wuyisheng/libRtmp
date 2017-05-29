package org.yeshen.video.librtmp;

import org.yeshen.video.librtmp.unstable.DataWatcher;
import org.yeshen.video.librtmp.unstable.net.packer.rtmp.RtmpPacker;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.RtmpSender;
import org.yeshen.video.librtmp.core.ILivingView;
import org.yeshen.video.librtmp.core.delegate.ICameraOpenDelegate;
import org.yeshen.video.librtmp.unstable.tools.Lg;
import org.yeshen.video.librtmp.unstable.tools.Options;
import org.yeshen.video.librtmp.unstable.tools.Toasts;

/*********************************************************************
 * Created by yeshen on 2017/05/29.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


class LivingController {

    private RtmpSender mRtmpSender;
    private DataWatcher mDataWatcher;
    private int mCurrentBps;

    LivingController(ILivingView view) {
        Options.getInstance().setOrientation(Options.Orientation.PORTRAIT);
        Options.getInstance().setFacing(Options.Facing.BACK);
        Options.getInstance().setSize(640, 360);
        view.syncConfig();

        view.setDelegate(new ICameraOpenDelegate() {
            @Override
            public void onOpenSuccess() {
                Toasts.str("camera open success");
            }

            @Override
            public void onOpenFail(int error) {
                Toasts.str("camera open fail");
            }
        });


        mRtmpSender = RtmpSender.getRtmpSender();
        mRtmpSender.setSenderListener(new RtmpSender.OnSenderListener() {
            @Override
            public void onConnecting() {

            }

            @Override
            public void onConnected() {
                mDataWatcher.start();
                mCurrentBps = Options.getInstance().video.maxBps;
            }

            @Override
            public void onDisConnected() {
                Toasts.str("fail to live");
                mDataWatcher.stop();
            }

            @Override
            public void onPublishFail() {
                Toasts.str("fail to publish stream");
            }

            @Override
            public void onNetGood() {
                if (mCurrentBps + 50 <= Options.getInstance().video.maxBps) {
                    Lg.d("BPS_CHANGE good up 50");
                    int bps = mCurrentBps + 50;
                    if (mDataWatcher != null) {
                        boolean result = mDataWatcher.setVideoBps(bps);
                        if (result) {
                            mCurrentBps = bps;
                        }
                    }
                } else {
                    Lg.d("BPS_CHANGE good good good");
                }
                Lg.d("Current Bps: " + mCurrentBps);
            }

            @Override
            public void onNetBad() {
                if (mCurrentBps - 100 >= Options.getInstance().video.minBps) {
                    Lg.d("BPS_CHANGE bad down 100");
                    int bps = mCurrentBps - 100;
                    if (mDataWatcher != null) {
                        boolean result = mDataWatcher.setVideoBps(bps);
                        if (result) {
                            mCurrentBps = bps;
                        }
                    }
                } else {
                    Lg.d("BPS_CHANGE bad down 100");
                }
                Lg.d("Current Bps: " + mCurrentBps);
            }
        });

        //data watcher
        mDataWatcher = new DataWatcher(view.getRenderer());
        mDataWatcher.syncVideoConfig();
        //初始化flv打包器
        RtmpPacker packer = new RtmpPacker();
        packer.initAudioParams(Options.DEFAULT_FREQUENCY, 16, false);
        mDataWatcher.setPacker(packer);
        mDataWatcher.setSender(mRtmpSender);
    }

    void push() {
        Toasts.str("start connecting");
        mRtmpSender.connect();
    }

    void close() {
        mDataWatcher.stop();
    }

    void onStop() {
        mDataWatcher.pause();
    }

    void onStart() {
        mDataWatcher.resume();
    }

    void onDestroy() {
        mDataWatcher.destroy();
    }

}
