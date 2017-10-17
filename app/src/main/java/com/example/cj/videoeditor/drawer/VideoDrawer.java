package com.example.cj.videoeditor.drawer;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;


import com.example.cj.videoeditor.filter.AFilter;
import com.example.cj.videoeditor.filter.NoFilter;
import com.example.cj.videoeditor.filter.RotationOESFilter;
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
    private RotationOESFilter mPreFilter;
    private AFilter mShow;

    /**控件的长宽*/
    private int viewWidth;
    private int viewHeight;

    /**创建离屏buffer*/
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];
    /**用于视频旋转的参数*/
    private int rotation;

    public VideoDrawer(Resources res){
        mPreFilter = new RotationOESFilter(res);//旋转相机操作
        mShow=new NoFilter(res);

        OM= MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM,false,true);//矩阵上下翻转
        mShow.setMatrix(OM);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int texture[]=new int[1];
        GLES20.glGenTextures(1,texture,0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        surfaceTexture = new SurfaceTexture(texture[0]);
        mPreFilter.create();
        mPreFilter.setTextureId(texture[0]);
        mShow.create();
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

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        surfaceTexture.updateTexImage();
        EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
        GLES20.glViewport(0,0,viewWidth,viewHeight);
        mPreFilter.draw();
        EasyGlUtils.unBindFrameBuffer();

        GLES20.glViewport(0,0,viewWidth,viewHeight);
        mShow.setTextureId(mPreFilter.getOutputTexture());
        mShow.draw();
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
}
