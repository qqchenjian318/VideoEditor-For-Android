package com.example.cj.videoeditor;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

/**
 * Created by qqche_000 on 2017/12/10.
 * 固定背景的view
 */

public class CustomBgView extends LinearLayout{

    private Paint paint;

    public CustomBgView(Context context) {
        this(context,null);
    }

    public CustomBgView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CustomBgView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE,null);
        paint = new Paint();
        paint.setColor(Color.parseColor("#aaaaaa"));
        paint.setMaskFilter(new BlurMaskFilter(50, BlurMaskFilter.Blur.SOLID));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("hero","---"+getWidth()+"---"+getHeight());
        Rect rect = new Rect(0,0,getWidth(),getHeight()-50);

        canvas.drawRect(rect,paint);
    }
}
