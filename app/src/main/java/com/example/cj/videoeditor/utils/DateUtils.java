package com.example.cj.videoeditor.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by cj on 2017/6/28.
 * desc
 */

public class DateUtils {

    public static String covertToDate(long duration){
        Date date = new Date(duration);
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        return format.format(date);
    }
}
