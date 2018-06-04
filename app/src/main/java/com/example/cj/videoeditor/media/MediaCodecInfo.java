package com.example.cj.videoeditor.media;

import android.media.MediaExtractor;

/**
 * Created by cj on 2017/7/11.
 * desc 音频解码的info类 包含了音频path 音频的MediaExtractor
 * 和本段音频的截取点cutPoint
 * 以及剪切时长 cutDuration
 */

public class MediaCodecInfo {
    public String path;
    public MediaExtractor extractor;
    public int cutPoint;
    public int cutDuration;
    public int duration;
}
