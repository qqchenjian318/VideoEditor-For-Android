package com.example.cj.videoeditor.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cj.videoeditor.MyApplication;
import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.activity.MediaSelectVideoActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地视频列表
 */
public class VideoSelectAdapter extends CursorAdapter {
    MediaSelectVideoActivity activity;

    List<String> coverList=new ArrayList();
    List<String> pathList=new ArrayList();

    int maxSize = -1;  // 最大size

    public VideoSelectAdapter(Context context, Cursor c) {
        super(context, c);
    }

    public VideoSelectAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public VideoSelectAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public void setMediaSelectVideoActivity(MediaSelectVideoActivity activity) {
        this.activity = activity;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }



    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        View inflate = View.inflate(context, R.layout.item_media_video, null);
        holder.pic = (ImageView) inflate.findViewById(R.id.iv_media_video);
        holder.is_true = (ImageView) inflate.findViewById(R.id.is_true);
        inflate.setTag(holder);
        return inflate;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final Uri uri = getUri(cursor);
        final String path = cursor.getString(cursor
                .getColumnIndex(MediaStore.Video.Media.DATA));

        holder.is_true.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 判断是否超过大小  （粉丝发的大小有限制）
                if (isToBigger(path)) {
                    Toast.makeText(MyApplication.getContext(),"选择的视频不能超过" + maxSize + "M~",Toast.LENGTH_SHORT).show();
                    return;
                }

                if (coverList.contains(uri.toString()) && pathList.contains(path)) {
                    coverList.remove(uri.toString());
                    pathList.remove(path);
                    holder.is_true.setImageResource(R.mipmap.icon_choice_nor);
                } else {
//                    if (TextUtils.isEmpty(tempCover) && TextUtils.isEmpty(tempPath)) {
//                        tempCover = uri.toString();
//                        tempPath = path;
//                        holder.is_true.setImageResource(R.mipmap.icon_choice_selected);
//                    } else {
//                        Toast.makeText(context,context.getString(R.string.video_select),Toast.LENGTH_SHORT).show();
//                    }
                    coverList.add(uri.toString());
                    pathList.add(path);
                    holder.is_true.setImageResource(R.mipmap.icon_choice_selected);
                }
                if (listener != null) {
                    listener.changed(pathList, coverList);
                }
            }
        });
        holder.is_true.setImageResource(pathList.contains(path) ? R.mipmap.icon_choice_selected : R.mipmap.icon_choice_nor);

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
        public ImageView pic;
        public ImageView is_true;
    }

    public void setOnSelectChangedListener(videoOnSelectChangedListener listener) {
        this.listener = listener;
    }

    videoOnSelectChangedListener listener;

    public interface videoOnSelectChangedListener {
        void changed(List pathList, List coverList);
    }


    private boolean isToBigger(String path) {
        if (maxSize == -1) { //没有限制
            return false;
        }
        try {
            File file = new File(path);
            long length = file.length();
            return length > maxSize * 1024 * 1024;
        } catch (Exception e) {
            return false;
        }


    }

}
