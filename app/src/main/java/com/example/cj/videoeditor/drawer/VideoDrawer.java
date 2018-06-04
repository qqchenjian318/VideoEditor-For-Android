package com.example.cj.videoeditor.drawer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.filter.AFilter;
import com.example.cj.videoeditor.filter.GroupFilter;
import com.example.cj.videoeditor.filter.NoFilter;
import com.example.cj.videoeditor.filter.ProcessFilter;
import com.example.cj.videoeditor.filter.RotationOESFilter;
import com.example.cj.videoeditor.filter.WaterMarkFilter;
import com.example.cj.videoeditor.gpufilter.SlideGpuFilterGroup;
import com.example.cj.videoeditor.gpufilter.basefilter.GPUImageFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicBeautyFilter;
import com.example.cj.videoeditor.media.VideoInfo;
import com.example.cj.videoeditor.utils.EasyGlUtils;
import com.example.cj.videoeditor.utils.MatrixUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by cj on 2017/10/16.
 * desc：添加水印和美白效果
 */

public class VideoDrawer implements GLSurfaceView.Renderer {
    /**用于后台绘制的变换矩阵*/
    private float[] OM;
    /**用于显示的变换矩阵*/
    private float[] SM = new float[16];
    private SurfaceTexture surfaceTexture;
    /**可选择画面的滤镜*/
    private RotationOESFilter mPreFilter;
    /**显示的滤镜*/
    private AFilter mShow;
    /**美白的filter*/
    private MagicBeautyFilter mBeautyFilter;
    private AFilter mProcessFilter;
    /**绘制水印的滤镜*/
    private final GroupFilter mBeFilter;
    /**多种滤镜切换*/
    private SlideGpuFilterGroup mSlideFilterGroup;

    /**绘制其他样式的滤镜*/
    private GPUImageFilter mGroupFilter;
    /**控件的长宽*/
    private int viewWidth;
    private int viewHeight;

    /**创建离屏buffer*/
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];
    /**用于视频旋转的参数*/
    private int rotation;
    /**是否开启美颜*/
    private boolean isBeauty = false;


    public VideoDrawer(Context context,Resources res){
        mPreFilter = new RotationOESFilter(res);//旋转相机操作
        mShow = new NoFilter(res);
        mBeFilter = new GroupFilter(res);
        mBeautyFilter = new MagicBeautyFilter();

        mProcessFilter=new ProcessFilter(res);

        mSlideFilterGroup = new SlideGpuFilterGroup();
        OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM,false,true);//矩阵上下翻转
//        mShow.setMatrix(OM);

        WaterMarkFilter waterMarkFilter = new WaterMarkFilter(res);
        waterMarkFilter.setWaterMark(BitmapFactory.decodeResource(res, R.mipmap.watermark));

        waterMarkFilter.setPosition(0,70,0,0);
        mBeFilter.addFilter(waterMarkFilter);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int texture[]=new int[1];
        GLES20.glGenTextures(1,texture,0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES ,texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        surfaceTexture = new SurfaceTexture(texture[0]);
        mPreFilter.create();
        mPreFilter.setTextureId(texture[0]);

        mBeFilter.create();
        mProcessFilter.create();
        mShow.create();
        mBeautyFilter.init();
        mBeautyFilter.setBeautyLevel(3);//默认设置3级的美颜
        mSlideFilterGroup.init();
    }
    public void onVideoChanged(VideoInfo info){
        setRotation(info.rotation);
        if(info.rotation==0||info.rotation==180){
            MatrixUtils.getShowMatrix(SM,info.width,info.height,viewWidth,viewHeight);
        }else{
            MatrixUtils.getShowMatrix(SM,info.height,info.width,viewWidth,viewHeight);
        }

        mPreFilter.setMatrix(SM);
    }
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth=width;
        viewHeight=height;
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);

        GLES20.glGenFramebuffers(1,fFrame,0);
        EasyGlUtils.genTexturesWithParameter(1,fTexture,0, GLES20.GL_RGBA,viewWidth,viewHeight);

        mBeFilter.setSize(viewWidth,viewHeight);
        mProcessFilter.setSize(viewWidth,viewHeight);
        mBeautyFilter.onDisplaySizeChanged(viewWidth,viewHeight);
        mBeautyFilter.onInputSizeChanged(viewWidth,viewHeight);
        mSlideFilterGroup.onSizeChanged(viewWidth,viewHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        surfaceTexture.updateTexImage();

        EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);

        GLES20.glViewport(0,0,viewWidth,viewHeight);

        mPreFilter.draw();

        EasyGlUtils.unBindFrameBuffer();


        mBeFilter.setTextureId(fTexture[0]);

        mBeFilter.draw();

        if (mBeautyFilter != null && isBeauty && mBeautyFilter.getBeautyLevel() != 0){
            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
            GLES20.glViewport(0,0,viewWidth,viewHeight);
            mBeautyFilter.onDrawFrame(mBeFilter.getOutputTexture());
            EasyGlUtils.unBindFrameBuffer();
            mProcessFilter.setTextureId(fTexture[0]);
        }else {

            mProcessFilter.setTextureId(mBeFilter.getOutputTexture());
        }
        mProcessFilter.draw();

        mSlideFilterGroup.onDrawFrame(mProcessFilter.getOutputTexture());

        if (mGroupFilter != null){
            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
            GLES20.glViewport(0,0,viewWidth,viewHeight);
            mGroupFilter.onDrawFrame(mSlideFilterGroup.getOutputTexture());
            EasyGlUtils.unBindFrameBuffer();
            mProcessFilter.setTextureId(fTexture[0]);
        }else {
            mProcessFilter.setTextureId(mSlideFilterGroup.getOutputTexture());
        }

        mProcessFilter.draw();


        GLES20.glViewport(0,0,viewWidth,viewHeight);

        mShow.setTextureId(mProcessFilter.getOutputTexture());

        mShow.draw();
        Log.e("videoo", "---卡完了 ？");
    }
    public SurfaceTexture getSurfaceTexture(){
        return surfaceTexture;
    }

    public void setRotation(int rotation){
        this.rotation=rotation;
        if(mPreFilter!=null){
            mPreFilter.setRotation(this.rotation);
        }
    }
    /**切换开启美白效果*/
    public void switchBeauty(){
        isBeauty = !isBeauty;
    }
    /**
     * 是否开启美颜功能
     * */
    public void isOpenBeauty(boolean isBeauty){
        this.isBeauty = isBeauty;
    }
    /**
     * 触摸事件监听
     * */
    public void onTouch(MotionEvent event){
        mSlideFilterGroup.onTouchEvent(event);
    }
    /**
     * 滤镜切换的监听
     * */
    public void setOnFilterChangeListener(SlideGpuFilterGroup.OnFilterChangeListener listener){
        mSlideFilterGroup.setOnFilterChangeListener(listener);
    }

    public void checkGlError(String s) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(s + ": glError " + error);
        }
    }

    public void setGpuFilter(GPUImageFilter filter) {
        if (filter != null){
            mGroupFilter = filter;
            mGroupFilter.init();
            mGroupFilter.onDisplaySizeChanged(viewWidth, viewWidth);
            mGroupFilter.onInputSizeChanged(viewWidth,viewHeight);
        }

    }
}
