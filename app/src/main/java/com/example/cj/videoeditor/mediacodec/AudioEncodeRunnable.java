package com.example.cj.videoeditor.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by cj on 2017/11/19.
 * pcm转音频的编码线程
 */

public class AudioEncodeRunnable implements Runnable {
    private String pcmPath;
    private String audioPath;
    private AudioCodec.AudioDecodeListener mListener;

    public AudioEncodeRunnable(String pcmPath, String audioPath, final AudioCodec.AudioDecodeListener listener) {
        this.pcmPath = pcmPath;
        this.audioPath = audioPath;
        mListener = listener;
    }

    @Override
    public void run() {
        try {
            if (!new File(pcmPath).exists()) {
                if (mListener != null) {
                    mListener.decodeFail();
                }
                return;
            }
            FileInputStream fis = new FileInputStream(pcmPath);
            byte[] buffer = new byte[8 * 1024];
            byte[] allAudioBytes;

            int inputIndex;
            ByteBuffer inputBuffer;
            int outputIndex;
            ByteBuffer outputBuffer;
            byte[] chunkAudio;
            int outBitSize;
            int outPacketSize;
            //初始化编码器
            MediaFormat encodeFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2);//mime type 采样率 声道数
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);

            MediaCodec mediaEncode = MediaCodec.createEncoderByType("audio/mp4a-latm");
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaEncode.start();

            ByteBuffer[] encodeInputBuffers = mediaEncode.getInputBuffers();
            ByteBuffer[] encodeOutputBuffers = mediaEncode.getOutputBuffers();
            MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();

            //初始化文件写入流
            FileOutputStream fos = new FileOutputStream(new File(audioPath));
            BufferedOutputStream bos = new BufferedOutputStream(fos, 500 * 1024);
            boolean isReadEnd = false;
            while (!isReadEnd) {
                for (int i = 0; i < encodeInputBuffers.length - 1; i++) {
                    if (fis.read(buffer) != -1) {
                        allAudioBytes = Arrays.copyOf(buffer, buffer.length);
                    } else {
                        Log.e("hero", "---文件读取完成---");
                        isReadEnd = true;
                        break;
                    }
                    Log.e("hero", "---io---读取文件-写入编码器--" + allAudioBytes.length);
                    inputIndex = mediaEncode.dequeueInputBuffer(-1);
                    inputBuffer = encodeInputBuffers[inputIndex];
                    inputBuffer.clear();//同解码器
                    inputBuffer.limit(allAudioBytes.length);
                    inputBuffer.put(allAudioBytes);//PCM数据填充给inputBuffer
                    mediaEncode.queueInputBuffer(inputIndex, 0, allAudioBytes.length, 0, 0);//通知编码器 编码
                }
                outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);//同解码器
                while (outputIndex >= 0) {
                    //从编码器中取出数据
                    outBitSize = encodeBufferInfo.size;
                    outPacketSize = outBitSize + 7;//7为ADTS头部的大小
                    outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
                    outputBuffer.position(encodeBufferInfo.offset);
                    outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
                    chunkAudio = new byte[outPacketSize];
                    AudioCodec.addADTStoPacket(chunkAudio, outPacketSize);//添加ADTS 代码后面会贴上
                    outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中 偏移量offset=7 你懂得
                    outputBuffer.position(encodeBufferInfo.offset);
                    Log.e("hero", "--编码成功-写入文件----" + chunkAudio.length);
                    bos.write(chunkAudio, 0, chunkAudio.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
                    bos.flush();

                    mediaEncode.releaseOutputBuffer(outputIndex, false);
                    outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
                }
            }
            mediaEncode.stop();
            mediaEncode.release();
            fos.close();
            if (mListener != null) {
                mListener.decodeOver();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mListener != null) {
                mListener.decodeFail();
            }
        }
    }
}
