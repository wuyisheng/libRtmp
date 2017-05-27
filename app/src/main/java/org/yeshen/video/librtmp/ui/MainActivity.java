package org.yeshen.video.librtmp.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.yeshen.video.librtmp.R;
import org.yeshen.video.librtmp.afix.interfaces.CameraListener;
import org.yeshen.video.librtmp.afix.interfaces.LivingStartListener;
import org.yeshen.video.librtmp.android.AudioConfiguration;
import org.yeshen.video.librtmp.android.CameraConfiguration;
import org.yeshen.video.librtmp.afix.VideoLivingView;
import org.yeshen.video.librtmp.android.VideoConfiguration;
import org.yeshen.video.librtmp.net.packer.rtmp.RtmpPacker;
import org.yeshen.video.librtmp.net.sender.rtmp.RtmpSender;
import org.yeshen.video.librtmp.tools.Lg;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public class MainActivity extends AppCompatActivity {

    private VideoLivingView mLFLiveView;
    private RtmpSender mRtmpSender;
    private VideoConfiguration mVideoConfiguration;
    private int mCurrentBps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
        findViewById(R.id.push).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                push();
            }
        });

        mLFLiveView = (VideoLivingView) findViewById(R.id.player);
        mLFLiveView.init();
        CameraConfiguration.Builder cameraBuilder = new CameraConfiguration.Builder();
        cameraBuilder.setOrientation(CameraConfiguration.Orientation.PORTRAIT)
                .setFacing(CameraConfiguration.Facing.BACK);
        CameraConfiguration cameraConfiguration = cameraBuilder.build();
        mLFLiveView.setCameraConfiguration(cameraConfiguration);

        VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
        videoBuilder.setSize(640, 360);
        mVideoConfiguration = videoBuilder.build();
        mLFLiveView.setVideoConfiguration(mVideoConfiguration);

        //设置预览监听
        mLFLiveView.setCameraOpenListener(new CameraListener() {
            @Override
            public void onOpenSuccess() {
                Toast.makeText(MainActivity.this, "camera open success", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOpenFail(int error) {
                Toast.makeText(MainActivity.this, "camera open fail", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCameraChange() {
                Toast.makeText(MainActivity.this, "camera switch", Toast.LENGTH_LONG).show();
            }
        });

        //初始化flv打包器
        RtmpPacker packer = new RtmpPacker();
        packer.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mLFLiveView.setPacker(packer);
        //设置发送器
        mRtmpSender = RtmpSender.getRtmpSender();
        mRtmpSender.setSenderListener(mSenderListener);
        mLFLiveView.setSender(mRtmpSender);
        mLFLiveView.setLivingStartListener(new LivingStartListener() {
            @Override
            public void startError(int error) {
                //直播失败
                Toast.makeText(MainActivity.this, "start living fail", Toast.LENGTH_SHORT).show();
                mLFLiveView.stop();
            }

            @Override
            public void startSuccess() {
                //直播成功
                Toast.makeText(MainActivity.this, "start living", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void stop() {
        mLFLiveView.stop();
    }

    public void push() {

        Toast.makeText(MainActivity.this, "start connecting", Toast.LENGTH_SHORT).show();
        mRtmpSender.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLFLiveView.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLFLiveView.resume();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mLFLiveView.release();
    }

    private RtmpSender.OnSenderListener mSenderListener = new RtmpSender.OnSenderListener() {
        @Override
        public void onConnecting() {

        }

        @Override
        public void onConnected() {
            mLFLiveView.start();
            mCurrentBps = mVideoConfiguration.maxBps;
        }

        @Override
        public void onDisConnected() {
            Toast.makeText(MainActivity.this, "fail to live", Toast.LENGTH_SHORT).show();
            mLFLiveView.stop();
        }

        @Override
        public void onPublishFail() {
            Toast.makeText(MainActivity.this, "fail to publish stream", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNetGood() {
            if (mCurrentBps + 50 <= mVideoConfiguration.maxBps){
                Lg.d( "BPS_CHANGE good up 50");
                int bps = mCurrentBps + 50;
                if(mLFLiveView != null) {
                    boolean result = mLFLiveView.setVideoBps(bps);
                    if(result) {
                        mCurrentBps = bps;
                    }
                }
            } else {
                Lg.d( "BPS_CHANGE good good good");
            }
            Lg.d( "Current Bps: " + mCurrentBps);
        }

        @Override
        public void onNetBad() {
            if (mCurrentBps - 100 >= mVideoConfiguration.minBps){
                Lg.d( "BPS_CHANGE bad down 100");
                int bps = mCurrentBps - 100;
                if(mLFLiveView != null) {
                    boolean result = mLFLiveView.setVideoBps(bps);
                    if(result) {
                        mCurrentBps = bps;
                    }
                }
            } else {
                Lg.d( "BPS_CHANGE bad down 100");
            }
            Lg.d( "Current Bps: " + mCurrentBps);
        }
    };

}
