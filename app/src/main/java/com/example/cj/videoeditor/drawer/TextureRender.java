package com.example.cj.videoeditor.drawer;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;


import com.example.cj.videoeditor.MyApplication;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.filter.AFilter;
import com.example.cj.videoeditor.filter.GroupFilter;
import com.example.cj.videoeditor.filter.NoFilter;
import com.example.cj.videoeditor.filter.RotationOESFilter;
import com.example.cj.videoeditor.filter.WaterMarkFilter;
import com.example.cj.videoeditor.gpufilter.basefilter.GPUImageFilter;
import com.example.cj.videoeditor.media.VideoInfo;
import com.example.cj.videoeditor.utils.EasyGlUtils;
import com.example.cj.videoeditor.utils.MatrixUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
public class TextureRender {
    private static final String TAG = "TextureRender";
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    //    private final float[] mTriangleVerticesData = {//原来的纹理坐标体系是错误的
//            // X, Y, Z, U, V
//            -1.0f, -1.0f, 0, 0.f, 0.f,
//            1.0f, -1.0f, 0, 1.f, 0.f,
//            -1.0f, 1.0f, 0, 0.f, 1.f,
//            1.0f, 1.0f, 0, 1.f, 1.f,
//    };
    private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 1.f,
            1.0f, -1.0f, 0, 1.f, 1.f,
            -1.0f, 1.0f, 0, 0.f, 0.f,
            1.0f, 1.0f, 0, 1.f, 0.f,
    };
    private FloatBuffer mTriangleVertices;
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";
    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    private int mProgram;
    private int mTextureID = -12345;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    //======================clip========================
    float[] SM = new float[16];     //用于显示的变换矩阵
    boolean isClipMode;
    int clipViewWidth;
    int clipViewHeight;
    int clipEncodeWidth;
    int clipEncodeHeight;


    //======================zoom========================
    //创建帧缓冲区
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[2];
    AFilter mShow;
    RotationOESFilter rotationFilter;
    GPUImageFilter mGpuFilter;
    GroupFilter mBeFilter;
    //第一段视频宽高(旋转后)
    int viewWidth;
    int viewHeight;
    //当前视频宽高(旋转后)
    int videoWidth;
    int videoHeight;
    //最终显示的宽高
    int width;
    int height;
    int x;
    int y;
    boolean videoChanged = false;
    //第一段视频信息
    VideoInfo info;
    /**
     * 用于后台绘制的变换矩阵
     */
    private float[] OM;

    public TextureRender(VideoInfo info) {
        this.info=info;
        mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);
        Matrix.setIdentityM(mSTMatrix, 0);
        Resources resources = MyApplication.getContext().getResources();
        mShow = new NoFilter(resources);
        mShow.setMatrix(MatrixUtils.flip(MatrixUtils.getOriginalMatrix(), false, true));
        rotationFilter = new RotationOESFilter(resources);
        mBeFilter = new GroupFilter(resources);
        //默认加上水印 可以取消掉
        WaterMarkFilter waterMarkFilter = new WaterMarkFilter(resources);
        waterMarkFilter.setWaterMark(BitmapFactory.decodeResource(resources, R.mipmap.watermark));

        waterMarkFilter.setPosition(0, 70, 0, 0);
        mBeFilter.addFilter(waterMarkFilter);
        OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM, false, false);//矩阵上下翻转
        mBeFilter.setMatrix(OM);
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void drawFrame(SurfaceTexture st) {
        zoomDraw(st);
    }

    public void zoomDraw(SurfaceTexture st){
        EasyGlUtils.bindFrameTexture(fFrame[0], fTexture[0]);
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        rotationFilter.draw();
        EasyGlUtils.unBindFrameBuffer();

        mBeFilter.setTextureId(fTexture[0]);
        mBeFilter.draw();

        if (mGpuFilter != null) {
            EasyGlUtils.bindFrameTexture(fFrame[0], fTexture[1]);
            mGpuFilter.onDrawFrame(mBeFilter.getOutputTexture());
            EasyGlUtils.unBindFrameBuffer();
        }

        if (videoChanged) {
            GLES20.glViewport(x, y, width, height);
        }

        mShow.setTextureId(fTexture[mGpuFilter == null ? 0 : 1]);
        mShow.draw();
        GLES20.glFinish();
    }

    public void preDraw(SurfaceTexture st) {
        checkGlError("onDrawFrame start");
//        st.getTransformMatrix(mSTMatrix);//当视频角度不为0时,这里会导致视频方向不对
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");
        Matrix.setIdentityM(mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    public void surfaceCreated() {
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        checkGlError("glBindTexture mTextureID");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);//改为线性过滤,是画面更加平滑(抗锯齿)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");

        mShow.create();
        rotationFilter.create();
        rotationFilter.setTextureId(mTextureID);

        mBeFilter.create();
        GLES20.glGenFramebuffers(1, fFrame, 0);

        if (info.rotation == 0 || info.rotation == 180) {
            EasyGlUtils.genTexturesWithParameter(2, fTexture, 0, GLES20.GL_RGBA, info.width, info.height);
            viewWidth = info.width;
            viewHeight = info.height;
        } else {
            EasyGlUtils.genTexturesWithParameter(2, fTexture, 0, GLES20.GL_RGBA, info.height, info.width);
            viewWidth = info.height;
            viewHeight = info.width;
        }

        rotationFilter.setRotation(info.rotation);
    }

    /**
     * Replaces the fragment shader.
     */
    public void changeFragmentShader(String fragmentShader) {
        GLES20.glDeleteProgram(mProgram);
        mProgram = createProgram(VERTEX_SHADER, fragmentShader);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public void addGpuFilter(GPUImageFilter filter) {
        if (mGpuFilter != null) {
            mGpuFilter.destroy();
        }
        mGpuFilter = filter;
        if (filter != null) {
            mGpuFilter.init();
            mGpuFilter.onDisplaySizeChanged(info.width, info.height);
            mGpuFilter.onInputSizeChanged(info.width, info.height);
        }
    }

    public void onVideoSizeChanged(VideoInfo info) {

        setVideoWidthAndHeight(info);
        adjustVideoPosition();
        videoChanged = true;
        mBeFilter.setSize(viewWidth, viewHeight);
    }

    public void setVideoWidthAndHeight(VideoInfo info) {
        rotationFilter.setRotation(info.rotation);
        if (info.rotation == 0 || info.rotation == 180) {
            this.videoWidth = info.width;
            this.videoHeight = info.height;
        } else {
            this.videoWidth = info.height;
            this.videoHeight = info.width;
        }
    }

    private void adjustVideoPosition() {
        float w = (float) viewWidth / videoWidth;
        float h = (float) viewHeight / videoHeight;
        if (w < h) {
            width = viewWidth;
            height = (int) ((float) videoHeight * w);
        } else {
            width = (int) ((float) videoWidth * h);
            height = viewHeight;
        }
        x = (viewWidth - width) / 2;
        y = (viewHeight - height) / 2;
    }

    /**
     * 清除掉水印
     * */
    public void clearWaterMark(){
        if (mBeFilter != null){
            mBeFilter.clearAll();
            mShow.setMatrix(OM);
        }
    }
}