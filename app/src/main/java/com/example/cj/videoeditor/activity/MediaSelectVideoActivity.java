package com.example.cj.videoeditor.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.adapter.VideoSelectAdapter;
import com.example.cj.videoeditor.bean.CutBean;
import com.example.cj.videoeditor.widget.TitleView;

import java.util.ArrayList;
import java.util.List;

/**
 * 选择视频
 */
public class MediaSelectVideoActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, VideoSelectAdapter.videoOnSelectChangedListener, View.OnClickListener {
    GridView gridview;
    TitleView title;

    TextView tv_title_bar_right_text; // 右边文字

    VideoSelectAdapter mMediaAdapter;

    public static final String PROJECT_VIDEO = MediaStore.MediaColumns._ID;


    int max_size = -1;//  最大size

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_select_video);
        gridview = (GridView) findViewById(R.id.gridview_media_video);
        title = (TitleView) findViewById(R.id.title_media_video);
        tv_title_bar_right_text = (TextView) findViewById(R.id.tv_title_bar_right_text);
        tv_title_bar_right_text.setOnClickListener(this);
        initView();
        initData();

    }

    private void initView() {
        tv_title_bar_right_text.setTextColor(getResources().getColor(R.color.iTextColor5));
    }

    private void initData() {
        max_size = getIntent().getIntExtra("max_size", -1);

        getLoaderManager().initLoader(0, null, this);
        title.setBtnLeftOnClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
        if (mMediaAdapter == null) {

            mMediaAdapter = new VideoSelectAdapter(getApplicationContext(), data);
            mMediaAdapter.setMediaSelectVideoActivity(this);
            mMediaAdapter.setOnSelectChangedListener(this);
            mMediaAdapter.setMaxSize(max_size);
        } else {
            mMediaAdapter.swapCursor(data);
        }


        if (gridview.getAdapter() == null) {
            gridview.setAdapter(mMediaAdapter);
        }
        mMediaAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mMediaAdapter != null)
            mMediaAdapter.swapCursor(null);
    }

    @Override
    protected void onDestroy() {
        getLoaderManager().destroyLoader(0);
        Glide.get(this).clearMemory();
        super.onDestroy();
    }

    List pathList;//临时保存
    List coverList;

    @Override
    public void changed(List pathList, List coverList) {
        if (pathList.isEmpty() && coverList.isEmpty()) {
            tv_title_bar_right_text.setClickable(false);
            tv_title_bar_right_text.setTextColor(getResources().getColor(R.color.iTextColor5));
            title.setTvRightNumVisibile(View.INVISIBLE);
        } else {
            tv_title_bar_right_text.setClickable(true);
            tv_title_bar_right_text.setTextColor(getResources().getColor(R.color.iTextColor6));
            this.pathList = pathList;
            this.coverList = coverList;
            title.setTvRightNum(pathList.size() + "");
        }

    }

    // 提交
    @Override
    public void onClick(View v) {
        if ((pathList == null || pathList.isEmpty()) && (coverList == null || coverList.isEmpty())) {
            Toast.makeText(this, "不选视频是要怎样", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, VideoConnectActivity.class);
        intent.putStringArrayListExtra("path", (ArrayList<String>) pathList);
        startActivity(intent);
        finish();
    }
}
