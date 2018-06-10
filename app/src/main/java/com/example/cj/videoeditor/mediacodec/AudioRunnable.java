package com.example.cj.videoeditor.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;

import com.example.cj.videoeditor.bean.AudioSettingInfo;
import com.example.cj.videoeditor.media.MediaCodecInfo;
import com.example.cj.videoeditor.media.VideoInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by cj on 2017/6/30.
 * desc 音频编解码线程
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioRunnable extends Thread {
    final int TIMEOUT_USEC = 0;
    private AudioSettingInfo mSettingInfo;//混音设置

    private int audioTrackIndex = -1;
    private MediaFormat audioMediaFormat;
    private MediaMuxerRunnable mMediaMuxer;
    private MediaCodecInfo mAudioDecode;
    private boolean mIsBgmLong;

    private List<VideoInfo> mVideoInfos;//多个音频的合并
    private List<Integer> mTrackIndex = new ArrayList<>();//用于记录不同分离器的audio信道的index
    private List<MediaCodecInfo> mAudioDecodes = new ArrayList<>();


    public AudioRunnable(List<VideoInfo> inputFiles, MediaMuxerRunnable mediaMuxer) {
        this.mMediaMuxer = mediaMuxer;
        mVideoInfos = inputFiles;
    }

    @Override
    public void run() {
        try {
            prepare();

            simpleAudioMix();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void prepare() throws IOException {

        for (int i = 0; i < mVideoInfos.size(); i++) {
            //给每一个视频文件都创建一个MediaExtractor
            MediaExtractor temp = new MediaExtractor();
            VideoInfo videoInfo = mVideoInfos.get(i);
            temp.setDataSource(videoInfo.path);

            MediaCodecInfo decode = new MediaCodecInfo();//音频解码info
            decode.extractor = temp;
            decode.path = videoInfo.path;
            decode.cutPoint = videoInfo.cutPoint;
            decode.cutDuration = videoInfo.cutDuration;
            decode.duration = videoInfo.duration;
            mAudioDecodes.add(decode);

        }

        for (int i = 0; i < mAudioDecodes.size(); i++) {//所有音频Extractor选择信道,并且记录信道
            MediaExtractor extractor = mAudioDecodes.get(i).extractor;
            int trackCount = extractor.getTrackCount();
            for (int j = 0; j < trackCount; j++) {
                MediaFormat trackFormat = extractor.getTrackFormat(j);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);

                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(j);
                    mTrackIndex.add(j);
                    break;
                }
            }

        }
    }


    //执行解码音频的代码块
    private AtomicBoolean bgmDecodeOver = new AtomicBoolean(false);//bgm解码完成
    private AtomicBoolean audioDecodeOver = new AtomicBoolean(false);//原声解码完成
    private AtomicInteger bgmCount = new AtomicInteger(0);//背景音pcm文件大小 byte
    private AtomicInteger audioCount = new AtomicInteger(0);//原声pcm文件大小 byte

    private long default_time = 23219;
    private List<Long> totalTime = new ArrayList<>();//每段音频的时长
    private List<Long> eachTime = new ArrayList<>();//每段音频的时间间隔(每帧之间的时间戳间隔)
    private List<Integer> frameCount = new ArrayList<>();//每段音频的帧数

    private int which = 0;//当前是第几段
    private long currentTime = 0;//当前的音频时长 用于记录总时长

    private class AudioMixAndEncodeRunnable implements Runnable {
        private String[] mixFiles;
        private AudioRunnable.EncodeListener mListener;
        private int muxerCount = 0;

        public AudioMixAndEncodeRunnable(String[] files, AudioRunnable.EncodeListener listener) {
            mixFiles = files;
            mListener = listener;
        }

        @Override
        public void run() {
            try {
                int inputIndex;
                ByteBuffer inputBuffer;
                int outputIndex;
                byte[] chunkPCM;
                long start = System.currentTimeMillis();
                //初始化编码器
                MediaFormat encodeFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2);//mime type 采样率 声道数
                encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);//比特率
                encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);

                MediaCodec mediaEncode = MediaCodec.createEncoderByType("audio/mp4a-latm");
                mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mediaEncode.start();

                ByteBuffer[] encodeInputBuffers = mediaEncode.getInputBuffers();
                ByteBuffer[] encodeOutputBuffers = mediaEncode.getOutputBuffers();
                MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();


                //初始化两个io
                int fileNum = mixFiles.length;
                FileInputStream[] audioFileStreams = new FileInputStream[fileNum];
                String audioFile;
                for (int fileIndex = 0; fileIndex < fileNum; ++fileIndex) {
                    audioFile = mixFiles[fileIndex];
                    audioFileStreams[fileIndex] = new FileInputStream(audioFile);
                }

                byte[] readBytes = new byte[8 * 1024];//读取的临时数组
                FileInputStream inputStream;
                byte[][] allAudioBytes = new byte[fileNum][];//用于承载读出的数据
                boolean[] streamDoneArray = new boolean[fileNum];//记录两个输入流是否已经读完
                long timeUs = 0;
                boolean isFirstOutputFrame = true;//是否是输出的第一帧
                boolean encodeDone = false;
                currentTime = totalTime.get(which);//当前段时长
                default_time = eachTime.get(which);//每帧时间戳间隔
                while (!encodeDone) {
                    if (!streamDoneArray[0] || !streamDoneArray[1]) {//数据没有读完
                        inputIndex = mediaEncode.dequeueInputBuffer(TIMEOUT_USEC);
                        if (inputIndex >= 0) {
                            inputBuffer = encodeInputBuffers[inputIndex];
                            inputBuffer.clear();//同解码器
                            //从两个文件中读取数据
                            for (int j = 0; j < fileNum; j++) {
                                inputStream = audioFileStreams[j];
                                if (!streamDoneArray[j] && inputStream.read(readBytes) != -1) {
                                    allAudioBytes[j] = Arrays.copyOf(readBytes, readBytes.length);
                                } else {
                                    //说明某个文件读取完成了
                                    if (mIsBgmLong) {
                                        //说明bgm比原声长，
                                        if (j == 0) {
                                            //说明是原声读取完毕了，那么bgm也不再读取
                                            streamDoneArray[j] = true;
                                            streamDoneArray[1] = true;//bgm也不再读取
                                            //并且跳出去，就不再读数据了
                                            mediaEncode.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                            for (FileInputStream temp : audioFileStreams) {
                                                temp.close();
                                            }
                                            break;
                                        }
                                    } else {
                                        //说明原声比bgm长
                                        if (j == 1) {
                                            //说明是bgm文件读取完了
                                            inputStream.skip(0);
                                            int tempSize = inputStream.read(readBytes);
                                            if (tempSize > 0) {
                                                allAudioBytes[j] = Arrays.copyOf(readBytes, readBytes.length);
                                            } else {
                                                allAudioBytes[j] = new byte[8 * 1024];//那么bgm的部分就用0替代
                                            }
                                        } else {
                                            //说明是原声读取完了
                                            streamDoneArray[j] = true;
                                            streamDoneArray[1] = true;//bgm也不再读取
                                            //结束读取数据和写入数据
                                            mediaEncode.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                            for (FileInputStream temp : audioFileStreams) {
                                                temp.close();
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!streamDoneArray[0] || !streamDoneArray[1]) {
                                //对两个byte数组进行合并
//                                chunkPCM = AudioHardcodeHandler.normalizationMix(allAudioBytes, mSettingInfo.volFirst, mSettingInfo.volSecond);
                                chunkPCM = AudioCodec.nativeAudioMix(allAudioBytes, 1, 1);
                                if (chunkPCM == null) {
                                    break;
                                }
                                inputBuffer.put(chunkPCM);//PCM数据填充给inputBuffer
                                mediaEncode.queueInputBuffer(inputIndex, 0, chunkPCM.length, 0, 0);//通知编码器 编码
                            }
                        }
                    }

                    boolean outputDone = false;
                    while (!outputDone) {//同解码器
                        outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, TIMEOUT_USEC);//同解码器
                        if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            outputDone = true;
                        } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = mediaEncode.getOutputFormat();
                            mMediaMuxer.addMediaFormat(MediaMuxerRunnable.MEDIA_TRACK_AUDIO, newFormat);
                        } else if (outputIndex < 0) {
                        } else {
                            if ((encodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                encodeDone = true;
                                break;
                            }
                            if ((encodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                mediaEncode.releaseOutputBuffer(outputIndex, false);
                                break;
                            }
                            ByteBuffer outputBuffer;
                            if (Build.VERSION.SDK_INT >= 21) {
                                outputBuffer = mediaEncode.getOutputBuffer(outputIndex);//拿到输出Buffer
                            } else {
                                outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
                            }
                            if (isFirstOutputFrame) {
                                timeUs = 0;
                                isFirstOutputFrame = false;
                            } else {
                                timeUs += default_time;
                            }
                            if (timeUs >= currentTime * 1000) {//说明音频切换了,要用下一段音频的平均帧间隔了
                                which++;

                                if (which >= totalTime.size()) {
                                    which = 0;
                                }
                                currentTime += totalTime.get(which);
                                //说明更换视频了
                                default_time = eachTime.get(which);
                            }
                            encodeBufferInfo.presentationTimeUs = timeUs;
                            muxerCount++;
                            mMediaMuxer.addMuxerData(MediaMuxerRunnable.MEDIA_TRACK_AUDIO, outputBuffer, encodeBufferInfo);

                            mediaEncode.releaseOutputBuffer(outputIndex, false);
                        }
                    }
                }
                mediaEncode.stop();
                mediaEncode.release();
                long end = System.currentTimeMillis();
                //删除掉传入的pcm文件
                for (int i = 0; i < mixFiles.length; i++) {
                    File file = new File(mixFiles[i]);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                if (mListener != null) {
                    mListener.encodeIsOver();
                }
            } catch (IOException e) {
                Log.e("hero", " init encoder error ");
            }
        }
    }

    private class AudioDecodeRunnable implements Runnable {
        private boolean isBgm;
        private String tempPcmFile;
        private AudioRunnable.DecodeOverListener mListener;
        private List<MediaCodecInfo> mMediaCodecInfos;//将多个分离器中的音频数据 写入一个pcm文件中
        private List<MediaCodec> mAudioCodec;
        private List<Integer> audioTrackList;//各个分离器中track的index
        private MediaCodec mMediaCodec;
        private MediaCodecInfo mMediaCodecInfo;

        public AudioDecodeRunnable(List<MediaCodecInfo> extractors, List<Integer> trackList, String pcmFile, boolean isBgm, AudioRunnable.DecodeOverListener listener) {
            mMediaCodecInfos = extractors;
            audioTrackList = trackList;
            this.isBgm = isBgm;
            tempPcmFile = pcmFile;
            mListener = listener;
        }

        @Override
        public void run() {
            //初始化多个视频源的原声解码器,
            mAudioCodec = new ArrayList<>();
            /**
             * 1、初始化多个解码器
             * */
            for (int i = 0; i < mMediaCodecInfos.size(); i++) {
                MediaExtractor extractor = mMediaCodecInfos.get(i).extractor;
                MediaFormat trackFormat = extractor.getTrackFormat(audioTrackList.get(i));
                try {
                    MediaCodec temp = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME));
                    temp.configure(trackFormat, null, null, 0);
                    mAudioCodec.add(temp);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String s = null;
            long ss = 0;

            if (!isBgm) {

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();

                for (int i = 0; i < mMediaCodecInfos.size(); i++) {
                    retriever.setDataSource(mMediaCodecInfos.get(i).path);
                    s = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    ss += Long.parseLong(s);
                    totalTime.add(Long.parseLong(s));//添加每段音频的时长
                }
            }


            /*
             * 2、选中第一个解码器
             * */
            mMediaCodec = mAudioCodec.get(0);
            mMediaCodec.start();
            mMediaCodecInfo = mMediaCodecInfos.get(0);
            if (mAudioDecodes.get(0).cutDuration > 0 && mAudioDecodes.get(0).cutPoint + mAudioDecodes.get(0).cutDuration <= mAudioDecodes.get(0).duration) {
                mMediaCodecInfo.extractor.seekTo(mAudioDecodes.get(0).cutPoint * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
            /*
             * 3、初始化当前解码器的相关参数
             * */
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo inputInfo = new MediaCodec.BufferInfo();
            boolean codeOver = false;//所有音频是否全部解码
            int curIndex = 0;//当前是输入的第几个音频
            boolean isNewStep = false;//音频段落切换标志
            boolean isFirstFrame = true;
            long samptime = 0;//记录上一帧音频时间戳,以便于取差值
            boolean inputDone = false;//整体输入结束标志
            int decode = 0;//用以记录每段输入了多少帧音频
            try {
                FileOutputStream fos = new FileOutputStream(tempPcmFile);
                long stratTime = System.currentTimeMillis();
                while (!codeOver) {
                    if (!inputDone) {
                        for (int i = 0; i < inputBuffers.length; i++) {
                            //遍历所以的编码器 然后将数据传入之后 再去输出端取数据
                            int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                            if (inputIndex >= 0) {
                                /*
                                 * 从分离器中拿到数据 写入解码器
                                 * */
                                ByteBuffer inputBuffer = inputBuffers[inputIndex];//拿到inputBuffer
                                inputBuffer.clear();//清空之前传入inputBuffer内的数据
                                int sampleSize = mMediaCodecInfo.extractor.readSampleData(inputBuffer, 0);//MediaExtractor读取数据到inputBuffer中

                                if (sampleSize < 0) {
                                    mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;
                                } else {
                                    /*音频对解码前的数据 进行抛弃*/
                                    long sampleTime = mMediaCodecInfo.extractor.getSampleTime();
                                    boolean isWrite = true;
                                    if (mMediaCodecInfo.cutDuration > 0 && sampleTime / 1000 < mMediaCodecInfo.cutPoint) {
                                        //说明还没有到剪切点
                                        isWrite = false;
                                        mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, 0);//释放inputBuffer
                                    }
                                    if (mMediaCodecInfo.cutDuration > 0 && sampleTime / 1000 > (mMediaCodecInfo.cutDuration + mMediaCodecInfo.cutPoint)) {
                                        isWrite = false;
                                        mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, 0);//虽然跳过了 但是需要释放当前的inputBuffer
                                    }

                                    if (isWrite) {//当前读取的数据可用,且在裁剪范围内
                                        inputInfo.offset = 0;
                                        inputInfo.size = sampleSize;
                                        inputInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;

                                        if (isNewStep) {//切换到了下一段音频
                                            if (!isBgm) {
                                                frameCount.add(decode);//保存之前一段的帧数
                                                decode = 0;
                                            }
                                            inputInfo.presentationTimeUs += 23219;
                                            isNewStep = false;
                                        } else {
                                            if (isFirstFrame) {//当为第一个音频的第一帧时
                                                inputInfo.presentationTimeUs = 0;
                                                isFirstFrame = false;
                                            } else {
                                                inputInfo.presentationTimeUs += mMediaCodecInfo.extractor.getSampleTime() - samptime;
                                            }
                                        }
                                        decode++;
                                        mMediaCodec.queueInputBuffer(inputIndex, inputInfo.offset, sampleSize, inputInfo.presentationTimeUs, 0);//通知MediaDecode解码刚刚传入的数据
                                    }
                                    samptime = mMediaCodecInfo.extractor.getSampleTime();
                                    mMediaCodecInfo.extractor.advance();//MediaExtractor移动到下一取样处
                                }
                            }
                        }
                    }

                    boolean decodeOutputDone = false;
                    byte[] chunkPCM;
                    while (!decodeOutputDone) {
                        int outputIndex = mMediaCodec.dequeueOutputBuffer(decodeBufferInfo, TIMEOUT_USEC);
                        if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            /**没有可用的解码器output*/
                            decodeOutputDone = true;
                        } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            outputBuffers = mMediaCodec.getOutputBuffers();
                        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = mMediaCodec.getOutputFormat();
                        } else if (outputIndex < 0) {
                        } else {
                            ByteBuffer outputBuffer;
                            if (Build.VERSION.SDK_INT >= 21) {
                                outputBuffer = mMediaCodec.getOutputBuffer(outputIndex);
                            } else {
                                outputBuffer = outputBuffers[outputIndex];
                            }
                            boolean isUseful = true;

                            if (isUseful) {
                                chunkPCM = new byte[decodeBufferInfo.size];
                                outputBuffer.get(chunkPCM);
                                outputBuffer.clear();
                                byte[] result = chunkPCM;
                                if (chunkPCM.length < 4096) {//这里看起来应该是16位单声道转16位双声道
                                    //说明是单声道的,需要转换一下
                                    byte[] stereoBytes = new byte[decodeBufferInfo.size * 2];
                                    for (int i = 0; i < chunkPCM.length; i += 2) {
                                        stereoBytes[i * 2 + 0] = chunkPCM[i];
                                        stereoBytes[i * 2 + 1] = chunkPCM[i + 1];
                                        stereoBytes[i * 2 + 2] = chunkPCM[i];
                                        stereoBytes[i * 2 + 3] = chunkPCM[i + 1];
                                    }
                                    result = stereoBytes;
                                }
                                if (isBgm) {
                                    int i = bgmCount.get() + result.length;//
                                    bgmCount.set(i);
                                } else {
                                    int i = audioCount.get() + result.length;
                                    audioCount.set(i);
                                }
                                fos.write(result);//数据写入文件中
                                fos.flush();
                            }
                            mMediaCodec.releaseOutputBuffer(outputIndex, false);
                            if ((decodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                curIndex++;
                                if (curIndex >= mMediaCodecInfos.size()) {
                                    codeOver = true;
                                    inputDone = true;
                                    if (!isBgm) {
                                        frameCount.add(decode);//加入最后一段音频帧数
                                    }
                                } else {
                                    /**
                                     * 更换分离器
                                     * 更换解码器
                                     * 开始解码下一个音频
                                     * */
                                    mMediaCodecInfo.extractor.release();
                                    mMediaCodecInfo = mMediaCodecInfos.get(curIndex);
                                    if (mMediaCodecInfo.cutDuration > 0) {
                                        mMediaCodecInfo.extractor.seekTo(mMediaCodecInfo.cutPoint * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                                    }
                                    mMediaCodec.stop();
                                    mMediaCodec.release();
                                    mMediaCodec = mAudioCodec.get(curIndex);
                                    mMediaCodec.start();
                                    isNewStep = true;
                                    codeOver = false;
                                    inputBuffers = mMediaCodec.getInputBuffers();
                                    outputBuffers = mMediaCodec.getOutputBuffers();
                                    inputDone = false;
                                }
                            }
                        }
                    }

                    if (isBgm && mIsBgmLong && audioDecodeOver.get()) {
                        //如果当前是bgm解码线程、并且是bgm比较长、并且原声解码完成了，就需要判定一下
                        //当前bgm解码个数是否大于等于audioCount解码个数，如果大于 就退出线程
                        if (bgmCount.get() >= audioCount.get()) {
                            break;
                        }
                    }
                }//大循环结束

                if (isBgm) {
                    //bgm解码完成
                    bgmDecodeOver.set(true);
                } else {
                    audioDecodeOver.set(true);
                }
                if (!isBgm) {
                    for (int i = 0; i < frameCount.size(); i++) {
                        long l = totalTime.get(i) * 1000 / frameCount.get(i);//计算每段音频的帧平均间隔
                        eachTime.add(l);
                    }
                }
                fos.close();//输出流释放
                //释放最后一个decoder
                mMediaCodec.stop();
                mMediaCodec.release();
                if (mListener != null) {
                    mListener.decodeIsOver();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void log(boolean isBgm, String log) {
        if (!isBgm) {
            Log.e("audioo", log);
        } else {
            Log.e("bgm", log);
        }
    }


    /**
     * 只是将原视频中的音频分离出来，然后进行添加
     * 不需要编解码等操作
     * 支持多个视频文件的音频连续写入
     * 暂时不考虑视频中没有音频的情况
     * 不考虑部分视频的音频有问题 导致音频速度加快或者减慢的情况（主要是因为立体声和单声道引起的
     * stereo和mono）
     */
    private void simpleAudioMix() {
        MediaExtractor mExtractor = mAudioDecodes.get(0).extractor;//拿到第一个分离器
        mAudioDecode = mAudioDecodes.get(0);//拿到第一个视频的信息
        int trackCount = mExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {//初始化第一段视频的音频format
            MediaFormat trackFormat = mExtractor.getTrackFormat(i);
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i;
                audioMediaFormat = trackFormat;
                break;
            }
        }
        //将第一个视频的audioFormat作为整体音频的audioFormat
        mMediaMuxer.addMediaFormat(MediaMuxerRunnable.MEDIA_TRACK_AUDIO, audioMediaFormat);

        ByteBuffer buffer = ByteBuffer.allocate(50 * 1024);

        //分离音频
        int curIndex = 0;//当前是第几段视频，从第0段开始，
        boolean isNextStep = false;
        boolean isFirstFrame = true;
        boolean isFirstSeek = false;
        long samptime = 0;
        long lastTime = 0;//上一帧的时间戳
        if (audioTrackIndex != -1) {
            if (mAudioDecode.cutDuration > 0 && (mAudioDecode.cutPoint + mAudioDecode.cutDuration) <= mAudioDecode.duration) {
                //说明是需要剪切的 跳转到剪切点 现在只是跳了第一个视频
                mAudioDecode.extractor.seekTo(mAudioDecode.cutPoint * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                isFirstSeek = true;
            }
            while (true) {
                int readSampleData = mExtractor.readSampleData(buffer, 0);
                if (readSampleData < 0) {
                    //说明 本地读取完毕了
                    curIndex++;
                    if (curIndex < mAudioDecodes.size()) {
                        //说明还有新的音频要添加
                        mAudioDecode.extractor.release();//老的释放掉
                        mExtractor = mAudioDecodes.get(curIndex).extractor;//更换extractor
                        mAudioDecode = mAudioDecodes.get(curIndex);//更换音频的那个啥
                        if (mAudioDecode.cutDuration > 0 && (mAudioDecode.cutPoint + mAudioDecode.cutDuration) <= mAudioDecode.duration) {
                            //说明是需要剪切的 跳转到剪切点 现在只是跳了第一个视频
                            mAudioDecode.extractor.seekTo(mAudioDecode.cutPoint * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                        }
                        isNextStep = true;
                    } else {
                        //说明已经没有其他的音频了 就break掉
                        break;
                    }
                } else {
                    long sampleTime = mAudioDecode.extractor.getSampleTime();//当前的时间戳
                    //如果这个时间戳以及大于了 就break掉
                    if (mAudioDecode.cutDuration > 0 && (mAudioDecode.cutPoint + mAudioDecode.cutDuration) <= sampleTime / 1000) {
                        /**
                         * 说明本地视频到了剪切点了，要做以下几件事情
                         * 1、跳出本次写入
                         * 2、更换源文件
                         * */
                        curIndex++;
                        if (curIndex < mAudioDecodes.size()) {
                            //说明还有源文件
                            mAudioDecode.extractor.release();
                            mAudioDecode = mAudioDecodes.get(curIndex);
                            if (mAudioDecode.cutDuration > 0 && (mAudioDecode.cutPoint + mAudioDecode.cutDuration) <= mAudioDecode.duration) {
                                //说明是需要剪切的 跳转到剪切点 现在只是跳了第一个视频
                                mAudioDecode.extractor.seekTo(mAudioDecode.cutPoint * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            }
                            isNextStep = true;
                            continue;
                        } else {
                            //说明已经没有了
                            break;
                        }
                    }

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.offset = 0;
                    info.size = readSampleData;
                    info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    if (isNextStep) {
                        //说明是新的一段音频
                        info.presentationTimeUs = lastTime + 23219;
                        isNextStep = false;
                    } else {
                        //当前帧的时间戳 减去上次帧的时间戳 加上 上次数据帧的时间 就是本次帧的时间戳
                        if (isFirstSeek) {
                            //说明第一个视频跳帧过
                            info.presentationTimeUs = 0;
                            isFirstSeek = false;
                        } else {
                            if (isFirstFrame) {
                                info.presentationTimeUs = 0;
                                isFirstFrame = false;
                            } else {
                                info.presentationTimeUs = lastTime + (mAudioDecode.extractor.getSampleTime() - samptime);
                            }
                        }
                    }
                    lastTime = info.presentationTimeUs;
                    samptime = mAudioDecode.extractor.getSampleTime();
                    mMediaMuxer.addMuxerData(MediaMuxerRunnable.MEDIA_TRACK_AUDIO, buffer, info);
                    mAudioDecode.extractor.advance();
                }
            }
            mAudioDecode.extractor.release();
            mMediaMuxer.audioIsOver();
        }
    }

    interface DecodeOverListener {
        void decodeIsOver();
    }

    interface EncodeListener {
        void encodeIsOver();
    }
}
