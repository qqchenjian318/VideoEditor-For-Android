package com.example.cj.videoeditor.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cj.videoeditor.Constants;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.mediacodec.AudioCodec;

import java.io.File;
import java.io.IOException;

/**
 * Created by cj on 2017/11/19.
 * 音频混音的页面
 */

public class AudioMixActivity extends BaseActivity implements View.OnClickListener {

    private TextView mPathOne;
    private TextView mPathTwo;
    private String audioPathOne;
    private String audioPathTwo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_mix);

        findViewById(R.id.select_one).setOnClickListener(this);
        findViewById(R.id.select_two).setOnClickListener(this);
        findViewById(R.id.audio_mix).setOnClickListener(this);
        mPathOne = (TextView) findViewById(R.id.path_one);
        mPathTwo = (TextView) findViewById(R.id.path_two);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.select_one:
                Intent intent = new Intent(this,AudioSelectActivity.class);
                intent.putExtra("type",AudioSelectActivity.TYPE_MIX);
                startActivityForResult(intent,1001);
                break;
            case R.id.select_two:
                Intent intent2 = new Intent(this,AudioSelectActivity.class);
                intent2.putExtra("type",AudioSelectActivity.TYPE_MIX);
                startActivityForResult(intent2,1002);
                break;
            case R.id.audio_mix:
                if (TextUtils.isEmpty(audioPathOne) || TextUtils.isEmpty(audioPathTwo)){
                    Toast.makeText(this,"请选择要混合的音频",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!new File(audioPathOne).exists() || !new File(audioPathTwo).exists() ){
                    Toast.makeText(this,"本地文件不存在，请重新选择",Toast.LENGTH_SHORT).show();
                    return;
                }
                final long l = System.currentTimeMillis();
                showLoading("音频混合中...");
                final String audioPath =  Constants.getPath("audio/outputAudio/", "mix_audio_"+System.currentTimeMillis()+".aac");
               AudioCodec.audioMix(audioPathOne, audioPathTwo, audioPath, new AudioCodec.AudioDecodeListener() {
                   @Override
                   public void decodeOver() {
                       Log.e("end","----decodeOver");
                       Toast.makeText(AudioMixActivity.this,"数据编码成功 文件保存位置为—>>"+audioPath,Toast.LENGTH_SHORT).show();
                       long end = System.currentTimeMillis();
                       Log.e("timee","---音频混合消耗的时间----"+(end - l));
                       endLoading();
                   }

                   @Override
                   public void decodeFail() {
                       Log.e("end","----decodeFail");
                       Toast.makeText(AudioMixActivity.this,"数据编码失败 please look at logcat—>>",Toast.LENGTH_SHORT).show();
                       endLoading();
                   }
               });

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001){
            if (data != null){
                String select_audio = data.getStringExtra("select_audio");
                mPathOne.setText(select_audio);
                audioPathOne = select_audio;
            }
        }else if (requestCode == 1002){
            if (data != null){
                String select_audio = data.getStringExtra("select_audio");
                mPathTwo.setText(select_audio);
                audioPathTwo = select_audio;
            }
        }
    }
}
