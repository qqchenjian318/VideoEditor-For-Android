package com.example.cj.videoeditor.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by qqche_000 on 2017/7/29.
 * 自定义的GlSurfaceView
 */

public class CustomGlSurfaceView extends GLSurfaceView{
    public CustomGlSurfaceView(Context context) {
        this(context,null);

    }

    public CustomGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

    }
}
