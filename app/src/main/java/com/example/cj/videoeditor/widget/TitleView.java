package com.example.cj.videoeditor.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.cj.videoeditor.R;


/**
 * Created by guohuan on 2016/2/20.
 * 标题栏控件
 */
@SuppressWarnings("unused")
public class TitleView extends FrameLayout {

    // 左边返回键
    private ImageButton btn_title_bar_left;
    // 右边btn键
    private ImageButton btn_title_bar_right;

    // 标题栏
    private TextView tv_title_bar_title;

    // 右边文字
    private TextView tv_title_bar_right_text;
    // 左边文字
    private TextView tv_title_bar_left;
    // 右边个数文字
    private TextView tv_title_bar_right_num;
    private View title_root;

    public TitleView(Context context) {
        super(context);
        initView(context);
    }

    public TitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        resolveAttr(context, attrs);
    }

    public TitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        resolveAttr(context, attrs);
    }

    private void initView(Context ctx) {
        View.inflate(ctx, R.layout.view_titlebar, this);
        title_root = findViewById(R.id.title_root);
        btn_title_bar_left = (ImageButton) findViewById(R.id.btn_title_bar_left);
        btn_title_bar_right = (ImageButton) findViewById(R.id.btn_title_bar_right);
        tv_title_bar_title = (TextView) findViewById(R.id.tv_title_bar_title);
        tv_title_bar_right_text = (TextView) findViewById(R.id.tv_title_bar_right_text);
        tv_title_bar_left = (TextView) findViewById(R.id.tv_title_bar_left);
        tv_title_bar_right_num = (TextView) findViewById(R.id.tv_title_bar_right_num);
    }

    // 解析属性
    private void resolveAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = attrs == null ? null : context.obtainStyledAttributes(attrs, R.styleable.TitleView);
        if (typedArray != null) {
            String title = typedArray.getString(R.styleable.TitleView_title_text);// 标题
            if (!TextUtils.isEmpty(title)) {
                tv_title_bar_title.setText(title);
            }
            String right_str = typedArray.getString(R.styleable.TitleView_right_text);   //右侧文字
            if (!TextUtils.isEmpty(right_str)) {
                tv_title_bar_right_text.setText(right_str);
                tv_title_bar_right_text.setVisibility(View.VISIBLE);
            }
            String right_num = typedArray.getString(R.styleable.TitleView_right_num_text);   //右侧文字
            if (!TextUtils.isEmpty(right_num)) {
                tv_title_bar_right_num.setText(right_num);
                tv_title_bar_right_num.setVisibility(View.VISIBLE);
            }
            String left_str = typedArray.getString(R.styleable.TitleView_left_text);   //左侧文字
            if (!TextUtils.isEmpty(left_str)) {
                tv_title_bar_left.setText(left_str);
                tv_title_bar_left.setVisibility(View.VISIBLE);
                btn_title_bar_left.setVisibility(View.INVISIBLE);
            }

            Drawable right_pic = typedArray.getDrawable(R.styleable.TitleView_right_pic);
            if (right_pic != null) {
                btn_title_bar_right.setImageDrawable(right_pic);
                btn_title_bar_right.setVisibility(View.VISIBLE);
            }
            Drawable left_pic = typedArray.getDrawable(R.styleable.TitleView_left_pic);
            if (left_pic != null) {
                btn_title_bar_left.setImageDrawable(left_pic);
                btn_title_bar_left.setVisibility(View.VISIBLE);
            }
            //取消标题栏背景设置
            /*Drawable title_bg = typedArray.getDrawable(R.styleable.TitleView_title_background);
            title_root.setBackground(title_bg);*/
        }
    }

    // 设置右边图片
    public void setBtnRight(Drawable drawable) {
        btn_title_bar_right.setImageDrawable(drawable);
        tv_title_bar_right_text.setVisibility(View.INVISIBLE);
        btn_title_bar_right.setVisibility(View.VISIBLE);
    }

    public void setBtnRight(Bitmap bitmap) {
        btn_title_bar_right.setImageBitmap(bitmap);
        tv_title_bar_right_text.setVisibility(View.INVISIBLE);
        btn_title_bar_right.setVisibility(View.VISIBLE);
    }

    public void setBtnRight(int id) {
        btn_title_bar_right.setImageResource(id);
        tv_title_bar_right_text.setVisibility(View.INVISIBLE);
        btn_title_bar_right.setVisibility(View.VISIBLE);
    }

    // 设置右边文字
    public void setTvRight(String str) {
        tv_title_bar_right_text.setText(str);
        tv_title_bar_right_text.setVisibility(View.VISIBLE);
        btn_title_bar_right.setVisibility(View.INVISIBLE);
    }

    public void setTvRight(int id) {
        tv_title_bar_right_text.setText(id);
        tv_title_bar_right_text.setVisibility(View.VISIBLE);
        btn_title_bar_right.setVisibility(View.INVISIBLE);
    }

    // 设置右边个数文字
    public void setTvRightNum(String str) {
        tv_title_bar_right_num.setText(str);
        tv_title_bar_right_num.setVisibility(View.VISIBLE);
        tv_title_bar_right_text.setVisibility(View.VISIBLE);
        btn_title_bar_right.setVisibility(View.INVISIBLE);
    }

    /**
     * 设置右边的个数文字是否显示
     *
     * @param visibility
     */
    public void setTvRightNumVisibile(int visibility) {
        tv_title_bar_right_num.setVisibility(visibility);
    }

    // 设置左边图标
    public void setBtnLeft(int id) {
        btn_title_bar_left.setImageResource(id);
    }

    // 设置左边图标
    public void setBtnLeft(Drawable drawable) {
        btn_title_bar_left.setImageDrawable(drawable);
    }

    // 设置左边图标是否显示
    public void setBtnLeftVisible(int vis) {
        btn_title_bar_left.setVisibility(vis);
    }

    // 设置标题
    public void setTitle(String title) {
        tv_title_bar_title.setText(title);
    }

    public void setTitle(int id) {
        tv_title_bar_title.setText(id);
    }

    public void setTitleColor(int colorId) {
        tv_title_bar_title.setTextColor(colorId);
    }

    /**
     * 设置左边点击事件
     *
     * @param listener
     */
    public void setBtnLeftOnClick(OnClickListener listener) {
        btn_title_bar_left.setOnClickListener(listener);
    }

    /**
     * 设置右边btn点击事件
     *
     * @param listener
     */
    public void setBtnRightOnClick(OnClickListener listener) {
        btn_title_bar_right.setOnClickListener(listener);
    }

    /**
     * 设置右边tv 点击事件
     *
     * @param listener
     */
    public void setTvRightOnClick(OnClickListener listener) {
        tv_title_bar_right_text.setOnClickListener(listener);
    }

    /**
     * 设置左侧 文字点击事件
     *
     * @param listener
     */
    public void setTvLeftOnclick(OnClickListener listener) {
        tv_title_bar_left.setOnClickListener(listener);
    }

    /**
     * 设置标题栏点击事件
     *
     * @param listener
     */
    public void setOnTitleClick(OnClickListener listener) {
        tv_title_bar_title.setBackgroundResource(R.drawable.selector_title_bar_btn);
        tv_title_bar_title.setOnClickListener(listener);
    }

    /**
     * 设置右边按钮是否可点击 默认可以
     */
    public void setBtnRightClickable(boolean b) {
        btn_title_bar_right.setClickable(b);
    }

    /**
     * 设置右边文字是否可点击 默认可以
     */
    public void setTvRightClickable(boolean b) {
        tv_title_bar_right_text.setClickable(b);
    }

    /**
     * 获取右边textView 对象
     */
    public TextView getRightTV() {
        return tv_title_bar_right_text;
    }


    /**
     * @return 获取右侧按钮
     */
    public ImageButton getRightBtn() {
        return btn_title_bar_right;
    }

    /**
     * 获取标题栏textView
     */
    public TextView getTitleTV() {
        return tv_title_bar_title;
    }


    /**
     * 设置右边textView是否可用
     *
     * @param enabled
     */
    public void setTvRightEnabled(boolean enabled) {
        tv_title_bar_right_text.setEnabled(enabled);
    }


    public View getLeftBtn() {
        return btn_title_bar_left;
    }

    /**
     * 设置右边textView是否激活
     *
     * @param activated
     */
    public void setTvRightActivated(boolean activated) {
        tv_title_bar_right_text.setActivated(activated);
    }

    public void hideRightBtn() {
        btn_title_bar_right.setVisibility(INVISIBLE);
    }

    public void hideBackBtn() {
        if (btn_title_bar_left != null) {
            btn_title_bar_left.setVisibility(GONE);
        }
    }

    public void showBackBtn() {
        if (btn_title_bar_left != null) {
            btn_title_bar_left.setVisibility(VISIBLE);
        }
    }
}
