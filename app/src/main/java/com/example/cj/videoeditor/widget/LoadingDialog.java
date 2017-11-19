package com.example.cj.videoeditor.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.cj.videoeditor.R;


/**
 * 自定义LoadingDialog
 */
public class LoadingDialog extends Dialog {
    /**
     * 标题
     */
    private TextView titleView;
    /**
     * 提示信息
     */
    private TextView tipsView;
    /**
     * 内容id
     */
    private int tipsId = 0;
    /**
     * 内容
     */
    private String strTips = null;
    /**
     * 标题
     */
    private String strTitle = null;

    /**
     * 是否可取消
     */
    private boolean cancelable = false;

    public interface BlossomDialogListener {
        void onClick(View view);
    }


    public LoadingDialog(Context context, String tips, boolean cancelable) {
        super(context, R.style.dialog_style);
        this.cancelable = cancelable;
        this.strTips = tips;
    }


    /**
     * 自定义Dialog
     *
     * @param context
     */
    public LoadingDialog(Context context) {
        super(context, R.style.dialog_style);
        //  getWindow().setWindowAnimations(R.style.dialogWindowAnim);
    }

    /**
     * 自定义Dialog
     *
     * @param context
     * @param tipsId  提示信息Id, 0为不改变提示信息
     */
    public LoadingDialog(Context context, int tipsId) {
        super(context, R.style.dialog_style);
        this.tipsId = tipsId;
        //   getWindow().setWindowAnimations(R.style.dialogWindowAnim);
    }

    /**
     * 自定义Dialog
     *
     * @param context
     */
    public LoadingDialog(Context context, String title, String tips) {
        super(context, R.style.dialog_style);
        this.strTitle = title;
        this.strTips = tips;
        //   getWindow().setWindowAnimations(R.style.dialogWindowAnim);
    }

    /**
     * 是否包含标题
     *
     * @return
     */
    public boolean isWithTitle() {
        return null != strTitle;
    }

    private Handler timerHandler = null;
    int count = 0;
    private Runnable timer = new Runnable() {
        @Override
        public void run() {
            try {
                timerHandler.postDelayed(timer, 1000);
                String dots = null;
                switch (count % 4) {
                    case 0:
                        dots = "";
                        break;
                    case 1:
                        dots = ".";
                        break;
                    case 2:
                        dots = "..";
                        break;
                    case 3:
                        dots = "...";
                        break;
                }
                tipsView.setText(strTips + dots);
                count++;
            } catch (Exception e) {
                Log.e("倒计时timer", e.toString());
            }
        }
    };

    public void setTips(String strTips) {
        this.strTips = strTips;
        if (null != tipsView && null != strTips)
            tipsView.setText(strTips);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.t_loading);
        setCancelable(cancelable);
        findViews();
        fillContent();
    }

    private void findViews() {
        tipsView = (TextView) findViewById(R.id.tipsLoding);
        titleView = (TextView) findViewById(R.id.title);
    }

    private void fillContent() {
        if (0 != tipsId) {
            tipsView.setText(tipsId);
        } else if (null != strTips) {
            tipsView.setText(strTips);
        }
        if (tipsId == 0 && TextUtils.isEmpty(strTips)) {
            tipsView.setVisibility(View.GONE);
        } else {
            tipsView.setVisibility(View.VISIBLE);
        }

        if (null != strTitle && null != titleView) {
            titleView.setText(strTitle);
            count = 0;
            timerHandler = new Handler();
            timerHandler.post(timer);
        }
    }

    @Override
    public void dismiss() {
        if (null != timerHandler)
            timerHandler.removeCallbacks(timer);
        super.dismiss();
    }

}
