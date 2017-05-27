package org.yeshen.video.librtmp.afix;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

/*********************************************************************
 * Created by yeshen on 2017/05/27.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class ControlThread {

    private static ControlThread sInstance = new ControlThread();

    public static ControlThread get() {
        return sInstance;
    }

    private ControlThread() {

    }

    private HandlerThread workThread;
    private Handler workHandler;

    public void post(Runnable runnable) {
        if(workHandler != null){
            workHandler.post(runnable);
        }else{
            start();
            workHandler.post(runnable);
        }
    }

    public void start(){
        stop();
        workThread = new HandlerThread(
                ControlThread.class.getSimpleName(),
                Process.THREAD_PRIORITY_BACKGROUND);
        workThread.start();
        workHandler  = new Handler(workThread.getLooper());
    }

    public void stop(){
        if(workHandler != null)workHandler.removeCallbacksAndMessages(null);
        if(workThread != null)workThread.quit();
    }

}
