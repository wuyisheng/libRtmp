package org.yeshen.video.librtmp.tools;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;

/*********************************************************************
 * Created by yeshen on 2017/05/18.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class Options {

    public static Options ins = new Options();

    public static Options getInstance() {
        return ins;
    }

    private Options() {
    }

    //网络请求配置
    public String protocol = "rtmp";
    public String service = "192.168.43.174";
    public int port = 1935;
    public String app = "live";
    public String stream = "ta";
    public boolean isProvider = true;

    //视频录制配置
    public int height = DEFAULT_HEIGHT;
    public int width = DEFAULT_WIDTH;
    public int fps = DEFAULT_FPS;
    public int bufferSize = DEFAULT_BUFFER_SIZE;
    public boolean front = true;

    //configuration
    public Audio audio = new Audio();
    public Camera camera = new Camera();
    public Video video = new Video();


    //默认配置
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    //audio
    public static final int DEFAULT_FREQUENCY = 44100;
    public static final int DEFAULT_MAX_BPS = 64;
    public static final int DEFAULT_MIN_BPS = 32;
    public static final int DEFAULT_ADTS = 0;
    public static final String DEFAULT_MIME = "audio/mp4a-latm";
    public static final int DEFAULT_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    public static final int DEFAULT_CHANNEL_COUNT = 1;
    public static final boolean DEFAULT_AEC = false;
    //camera
    public static final int DEFAULT_HEIGHT = 1280;
    public static final int DEFAULT_WIDTH = 720;
    public static final int DEFAULT_FPS = 15;
    public static final Facing DEFAULT_FACING = Facing.FRONT;
    public static final Orientation DEFAULT_ORIENTATION = Orientation.PORTRAIT;
    public static final FocusMode DEFAULT_FOCUSMODE = FocusMode.AUTO;
    //video
    public static final int VIDEO_DEFAULT_HEIGHT = 640;
    public static final int VIDEO_DEFAULT_WIDTH = 360;
    public static final int VIDEO_DEFAULT_FPS = 15;
    public static final int VIDEO_DEFAULT_MAX_BPS = 1300;
    public static final int VIDEO_DEFAULT_MIN_BPS = 400;
    public static final int VIDEO_DEFAULT_IFI = 2;
    public static final String VIDEO_DEFAULT_MIME = "video/avc";

    public enum  Facing {
        FRONT,
        BACK
    }

    public enum  Orientation {
        LANDSCAPE,
        PORTRAIT
    }

    public enum  FocusMode {
        AUTO,
        TOUCH
    }

    public void setFacing(Facing facing) {
        camera.facing = facing;
    }

    public void setOrientation(Orientation orientation) {
        camera.orientation = orientation;
    }

    public void setSize(int width, int height) {
        video.width = width;
        video.height = height;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return protocol + "://" + service + ":" + port + "/" + app + "/" + stream;
    }

    public static class Audio{
        public int minBps = DEFAULT_MIN_BPS;
        public int maxBps = DEFAULT_MAX_BPS;
        public int frequency = DEFAULT_FREQUENCY;
        public int encoding = DEFAULT_AUDIO_ENCODING;
        public int channelCount = DEFAULT_CHANNEL_COUNT;
        public int adts = DEFAULT_ADTS;
        public String mime = DEFAULT_MIME;
        public int aacProfile = DEFAULT_AAC_PROFILE;
        public boolean aec = DEFAULT_AEC;
    }

    public static class Camera{
        public int height = DEFAULT_HEIGHT;
        public int width = DEFAULT_WIDTH;
        public int fps = DEFAULT_FPS;
        public Facing facing = DEFAULT_FACING;
        public Orientation orientation = DEFAULT_ORIENTATION;
        public FocusMode focusMode = DEFAULT_FOCUSMODE;
    }

    public static class Video{
        public int height = VIDEO_DEFAULT_HEIGHT;
        public int width = VIDEO_DEFAULT_WIDTH;
        public int minBps = VIDEO_DEFAULT_MIN_BPS;
        public int maxBps = VIDEO_DEFAULT_MAX_BPS;
        public int fps = VIDEO_DEFAULT_FPS;
        public int ifi = VIDEO_DEFAULT_IFI;
        public String mime = VIDEO_DEFAULT_MIME;
    }
}
