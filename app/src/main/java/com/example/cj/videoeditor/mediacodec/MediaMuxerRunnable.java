package com.example.cj.videoeditor.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;


import com.example.cj.videoeditor.gpufilter.helper.MagicFilterType;
import com.example.cj.videoeditor.media.VideoInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by cj on 2017/6/30.
 * desc 音视频混合器，只要是将音视频的混合分成两条子线程进行处理
 * 音频线程AudioRunnable 进行音频的编解码 以及向混合器中写入数据
 * 视频线程VideoRunnable 进行视频的编解码 以及向混合器中写入数据
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaMuxerRunnable extends Thread {
    public static final int MEDIA_TRACK_AUDIO = 1;
    public static final int MEDIA_TRACK_VIDEO = 2;

    private AudioRunnable mAudioRunnable;
    private VideoRunnable mVideoRunnable;
    private final Object lock = new Object();

    private String outputFilePath;
    private MediaMuxer mMediaMuxer;
    private int mVideoTrack;
    private int mAudioTrack;
    private boolean isVideoAdd = false;
    private boolean isAudioAdd = false;
    private boolean isMuxerStarted = false;
    private boolean isVideoOver = false;
    private boolean isAudioOver = false;

    private MagicFilterType mFilterType;
    private MuxerListener mListener;
    private List<VideoInfo> mVideoInfos;//视频文件的信息
    int curMode;

    public MediaMuxerRunnable() {
    }

    //设置要合并的视频信息和输出文件path
    public void setVideoInfo(List<VideoInfo> inputVideoInfo, String outputFile) {
        mVideoInfos = inputVideoInfo;
        outputFilePath = outputFile;
    }

    public void addMuxerListener(MuxerListener listener) {
        mListener = listener;
    }

    public void setFilterType(MagicFilterType filterType) {
        mFilterType = filterType;
    }

    @Override
    public void run() {
        //
        if (mListener != null) {
            mListener.onStart();
        }
        initMuxer();
        mAudioRunnable = new AudioRunnable(mVideoInfos, this);
        mVideoRunnable = new VideoRunnable(mVideoInfos, this);
        mAudioRunnable.start();
        mVideoRunnable.start();
    }

    //初始化混合器，编解码线程
    private void initMuxer() {
        checkUseful();
        try {
            mMediaMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //检查各项参数是否正确
    private void checkUseful() {
        if (mVideoInfos == null || mVideoInfos.size() == 0) {
            throw new IllegalStateException(" 必须先设置要处理的视频");
        }
        if (TextUtils.isEmpty(outputFilePath)) {
            throw new IllegalStateException(" 必须设置视频输出路径");
        }
    }

    /**
     * @param trackIndex audio_track == 1  video_track == 2
     * @param format     MediaFormat
     */
    public void addMediaFormat(int trackIndex, MediaFormat format) {
        synchronized (lock) {
            if (mMediaMuxer == null) {
                return;
            }
            if (trackIndex == MEDIA_TRACK_AUDIO) {
                mAudioTrack = mMediaMuxer.addTrack(format);
                isAudioAdd = true;

            } else if (trackIndex == MEDIA_TRACK_VIDEO) {
                mVideoTrack = mMediaMuxer.addTrack(format);
                isVideoAdd = true;
            }
            if (isAudioAdd && isVideoAdd) {
                mMediaMuxer.start();
                isMuxerStarted = true;
                lock.notify();
                Log.e("muxer", "start media muxer waiting for data...");
            }
        }
    }

    //往混合器中添加数据
    public void addMuxerData(int trackIndex, ByteBuffer buffer, MediaCodec.BufferInfo info) {
        if (!isMuxerStarted) {
            synchronized (lock) {
                if (!isMuxerStarted) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (trackIndex == MEDIA_TRACK_AUDIO) {
            mMediaMuxer.writeSampleData(mAudioTrack, buffer, info);
        } else if (trackIndex == MEDIA_TRACK_VIDEO) {
            mMediaMuxer.writeSampleData(mVideoTrack, buffer, info);
        }
    }

    /**
     * 如果当前没有数据的话，会lock一直在wait，如果解码完成
     */
    //音频解码完成
    public void audioIsOver() {
        isAudioOver = true;
        editorFinish();
    }

    //视频解码编辑完成
    public void videoIsOver() {
        isVideoOver = true;
        editorFinish();
    }
    public void editorFinish(){
        synchronized (lock){
            if(isAudioOver&&isVideoOver){
                stopMediaMuxer();
                if (mListener != null) {
                    mListener.onFinish();
                }
            }
        }
    }
    /**
     * 停止MediaMuxer
     */
    private void stopMediaMuxer() {
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            isAudioAdd = false;
            isVideoAdd = false;
            mMediaMuxer = null;
        }

    }


    public interface MuxerListener {
        void onStart();

        void onFinish();
    }
}
