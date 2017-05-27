package org.yeshen.video.librtmp.tools;

import android.widget.Toast;

import org.yeshen.video.librtmp.App;

/*********************************************************************
 * Created by yeshen on 2017/05/20.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class Toasts {

    public static void str(String msg) {
        Toast.makeText(App.getInstance(), msg, Toast.LENGTH_SHORT).show();
    }
}
