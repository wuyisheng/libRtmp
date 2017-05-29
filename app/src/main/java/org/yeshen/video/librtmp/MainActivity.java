package org.yeshen.video.librtmp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.yeshen.video.librtmp.unstable.LivingView;

/*********************************************************************
 * Created by yeshen on 2017/05/17.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

public class MainActivity extends AppCompatActivity {

    private LivingController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controller != null) controller.close();
            }
        });
        findViewById(R.id.push).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controller != null) controller.push();
            }
        });

        LivingView liveView = (LivingView) findViewById(R.id.player);
        controller = new LivingController(liveView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (controller != null) controller.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (controller != null) controller.onStop();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null) controller.onDestroy();
    }

}
