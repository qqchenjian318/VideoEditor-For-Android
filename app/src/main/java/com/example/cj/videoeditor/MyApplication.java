package com.example.cj.videoeditor;

import android.app.Application;
import android.content.Context;

/**
 * Created by qqche_000 on 2017/8/6.
 */

public class MyApplication extends Application{
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext() {
        return mContext;
    }
}
