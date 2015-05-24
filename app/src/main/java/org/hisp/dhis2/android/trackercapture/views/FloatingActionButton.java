package org.hisp.dhis2.android.trackercapture.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;

import org.hisp.dhis2.android.trackercapture.R;

public class FloatingActionButton extends ImageButton {
    private final static OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator();
    private final static AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_MINI = 1;

    private static final int SHADOW_LAYER_INSET_INDEX = 1;

    private int mType;
    private int mColorNormal;
    private int mColorPressed;
    private int mShadowSize;
    private boolean mShadow;
    private boolean mHidden;

    public FloatingActionButton(Context context) {
        super(context);
        init(null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attributeSet) {
        setClickable(true);

        mType = TYPE_NORMAL;

        mColorNormal = getColor(R.color.navy_blue);
        mColorPressed = getColor(R.color.dark_navy_blue);

        mShadow = true;
        mShadowSize = getDimension(R.dimen.floating_action_button_shadow_size);

        if (attributeSet != null) {
            TypedArray attrs = getContext().obtainStyledAttributes(attributeSet, R.styleable.FloatingActionButton);
            if (attrs != null) {
                try {
                    mColorNormal = attrs.getColor(R.styleable.FloatingActionButton_colorNormal, mColorNormal);
                    mColorPressed = attrs.getColor(R.styleable.FloatingActionButton_colorPressed, mColorPressed);
                    mShadow = attrs.getBoolean(R.styleable.FloatingActionButton_shadow, mShadow);
                    mType = attrs.getInt(R.styleable.FloatingActionButton_type, TYPE_NORMAL);
                } finally {
                    attrs.recycle();
                }
            }
        }

        updateBackground();
    }

    private Drawable createDrawable(int color) {
        OvalShape ovalShape = new OvalShape();
        ShapeDrawable shapeDrawable = new ShapeDrawable(ovalShape);
        shapeDrawable.getPaint().setColor(color);

        if (mShadow) {
            Drawable shadowDrawable;
            if (mType == TYPE_NORMAL) {
                shadowDrawable = getResources().getDrawable(R.drawable.shadow);
            } else {
                shadowDrawable = getResources().getDrawable(R.drawable.shadow_mini);
            }

            Drawable[] layerDrawableArray = new Drawable[]{
                    shadowDrawable, shapeDrawable
            };

            LayerDrawable layerDrawable = new LayerDrawable(layerDrawableArray);
            layerDrawable.setLayerInset(
                    SHADOW_LAYER_INSET_INDEX, mShadowSize, mShadowSize, mShadowSize, mShadowSize
            );

            return layerDrawable;
        } else {
            return shapeDrawable;
        }
    }

    private void updateBackground() {
        StateListDrawable stateList = new StateListDrawable();
        // Empty array goes for every other state of button
        int[] unpressedState = new int[]{};
        int[] pressedState = new int[]{android.R.attr.state_pressed};

        stateList.addState(pressedState, createDrawable(mColorPressed));
        stateList.addState(unpressedState, createDrawable(mColorNormal));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(stateList);
        } else {
            setBackgroundDrawable(stateList);
        }
    }

    public void setColorNormalResId(int colorResId) {
        setColorNormal(getColor(colorResId));
    }

    public int getColorNormal() {
        return mColorNormal;
    }

    public void setColorNormal(int color) {
        if (color != mColorNormal) {
            mColorNormal = color;
            updateBackground();
        }
    }

    public void setColorPressedResId(int colorResId) {
        setColorPressed(getColor(colorResId));
    }

    public int getColorPressed() {
        return mColorPressed;
    }

    public void setColorPressed(int color) {
        if (color != mColorPressed) {
            mColorPressed = color;
            updateBackground();
        }
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        if (type != mType) {
            mType = type;
            updateBackground();
        }
    }

    public void setShadow(boolean shadow) {
        if (shadow != mShadow) {
            mShadow = shadow;
            updateBackground();
        }
    }

    public boolean hasShadow() {
        return mShadow;
    }

    private int getColor(int id) {
        return getResources().getColor(id);
    }

    private int getDimension(int id) {
        return getResources().getDimensionPixelSize(id);
    }

    public void hide() {
        if (!mHidden) {
            setVisibility(View.GONE);
            mHidden = true;
        }
    }

    public void show() {
        if (mHidden) {
            setVisibility(View.VISIBLE);
            mHidden = false;

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0, 1);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0, 1);
            AnimatorSet animSetXY = new AnimatorSet();
            animSetXY.playTogether(scaleX, scaleY);
            animSetXY.setInterpolator(OVERSHOOT_INTERPOLATOR);
            animSetXY.setDuration(200);
            animSetXY.start();
        }
    }
}
