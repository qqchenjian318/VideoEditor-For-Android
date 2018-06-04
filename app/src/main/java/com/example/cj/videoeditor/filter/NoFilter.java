package com.example.cj.videoeditor.filter;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Description:
 */
public class NoFilter extends AFilter {

    public NoFilter(Resources res) {
        super(res);
    }

    @Override
    protected void onCreate() {
        Log.e("thread", "---初始化NoFilter "+Thread.currentThread());
        createProgramByAssetsFile("shader/base_vertex.sh",
            "shader/base_fragment.sh");
    }

    /**
     * 背景默认为黑色
     */
    @Override
    protected void onClear() {
        Log.e("thread", "---onClear？ 1  "+Thread.currentThread());

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        Log.e("thread", "---onClear？ 2  "+Thread.currentThread());
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Log.e("thread", "---onClear？ 3  ");
    }

    @Override
    protected void onSizeChanged(int width, int height) {

    }
}
