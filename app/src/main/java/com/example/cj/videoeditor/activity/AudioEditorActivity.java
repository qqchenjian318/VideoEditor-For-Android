package com.example.cj.videoeditor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.example.cj.videoeditor.Constants;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.mediacodec.AudioCodec;

import java.io.File;

/**
 * Created by cj on 2017/11/5.
 *
 */

public class AudioEditorActivity extends BaseActivity implements View.OnClickListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        findViewById(R.id.video_select).setOnClickListener(this);
        findViewById(R.id.audio_select).setOnClickListener(this);
        findViewById(R.id.pcm_to_audio).setOnClickListener(this);
        findViewById(R.id.audio_mix).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.video_select:
                //去选择视频
                VideoSelectActivity.openActivity(this);
                break;
            case R.id.audio_select:
                startActivity(new Intent(AudioEditorActivity.this , AudioSelectActivity.class));
                break;
            case R.id.pcm_to_audio:
                //pcm文件转音频
                String path =  Constants.getPath("audio/outputPCM/", "PCM_1511078423497.pcm");
                if (!new File(path).exists()){
                    Toast.makeText(this,"PCM文件不存在，请设置为本地已有PCM文件",Toast.LENGTH_SHORT).show();
                    return;
                }
                final String audioPath =  Constants.getPath("audio/outputAudio/", "audio_"+System.currentTimeMillis()+".aac");
                showLoading("音频编码中...");
                AudioCodec.PCM2Audio(path, audioPath, new AudioCodec.AudioDecodeListener() {
                    @Override
                    public void decodeOver() {
                        Toast.makeText(AudioEditorActivity.this,"数据编码成功 文件保存位置为—>>"+audioPath,Toast.LENGTH_SHORT).show();
                        endLoading();
                    }

                    @Override
                    public void decodeFail() {
                        Toast.makeText(AudioEditorActivity.this,"数据编码失败 maybe same Exception ，please look at logcat  "+audioPath,Toast.LENGTH_SHORT).show();
                        endLoading();
                    }
                });
                break;
            case R.id.audio_mix:
              startActivity(new Intent(this,AudioMixActivity.class));
                break;
        }
    }
}
