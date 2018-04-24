package com.example.cj.videoeditor.activity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.cj.videoeditor.Constants;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.gpufilter.SlideGpuFilterGroup;
import com.example.cj.videoeditor.gpufilter.filter.MagicAntiqueFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicBeautyFilter;
import com.example.cj.videoeditor.gpufilter.helper.MagicFilterType;
import com.example.cj.videoeditor.media.MediaPlayerWrapper;
import com.example.cj.videoeditor.media.VideoInfo;
import com.example.cj.videoeditor.mediacodec.VideoClipper;
import com.example.cj.videoeditor.widget.VideoPreviewView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * Created by cj on 2017/10/16.
 * desc: 循环播放选择的视频的页面，可以对视频设置水印和美白效果
 */

public class PreviewActivity extends BaseActivity implements View.OnClickListener, MediaPlayerWrapper.IMediaCallback, SlideGpuFilterGroup.OnFilterChangeListener, View.OnTouchListener {


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
                    Toast.makeText(PreviewActivity.this, "视频保存地址   "+outputPath, Toast.LENGTH_SHORT).show();
                    endLoading();
                    finish();
                    //TODO　已经渲染完毕了　

                    break;
            }
        }
    };
    private ImageView mBeauty;
    private MagicFilterType filterType = MagicFilterType.NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);
        initView();
        initData();
    }

    private void initView() {
        mVideoView = (VideoPreviewView) findViewById(R.id.videoView);
        ImageView back = (ImageView) findViewById(R.id.iv_back);
        ImageView confirm = (ImageView) findViewById(R.id.iv_confirm);
        ImageView close = (ImageView) findViewById(R.id.iv_close);
        mBeauty = (ImageView) findViewById(R.id.iv_beauty);

        back.setOnClickListener(this);
        confirm.setOnClickListener(this);
        close.setOnClickListener(this);
        mBeauty.setOnClickListener(this);
        mVideoView.setOnFilterChangeListener(this);
        mVideoView.setOnTouchListener(this);
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
        Toast.makeText(this,R.string.change_filter,Toast.LENGTH_SHORT).show();
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
        mHandler.removeCallbacksAndMessages(null);
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
                if (isLoading()){
                    endLoading();
                }
                finish();
                break;
            case R.id.iv_beauty:
                mVideoView.switchBeauty();
                if (mBeauty.isSelected()){
                    mBeauty.setSelected(false);
                }else {
                    mBeauty.setSelected(true);
                }
                break;
            case R.id.iv_confirm:
                if (isLoading()){
                    return;
                }
                mVideoView.pause();
                showLoading("视频处理中",false);

                VideoClipper clipper = new VideoClipper();
                if (mBeauty.isSelected()){
                    clipper.showBeauty();
                }
                clipper.setInputVideoPath(mPath);
                outputPath = Constants.getPath("video/clip/", System.currentTimeMillis() + "");
                clipper.setFilterType(filterType);
                clipper.setOutputVideoPath(outputPath);
                clipper.setOnVideoCutFinishListener(new VideoClipper.OnVideoCutFinishListener() {
                    @Override
                    public void onFinish() {
                        mHandler.sendEmptyMessage(VIDEO_CUT_FINISH);
                    }
                });
                try {
                    Log.e("hero","-----PreviewActivity---clipVideo");
                    clipper.clipVideo(0,mVideoView.getVideoDuration()*1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }


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

    @Override
    public void onFilterChange(final MagicFilterType type) {
        this.filterType = type;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PreviewActivity.this,"滤镜切换为---"+type,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mVideoView.onTouch(event);
        return true;
    }
}
