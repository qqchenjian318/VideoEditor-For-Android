package com.example.cj.videoeditor.mediacodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by qqche_000 on 2017/11/19.
 */

public class AudioDecodeRunnable implements Runnable{
    final static int TIMEOUT_USEC = 0;

    private MediaExtractor extractor;
    private int audioTrack;
    private AudioCodec.DecodeOverListener mListener;
    private String mPcmFilePath;
    public AudioDecodeRunnable(MediaExtractor extractor, int trackIndex, String savePath, AudioCodec.DecodeOverListener listener) {
        this.extractor = extractor;
        audioTrack = trackIndex;
        mListener = listener;
        mPcmFilePath = savePath;
    }
    @Override
    public void run() {
        try {
            MediaFormat trackFormat = extractor.getTrackFormat(audioTrack);
            //初始化音频的解码器
            MediaCodec audioCodec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME));
            audioCodec.configure(trackFormat, null, null, 0);

            audioCodec.start();

            ByteBuffer[] inputBuffers = audioCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = audioCodec.getOutputBuffers();
            MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo inputInfo = new MediaCodec.BufferInfo();
            boolean codeOver = false;
            boolean inputDone = false;//整体输入结束标志
            FileOutputStream fos = new FileOutputStream(mPcmFilePath);
            while (!codeOver) {
                if (!inputDone) {
                    for (int i = 0; i < inputBuffers.length; i++) {
                        //遍历所以的编码器 然后将数据传入之后 再去输出端取数据
                        int inputIndex = audioCodec.dequeueInputBuffer(TIMEOUT_USEC);
                        if (inputIndex >= 0) {
                            /**从分离器中拿到数据 写入解码器 */
                            ByteBuffer inputBuffer = inputBuffers[inputIndex];//拿到inputBuffer
                            inputBuffer.clear();//清空之前传入inputBuffer内的数据
                            int sampleSize = extractor.readSampleData(inputBuffer, 0);//MediaExtractor读取数据到inputBuffer中

                            if (sampleSize < 0) {
                                audioCodec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                inputDone = true;
                            } else {

                                inputInfo.offset = 0;
                                inputInfo.size = sampleSize;
                                inputInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                                inputInfo.presentationTimeUs = extractor.getSampleTime();
                                Log.e("hero","往解码器写入数据---当前帧的时间戳----"+inputInfo.presentationTimeUs);

                                audioCodec.queueInputBuffer(inputIndex, inputInfo.offset, sampleSize, inputInfo.presentationTimeUs, 0);//通知MediaDecode解码刚刚传入的数据
                                extractor.advance();//MediaExtractor移动到下一取样处
                            }
                        }
                    }
                }

                boolean decodeOutputDone = false;
                byte[] chunkPCM;
                while (!decodeOutputDone) {
                    int outputIndex = audioCodec.dequeueOutputBuffer(decodeBufferInfo, TIMEOUT_USEC);
                    if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        /**没有可用的解码器output*/
                        decodeOutputDone = true;
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        outputBuffers = audioCodec.getOutputBuffers();
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = audioCodec.getOutputFormat();
                    } else if (outputIndex < 0) {
                    } else {
                        ByteBuffer outputBuffer;
                        if (Build.VERSION.SDK_INT >= 21) {
                            outputBuffer = audioCodec.getOutputBuffer(outputIndex);
                        } else {
                            outputBuffer = outputBuffers[outputIndex];
                        }

                        chunkPCM = new byte[decodeBufferInfo.size];
                        outputBuffer.get(chunkPCM);
                        outputBuffer.clear();

                        fos.write(chunkPCM);//数据写入文件中
                        fos.flush();
                        Log.e("hero","---释放输出流缓冲区----:::"+outputIndex);
                        audioCodec.releaseOutputBuffer(outputIndex, false);
                        if ((decodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            /**
                             * 解码结束，释放分离器和解码器
                             * */
                            extractor.release();

                            audioCodec.stop();
                            audioCodec.release();
                            codeOver = true;
                            decodeOutputDone = true;
                        }
                    }

                }
            }
            fos.close();//输出流释放
            mListener.decodeIsOver();
            if (mListener != null){
                mListener.decodeIsOver();
            }
        } catch (Exception e) {
            e.printStackTrace();

            if (mListener != null) {
                mListener.decodeFail();
            }
        }
    }
}
