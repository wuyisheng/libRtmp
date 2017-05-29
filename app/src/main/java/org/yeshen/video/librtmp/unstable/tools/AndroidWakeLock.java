package org.yeshen.video.librtmp.unstable.tools;

import android.content.Context;
import android.os.PowerManager;

/*********************************************************************
 * Created by yeshen on 2017/05/28.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class AndroidWakeLock {
    private final static String TAG = AndroidWakeLock.class.getSimpleName();
    private PowerManager.WakeLock mWakeLock;
    public AndroidWakeLock(Context context){
        PowerManager mPowerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE, TAG);
    }

    public void acquire(){
        if (mWakeLock != null) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        }
    }

    public void release(){
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }
}
