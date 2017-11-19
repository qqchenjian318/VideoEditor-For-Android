package com.example.cj.videoeditor.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.bean.Song;
import com.example.cj.videoeditor.utils.DateUtils;

import java.util.List;


/**
 * Created by cj on 2017/6/27.
 * desc
 */

public class AudioAdapter extends BaseAdapter{
    private List<Song> mData;
    private Context mContext;
    public AudioAdapter(Context context, List<Song> data){
        mContext = context;
        mData = data;

    }
    public void setData(List<Song> data){
        mData = data;
    }
    @Override
    public int getCount() {
        if (mData != null && mData.size() > 0)
            return mData.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null){
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_media_audio , parent,false);
            holder.audioType = (ImageView) convertView.findViewById(R.id.audio_type);
            holder.audioName = (TextView) convertView.findViewById(R.id.audio_name);
            holder.audioSize= (TextView) convertView.findViewById(R.id.audio_size);
            holder.audioDuration = (TextView) convertView.findViewById(R.id.audio_duration);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        Song song = mData.get(position);
        holder.audioName.setText(song.getName());

        holder.audioSize.setText(song.getSize());
        holder.audioDuration.setText(DateUtils.covertToDate(song.getDuration()));
        if ("mp3".equals(song.getType())){
            holder.audioType.setImageResource(R.mipmap.img_mp3);
        }else if ("aac".equals(song.getType())){
            holder.audioType.setImageResource(R.mipmap.img_aac);
        }else if ("wma".equals(song.getType())){
            holder.audioType.setImageResource(R.mipmap.img_wma);
        }else {
            holder.audioType.setImageResource(R.mipmap.ic_launcher);
        }

        return convertView;
    }
    class ViewHolder{
        ImageView audioType;
        TextView audioName;
        TextView audioSize;
        TextView audioDuration;
    }
}
