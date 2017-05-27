package org.yeshen.video.librtmp;

import android.app.Application;
import android.content.Context;

/*********************************************************************
 * Created by yeshen on 2017/05/20.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class App extends Application {

    public static App ins;

    public static App getInstance() {
        return ins;
    }

    public static Context getContext() {
        return ins.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ins = this;
    }
}
