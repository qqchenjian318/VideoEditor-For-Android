package com.example.cj.videoeditor.activity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.example.cj.videoeditor.Constants;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.media.MediaPlayerWrapper;
import com.example.cj.videoeditor.media.VideoInfo;
import com.example.cj.videoeditor.widget.VideoPreviewView;

import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * Created by cj on 2017/10/16.
 * desc: 循环播放选择的视频的页面，可以对视频设置水印和美白效果
 */

public class PreviewActivity extends BaseActivity implements View.OnClickListener, MediaPlayerWrapper.IMediaCallback {

    private ImageView mBack;
    private ImageView mConfirm;
    private ImageView mClose;
    private VideoPreviewView mVideoView;
    private String mPath;
    private boolean resumed;
    private boolean isDestroy;
    private boolean isPlaying = false;

    int startPoint;

    private String outputPath;
    static final int VIDEO_PREPARE = 0;
    static final int VIDEO_START = 1;
    static final int VIDEO_UPDATE = 2;
    static final int VIDEO_PAUSE = 3;
    static final int VIDEO_CUT_FINISH = 4;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VIDEO_PREPARE:
                    Executors.newSingleThreadExecutor().execute(update);
                    break;
                case VIDEO_START:
                    isPlaying = true;
                    break;
                case VIDEO_UPDATE:
                  /*  int curDuration = mVideoView.getCurDuration();
                    if (curDuration > startPoint + clipDur) {
                        mVideoView.seekTo(startPoint);
                        mVideoView.start();
                    }*/
                    break;
                case VIDEO_PAUSE:
                    isPlaying = false;
                    break;
                case VIDEO_CUT_FINISH:
                    endLoading();
                    //TODO　已经渲染完毕了　

                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);
        initView();
        initData();
    }

    private void initView() {
        mVideoView = (VideoPreviewView) findViewById(R.id.videoView);
        mBack = (ImageView) findViewById(R.id.iv_back);
        mConfirm = (ImageView) findViewById(R.id.iv_confirm);
        mClose = (ImageView) findViewById(R.id.iv_close);
        mBack.setOnClickListener(this);
        mConfirm.setOnClickListener(this);
        mClose.setOnClickListener(this);
        setLoadingCancelable(false);

    }
    private void initData() {
        Intent intent = getIntent();
        //选择的视频的本地播放地址
        mPath = intent.getStringExtra("path");
        ArrayList<String> srcList = new ArrayList<>();
        srcList.add(mPath);
        mVideoView.setVideoPath(srcList);
        mVideoView.setIMediaCallback(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (resumed) {
            mVideoView.start();
        }
        resumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroy = true;
        mVideoView.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(!isLoading()){
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
            case R.id.iv_close:
                finish();
                break;
            case R.id.iv_confirm:
                if (isLoading()){
                    return;
                }
                mVideoView.pause();
                showLoading("视频处理中");

                outputPath = Constants.getPath("video/clip/", System.currentTimeMillis() + "");


                break;

        }
    }

    @Override
    public void onVideoPrepare() {
        mHandler.sendEmptyMessage(VIDEO_PREPARE);
    }

    @Override
    public void onVideoStart() {
        mHandler.sendEmptyMessage(VIDEO_START);
    }

    @Override
    public void onVideoPause() {
        mHandler.sendEmptyMessage(VIDEO_PAUSE);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mVideoView.seekTo(startPoint);
        mVideoView.start();
    }

    @Override
    public void onVideoChanged(VideoInfo info) {

    }
    private Runnable update = new Runnable() {
        @Override
        public void run() {
            while (!isDestroy) {
                if (!isPlaying) {
                    try {
                        Thread.currentThread().sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                mHandler.sendEmptyMessage(VIDEO_UPDATE);
                try {
                    Thread.currentThread().sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
