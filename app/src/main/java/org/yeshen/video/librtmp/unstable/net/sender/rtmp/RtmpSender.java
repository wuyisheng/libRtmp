package org.yeshen.video.librtmp.unstable.net.sender.rtmp;

import org.yeshen.video.librtmp.unstable.net.packer.rtmp.RtmpPacker;
import org.yeshen.video.librtmp.unstable.net.sender.Sender;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.io.RtmpConnectListener;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.io.RtmpConnection;
import org.yeshen.video.librtmp.unstable.net.sender.sendqueue.ISendQueue;
import org.yeshen.video.librtmp.unstable.net.sender.sendqueue.NormalSendQueue;
import org.yeshen.video.librtmp.unstable.net.sender.sendqueue.SendQueueListener;
import org.yeshen.video.librtmp.unstable.tools.GlobalAsyncThread;
import org.yeshen.video.librtmp.unstable.tools.Options;
import org.yeshen.video.librtmp.unstable.tools.WeakHandler;

/**
 * @Title: RtmpSender
 * @Package org.yeshen.video.librtmp.afix.net.sender.rtmp
 * @Description:
 * @Author Jim
 * @Date 16/9/21
 * @Time 上午11:16
 * @Version
 */
public class RtmpSender implements Sender, SendQueueListener {

    public static RtmpSender getRtmpSender() {
        return new RtmpSender();
    }

    private RtmpConnection rtmpConnection;
    private String mRtmpUrl;
    private OnSenderListener mListener;
    private WeakHandler mHandler = new WeakHandler();
    private ISendQueue mSendQueue = new NormalSendQueue();

    @Override
    public void good() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onNetGood();
                }
            }
        });
    }

    @Override
    public void bad() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onNetBad();
                }
            }
        });
    }

    public interface OnSenderListener {
        void onConnecting();

        void onConnected();

        void onDisConnected();

        void onPublishFail();

        void onNetGood();

        void onNetBad();
    }

    private RtmpSender() {
        rtmpConnection = new RtmpConnection();
        rtmpConnection.setVideoParams(Options.getInstance().video.width,
                Options.getInstance().video.height);
        rtmpConnection.setAudioParams(Options.DEFAULT_FREQUENCY, 16, false);
        mRtmpUrl = Options.getInstance().toString();
    }

    public void setSenderListener(OnSenderListener listener) {
        mListener = listener;
    }

    public void connect() {
        rtmpConnection.setSendQueue(mSendQueue);
        GlobalAsyncThread.post(new Runnable() {
            @Override
            public void run() {
                rtmpConnection.setConnectListener(listener);
                rtmpConnection.connect(mRtmpUrl);
            }
        });
        if (mListener != null) {
            mListener.onConnecting();
        }
    }

    @Override
    public synchronized void start() {
        mSendQueue.setSendQueueListener(this);
        mSendQueue.start();
    }

    @Override
    public void onData(byte[] data, int type) {
        if (type == RtmpPacker.FIRST_AUDIO || type == RtmpPacker.AUDIO) {
            rtmpConnection.publishAudioData(data, type);
        } else if (type == RtmpPacker.FIRST_VIDEO ||
                type == RtmpPacker.INTER_FRAME || type == RtmpPacker.KEY_FRAME) {
            rtmpConnection.publishVideoData(data, type);
        }
    }

    @Override
    public synchronized void stop() {
        rtmpConnection.stop();
        rtmpConnection.setConnectListener(null);
        mSendQueue.setSendQueueListener(null);
        mSendQueue.stop();
    }


    private void sendDisconnectMsg() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onDisConnected();
                }
            }
        });
    }

    private void sendPublishFail() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onPublishFail();
                }
            }
        });
    }

    private RtmpConnectListener listener = new RtmpConnectListener() {
        @Override
        public void onUrlInvalid() {
            sendPublishFail();
        }

        @Override
        public void onSocketConnectSuccess() {

        }

        @Override
        public void onSocketConnectFail() {
            sendPublishFail();
        }

        @Override
        public void onHandshakeSuccess() {

        }

        @Override
        public void onHandshakeFail() {
            sendPublishFail();
        }

        @Override
        public void onRtmpConnectSuccess() {

        }

        @Override
        public void onRtmpConnectFail() {
            sendPublishFail();
        }

        @Override
        public void onCreateStreamSuccess() {

        }

        @Override
        public void onCreateStreamFail() {
            sendPublishFail();
        }

        @Override
        public void onPublishSuccess() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onConnected();
                    }
                }
            });
        }

        @Override
        public void onPublishFail() {
            sendPublishFail();
        }

        @Override
        public void onSocketDisconnect() {
            sendDisconnectMsg();
        }

        @Override
        public void onStreamEnd() {
            sendDisconnectMsg();
        }
    };

}
