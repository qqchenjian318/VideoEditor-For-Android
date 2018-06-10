package com.example.cj.videoeditor.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;


import com.example.cj.videoeditor.media.MediaCodecInfo;
import com.example.cj.videoeditor.media.VideoInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cj on 2017/6/30.
 * desc 视频编解码线程
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoRunnable extends Thread {
    private static final String MIME_TYPE = "video/avc";
    private static final int bitRate = 3000000;       //视频编码波特率
    private static final int frameRate = 30;           //视频编码帧率
    private static final int frameInterval = 1;
    private MediaMuxerRunnable mMediaMuxer;

//    private GPUImageFilter filter;

    private MediaFormat videoOutputFormat;

    //处理多段视频
    private List<VideoInfo> mVideoInfos;//多段视频的信息
    private List<MediaExtractor> mExtractors;
    private List<MediaCodecInfo> mMediaCodecInfos;
    private VideoInfo mInfo;

    public VideoRunnable(List<VideoInfo> inputFiles, MediaMuxerRunnable mediaMuxer) {
        this.mMediaMuxer = mediaMuxer;
        this.mVideoInfos = inputFiles;
//        this.filter = MagicFilterFactory.initFilters(filterType);
    }

    @Override
    public void run() {
        try {
            prepare();
            editVideo(videoOutputFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editVideo(MediaFormat outputFormat) throws IOException {
        MediaCodec videoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        videoEncoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        InputSurface inputSurface = new InputSurface(videoEncoder.createInputSurface());
        inputSurface.makeCurrent();
        videoEncoder.start();//编码器启动

        OutputSurface outputSurface = new OutputSurface(mInfo);

        List<MediaCodec> decodeList = new ArrayList<>();
        List<MediaFormat> formatList = new ArrayList<>();
        try {
            //初始化decoder,format,extractor
            for (int i = 0; i < mVideoInfos.size(); i++) {
                MediaExtractor extractor = mExtractors.get(i);
                int trackCount = extractor.getTrackCount();
                for (int j = 0; j < trackCount; j++) {
                    MediaFormat format = extractor.getTrackFormat(j);
                    if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                        MediaCodec videoDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
                        decodeList.add(videoDecoder);
                        formatList.add(format);
                        extractor.selectTrack(j);
                        break;
                    }
                }
            }

            editVideoData(mMediaCodecInfos, decodeList, formatList, outputSurface, inputSurface, videoEncoder);
        } finally {

            outputSurface.release();
            inputSurface.release();
            videoEncoder.stop();
            videoEncoder.release();
        }
    }

    //重构分离数据 解码数据 编码数据的流程
    private void editVideoData(List<MediaCodecInfo> mediaCodecInfos, List<MediaCodec> decodeList,
                               List<MediaFormat> formatList, OutputSurface outputSurface,
                               InputSurface inputSurface, MediaCodec videoEncoder) {
        long start = System.currentTimeMillis();
        final int TIMEOUT_USEC = 0;
        /*
         * 1、初始化第一个解码器
         * */
        MediaCodec decoder = decodeList.get(0);
        decoder.configure(formatList.get(0), outputSurface.getSurface(), null, 0);
        decoder.start();
        ByteBuffer[] inputBuffers = decoder.getInputBuffers();

        /*
         * 2、初始化第一个分离器
         * */
        MediaCodecInfo mediaCodecInfo = mediaCodecInfos.get(0);

        /* 3、初始化编码器*/
        ByteBuffer[] encoderOutputBuffers = videoEncoder.getOutputBuffers();
        /*
         * 4、初始化解码器和编码器的bufferInfo
         * */
        MediaCodec.BufferInfo encodeOutputInfo = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo decodeOutputInfo = new MediaCodec.BufferInfo();

        /*
         * 5、初始化while循环 while循环判断结束的标准在于 所有编码器中的数据 都已经添加到了分离器
         * */
        boolean outputDone = false;//是否整体编解码完毕
        boolean isNextStep = false;//是否新加了一段视频
        boolean isFirstDecodeInputFrame = true;//是否是解码输入的第一帧
        boolean isFirstDecodeOutputFrame = true;//是否是第一段视频的第一帧
        boolean inputDone = false;//是否输入结束
        boolean isChangeVideo = false;//是否切换视频

        int curVideoIndex = 0;//当前解码视频的位置

        long decodeInputTimeStamp = 0;
        long lastInputTime = 0;
        long encodeInputTimeStamp = 0;
        long lastVideoTime = 0;

        while (!outputDone) {
            /*
             * 6、循环的第一步 从分离器中取数据 写入到解码器中
             * */
            if (!inputDone) {
                int inputIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                Log.e("videoo", "---解码器 是否可用  " + inputIndex);
                if (inputIndex >= 0) {
                    /*说明解码器有可用的ByteBuffer*/
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];
                    inputBuffer.clear();
                    /*从分离器中取数据*/
                    int readSampleData = mediaCodecInfo.extractor.readSampleData(inputBuffer, 0);
                    if (readSampleData < 0) {
                        /*说明该分离器中 没有数据了 发送一个解码流结束的标志位*/
                        Log.e("send", "-----发送end--flag");
                        inputDone = true;
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        /*
                         * 重写输入数据的时间戳
                         * 关键点在于 如果是下一段视频的数据
                         * 那么 + 30000
                         * */
                        if (isNextStep) {
                            //说明是新增了一段视频
                            decodeInputTimeStamp += 30000;
                            isNextStep = false;
                        } else {
                            if (isFirstDecodeInputFrame) {
                                decodeInputTimeStamp = 0;
                                isFirstDecodeInputFrame = false;
                            } else {
                                decodeInputTimeStamp += mediaCodecInfo.extractor.getSampleTime() - lastInputTime;
                            }
                        }
                        lastInputTime = mediaCodecInfo.extractor.getSampleTime();
                        /*将分离器的数据 插入解码器中*/
                        decoder.queueInputBuffer(inputIndex, 0, readSampleData, decodeInputTimeStamp, 0);
                        mediaCodecInfo.extractor.advance();
                    }
                }
            }

            /*
             * 7、循环的第二步，轮询取出解码器解码完成的数据 并且加入到编码器中
             * */
            boolean decodeOutputDone = false;
            boolean encodeDone = false;
            while (!decodeOutputDone || !encodeDone) {
                /*说明解码器的输出output有数据*/
                int outputIndex = decoder.dequeueOutputBuffer(decodeOutputInfo, TIMEOUT_USEC);
                Log.e("videoo", "  解码器出来的index   " + outputIndex);
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    /*没有可用的解码器output*/
                    decodeOutputDone = true;
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                } else if (outputIndex < 0) {
                } else {
                    /*
                     * 8、判断本次是否有数据 以及本次数据是否需要传入编码器
                     * */
                    boolean doRender = (decodeOutputInfo.size != 0);
                    /*
                     * 9、根据当前解码出来的数据的时间戳 判断 是否需要写入编码器
                     * */
                    boolean isUseful = true;
                    if (decodeOutputInfo.presentationTimeUs <= 0) {
                        doRender = false;
                    }

                    decoder.releaseOutputBuffer(outputIndex, doRender && isUseful);

                    if (doRender && isUseful) {
                        /*
                         * 是有效数据 让他写到编码器中
                         * 并且对时间戳 进行重写
                         * */
                        Log.e("videoo", "---卡主了？ 一  " + decodeOutputInfo.size);
                        outputSurface.awaitNewImage();
                        Log.e("videoo", "---卡住了  === 二");
                        outputSurface.drawImage();
                        Log.e("videoo", "---卡住了  === 三！！！");
                        if (isFirstDecodeOutputFrame) {
                            /*如果是第一个视频的话，有可能时间戳不是从0 开始的 所以需要初始化*/
                            isFirstDecodeOutputFrame = false;
                        } else {
                            /*
                             * 如果是更换了一个视频源 就+30000us
                             */
                            if (isChangeVideo) {
                                Log.e("videoo", "---更换了一个视频源===+30000");
                                isChangeVideo = false;
                                encodeInputTimeStamp = (encodeInputTimeStamp + 30000);
                            } else {
                                encodeInputTimeStamp = (encodeInputTimeStamp + (decodeOutputInfo.presentationTimeUs - lastVideoTime));
                            }
                        }
                        Log.e("videooo", "---在编码画面帧的时候，重置时间戳===" + encodeInputTimeStamp);
                        inputSurface.setPresentationTime(encodeInputTimeStamp * 1000);
                        inputSurface.swapBuffers();

                    } else {
                        Log.e("videoo", "---解码出来的视频有问题=== " + doRender + "   " + isUseful);
                    }
                    lastVideoTime = decodeOutputInfo.presentationTimeUs;

                    if ((decodeOutputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        /**
                         * 解码器解码完成了，说明该分离器的数据写入完成了 并且都已经解码完成了
                         * 更换分离器和解码器或者结束编解码
                         * */
                        curVideoIndex++;
                        if (curVideoIndex < mediaCodecInfos.size()) {
                            /*说明还有需要解码的*/
                            /*
                             * 1)、更换分离器
                             * 2）、更换解码器
                             * */
                            mediaCodecInfo.extractor.release();
                            mediaCodecInfo = mediaCodecInfos.get(curVideoIndex);

                            decoder.stop();
                            decoder.release();
                            decoder = decodeList.get(curVideoIndex);
                            decoder.configure(formatList.get(curVideoIndex), outputSurface.getSurface(), null, 0);
                            decoder.start();
                            inputBuffers = decoder.getInputBuffers();
                            outputSurface.onVideoSizeChanged(mVideoInfos.get(curVideoIndex));
                            inputDone = false;//解码器继续写入数据
                            isChangeVideo = true;
                            isNextStep = true;
                            Log.e("videoo", "---更换分离器 and 解码器---==");
                        } else {
                            /*没有数据了 就给编码器发送一个结束的标志位*/
                            videoEncoder.signalEndOfInputStream();
                            inputDone = true;
                            Log.e("videoo", "---所有视频都解码完成了 告诉编码器 可以结束了---==");
                        }
                    }
                }
                /*
                 * 10、从编码器中取数据 重写时间戳 添加到混合器中
                 *   如果发生了更换视频源 那么 就先把编码器的所有数据读取出来
                 *   然后再向解码器输入数据 以确保编码器时间戳的正确性
                 * */
                while (!encodeDone) {
                    int encodeOutputState = videoEncoder.dequeueOutputBuffer(encodeOutputInfo, TIMEOUT_USEC);
                    if (encodeOutputState == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        /* 说明没有可用的编码器 */
                        encodeDone = true;
                    } else if (encodeOutputState == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        encoderOutputBuffers = videoEncoder.getOutputBuffers();
                    } else if (encodeOutputState == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        /*初始化混合器的MediaFormat*/
                        MediaFormat newFormat = videoEncoder.getOutputFormat();
                        mMediaMuxer.addMediaFormat(MediaMuxerRunnable.MEDIA_TRACK_VIDEO, newFormat);
                        Log.e("videoo", "---添加MediaFormat");
                    } else if (encodeOutputState < 0) {
                    } else {
                        /*判断编码器是否完成了编码*/
                        outputDone = (encodeOutputInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        if (outputDone) {
                            break;
                        }
                        if ((encodeOutputInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            encodeOutputInfo.size = 0;
                        }
                        ByteBuffer encoderOutputBuffer = encoderOutputBuffers[encodeOutputState];
                        if (encodeOutputInfo.size != 0) {
                            Log.e("videoo", "--写入混合器的数据----presentationTime===" + encodeOutputInfo.presentationTimeUs + "===size===" + encodeOutputInfo.size + "----flags==" + encodeOutputInfo.flags);
                            mMediaMuxer.addMuxerData(MediaMuxerRunnable.MEDIA_TRACK_VIDEO, encoderOutputBuffer, encodeOutputInfo);
                        }
                        videoEncoder.releaseOutputBuffer(encodeOutputState, false);
                    }
                }
            }
        }
        //释放最后一个decoder
        decoder.stop();
        decoder.release();
        mMediaMuxer.videoIsOver();
        long end = System.currentTimeMillis();
        Log.e("timee", "---视频编码完成---视频编码耗时-==" + (end - start));
    }


    private void prepare() throws IOException {
        mExtractors = new ArrayList<>();
        mMediaCodecInfos = new ArrayList<>();

        //初始化所以的分离器
        for (int i = 0; i < mVideoInfos.size(); i++) {
            MediaExtractor temp = new MediaExtractor();
            VideoInfo videoInfo = mVideoInfos.get(i);
            temp.setDataSource(videoInfo.path);
            mExtractors.add(temp);
            //多个视频剪切，根据视频所在位置 对本视频剪切点进行调整
            MediaCodecInfo decode = new MediaCodecInfo();
            decode.path = videoInfo.path;
            decode.extractor = temp;
            decode.duration = videoInfo.duration;
            mMediaCodecInfos.add(decode);
        }

        MediaExtractor mExtractor = mExtractors.get(0);
        int trackCount = mExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {//根据第一个视频信息来确定编码信息
            MediaFormat trackFormat = mExtractor.getTrackFormat(i);
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                mInfo = mVideoInfos.get(0);
                if (mInfo.rotation == 0 || mInfo.rotation == 180) {
                    videoOutputFormat = MediaFormat.createVideoFormat(MIME_TYPE, mInfo.width, mInfo.height);
                } else {
                    videoOutputFormat = MediaFormat.createVideoFormat(MIME_TYPE, mInfo.height, mInfo.width);
                }
                videoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
                videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frameInterval);
                break;
            }
        }
    }
}
