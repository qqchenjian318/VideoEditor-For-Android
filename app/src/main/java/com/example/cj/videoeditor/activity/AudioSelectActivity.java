package com.example.cj.videoeditor.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.cj.videoeditor.Constants;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.adapter.AudioAdapter;
import com.example.cj.videoeditor.bean.Song;
import com.example.cj.videoeditor.mediacodec.AudioCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cj on 2017/11/19.
 * 本地音频选择界面
 */

public class AudioSelectActivity extends BaseActivity implements View.OnClickListener {
    public static final String TYPE_EX = "extractor_audio";//
    public static final String TYPE_MIX= "mix_audio";//
    ListView mLv;

    List<Song> data;

    private String type = TYPE_EX;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_select);
        initView();
        initData();
        initAdapter();
    }

    private void initView() {
        findViewById(R.id.btn_back).setOnClickListener(this);
        mLv = (ListView) findViewById(R.id.music_lv);
        mLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song song = data.get(position);
                final String path = song.getPath();
                Log.e("hero", "---select audio path " + path);
                if (TYPE_EX.equals(type) || TextUtils.isEmpty(type)){
                    showExDialog(path);

                }else if (TYPE_MIX.equals(type)){
                    Intent intent = new Intent();
                    intent.putExtra("select_audio",path);
                    setResult(101,intent);
                    finish();
                }

            }
        });
    }
    public void showExDialog(final String path){
        AlertDialog.Builder mDialog = new AlertDialog.Builder(AudioSelectActivity.this);
        mDialog.setMessage("音频转原始音频格式");
        mDialog.setPositiveButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //跳转预览界面
                dialog.dismiss();
            }
        });
        mDialog.setNegativeButton("音频转PCM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!TextUtils.isEmpty(path)){
                    final String path2 = Constants.getPath("audio/outputPCM/", "PCM_"+System.currentTimeMillis()+".pcm");
                    dialog.dismiss();
                    showLoading("音频解码中",false);
                    AudioCodec.getPCMFromAudio(path, path2, new AudioCodec.AudioDecodeListener() {
                        @Override
                        public void decodeOver() {
                            Toast.makeText(AudioSelectActivity.this,"解码完毕  PCM保存路径为----  "+path2,Toast.LENGTH_SHORT).show();
                            endLoading();
                        }

                        @Override
                        public void decodeFail() {
                            Toast.makeText(AudioSelectActivity.this,"解码失败   maybe same Exception ，please look at logcat  ",Toast.LENGTH_SHORT).show();
                            endLoading();
                        }
                    });
                }
            }
        });
        mDialog.show();
    }

    private void initAdapter() {
        AudioAdapter adapter = new AudioAdapter(this, data);
        mLv.setAdapter(adapter);
    }

    private void initData() {
        Intent intent = getIntent();
        type = intent.getStringExtra("type");

        data = new ArrayList<>();
        Log.e("hero", "--begin read audio data");

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATA}
                , MediaStore.Audio.Media.DURATION+">=?", new String[]{"20000"}, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor.moveToFirst()) {
            Song song;
            do {
                song = new Song();
                // 文件名
                song.setName(cursor.getString(1));
                // 歌曲名
                song.setTitle(cursor.getString(2));
                // 时长
                song.setDuration(cursor.getInt(3));
                // 歌手名
                song.setArtist(cursor.getString(4));
                // 歌曲格式
                if ("audio/mpeg".equals(cursor.getString(5).trim()) || "audio/ext-mpeg".equals(cursor.getString(5).trim())) {
                    song.setType("mp3");
                } else if ("audio/x-ms-wma".equals(cursor.getString(5).trim())) {
                    song.setType("wma");
                } else if ("audio/mp4a-latm".equals(cursor.getString(5).trim())) {
                    song.setType("aac");
                }
                // 文件大小
                if (cursor.getString(6) != null) {
                    float size = cursor.getInt(6) / 1024f / 1024f;
                    song.setSize((size + "").substring(0, 4) + "M");
                } else {
                    song.setSize("未知");
                }
                // 文件路径
                if (cursor.getString(7) != null) {
                    song.setPath(cursor.getString(7));
                }
                data.add(song);
            } while (cursor.moveToNext());

            cursor.close();
        }
        Log.e("hero", "audio data is ready");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;
        }
    }
}
