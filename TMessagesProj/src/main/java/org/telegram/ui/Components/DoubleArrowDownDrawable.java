package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.R;

public class DoubleArrowDownDrawable extends Drawable implements Drawable.Callback {

    private final Drawable icon;
    private int h;
    private int w;

    private ValueAnimator valueAnimator;
    private float animatedTranslationY;

    public DoubleArrowDownDrawable(Context context) {
        icon = ContextCompat.getDrawable(context, R.drawable.msg_go_down);
        assert icon != null;
        w = icon.getIntrinsicWidth();
        h = icon.getIntrinsicHeight();
        icon.setCallback(this);
    }

    public void setSize(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public void start() {
        stop();

        valueAnimator = ValueAnimator.ofFloat(-.08f, .08f);
        valueAnimator.addUpdateListener(a -> {
            animatedTranslationY = (float) a.getAnimatedValue();
            invalidateSelf();
        });
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.setRepeatMode(ValueAnimator.REVERSE);
        valueAnimator.setDuration(400L);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.start();
    }

    public void stop() {
        if (valueAnimator != null) {
            valueAnimator.removeAllUpdateListeners();
            valueAnimator.cancel();
        }
        valueAnimator = null;
        animatedTranslationY = 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return w != 0 ? w : icon.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return h != 0 ? h : icon.getIntrinsicHeight();
    }

    @Override
    public int getMinimumHeight() {
        return h != 0 ? h : icon.getIntrinsicHeight();
    }

    @Override
    public int getMinimumWidth() {
        return w != 0 ? w : icon.getIntrinsicWidth();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        icon.setBounds(left, top, right, bottom);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.translate(0, getIntrinsicHeight() * animatedTranslationY);
        canvas.translate(0, -getIntrinsicHeight() / 6f);
        icon.draw(canvas);
        canvas.translate(0, 2 * getIntrinsicHeight() / 6f);
        icon.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        icon.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        icon.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return icon.getOpacity();
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        unscheduleSelf(what);
    }
}