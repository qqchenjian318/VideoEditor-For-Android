package com.example.cj.videoeditor.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.cj.videoeditor.Constants;
import com.example.cj.videoeditor.activity.AudioMixActivity;
import com.example.cj.videoeditor.jni.AudioJniUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by cj on 2017/11/5.
 * 音频相关的操作类
 */

public class AudioCodec {
    final static int TIMEOUT_USEC = 0;
    private static Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 从视频文件中分离出音频，并保存到本地
     * */
    public static void getAudioFromVideo(String videoPath, final String audioSavePath, final AudioDecodeListener listener){
        final MediaExtractor extractor = new MediaExtractor();
        int audioTrack = -1;
        boolean hasAudio = false;
        try {
            extractor.setDataSource(videoPath);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioTrack = i;
                    hasAudio = true;
                    break;
                }
            }
            if (hasAudio) {
                extractor.selectTrack(audioTrack);
                final int finalAudioTrack = audioTrack;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            MediaMuxer mediaMuxer = new MediaMuxer(audioSavePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                            MediaFormat trackFormat = extractor.getTrackFormat(finalAudioTrack);
                            int writeAudioIndex = mediaMuxer.addTrack(trackFormat);
                            mediaMuxer.start();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                            extractor.readSampleData(byteBuffer, 0);
                            if (extractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                                extractor.advance();
                            }
                            while (true) {
                                int readSampleSize = extractor.readSampleData(byteBuffer, 0);
                                Log.e("hero","---读取音频数据，当前读取到的大小-----：：："+readSampleSize);
                                if (readSampleSize < 0) {
                                    break;
                                }

                                bufferInfo.size = readSampleSize;
                                bufferInfo.flags = extractor.getSampleFlags();
                                bufferInfo.offset = 0;
                                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                                Log.e("hero","----写入音频数据---当前的时间戳：：："+extractor.getSampleTime());

                                mediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo);
                                extractor.advance();//移动到下一帧
                            }
                            mediaMuxer.release();
                            extractor.release();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null){
                                        listener.decodeOver();
                                    }
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null){
                                        listener.decodeFail();
                                    }
                                }
                            });
                        }
                    }
                }).start();
            }else {
                Log.e("hero", " extractor failed !!!! 没有音频信道");
                if (listener != null){
                    listener.decodeFail();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e("hero", " extractor failed !!!!");
            if (listener != null){
                listener.decodeFail();
            }
        }
    }

    /**
     * 将音频文件解码成原始的PCM数据
     */
    public static void getPCMFromAudio(String audioPath, String audioSavePath, final AudioDecodeListener listener) {
        MediaExtractor extractor = new MediaExtractor();
        int audioTrack = -1;
        boolean hasAudio = false;
        try {
            extractor.setDataSource(audioPath);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioTrack = i;
                    hasAudio = true;
                    break;
                }
            }
            if (hasAudio) {
                extractor.selectTrack(audioTrack);

                //原始音频解码
                new Thread(new AudioDecodeRunnable(extractor, audioTrack, audioSavePath, new DecodeOverListener() {
                    @Override
                    public void decodeIsOver() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null){
                                    listener.decodeOver();
                                }
                            }
                        });
                    }
                    @Override
                    public void decodeFail() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null){
                                    listener.decodeFail();
                                }
                            }
                        });
                    }
                })).start();

            } else {
                Log.e("hero", " select audio file has no auido track");
                if (listener != null){
                    listener.decodeFail();
                }
            }
        } catch (IOException  e) {
            e.printStackTrace();
            Log.e("hero", " decode failed !!!!");
            if (listener != null){
                listener.decodeFail();
            }
        }
    }
    /**
     * PCM文件转音频
     * 从pcm文件中读取byte数据
     * byte数据放进编码器
     * 把编码后的数据放进文件
     * */
    public static void PCM2Audio(String pcmPath,String audioPath,final AudioDecodeListener listener){
       new Thread(new AudioEncodeRunnable(pcmPath, audioPath, new AudioDecodeListener() {
           @Override
           public void decodeOver() {
               if (listener != null){
                   handler.post(new Runnable() {
                       @Override
                       public void run() {
                           listener.decodeOver();
                       }
                   });
               }
           }

           @Override
           public void decodeFail() {
               if (listener != null){
                   handler.post(new Runnable() {
                       @Override
                       public void run() {
                           listener.decodeFail();
                       }
                   });
               }
           }
       })).start();
    }
    public static void audioMix(String audioPathOne, String audioPathTwo, final String outPath, final AudioDecodeListener listener){
        final String path = Constants.getPath("audio/outputPCM/", "PCM_"+System.currentTimeMillis()+".pcm");
        final boolean[] isDecoderOver ={false,false};
        final boolean[] isDecoderFailed ={false,false};

        AudioCodec.getPCMFromAudio(audioPathOne, path, new AudioCodec.AudioDecodeListener() {
            @Override
            public void decodeOver() {
                isDecoderOver[0] = true;
            }

            @Override
            public void decodeFail() {
                isDecoderFailed[0] = true;
            }
        });
        final String path2 = Constants.getPath("audio/outputPCM/", "PCM_"+System.currentTimeMillis()+".pcm");
        AudioCodec.getPCMFromAudio(audioPathTwo, path2, new AudioCodec.AudioDecodeListener() {
            @Override
            public void decodeOver() {
                isDecoderOver[1] = true;
            }

            @Override
            public void decodeFail() {
                isDecoderFailed[1] = true;
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isEnd = false;
                while (!isEnd){
                    if (isDecoderOver[0] &&  isDecoderOver[1]){
                        File file1 = new File(path);
                        File file2 = new File(path2);
                        if (!file1.exists() || !file2.exists()){
                            if (listener != null){
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                      listener.decodeFail();
                                    }
                                });
                            }
                            break;
                        }
                        File[] files = {file1,file2};
                        try {
                            AudioCodec.pcmMix(files, outPath, 1, 1, new AudioDecodeListener() {
                                @Override
                                public void decodeOver() {
                                    if (listener != null){
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.decodeOver();
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void decodeFail() {
                                    if (listener != null){
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.decodeFail();
                                            }
                                        });
                                    }
                                }
                            });

                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (listener != null){
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.decodeFail();
                                    }
                                });
                            }
                            break;
                        }
                    }
                    if (isDecoderFailed[0] || isDecoderFailed[1]){
                        if (listener != null){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.decodeFail();
                                }
                            });
                        }
                        isEnd = true;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 音频混合
     */
    public static void pcmMix(File[] rawAudioFiles, final String outFile, int firstVol, int secondVol,final AudioDecodeListener listener) throws IOException {
        File file = new File(outFile);
        if (file.exists()) {
            file.delete();
        }

        final int fileSize = rawAudioFiles.length;

        FileInputStream[] audioFileStreams = new FileInputStream[fileSize];
        File audioFile = null;

        FileInputStream inputStream;
        byte[][] allAudioBytes = new byte[fileSize][];
        boolean[] streamDoneArray = new boolean[fileSize];
        byte[] buffer = new byte[8 * 1024];


        for (int fileIndex = 0; fileIndex < fileSize; ++fileIndex) {
            audioFile = rawAudioFiles[fileIndex];
            audioFileStreams[fileIndex] = new FileInputStream(audioFile);
        }
        final boolean[] isStartEncode = {false};
        while (true) {

            for (int streamIndex = 0; streamIndex < fileSize; ++streamIndex) {

                inputStream = audioFileStreams[streamIndex];
                if (!streamDoneArray[streamIndex] && ( inputStream.read(buffer)) != -1) {
                    allAudioBytes[streamIndex] = Arrays.copyOf(buffer, buffer.length);
                } else {
                    streamDoneArray[streamIndex] = true;
                    allAudioBytes[streamIndex] = new byte[8 * 1024];
                }
            }

            byte[] mixBytes = nativeAudioMix(allAudioBytes, firstVol, secondVol);
            putPCMData(mixBytes);
            //mixBytes 就是混合后的数据
            Log.e("hero", "-----混音后的数据---" + mixBytes.length+"---isStartEncode--"+isStartEncode[0]);
            if (!isStartEncode[0]){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isStartEncode[0] = true;
                        try {
                            Log.e("hero","start encode thread.....");
                            PCM2AAC("audio/mp4a-latm", outFile);
                            if (listener != null){
                                listener.decodeOver();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("hero"," encode error-----------error------");
                            if (listener != null){
                                listener.decodeFail();
                            }
                        }
                    }
                }).start();
            }
            boolean done = true;
            for (boolean streamEnd : streamDoneArray) {
                if (!streamEnd) {
                    done = false;
                }
            }

            if (done) {
                isDecodeOver = true;
                break;
            }
        }

    }
    /**
     * 原始pcm数据，转aac音频
     * */
    static boolean isDecodeOver = false;
    public static void PCM2AAC(String encodeType, String outputFile) throws IOException {
        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;
        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        byte[] chunkPCM;
        //初始化编码器
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(encodeType,44100,2);//mime type 采样率 声道数
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);

        MediaCodec mediaEncode = MediaCodec.createEncoderByType(encodeType);
        mediaEncode.configure(encodeFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaEncode.start();

        ByteBuffer[] encodeInputBuffers = mediaEncode.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mediaEncode.getOutputBuffers();
        MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();

        //初始化文件写入流
        FileOutputStream fos = new FileOutputStream(new File(outputFile));
        BufferedOutputStream bos = new BufferedOutputStream(fos,500*1024);
        Log.e("hero","--encodeBufferInfo---"+encodeBufferInfo.size);

        while (!chunkPCMDataContainer.isEmpty() || !isDecodeOver){
            for (int i = 0; i < encodeInputBuffers.length - 1; i++) {
                chunkPCM=getPCMData();//获取解码器所在线程输出的数据 代码后边会贴上
                if (chunkPCM == null) {
                    break;
                }
                Log.e("hero","--AAC编码器--取数据---"+chunkPCM.length);

                inputIndex = mediaEncode.dequeueInputBuffer(-1);
                inputBuffer = encodeInputBuffers[inputIndex];
                inputBuffer.clear();//同解码器
                inputBuffer.limit(chunkPCM.length);
                inputBuffer.put(chunkPCM);//PCM数据填充给inputBuffer
                mediaEncode.queueInputBuffer(inputIndex, 0, chunkPCM.length, 0, 0);//通知编码器 编码
            }

            outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);//同解码器
            while (outputIndex >= 0) {//同解码器

                outBitSize=encodeBufferInfo.size;
                outPacketSize=outBitSize+7;//7为ADTS头部的大小
                if (Build.VERSION.SDK_INT >= 21){
                    outputBuffer = mediaEncode.getOutputBuffer(outputIndex);//拿到输出Buffer
                }else {
                    outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
                }

                outputBuffer.position(encodeBufferInfo.offset);
                outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
                chunkAudio = new byte[outPacketSize];
                addADTStoPacket(chunkAudio,outPacketSize);//添加ADTS 代码后面会贴上
                outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中 偏移量offset=7 你懂得
                outputBuffer.position(encodeBufferInfo.offset);
                try {
                    Log.e("hero","---保存文件----"+chunkAudio.length);
                    bos.write(chunkAudio,0,chunkAudio.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
                    bos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaEncode.releaseOutputBuffer(outputIndex,false);
                outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
            }
        }
        mediaEncode.stop();
        mediaEncode.release();
        fos.close();
        ByteBuffer buffer = ByteBuffer.allocate(9 * 1024);
    }
    private static ArrayList<byte[]> chunkPCMDataContainer;
    private static void putPCMData(byte[] pcmChunk) {
        synchronized (AudioCodec.class) {//记得加锁
            if (chunkPCMDataContainer == null){
                chunkPCMDataContainer = new ArrayList<>();
            }
            chunkPCMDataContainer.add(pcmChunk);
        }
    }
    private static byte[] getPCMData() {
        synchronized (AudioCodec.class) {//记得加锁
            if (chunkPCMDataContainer.isEmpty()) {
                return null;
            }

            byte[] pcmChunk = chunkPCMDataContainer.get(0);//每次取出index 0 的数据
            chunkPCMDataContainer.remove(pcmChunk);//取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
            return pcmChunk;
        }
    }
    /**
     * jni进行音频的混音处理，提升速度
     * */
    public static byte[] nativeAudioMix(byte[][] allAudioBytes, float firstVol, float secondVol){
        if (allAudioBytes == null || allAudioBytes.length == 0)
            return null;

        byte[] realMixAudio = allAudioBytes[0];

        //如果只有一个音频的话，就返回这个音频数据
        if(allAudioBytes.length == 1)
            return realMixAudio;

        return AudioJniUtils.audioMix(allAudioBytes[0], allAudioBytes[1], realMixAudio, firstVol, secondVol);
    }
    /**
     * 归一化混音
     * */
    public static byte[] normalizationMix(byte[][] allAudioBytes, int firstVol, int secondVol){
        if (allAudioBytes == null || allAudioBytes.length == 0)
            return null;

        byte[] realMixAudio = allAudioBytes[0];
        //如果只有一个音频的话，就返回这个音频数据
        if(allAudioBytes.length == 1)
            return realMixAudio;

        //row 有几个音频要混音
        int row = realMixAudio.length /2;
        //
        short[][] sourecs = new short[allAudioBytes.length][row];
        for (int r = 0; r < 2; ++r) {
            for (int c = 0; c < row; ++c) {
                sourecs[r][c] = (short) ((allAudioBytes[r][c * 2] & 0xff) | (allAudioBytes[r][c * 2 + 1] & 0xff) << 8);
            }
        }

        //coloum第一个音频长度 / 2
        short[] result = new short[row];
        //转成short再计算的原因是，提供精确度，高端的混音软件据说都是这样做的，可以测试一下不转short直接计算的混音结果
        for (int i = 0; i < row; i++) {
            int a = sourecs[0][i] * firstVol;
            int b = sourecs[1][i] * secondVol;
            if (a <0 && b<0){
                int i1 = a  + b  - a  * b / (-32768);
                if (i1 > 32767){
                    result[i] = 32767;
                }else if (i1 < - 32768){
                    result[i] = -32768;
                }else {
                    result[i] = (short) i1;
                }
            }else if (a > 0 && b> 0){
                int i1 = a + b - a  * b  / 32767;
                if (i1 > 32767){
                    result[i] = 32767;
                }else if (i1 < - 32768){
                    result[i] = -32768;
                }else {
                    result[i] = (short) i1;
                }
            }else {
                int i1 = a + b ;
                if (i1 > 32767){
                    result[i] = 32767;
                }else if (i1 < - 32768){
                    result[i] = -32768;
                }else {
                    result[i] = (short) i1;
                }
            }
        }
        return toByteArray(result);
    }
    public static byte[] toByteArray(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2 +1] = (byte) ((src[i] & 0xFF00) >> 8);
            dest[i * 2] = (byte) ((src[i] & 0x00FF));
        }
        return dest;
    }

    /**
     * 写入ADTS头部数据
     * */
    public static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    interface DecodeOverListener {
        void decodeIsOver();

        void decodeFail();
    }

    public interface AudioDecodeListener{
        void decodeOver();
        void decodeFail();
    }
}
