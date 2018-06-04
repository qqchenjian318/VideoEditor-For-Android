package com.example.cj.videoeditor;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;

import com.example.cj.videoeditor.media.VideoInfo;
import com.example.cj.videoeditor.mediacodec.VideoRunnable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by cj on 2017/8/6.
 *
 */

public class MyApplication extends Application{
    private static Context mContext;



    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.e("thread","  线程值  "+Thread.currentThread());
        Constants.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    public static Context getContext() {
        return mContext;
    }
}
