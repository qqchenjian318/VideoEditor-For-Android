package com.example.cj.videoeditor.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.adapter.VideoAdapter;

import java.io.IOException;

/**
 * Created by cj on 2017/10/16.
 * desc: local video select activity
 */

public class VideoSelectActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>,VideoAdapter.OnVideoSelectListener {
    public static final int TYPE_SHOW_DIALOG = 1;
    public static final int TYPE_BACK_PATH = 2;

    ImageView ivClose;
    GridView gridview;
    public static final String PROJECT_VIDEO = MediaStore.MediaColumns._ID;
    private VideoAdapter mVideoAdapter;
    private int pageType = TYPE_SHOW_DIALOG;

    public static void openActivity(Context context){
        Intent intent = new Intent(context,VideoSelectActivity.class);
        intent.putExtra("type",TYPE_SHOW_DIALOG);
        context.startActivity(intent);
    }

    public static void openActivityForResult(Activity context,int requestCodde){
        Intent intent = new Intent(context,VideoSelectActivity.class);
        intent.putExtra("type",TYPE_BACK_PATH);
        context.startActivityForResult(intent,requestCodde);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_select);
        initView();
        initData();
    }

    private void initView() {
        ivClose= (ImageView) findViewById(R.id.iv_close);
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        gridview=(GridView)findViewById(R.id.gridview_media_video);
    }
    private void initData() {
        pageType = getIntent().getIntExtra("type", TYPE_SHOW_DIALOG);


        getLoaderManager().initLoader(0,null,this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String order = MediaStore.MediaColumns.DATE_ADDED + " DESC";
        return new CursorLoader(getApplicationContext(), videoUri, new String[]{MediaStore.Video.Media.DATA, PROJECT_VIDEO}, null, null, order);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() <= 0) {
            return;
        }
        if (mVideoAdapter == null) {
            mVideoAdapter = new VideoAdapter(getApplicationContext(), data);
            mVideoAdapter.setMediaSelectVideoActivity(this);
            mVideoAdapter.setOnSelectChangedListener(this);
        } else {
            mVideoAdapter.swapCursor(data);
        }


        if (gridview.getAdapter() == null) {
            gridview.setAdapter(mVideoAdapter);
        }
        mVideoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mVideoAdapter != null)
            mVideoAdapter.swapCursor(null);
    }

    @Override
    protected void onDestroy() {
        getLoaderManager().destroyLoader(0);
        Glide.get(this).clearMemory();
        super.onDestroy();
    }

    @Override
    public void onSelect(final String path, String cover) {
        //处理音频，视频
        int videoTrack=-1;
        int audioTrack=-1;
        MediaExtractor extractor=new MediaExtractor();
        try {
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                    videoTrack=i;
                    String videoMime = format.getString(MediaFormat.KEY_MIME);
                    if(!MediaFormat.MIMETYPE_VIDEO_AVC.equals(videoMime) ){
                        Toast.makeText(this,"视频格式不支持",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    continue;
                }
                if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    audioTrack=i;
                    String audioMime = format.getString(MediaFormat.KEY_MIME);
                    if(!MediaFormat.MIMETYPE_AUDIO_AAC.equals(audioMime)){
                        Toast.makeText(this,"视频格式不支持",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    continue;
                }
            }
            extractor.release();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,"视频格式不支持",Toast.LENGTH_SHORT).show();
            extractor.release();
            return;
        }
        if(videoTrack==-1||audioTrack==-1){
            Toast.makeText(this,"视频格式不支持",Toast.LENGTH_SHORT).show();
            return;
        }
        if (pageType == TYPE_BACK_PATH){
            Intent intent = getIntent();
            intent.putExtra("path",path);
            setResult(0,intent);
            finish();
            return;
        }
        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        mDialog.setMessage("去分离音频还是添加滤镜");
        mDialog.setPositiveButton("加滤镜", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //跳转预览界面
                if(!TextUtils.isEmpty(path)){
                    Intent intent=new Intent(VideoSelectActivity.this,PreviewActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);
                    dialog.dismiss();
                }
            }
        });
        mDialog.setNegativeButton("分离音频", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!TextUtils.isEmpty(path)){
                    Intent intent=new Intent(VideoSelectActivity.this,AudioPreviewActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);
                    dialog.dismiss();
                }
            }
        });
        mDialog.show();
    }
}
