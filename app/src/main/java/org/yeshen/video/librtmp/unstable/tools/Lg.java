package org.yeshen.video.librtmp.unstable.tools;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/*********************************************************************
 * Created by yeshen on 2017/05/18.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class Lg {

    private static final String TAG = Lg.class.getSimpleName();

    public static void e(@Nullable String value) {
        Log.e(TAG, value);
    }

    public static void e(@Nullable Exception value) {
        Log.e(TAG, Error.UN_KNOW);
        if (value == null) return;
        value.printStackTrace();
    }

    public static void e(@NonNull String msg, @Nullable Exception value) {
        Log.e(TAG, msg);
        if (value == null) return;
        value.printStackTrace();
    }

    public static void d(@Nullable String value) {
        Log.d(TAG, value);
    }

    public static void d(@Nullable Exception value) {
        if (value == null) return;
        value.printStackTrace();
    }

    public static void d(@NonNull String msg, @Nullable String value) {
        Log.d(TAG, msg + " >> " + value);
    }


}
