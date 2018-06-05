package com.example.cj.videoeditor;

/**
 * Created by cj on 2018/1/9.
 * desc
 */

public class MyClassLoader extends ClassLoader{
    public MyClassLoader(){
        super();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
}
