package org.yeshen.video.librtmp.tools;

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


    //默认配置
    private static final int DEFAULT_HEIGHT = 1280;
    private static final int DEFAULT_WIDTH = 720;
    private static final int DEFAULT_FPS = 15;
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    @Override
    public String toString() {
        return protocol + "://" + service + ":" + port + "/" + app + "/" + stream;
    }

}
