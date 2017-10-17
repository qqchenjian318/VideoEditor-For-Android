package com.example.cj.videoeditor.adapter;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.activity.VideoSelectActivity;
import com.example.cj.videoeditor.utils.TimeFormatUtils;

import java.io.File;

/**
 * 本地视频列表
 */
public class VideoAdapter extends CursorAdapter {
    VideoSelectActivity activity;

    MediaMetadataRetriever retriever;

    public VideoAdapter(Context context, Cursor c) {
        super(context, c);
        retriever=new MediaMetadataRetriever();
    }

    public VideoAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public VideoAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public void setMediaSelectVideoActivity(VideoSelectActivity activity) {
        this.activity = activity;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        View content = View.inflate(context, R.layout.item_video_select, null);
        holder.content=content;
        holder.pic = (ImageView) content.findViewById(R.id.iv_media_video);
        holder.dur = (TextView) content.findViewById(R.id.tv_duration);
        content.setTag(holder);
        return content;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final Uri uri = getUri(cursor);
        final String path = cursor.getString(cursor
                .getColumnIndex(MediaStore.Video.Media.DATA));
        if(TextUtils.isEmpty(path)||!new File(path).exists()){
            return;
        }
        try{
            retriever.setDataSource(path);
        }catch (Exception e){
            e.printStackTrace();
            view.setOnClickListener(null);
            return;
        }

        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if(TextUtils.isEmpty(duration)||"null".equals(duration)){
            return;
        }
        int dur = Integer.parseInt(duration);
        String time = TimeFormatUtils.formatMillisec(dur);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener!=null){
                    listener.onSelect(path,uri.toString());
                }
            }
        });
        holder.dur.setText(time);
        Glide.with(context)
                .load(uri)
                .placeholder(R.mipmap.editor_img_def_video)
                .error(R.mipmap.editor_img_def_video)
                .crossFade()
                .into(holder.pic);
    }

    public Uri getUri(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
        return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
    }

    @Override
    public Object getItem(int position) {
        return super.getItem(position);
    }

    class ViewHolder {
        View content;
        ImageView pic;
        TextView dur;
    }

    public void setOnSelectChangedListener(OnVideoSelectListener listener) {
        this.listener = listener;
    }

    OnVideoSelectListener listener;

    public interface OnVideoSelectListener {
        void onSelect(String path, String cover);
    }
}
