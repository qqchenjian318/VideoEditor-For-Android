package com.example.cj.videoeditor.activity;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.cj.videoeditor.Constants;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.bean.AudioSettingInfo;
import com.example.cj.videoeditor.bean.CutBean;
import com.example.cj.videoeditor.gpufilter.SlideGpuFilterGroup;
import com.example.cj.videoeditor.gpufilter.helper.MagicFilterType;
import com.example.cj.videoeditor.media.MediaPlayerWrapper;
import com.example.cj.videoeditor.media.VideoInfo;
import com.example.cj.videoeditor.mediacodec.MediaMuxerRunnable;
import com.example.cj.videoeditor.utils.TimeFormatUtils;
import com.example.cj.videoeditor.widget.VideoPreviewView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by cj on 2018/1/2.
 * desc
 */

public class VideoConnectActivity extends BaseActivity implements View.OnClickListener, MediaPlayerWrapper.IMediaCallback, SlideGpuFilterGroup.OnFilterChangeListener {
    private VideoPreviewView mPreviewView;
    List<String> inputPath;
    ExecutorService pool;
    AudioSettingInfo settingInfo;

    static final int ENCODE_START = 0;
    static final int ENCODE_FINISH = 1;
    static final int UPDATE_SEEKBAR = 2;
    static final int VIDEO_PREPARE = 3;
    static final int VIDEO_START = 4;
    static final int VIDEO_PAUSE = 5;
    static final int VIDEO_CHANGED = 6;
    static final int VIDEO_COMPLETION = 7;

    private String outputPath;

    private MagicFilterType currentFilterType;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ENCODE_START:
                    break;
                case ENCODE_FINISH:
                    endLoading();
                    Toast.makeText(VideoConnectActivity.this, "Finish Encode path=" + outputPath, Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_SEEKBAR:
                    break;
                case VIDEO_PREPARE:

                    break;
                case VIDEO_START:
                    break;
                case VIDEO_PAUSE:
                    break;
                case VIDEO_CHANGED:
                    break;
                case VIDEO_COMPLETION:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        initView();
        initData();
    }

    private void initData() {
        inputPath = getIntent().getStringArrayListExtra("path");

        mPreviewView.setVideoPath(inputPath);
        mPreviewView.setIMediaCallback(this);
        mPreviewView.setOnFilterChangeListener(this);
        mPreviewView.clearWaterMark();//设置不显示水印

        pool = Executors.newCachedThreadPool();
        settingInfo = new AudioSettingInfo();
        settingInfo.volFirst = 1;
        settingInfo.volSecond = 1;
    }

    private void initView() {
        mPreviewView = (VideoPreviewView) findViewById(R.id.connect_video_view);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_confirm).setOnClickListener(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mPreviewView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPreviewView.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.iv_confirm:
                mPreviewView.pause();
                showLoading("视频编辑中");

                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        outputPath = Constants.getPath("output/", System.currentTimeMillis() + ".mp4");
                        final long startTime = System.currentTimeMillis();
                        MediaMuxerRunnable instance = new MediaMuxerRunnable();
                        instance.setVideoInfo(mPreviewView.getVideoInfo(), outputPath);
                        instance.setAudioSetting(settingInfo);
                        instance.setFilterType(currentFilterType);
                        instance.addMuxerListener(new MediaMuxerRunnable.MuxerListener() {
                            @Override
                            public void onStart() {
                                Log.e("hero", "===muxer  onStart====");
                            }

                            @Override
                            public void onFinish() {
                                Log.e("hero", "===muxer  onFinish====");
                                long endTime = System.currentTimeMillis();
                                Log.e("timee", "---视频编辑消耗的时间===" + (endTime - startTime));
                                mHandler.sendEmptyMessage(1);
                            }
                        });
                        instance.start();
                    }
                });
                break;
        }
    }

    @Override
    public void onVideoPrepare() {

    }

    @Override
    public void onVideoStart() {

    }

    @Override
    public void onVideoPause() {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onVideoChanged(VideoInfo info) {

    }

    @Override
    public void onFilterChange(MagicFilterType type) {
        currentFilterType = type;
    }
}
