package com.example.cj.videoeditor.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by cj on 2017/7/10.
 * desc 以video的path和position为关键key
 * 记录 当前视频剪切的选择参数
 * 便于后面剪切视频
 */

public class CutBean implements Parcelable {
    public int position;//这个选择参数 在当前视频集里的位置
    public String videoPath;
    public long startPoint;//开始剪切的时间点
    public long cutDuration;//剪切的时长
    public long videoDuration;//video的总长度
    public CutBean(){

    }

    protected CutBean(Parcel in) {
        position = in.readInt();
        videoPath = in.readString();
        startPoint = in.readLong();
        cutDuration = in.readLong();
        videoDuration = in.readLong();
    }

    public static final Creator<CutBean> CREATOR = new Creator<CutBean>() {
        @Override
        public CutBean createFromParcel(Parcel in) {
            return new CutBean(in);
        }

        @Override
        public CutBean[] newArray(int size) {
            return new CutBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(position);
        dest.writeString(videoPath);
        dest.writeLong(startPoint);
        dest.writeLong(cutDuration);
        dest.writeLong(videoDuration);
    }
}
