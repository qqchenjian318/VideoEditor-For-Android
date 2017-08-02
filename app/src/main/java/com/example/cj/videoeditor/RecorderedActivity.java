package com.example.cj.videoeditor;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * Created by cj on 2017/7/25.
 * desc 视频录制
 * 主要包括 音视频录制、断点续录、对焦等功能
 */

public class RecorderedActivity extends Activity{

    private CameraView mCameraView;
    private Camera mCamera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorde);
        initView();
    }

    private void initView() {
        mCameraView = (CameraView) findViewById(R.id.camera_view);
    }


}
