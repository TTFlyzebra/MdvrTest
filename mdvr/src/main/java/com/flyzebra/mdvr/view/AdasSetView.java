/**
 * FileName: MoveRelativeLayout
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2022/7/25 8:12
 * Description:
 */
package com.flyzebra.mdvr.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibInfo;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.R;
import com.flyzebra.utils.FlyLog;

public class AdasSetView extends RelativeLayout {
    private ArcADASCalibInfo calibInfo = new ArcADASCalibInfo();
    private RelativeLayout horizonView_parent;
    private RelativeLayout horizonView;
    private RelativeLayout horizonView_child;
    private RelativeLayout carMiddleView;
    private RelativeLayout carMiddleView_child;
    private int width;
    private int height;

    private float horizon_down;
    private float horizon_y;

    private float carMiddle_down;
    private float carMiddle_x;

    public AdasSetView(Context context) {
        this(context, null);
    }

    public AdasSetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdasSetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        horizonView_parent = new RelativeLayout(context);
        RelativeLayout.LayoutParams params_parent = new RelativeLayout.LayoutParams(-1, 100);
        addView(horizonView_parent, params_parent);
        horizonView_parent.setBackgroundResource(R.drawable.horizon_background);
        horizonView = new RelativeLayout(context);
        horizonView_child = new RelativeLayout(context);
        LayoutParams params_hc = new LayoutParams(-1, 2);
        horizonView.addView(horizonView_child, params_hc);
        horizonView_child.setBackgroundColor(0xFF00FF00);
        LayoutParams params_h = new LayoutParams(-1, 100);
        addView(horizonView, params_h);
        horizonView.setOnClickListener(v -> {
        });
        horizonView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    horizon_y = event.getRawY();
                    horizon_down = calibInfo.horizon;
                    break;
                case MotionEvent.ACTION_MOVE:
                    calibInfo.horizon = (int) (horizon_down + (event.getRawY() - horizon_y) * Config.CAM_HEIGHT / height);
                    calibInfo.horizon = Math.max(0, calibInfo.horizon);
                    calibInfo.horizon = Math.min(Config.CAM_HEIGHT, calibInfo.horizon);
                    updateHorizonView();
                    if (moveLisenter != null) moveLisenter.notifyHorizon(calibInfo.horizon);
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return false;
        });

        carMiddleView = new RelativeLayout(context);
        carMiddleView_child = new RelativeLayout(context);
        LayoutParams params_mc = new LayoutParams(2, -1);
        carMiddleView.addView(carMiddleView_child, params_mc);
        carMiddleView_child.setBackgroundColor(0xFFFFFF00);
        LayoutParams params_m = new LayoutParams(100, -1);
        addView(carMiddleView, params_m);
        carMiddleView.setOnClickListener(v -> {
        });
        carMiddleView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    carMiddle_x = event.getRawX();
                    carMiddle_down = calibInfo.carMiddle;
                    break;
                case MotionEvent.ACTION_MOVE:
                    calibInfo.carMiddle = (int) (carMiddle_down + (event.getRawX() - carMiddle_x) * Config.CAM_WIDTH / width);
                    updateCarMiddleView();
                    if (moveLisenter != null) moveLisenter.notiryCarMiddle(calibInfo.carMiddle);
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return false;
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = (int) (width * 9f / 16f);
        try {
            LayoutParams params0 = (LayoutParams) horizonView_parent.getLayoutParams();
            if (params0 != null) {
                params0.height = height / 2 - height / 3 + 6;
                params0.topMargin = height / 3 - 3;
            }
            LayoutParams params1 = (LayoutParams) horizonView.getLayoutParams();
            if (params1 != null) {
                params1.height = height / 10;
                params1.topMargin = calibInfo.horizon * height / Config.CAM_HEIGHT - height / 20;
            }
            LayoutParams params2 = (LayoutParams) horizonView_child.getLayoutParams();
            if (params2 != null) {
                params2.topMargin = height / 20 - 1;
            }
            LayoutParams params3 = (LayoutParams) carMiddleView_child.getLayoutParams();
            if (params3 != null) {
                params3.leftMargin = height / 20 - 1;
            }
            LayoutParams params4 = (LayoutParams) carMiddleView.getLayoutParams();
            if (params4 != null) {
                params4.width = height / 10;
                params4.leftMargin = (calibInfo.carMiddle + Config.CAM_WIDTH / 2) * width / Config.CAM_WIDTH - height / 20;
            }
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public void upCalibInfo(ArcADASCalibInfo calibInfo) {
        this.calibInfo = calibInfo;
        updateHorizonView();
        updateCarMiddleView();
    }

    public void updateHorizonView() {
        int top = calibInfo.horizon * height / Config.CAM_HEIGHT - height / 20;
        horizonView.layout(0, top, horizonView.getWidth(), top + horizonView.getHeight());
    }

    public void updateCarMiddleView() {
        int left = (calibInfo.carMiddle + Config.CAM_WIDTH / 2) * width / Config.CAM_WIDTH - height / 20;
        carMiddleView.layout(left, 0, left + carMiddleView.getWidth(), carMiddleView.getHeight());
    }

    public interface MoveLisenter {
        void notifyHorizon(int vaule);

        void notiryCarMiddle(int value);
    }

    private MoveLisenter moveLisenter;

    public void setMoveLisenter(MoveLisenter moveLisenter) {
        this.moveLisenter = moveLisenter;
    }
}
