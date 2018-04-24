package com.example.cj.videoeditor.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.cj.videoeditor.R;
import com.example.cj.videoeditor.jni.AudioJniUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import dalvik.system.DexClassLoader;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button recordBtn = (Button) findViewById(R.id.record_activity);
        Button selectBtn = (Button) findViewById(R.id.select_activity);
        Button audioBtn = (Button) findViewById(R.id.audio_activity);
        Button videoBtn = (Button) findViewById(R.id.video_connect);


        recordBtn.setOnClickListener(this);
        selectBtn.setOnClickListener(this);
        audioBtn.setOnClickListener(this);
        videoBtn.setOnClickListener(this);
//        test(new int[]{2,7,11,15},9);
        new Thread(new Runnable() {
            @Override
            public void run() {
//                test();
            }


        }).start();

        final int max = Integer.MAX_VALUE >>> 4;
        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < 200; i++) {
            int hash = random.nextInt(max);
//            testHash(hash);
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(" ").append(indexOne[i]);
            sb2.append(" ").append(indexTwo[i]);
        }
        Log.e("hero",sb.toString());
        Log.e("hero",sb2.toString());
        HashMap map = new HashMap();

        testJNI();
    }

    private void testJNI() {
        Log.e("hero","---"+   AudioJniUtils.putString("  hello JNI"));
    }

    //测试两种不同hash的效果
    private int[] indexOne = new int[20];
    private int[] indexTwo = new int[20];
    public void testHash(Object key){
        int hash1 = singleWordWangJenkinsHash(key);
        int hash2 = hashNew(key);
        int index1 = hash1 & 15;
        int index2 = hash2 & 15;
        Log.e("hero",toBinaryString(hash1)+"   ---------->>>>   "+toBinaryString(hash2)+"----::"+index1+":::"+index2);
        indexOne[index1] = indexOne[index1] + 1;
        indexTwo[index2] = indexTwo[index2] + 1;
    }
    public  int singleWordWangJenkinsHash(Object k) {
        int h = k.hashCode();

        h += (h <<  15) ^ 0xffffcd7d;//左移 15位 相当于h 乘以2~15次方 再异或0xffffcd7d ？为啥要异或这个值
        h ^= (h >>> 10);//这行代码可以翻译为 h = h ^ (h >>> 10)，即h 异或 h无符号右移10位的值
        h += (h <<   3);//h = h +  (h <<   3)，即h = h 加上 h左移3位
        h ^= (h >>>  6);//h = h ^  (h >>>  6) ，即 h 异或 h无符号右移6位的值
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }
    public  int hashNew(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
    final static char[] digits = {'0','1'};

    static String toBinaryString(int i) {
        char[] buf = new char[32];
        int pos = 32;
        int mask = 1;
        do {
            buf[--pos] = digits[i & mask];
            i >>>= 1;
        } while (pos > 0);
        return new String(buf, pos, 32);
    }

    public void  test(Object key1,Object key2){
        int h;
        int i;
        int s = (key1 == null) ? 0 : (h = key1.hashCode()) ^ (h >>> 16);
        int ss = (key2 == null) ? 0 : (i = key2.hashCode()) ^ (i >>> 16);

        Log.e("hero","模拟计算key--"+(s & 15)+"---"+(ss & 15));
    }
    public void test(Object k1,Object k2,Object k3){
        Log.e("hero","模拟计算key--"+(k1.hashCode() % 16)+"---"+(k2.hashCode() % 16)+"---"+(k3.hashCode() % 16));
    }

    private static AtomicInteger nextHashCode =
            new AtomicInteger();
    private void test() {
        int HASH_INCREMENT = 0x61c88647;

        List<Integer> ss = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int andAdd = nextHashCode.getAndAdd(HASH_INCREMENT);
            ss.add(andAdd);
        }

        for (int i = 0; i < 16; i++) {
            Integer integer = ss.get(i * 2);
            int i1 = integer & 15;
//            Log.e("hero","-----递增的值--==="+integer+"---计算后的值==="+i1);
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.record_activity:
                startActivity(new Intent(MainActivity.this , RecordedActivity.class));
                break;
            case R.id.select_activity:
                startActivity(new Intent(MainActivity.this , VideoSelectActivity.class));
                break;
            case R.id.audio_activity:
                startActivity(new Intent(MainActivity.this , AudioEditorActivity.class));
                break;
            case R.id.video_connect:
                startActivity(new Intent(MainActivity.this , MediaSelectVideoActivity.class));
                break;
        }
    }

    public void test(int[] arrs,int target){
        SparseIntArray da = new SparseIntArray();
        for (int i = 0; i < arrs.length; i++) {
            da.append(arrs[i],i);
        }
        for (int i = 0; i < arrs.length; i++) {
            int count = target - arrs[i];
            if (da.get(count) >= 0 && da.get(count) > i){
                Log.e("hero","---第一个值是："+i+"----第二个值是："+da.get(count));
            }
        }
    }
}
