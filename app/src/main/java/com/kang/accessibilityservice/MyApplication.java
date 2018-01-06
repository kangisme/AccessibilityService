package com.kang.accessibilityservice;


import android.app.Application;
import android.content.Intent;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

/**
 * custom Application
 * Created by kangren on 2018/1/4.
 */

public class MyApplication extends Application {

    /**
     * 共享从startActivityForResult返回的intent
     * 必须用这个intent才能创建MediaProjection
     * {@link android.media.projection.MediaProjectionManager#getMediaProjection(int, Intent)}
     */
    private Intent data;

    public Intent getData()
    {
        return data;
    }

    public void setData(Intent resultData)
    {
        data = resultData;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)
                .tag("accessibilityLogger")
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy){
            @Override
            public boolean isLoggable(int priority, String tag) {
                return BuildConfig.DEBUG;
            }
        });
    }
}
