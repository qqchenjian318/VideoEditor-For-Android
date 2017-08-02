package com.example.cj.videoeditor.camera;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

/**
 * Created by cj on 2017/8/2.
 * desc 相机的管理类
 */

public class CameraController implements ICamera{

    private ICamera.Config mConfig;
    private Camera mCamera;

    public CameraController(){
        /**初始化一个默认的格式大小*/
        mConfig = new ICamera.Config();
        mConfig.minPreviewWidth=720;
        mConfig.minPictureWidth=720;
        mConfig.rate=1.778f;
    }

    public void open(int cameraId) {
        mCamera = Camera.open(cameraId);
    }

    @Override
    public void setPreviewTexture(SurfaceTexture texture) {

    }

    @Override
    public void setConfig(Config config) {

    }

    @Override
    public void setOnPreviewFrameCallback(PreviewFrameCallback callback) {

    }

    @Override
    public void preview() {

    }

    @Override
    public Point getPreviewSize() {
        return null;
    }

    @Override
    public Point getPictureSize() {
        return null;
    }

    @Override
    public boolean close() {
        return false;
    }
}
