package com.example.cj.videoeditor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.example.cj.videoeditor.R;

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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.video_select:
                //去选择视频
                startActivity(new Intent(AudioEditorActivity.this , VideoSelectActivity.class));
                break;
        }
    }
}
