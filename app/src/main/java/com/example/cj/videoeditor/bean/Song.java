package com.example.cj.videoeditor.bean;

/**
 * Created by cj on 2017/6/27.
 * desc 歌曲的bean
 */

public class Song {
    public String artist;
    public String path;
    public String title;
    public String name;
    public String size;
    public int duration;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
