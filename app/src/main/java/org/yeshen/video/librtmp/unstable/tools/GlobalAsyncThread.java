package org.yeshen.video.librtmp.unstable.tools;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

/*********************************************************************
 * Created by yeshen on 2017/05/27.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class GlobalAsyncThread {

    private static GlobalAsyncThread sInstance = new GlobalAsyncThread();

    public static GlobalAsyncThread get() {
        return sInstance;
    }

    public static void post(final Runnable runnable){
        sInstance.posts(runnable);
    }

    public static void start(){
        sInstance.starts();
    }

    public static void stop(){
        sInstance.stops();
    }

    private GlobalAsyncThread() {
    }

    private HandlerThread workThread;
    private Handler workHandler;

    private void posts(final Runnable runnable) {
        if(workHandler != null){
            workHandler.post(runnable);
        }else{
            starts();
            workHandler.post(runnable);
        }
    }

    private void starts(){
        stops();
        workThread = new HandlerThread(
                GlobalAsyncThread.class.getSimpleName(),
                Process.THREAD_PRIORITY_BACKGROUND);
        workThread.start();
        workHandler  = new Handler(workThread.getLooper());
    }

    private void stops(){
        if(workHandler != null)workHandler.removeCallbacksAndMessages(null);
        if(workThread != null)workThread.quit();
    }

}
