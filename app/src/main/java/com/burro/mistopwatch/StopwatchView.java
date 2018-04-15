package com.burro.mistopwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by burro on 2018/4/15.
 */

public class StopwatchView extends View {
    private final Paint mTrianglePaint;//三角形画笔
    private final Paint mLinePaint;//刻度线的画笔
    private final Paint mTextPaint;//文字画笔
    private final Paint mInnerCirclePaint;//内部圆形

    private int mLen; //实际尺寸大小
    private int mMilliseconds; //计时的总毫秒数
    private int outerAngle;//外圆指针的角度
    private int innerAngle;//小圆指针角度
    private float mTriangleLen;
    private boolean isPause;//是否暂停的开关
    float eachLineAngle = 360f / 240f; //两个刻度线之间的角度1.5° 共240条线 240间隔
    private Timer mTimer;   //定时器
    private String mShowContent; //显示总的时间

    public StopwatchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //三角形指针画笔
        mTrianglePaint = new Paint();
        mTrianglePaint.setColor(Color.WHITE);
        mTrianglePaint.setAntiAlias(true); //抗锯齿
        //刻度线的画笔
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(2); //设线宽
        //文字画笔
        mTextPaint = new Paint();
        mTextPaint.setTextAlign(Paint.Align.CENTER); //文字居中
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStrokeWidth(2);
        //内部圆形画笔
        mInnerCirclePaint = new Paint();
        mInnerCirclePaint.setColor(Color.WHITE);
        mInnerCirclePaint.setStyle(Paint.Style.STROKE); //无填充
        mInnerCirclePaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //重新定义尺寸,保证为正方形
        int width = measuredDimension(widthMeasureSpec);
        int height = measuredDimension(heightMeasureSpec);
        mLen = Math.min(width, height);
        //小三角形指针端点到外圆之间的距离，用于计算三角形坐标[这里取整体宽度的1/16]
        mTriangleLen = (float) mLen / 16.0f;
        //提交设置新的值
        setMeasuredDimension(mLen, mLen);
    }

    //适配不同尺寸
    private int measuredDimension(int measureSpec) {
        int defaultSize = 800; //默认大小
        int mode = MeasureSpec.getMode(measureSpec); //宽高度设定方式
        int size = MeasureSpec.getSize(measureSpec); //宽高度测量大小
        switch (mode) {
            case MeasureSpec.EXACTLY: //尺寸指定
                return size;
            case MeasureSpec.AT_MOST: //match_parent
                return size;
            case MeasureSpec.UNSPECIFIED: //wrap_content
                return defaultSize;
            default:
                return defaultSize;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calculateValue();
        drawTriangle(canvas);
        drawLine(canvas);
        drawText(canvas);
        drawSecondHand(canvas);
    }

    //计算相关值【根据当前毫秒值，计算外指针角度和内圆指针角度】
    private void calculateValue() {
        //文字
        int hours = mMilliseconds / (1000 * 60 * 60);
        int minutes = (mMilliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (mMilliseconds - hours * (1000 * 60 * 60) - minutes * (1000 * 60)) / 1000;
        int milliSec = mMilliseconds % 1000 / 100;
        if (hours == 0) {
            mShowContent = toDoubleDigit(minutes) + ":" + toDoubleDigit(seconds) + "." + milliSec;
        } else {
            mShowContent = toDoubleDigit(hours) + ":" + toDoubleDigit(minutes) + ":" + toDoubleDigit(seconds) + "." + milliSec;
        }

        //外角度
        outerAngle = 360 * (mMilliseconds % 60000) / 60000;
        //内角度
        innerAngle = 360 * (mMilliseconds % 1000) / 1000;
    }

    //根据角度绘制内部秒针
    private void drawSecondHand(Canvas canvas) {
        canvas.save();
        canvas.translate(mLen / 2, (float) mLen * 3 / 4.0f - mLen / 16);
        canvas.drawCircle(0, 0, mLen / 12, mInnerCirclePaint);
        canvas.drawCircle(0, 0, mLen / 80, mInnerCirclePaint);
        canvas.rotate(innerAngle);
        canvas.drawLine(0, mLen / 80, 0, mLen / 14, mInnerCirclePaint);
        canvas.restore();
    }

    //绘制文字
    private void drawText(Canvas canvas) {
        canvas.save();
        canvas.translate(mLen / 2, mLen / 2);
        mTextPaint.setTextSize(mLen / 10);
        canvas.drawText(mShowContent, 0, 0, mTextPaint);
        canvas.restore();
    }

    //绘制外部刻度线
    private void drawLine(Canvas canvas) {
        canvas.save();
        canvas.translate(mLen / 2, mLen / 2);
        int totalLines = (int) (360f / eachLineAngle); //240条线
        int lastLine = (int) (outerAngle / eachLineAngle);  //最亮的线条
        int firstLine = lastLine - ((int) (90 / eachLineAngle)); //最暗的一条
        boolean negativeFlag = false; //负数标志【即表示跨过了0起始坐标】
        if (firstLine < 0) {
            negativeFlag = true;
            firstLine = totalLines - Math.abs(firstLine);
        }
        int count = 0;
        for (int i = 0; i < totalLines; i++) {
            canvas.rotate(eachLineAngle);
            int color = 0;
            if (!negativeFlag) {
                //没有跨过起始点标志
                if (i >= firstLine && i <= lastLine && count < (totalLines / 4)) {
                    count++;
                    color = Color.argb(255 - ((totalLines / 4 - count) * 3), 255, 255, 255);
                } else {
                    color = Color.argb(255 - (int) (360f * 3 / (eachLineAngle * 4)), 255, 255, 255);
                }
            } else {
                //跨过起始点
                if (i >= 0 && i < lastLine) {
                    if (count == 0) {
                        count = totalLines / 4 - lastLine;
                    } else {
                        count++;
                    }
                    color = Color.argb(255 - ((totalLines / 4 - count) * 3), 255, 255, 255);
                } else if (mMilliseconds != 0 && i < totalLines && i >= firstLine) {  //mMilliseconds!=0 条件限制，目的是初始化时 都是灰色线条
                    count++;
                    color = Color.argb(255 - ((totalLines / 4 - (i - firstLine)) * 3), 255, 255, 255);
                } else {
                    color = Color.argb(255 - (int) (360f * 3 / (eachLineAngle * 4)), 255, 255, 255);
                }
            }
            mLinePaint.setColor(color);
            //mTriangleLen/5距离 目的是为了三角形到线条之间保留的距离
            canvas.drawLine(0, (float) (mLen / 2 - (mTriangleLen + mTriangleLen / 5)), 0, (float) (mLen / 2 - (2 * mTriangleLen + mTriangleLen / 5)), mLinePaint);

        }
        canvas.restore();
    }

    //根据角度绘制三角形
    private void drawTriangle(Canvas canvas) {
        canvas.save();
        //确定坐标
        canvas.translate(mLen / 2, mLen / 2);
        canvas.rotate(outerAngle);
        //画三角形
        Path p = new Path();
        //指针点
        p.moveTo(0, mLen / 2 - mTriangleLen);
        //左右侧点
        p.lineTo(0.5f * mTriangleLen, mLen / 2 - 0.134f * mTriangleLen);
        p.lineTo(-0.5f * mTriangleLen, mLen / 2 - 0.134f * mTriangleLen);
        p.close();
        canvas.drawPath(p, mTrianglePaint);
        canvas.restore();
    }

    //转成两位数 如传入1 返回01，
    private String toDoubleDigit(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return "" + value;
        }
    }

    //开始
    public void start() {
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isPause) {
                        mMilliseconds += 50;
                        //工作线程中用postInvalidate(); UI线程用invalidate()
                        postInvalidate();
                    }
                }
            }, 50, 50);
        } else {
            resume();
        }
    }

    //暂停
    public void pause() {
        isPause = true;
    }

    //继续
    private void resume() {
        isPause = false;
    }

    //重置
    public void reset() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        isPause = false;
        mMilliseconds = 0;
        invalidate();
    }

    //记录
    public int record() {
        return mMilliseconds;
    }
}
