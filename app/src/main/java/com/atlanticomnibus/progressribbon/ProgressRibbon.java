/*
 * ProgressRibbon v1.0
 *
 * Copyright (c) 2019 Attila Orosz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.atlanticomnibus.progressribbon;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.constraintlayout.widget.ConstraintSet; //In case you'd uncomment the right code in the right place (will not tell you where, hehe):
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class ProgressRibbon extends FrameLayout {

    /**************************************Constants************************************************/

    //Taken frm the original ProgressBar
    private static final int MAX_LEVEL = 10000;

    /**
     * Plain ole' integers holding values described by their names
     */
    public static final int BAR_ROUND = 0,
            BAR_HORIZONTAL=1,
            DO_NOT_ANIMATE = 0,
            ANIMATE_FADE =1,
            ANIMATE_SCALE=2,
            ANIMATE_SCALE_FADE = 3,
            PARENT_HEIGHT_PERCENT=999;

    /**
     * Boolean replacements. 1=true, 0=false, obviously
     */
    public static final int FREEZE_PROGRESS=1,
            UNFREEZE_PROGRESS=0,
            TEXT_BESIDE_BAR=1,
            TEXT_UNDER_BAR=0,
            INDETERMINATE=1,
            DETERMINATE =0;

    /**
     * Internal stuff. Stop poking your nose here
     */
    private static final int PROGRESS_BAR_ID=666,
            ANIMATION_HIDE=0,
            ANIMATION_SHOW=1,
            MARGIN_TOP=0,
            MARGIN_BOTTOM=1,
            PADDING_TOP=0,
            PADDING_BOTTOM=1;

    /*******************************Enforcing enforcements*****************************************/


    /** @hide */
    @IntDef({BAR_ROUND, BAR_HORIZONTAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProgressStyle{}

    /** @hide */
    @IntDef({FREEZE_PROGRESS, UNFREEZE_PROGRESS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProgressFrozen{}

    /** @hide */
    @IntDef({INDETERMINATE, DETERMINATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IndetermiateState{}

    /** @hide */
    @IntDef({TEXT_BESIDE_BAR, TEXT_UNDER_BAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextPosition{}

    /** @hide */
    @IntDef({ANIMATION_SHOW, ANIMATION_HIDE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimationDirection{}

    /** @hide */
    @IntDef({ANIMATE_FADE, ANIMATE_SCALE, ANIMATE_SCALE_FADE, DO_NOT_ANIMATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimationType{}

    /** @hide */
    @IntDef({MARGIN_BOTTOM, MARGIN_TOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RibbonMarginPosition{}

    /** @hide */
    @IntDef({PADDING_BOTTOM, PADDING_TOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RibbonPaddingPosition{}

    /** @hide */
    @IntDef({TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ValidSizeUnit{}

    /** @hide */
    @IntDef({TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM, PARENT_HEIGHT_PERCENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RibbonMarginUnit{}

    /** @hide */
    @IntDef({android.R.attr.colorPrimary, android.R.attr.colorAccent, android.R.attr.textColorPrimary})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ValidColorAttrIds{}


    /*************************************Viewable views*******************************************/

    private LinearLayout ribbonContainer;
    private TextView progressTextView;
    private ProgressBar progressBar;
    private ViewGroup viewParent;


    /**********************************Various Variables*******************************************/

    private Activity activity;
    private String progressText;
    private RibbonData ribbonData;
    private boolean isDynamicallyCreated;

    private boolean isProgressFrozen;
    private int parentHeight; //Just to be able to check whether it had changed. This is used when top/bottom margin are set as a %
    private boolean xmlMarginTopPercentIsSet;
    private boolean xmlMarginBottomPercentIsSet;
    private boolean isShowing;
    private boolean animationInProgress; //Failsafe to prevent changing animation settings when it's being shown
    private boolean layoutIsRTL;
    private boolean doNotShowOnAttachFromXML; //if, for any reason, you1d not want to XML-declared Ribbon to appear until you explicitly show it...


    /*******************************Listeners that listen******************************************/

    private RibbonStateChangeListener ribbonStateChangeListener;
    private OnIndeterminateStatusChangeListener onIndeterminateStatusChangeListener;
    private OnStartListener onStartListener;
    private OnStopListener onStopListener;
    private OnRibbonAttachDetachListener onRibbonAttachDetachListener;
    private OnRibbonShowListener onRibbonShowListener;
    private OnRibbonHideListener onRibbonHideListener;
    private OnRibbonProgressUpdateListener onRibbonProgressUpdateListener;


    /**
     * "Chainable" static solution for one-liners. Depending omn whether a parentView is supplied it will create an orphan or an attached Ribbon
     * @param activity The activity for context
     * @param parentView Optional parent ViewGroup
     * @return
     */
    public static ProgressRibbon newInstance(Activity activity, @Nullable ViewGroup parentView){
        ProgressRibbon pr;

        if(parentView==null){
            pr=new ProgressRibbon(activity);
        } else {
            pr=new ProgressRibbon(activity, parentView);
        }

        return pr;
    }


    /*******************Dynamic constructors. Used when adding Ribbon from code********************/

    /**
     * This one creates ribbon attached to the Window directly, with no parent (orphan mode)
     * It's called an orphan, for it has no parent :,(
     */
    public ProgressRibbon(Activity activity) {
        super(activity);

        this.activity = activity;
        progressText = "";
        isDynamicallyCreated = true;
        ribbonData= new RibbonData(activity, true);
        initRibbon(activity);
    }

    /**
     * A dynamic constructor that allows for creating a Ribbon wih a few pre-determined non-default values
     *
     * @param activity - Need an activity in case it's an orphan view
     * @param parentView - Optional. If null, it will be in orphan mode, otherwise attached to the ViewGroup supplied
     * @param blocksUnderlyingView - Whether the ribbon allows touches to views underneath it in the same ViewGroup, or the whole window (in orphan mode)
     * @param animationType - Whether to animate appearing/disappearing and how
     * @param progressBarStyle - Flat or round progressBar
     * @param indeterminateState - Indeterminate or Determinate progressbar (round determinate also available)
     */
    public ProgressRibbon(Activity activity, @Nullable ViewGroup parentView, boolean blocksUnderlyingView, @AnimationType int animationType, @ProgressStyle int progressBarStyle, @IndetermiateState int indeterminateState) {
        super(activity);

        this.activity = activity;

        ribbonData= new RibbonData(activity, true);

        if(parentView!=null){
            viewParent=parentView;
            ribbonData.isInOrphanMode =false;
        } else {
            ribbonData.isInOrphanMode =true;
        }

        ribbonData.progressBarStyle = progressBarStyle;
        ribbonData.animationType= animationType;
        ribbonData.blocksUnderlying =blocksUnderlyingView;
        ribbonData.isIndeterminate = (indeterminateState==INDETERMINATE);

        progressText = "";
        isDynamicallyCreated = true;

        initRibbon(activity);
    }


    /**
     * This one creates ribbon attached to a parent ViewGroup
     */
    public ProgressRibbon(Context context, ViewGroup parentView) {
        super(context);

        viewParent = parentView;
        progressText = "";

        ribbonData= new RibbonData(context, true);
        isDynamicallyCreated = true;

        initRibbon(context);
    }


    /*************************Standard constructors for XML inflation******************************/

    public ProgressRibbon(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ribbonData= new RibbonData(context, false);
        isDynamicallyCreated =false;
        getAttributes(context, attrs);
        initRibbon(context);
    }

    public ProgressRibbon(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ribbonData= new RibbonData(context, false);
        isDynamicallyCreated =false;
        getAttributes(context, attrs);
        initRibbon(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProgressRibbon(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        ribbonData= new RibbonData(context, false);
        isDynamicallyCreated =false;
        getAttributes(context, attrs);
        initRibbon(context);
    }


    /*******************************Other necessary stuff******************************************/

    /**
     * Way to get the attributes from an XML defined Ribbon
     * @param context The Context
     * @param attrs The attributes to get
     */
    private void getAttributes(Context context, AttributeSet attrs){

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ProgressRibbon,
                0, 0);

        try {
            progressText=a.getString(R.styleable.ProgressRibbon_progressText);
            ribbonData.showDelay=a.getInteger(R.styleable.ProgressRibbon_showDelay, ribbonData.DEFAULT_RIBBON_SHOW_DELAY);
            ribbonData.hideDelay=a.getInteger(R.styleable.ProgressRibbon_hideDelay, ribbonData.DEFAULT_RIBBON_HIDE_DELAY);
            ribbonData.ribbonBorderColor =a.getColor(R.styleable.ProgressRibbon_borderColor, ribbonData.DEFAULT_RIBBON_BORDER_COLOR);
            ribbonData.isIndeterminate=a.getBoolean(R.styleable.ProgressRibbon_isIndeterminate, true);
            ribbonData.min=a.getInteger(R.styleable.ProgressRibbon_min, ribbonData.DEFAULT_RIBBON_MIN);
            ribbonData.max=a.getInteger(R.styleable.ProgressRibbon_max, ribbonData.DEFAULT_RIBBON_MAX);
            ribbonData.progress=a.getInteger(R.styleable.ProgressRibbon_progress, 0);
            ribbonData.backgroundColor=a.getColor(R.styleable.ProgressRibbon_ribbonBackgroundColor,ribbonData.DEFAULT_RIBBON_BG_COLOR);
            ribbonData.blocksUnderlying =a.getBoolean(R.styleable.ProgressRibbon_blockUnderlyingViews, false);
            ribbonData.progressBarStyle =a.getInteger(R.styleable.ProgressRibbon_progressBarType, BAR_ROUND);
            ribbonData.animationDuration=a.getInteger(R.styleable.ProgressRibbon_animationDuration, ribbonData.DEFAULT_ANIMATION_DURATION);
            ribbonData.animationType=a.getInteger(R.styleable.ProgressRibbon_animationType, DO_NOT_ANIMATE);
            ribbonData.textBesideBar =a.getBoolean(R.styleable.ProgressRibbon_textBesideProgressBar, false);
            ribbonData.isInDialogueMode=a.getBoolean(R.styleable.ProgressRibbon_dialogueMode, false);
            ribbonData.ribbonBorderSize=a.getDimensionPixelSize(R.styleable.ProgressRibbon_borderThickness,ribbonData.DEFAULT_RIBBON_BORDER_SIZE);
            ribbonData.progressTextSize=a.getDimensionPixelSize(R.styleable.ProgressRibbon_progressTextSize, ribbonData.DEFAULT_RIBBON_TEXT_SIZE);
            ribbonData.progressTextColor=a.getColor(R.styleable.ProgressRibbon_progressTextColor, ribbonData.DEFAULT_RIBBON_TEXT_COLOR);
            ribbonData.ribbonPaddingTop =a.getDimensionPixelSize(R.styleable.ProgressRibbon_ribbonPadding, ribbonData.DEFAULT_RIBBON_PADDING);
            ribbonData.ribbonPaddingBottom =a.getDimensionPixelSize(R.styleable.ProgressRibbon_ribbonPadding, ribbonData.DEFAULT_RIBBON_PADDING);
            ribbonData.reportProgressAsMaxPercent=a.getBoolean(R.styleable.ProgressRibbon_reportProgressAsMaxPercent, false);

            float defaultElevationPixelSize= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ribbonData.DEFAULT_RIBBON_BORDER_SIZE, getResources().getDisplayMetrics());
            int elevationPixelSize= a.getDimensionPixelSize(R.styleable.ProgressRibbon_ribbonElevation, Math.round(defaultElevationPixelSize));
            ribbonData.ribbonElevation= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, elevationPixelSize, getResources().getDisplayMetrics());


            /*Need the extra step here, as this is not defined as a dimension attribute, but needs to be translated, UNLESS it's explicitly zero*/
            float rawRadius=a.getFloat(R.styleable.ProgressRibbon_borderRadius, 0.0f);
            if(rawRadius==0.0f){
                ribbonData.ribbonBorderRadius=ribbonData.DEFAULT_RIBBON_BORDER_RADIUS;
            } else {
                ribbonData.ribbonBorderRadius=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rawRadius, getResources().getDisplayMetrics());
            }

            if(a.hasValue(R.styleable.ProgressRibbon_ribbonMarginBottom)) {
                ribbonData.ribbonMarginBottom = a.getDimensionPixelSize(R.styleable.ProgressRibbon_ribbonMarginBottom, ribbonData.DEFAULT_RIBBON_MARGIN);
            } else if (a.hasValue(R.styleable.ProgressRibbon_ribbonMarginPercentBottom)){
                ribbonData.ribbonMarginBottom = a.getInteger(R.styleable.ProgressRibbon_ribbonMarginPercentBottom, ribbonData.DEFAULT_RIBBON_MARGIN);
                xmlMarginBottomPercentIsSet=true;
            }
            if(a.hasValue(R.styleable.ProgressRibbon_ribbonMarginTop)) {
                ribbonData.ribbonMarginTop = a.getDimensionPixelSize(R.styleable.ProgressRibbon_ribbonMarginTop, ribbonData.DEFAULT_RIBBON_MARGIN);
            } else if (a.hasValue(R.styleable.ProgressRibbon_ribbonMarginPercentTop)){
                ribbonData.ribbonMarginTop = a.getInteger(R.styleable.ProgressRibbon_ribbonMarginPercentTop, ribbonData.DEFAULT_RIBBON_MARGIN);
                xmlMarginTopPercentIsSet=true;
            }

            doNotShowOnAttachFromXML=a.getBoolean(R.styleable.ProgressRibbon_doNotShowOnAttach, false);


        } finally {
            a.recycle();
        }
    }

    /**
     * This is overridden so that measurewd height of the parent can be used ot determine correct percentage of margins if
     * margin is set as a percentage
     * @param widthMeasureSpec See super
     * @param heightMeasureSpec See super
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(!isDynamicallyCreated && viewParent==null){
            //Need to get viewparent. Might as well do it here
            viewParent=(ViewGroup)getParent();
            //Overriding XML settings. It muct be MATCH_PARENT for both length and height for it to work.
            setLayoutParamsInternal();
        }

        if(!ribbonData.isInOrphanMode && viewParent.getMeasuredHeight()!=parentHeight){
            parentHeight=viewParent.getMeasuredHeight();

            if(ribbonData.ribbonMarginTop>0 && xmlMarginTopPercentIsSet){
                setRibbonMarginTop(PARENT_HEIGHT_PERCENT, ribbonData.ribbonMarginTop);
            }

            if(ribbonData.ribbonMarginBottom>0 && xmlMarginBottomPercentIsSet){
                setRibbonMarginBottom(PARENT_HEIGHT_PERCENT, ribbonData.ribbonMarginBottom);
            }
        }
    }

    /**
     * Inflates Ribbon views from their default layout file `progress_ribbon.xml`
     *
     * @param context Of the many meanings of the word "context", I'll let you choose the one that suits this here
     */
    private void initRibbon(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.progress_ribbon, this,true);

        if(isDynamicallyCreated){
            /*This should be called automatically for XML inflated stuff, but need to do it by hand if added from code*/
            onFinishInflate();
        }
    }

    /**
     * Called automatically when layout is added form XLM. has to be explicitly called if
     * constructor was called from Java code
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ribbonContainer=findViewById(R.id.ribbon_container);
        progressTextView=findViewById(R.id.progress_ribbon_text);

        setRibbonStateChangedListenerInternal();
        setRibbonAttributes(false);

    }

    /**
     * As a view's style cannot be dynamically changed, the ProgressBar element needs to be recreated
     * when changing between flat and round styes. that being the case, it's beter do dynamically create them anyway.
     *
     * @param style Style of the ProgressBar BAR_ROUND, or BAR_HORIZONTAL are accepted values
     * @param indeterminate boolean switch, marking whether the PB should be indeterminate
     */
    private void initProgressbar(@ProgressStyle int style, boolean indeterminate){

        if(progressBar!=null){
            if(ribbonContainer.findViewById(PROGRESS_BAR_ID)!=null){
                ribbonContainer.removeView(progressBar);
                progressBar=null;
            }
        }

        LinearLayout.LayoutParams params;
        int circularSize=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        int circularMargin=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());

        if(style==BAR_HORIZONTAL){
            progressBar= new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            params= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity=Gravity.CENTER;
        } else {

            if(indeterminate){
                progressBar= new ProgressBar(getContext(), null, android.R.attr.progressBarStyle);
            } else {
                progressBar= new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
                progressBar.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.circular_determinate_progressbar, getContext().getTheme()));
            }

            params=new LinearLayout.LayoutParams(circularSize, circularSize);
            params.gravity=Gravity.CENTER;
            params.rightMargin=circularMargin;
            params.leftMargin=circularMargin;
        }

        progressBar.setMax(ribbonData.max);
        progressBar.setLayoutParams(params);
        progressBar.setIndeterminate(indeterminate);
        progressBar.setId(PROGRESS_BAR_ID);

        ribbonContainer.addView(progressBar, 0);
    }

    /**
     * One interface to rule them all. This is an internal listener by default, which handles all
     * the smaller ones, if they are attached. If overridden, the smaller ones never get called,
     * but all the functions will get exposed in one central location to the caller.
     */
    private void setRibbonStateChangedListenerInternal(){
        if(ribbonStateChangeListener==null){
            ribbonStateChangeListener=new RibbonStateChangeListener() {
                @Override
                public void onRibbonSignalledToShow(int showDelay) {
                    if(onRibbonShowListener !=null) {
                        onRibbonShowListener.onRibbonSignalledToShow(showDelay);
                    }
                }

                @Override
                public void onRibbonShow() {
                    if(onRibbonShowListener !=null) {
                        onRibbonShowListener.onRibbonShow();
                    }
                }

                @Override
                public void onRibbonIndeterminateStatusChanged(boolean ribbonIsIndeternimate) {
                    if(onIndeterminateStatusChangeListener !=null){
                        onIndeterminateStatusChangeListener.onRibbonIndeterminateStatusChanged(ribbonIsIndeternimate);
                    }
                }

                @Override
                public void onRibbonProgressStarted(int startValue) {
                    if(onStartListener!=null){
                        onStartListener.onRibbonProgressStarted(startValue);
                    }
                }

                @Override
                public void onRibbonProgressStopped(int stopValue) {
                    if(onStopListener!=null){
                        onStopListener.onRibbonProgressStopped(stopValue);
                    }
                }

                @Override
                public void onRibbonProgressChange(int currentValue) {
                    if(onRibbonProgressUpdateListener !=null){
                        onRibbonProgressUpdateListener.onRibbonProgressChange(currentValue);
                    }
                }

                @Override
                public void onRibbonSignalledToHide(int hideDelay) {
                    if(onRibbonHideListener !=null){
                        onRibbonHideListener.onRibbonSignalledToHide(hideDelay);
                    }
                }

                @Override
                public void onRibbonHide() {
                    if(onRibbonHideListener !=null){
                        onRibbonHideListener.onRibbonHide();
                    }
                }

                @Override
                public void onRibbonAttached(boolean hasViewParent) {
                    if(onRibbonAttachDetachListener !=null){
                        onRibbonAttachDetachListener.onRibbonAttached(hasViewParent);
                    }
                }

                @Override
                public void onRibbonRemoved() {
                    if(onRibbonAttachDetachListener !=null){
                        onRibbonAttachDetachListener.onRibbonRemoved();
                    }
                }
            };
        }
    }

    /**
     * Set up all sorts of attributes of the Ribbon. this can get called independently from onFinishedInflate(liek ni restoring state)
     * If called form onRestoreInstanceState, the ribbon has ot be shown again
     *
     * @param fromRestoreState Whether it's called from onRestoreInstanceState
     */
    private void setRibbonAttributes(boolean fromRestoreState){

        if(isDynamicallyCreated){
            setLayoutParamsInternal();
        }

        initProgressbar(ribbonData.progressBarStyle, ribbonData.isIndeterminate);

        setBackgroundColor(getResources().getColor(android.R.color.transparent));
        setProgressTextColour(ribbonData.progressTextColor);
        setProgressTextSize(TypedValue.COMPLEX_UNIT_PX, ribbonData.progressTextSize);

        setMin(ribbonData.min);
        setMax(ribbonData.max);
        setElevation(ribbonData.ribbonElevation);

        setRibbonTextPosition(ribbonData.textBesideBar ? TEXT_BESIDE_BAR:TEXT_UNDER_BAR);

        /**
         * This will also set the border colour and thickness, and border radius if in dialogue mode
         */
        setRibbonInDialogueMode(ribbonData.isInDialogueMode);


        /**
         * Margins and paddings
         */

        if(ribbonData.ribbonPaddingTop>0) {
            setRibbonPadding(PADDING_TOP, TypedValue.COMPLEX_UNIT_PX, ribbonData.ribbonPaddingTop);
        }

        if(ribbonData.ribbonPaddingBottom>0) {
            setRibbonPadding(PADDING_BOTTOM, TypedValue.COMPLEX_UNIT_PX, ribbonData.ribbonPaddingBottom);
        }

        if(ribbonData.ribbonMarginTop>0){
            if(!xmlMarginTopPercentIsSet) {
                setRibbonMarginTop(TypedValue.COMPLEX_UNIT_PX, ribbonData.ribbonMarginTop);
            }
        }

        if(ribbonData.ribbonMarginBottom>0){
            if(!xmlMarginBottomPercentIsSet){
                setRibbonMarginBottom(TypedValue.COMPLEX_UNIT_PX, ribbonData.ribbonMarginBottom);
            }
        }

        /**
         * Only works after initialising progressbar, because it also sets layoutDirection on thjat
         */
        if(getLayoutDirection()!=LAYOUT_DIRECTION_LTR) {
            setLayoutDirection(getLayoutDirection());
        } else {
            setLayoutDirection(getConfigLayoutDirection());
        }

        progressBar.setProgress(ribbonData.progress);
        progressTextView.setText(progressText);
        setProgressTextColour(ribbonData.progressTextColor);


        /**
         * The Ribbon itself always blocks touches
         */
        ribbonContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        if(fromRestoreState && isShowing){
            showNoDelay();
        } else if(!isDynamicallyCreated && !doNotShowOnAttachFromXML) {
            /**
             * This is so that it can be manually shown after setup is complete from code
             */
            show();
        } else if (!isDynamicallyCreated && doNotShowOnAttachFromXML){
            hideNoDelay();
        }
    }

    /**
     * An attempt to resolve application's primary colour ina failsafe way. if nothign helps, use the declared default
     *
     * Credit for this solution goes to <https://mbcdev.com/2017/01/16/resolving-android-theme-colours-programmatically/>
     * @return
     */
    private int resolveAppThemeColour(@ValidColorAttrIds int attrId){

        TypedValue outValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        boolean wasResolved =
                theme.resolveAttribute(
                        attrId, outValue, true);
        if (wasResolved) {
            return outValue.resourceId == 0
                    ? outValue.data
                    : ContextCompat.getColor(
                    getContext(), outValue.resourceId);
        } else {
            // fallback colour handling

            switch(attrId){
                case android.R.attr.colorPrimary: {
                    return getResources().getColor(R.color.colorPrimary);
                }
                case android.R.attr.colorAccent: {
                    return getResources().getColor(R.color.colorAccent);
                }
                case android.R.attr.textColorPrimary: {
                    return getResources().getColor(R.color.textColorPrimary);
                }
            }

            return -1;

        }
    }

    /**
     * An attempt to determine the type of LayoutParams needed
     * Probably needs some work/attention, there might be a more efficient solution to do this
     */
    private void setLayoutParamsInternal(){
        ribbonData.isInOrphanMode =false;

        if(viewParent instanceof RelativeLayout) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            /*Uncomment the below line if you change the height width above and want to keep things centered*/
            //params.addRule(RelativeLayout.CENTER_IN_PARENT);
            setLayoutParams(params);
        } else if (viewParent instanceof CoordinatorLayout){
            CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT);
            /*Uncomment the below line if you change the height width above and want to keep things centered*/
            //  params.gravity=Gravity.CENTER;
            setLayoutParams(params);
        } else if (viewParent instanceof LinearLayout){
            LinearLayout.LayoutParams params= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            /*Uncomment the below line if you change the height width above and want to keep things centered*/
            //params.gravity= Gravity.CENTER;
            setLayoutParams(params);
        } else if (viewParent instanceof ConstraintLayout){
            ConstraintLayout.LayoutParams params= new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
            setLayoutParams(params);
            /*Uncomment the below block if you change the height width above and want to keep things centered*/
                /*
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone((ConstraintLayout) viewParent);
                constraintSet.connect(this.getId(), ConstraintSet.LEFT, viewParent.getId(), ConstraintSet.LEFT, 0);
                constraintSet.connect(this.getId(), ConstraintSet.RIGHT, viewParent.getId(), ConstraintSet.RIGHT, 0);
                constraintSet.connect(this.getId(), ConstraintSet.TOP, viewParent.getId(), ConstraintSet.TOP, 0);
                constraintSet.connect(this.getId(), ConstraintSet.BOTTOM, viewParent.getId(), ConstraintSet.BOTTOM, 0);
                constraintSet.applyTo((ConstraintLayout) viewParent);
                */
        } else if (viewParent instanceof ViewGroup){
            /*Note: You will probably need to do some magic to be able to center the Ribbon in the parent view, if you change the height/width here*/
            ViewGroup.LayoutParams params= new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            setLayoutParams(params);
        } else {
            /**
             * There was either no parent, or the parent's type was not recognised as valid, or somethng really, really bad happened
             * In either case, we attach to the Window instead. The default settin in this case is to block everything. This can be changed
             * manually in setViewBlocking()
             */
            WindowManager.LayoutParams params;

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);

            setLayoutParams(params);
            ribbonData.blocksUnderlying =true;
            ribbonData.isInOrphanMode =true;
        }
    }


    /**
     * The transparent overly only blocks touches when it's set to do so
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return ribbonData.blocksUnderlying;
    }


    /***************************************Getters and setters************************************/


    /**
     * Get the RibbonData object, containing all the... wait for it... RIBBON DATA! Yay!
     * Individual bits of the RibbonData, including default constants are not accessible otherwise
     * @return the RibbonData object
     */
    public RibbonData getRibbonData(){
        return ribbonData;
    }


    /**
     * Exposes internal progressbar widget, to set or whatever is not exposed explicitly in the main widget
     *
     * @return The dynamically created ProgressBar widhet that is part of the compound layout
     */
    public ProgressBar getProgressbar(){
        return progressBar;
    }

    /**
     * Exposes the internal TextView holding the progress text to allow fine settings manipulation
     *
     * @return The TextView widget that is part of the compound layout
     */
    public TextView getProgressTextView(){
        return progressTextView;
    }

    /**
     * Set the Ribbon's top padding (the distance between it's top border and the topmost element(s)) in DIP
     *
     * @param paddingTop Padding value as DIP, must be a non-negative integer
     */
    public ProgressRibbon setRibbonPaddingTop(@IntRange(from=0) int paddingTop){
        return setRibbonPadding(PADDING_TOP, TypedValue.COMPLEX_UNIT_DIP, paddingTop);
    }

    /**
     * Set the Ribbon's top padding (the distance between it's top border and the topmost element(s)) in the unit of your choice (use TypedValue complex units)
     *
     * @param unit Chose from TypedValue units: TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM,
     * @param paddingTop The padding value. Must be a non-negative integer
     */
    public ProgressRibbon setRibbonPaddingTop(@ValidSizeUnit int unit, @IntRange(from=0) int paddingTop){
        return setRibbonPadding(PADDING_TOP, unit, paddingTop);
    }

    /**
     * Set the Ribbon's bottom padding (the distance between it's top border and the bottom-most element(s)) in DIP
     *
     * @param paddingBottom Padding value as DIP, must be a non-negative integer
     */
    public ProgressRibbon setRibbonPaddingBottom(@IntRange(from=0) int paddingBottom){
        return setRibbonPadding(PADDING_BOTTOM, TypedValue.COMPLEX_UNIT_DIP, paddingBottom);
    }

    /**
     * Set the Ribbon's top padding (the distance between it's bottom border and the bottom-most element(s)) in the unit of your choice (use TypedValue complex units)
     *
     * @param unit Chose from TypedValue units: TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM,
     * @param paddingBottom The padding value. Must be a non-negative integer
     */
    public ProgressRibbon setRibbonPaddingBottom(@ValidSizeUnit int unit, @IntRange(from=0) int paddingBottom){
        return setRibbonPadding(PADDING_BOTTOM, unit, paddingBottom);
    }

    /**
     * Internal method to actually set the Ribbon's paddings
     *
     * @param position Accepted values are PADDING_BOTTOM or PADDING_TOP
     * @param unit Accepted values are TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM
     * @param padding Non-negative integer value of the padding to be set
     */
    private ProgressRibbon setRibbonPadding(@RibbonPaddingPosition int position, @ValidSizeUnit int unit, @IntRange(from=0) int padding){

        padding=(int) TypedValue.applyDimension(unit, padding, getResources().getDisplayMetrics());

        if(position==PADDING_BOTTOM){
            ribbonData.ribbonPaddingBottom=padding;
        } else {
            ribbonData.ribbonPaddingTop=padding;
        }

        ribbonContainer.setPadding(ribbonContainer.getPaddingLeft(), ribbonData.ribbonPaddingBottom, ribbonContainer.getPaddingRight(), ribbonData.ribbonPaddingBottom);
        return this;
    }

    /**
     * Get the ribbon's internal paddings, both bottom and top, as an integer array
     * @return array of padding with [0]: ribbonPaddingTop and [1]:ribbonPaddingBottom in DP
     */
    public int[] getRibbonPadding(){
        int[] result= {Math.round(ribbonData.ribbonPaddingTop / getResources().getDisplayMetrics().density),  Math.round(ribbonData.ribbonPaddingBottom/ getResources().getDisplayMetrics().density)};
        return result;
    }

    /**
     * Get the ribbon's internal top padding
     * @return the top padding in DP
     */
    public int getRibbonTopPadding(){
        return Math.round(ribbonData.ribbonPaddingTop / getResources().getDisplayMetrics().density);
    }

    /**
     * Get the ribbon's internal top padding
     * @return the bottom padding in DP
     */
    public int getRibbonBottomPadding(){
        return Math.round(ribbonData.ribbonPaddingBottom / getResources().getDisplayMetrics().density);

    }

    /**
     * Set a top margin on the Ribbon in DIP, which will push the Ribbon downwards
     *
     * @param marginTop The top margin value in DIP
     */
    public ProgressRibbon setRibbonMarginTop(@IntRange(from=0) int marginTop){
        return setRibbonVerticalMargin(MARGIN_TOP, TypedValue.COMPLEX_UNIT_DIP, marginTop);
    }

    /**
     * Set a top margin on the Ribbon in any valid unit, which will push the Ribbon downwards.
     * As a convenience, the margin can be set as a percentage of the containing layout's height,
     * which will be automatically calculated and applied. use TypedValue complex units or PARENT_HEIGHT_PERCENT
     *
     * @param unit Accepted values are PARENT_HEIGHT_PERCENT and TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM
     * @param marginTop the value of the top margin as a non-negative integer
     */
    public ProgressRibbon setRibbonMarginTop(@RibbonMarginUnit int unit, @IntRange(from=0) int marginTop){
        return setRibbonVerticalMargin(MARGIN_TOP, unit, marginTop);
    }

    /**
     * Set a bottom margin on the Ribbon in DIP, which will push the Ribbon upwards
     *
     * @param marginBottom The bottom margin value in DIP
     */
    public ProgressRibbon setRibbonMarginBottom(@IntRange(from=0) int marginBottom){
        return setRibbonVerticalMargin(MARGIN_BOTTOM, TypedValue.COMPLEX_UNIT_DIP, marginBottom);
    }

    /**
     * Set a bottom margin on the Ribbon in any valid unit, which will push the Ribbon upwards.
     * As a convenience, the margin can be set as a percentage of the containing layout's height,
     * which will be automatically calculated and applied. use TypedValue complex units or PARENT_HEIGHT_PERCENT
     *
     * @param unit Accepted values are PARENT_HEIGHT_PERCENT and TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM
     * @param marginBottom the value of the bottom margin as a non-negative integer
     */
    public ProgressRibbon setRibbonMarginBottom(@RibbonMarginUnit int unit, @IntRange(from=0) int marginBottom){
        return setRibbonVerticalMargin(MARGIN_BOTTOM, unit, marginBottom);
    }

    /**
     * Internal method to set the Ribbon's vertical margins for real.
     *
     * @param position Acecpted values are MARGIN_BOTTOM, MARGIN_TOP
     * @param unit Accepted values are PARENT_HEIGHT_PERCENT and TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM
     * @param margin Non-negative integer value of the margin to be set
     */
    private ProgressRibbon setRibbonVerticalMargin(@RibbonMarginPosition int position, @RibbonMarginUnit int unit, @IntRange(from=0) int margin){

        DisplayMetrics metrics = new DisplayMetrics();
        int marginTop = 0;
        int marginBottom = 0;

        if(unit==PARENT_HEIGHT_PERCENT){
            ribbonData.marginIsPercentage=true;
            int parentHeight;

            if (viewParent != null) {
                parentHeight=viewParent.getMeasuredHeight();
            } else if(isDynamicallyCreated) {
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                parentHeight = metrics.heightPixels;
            } else {
                parentHeight=0;
            }

            if(parentHeight>0) {
                float percentMargin= ((float)margin/100.0f)*((float)parentHeight);

                if (position == MARGIN_TOP) {
                    ribbonData.ribbonMarginTop=margin;
                    marginTop = Math.round(percentMargin);
                } else {
                    ribbonData.ribbonMarginBottom=margin;
                    marginBottom = Math.round(percentMargin);
                }
            }
        } else {
            ribbonData.marginIsPercentage=false;
            if(position==MARGIN_TOP) {
                marginTop= (int) TypedValue.applyDimension(unit, margin, getResources().getDisplayMetrics());
                ribbonData.ribbonMarginTop=marginTop;
            } else {
                marginBottom=(int) TypedValue.applyDimension(unit, margin, getResources().getDisplayMetrics());
                ribbonData.ribbonMarginBottom = marginBottom;
            }
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) ribbonContainer.getLayoutParams();

        if(ribbonData.isInOrphanMode && !ribbonData.blocksUnderlying) {
            params.bottomMargin=0;
            params.topMargin=0;

            WindowManager.LayoutParams windowParams= (WindowManager.LayoutParams) getLayoutParams();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            int y = (metrics.heightPixels/2);

            windowParams.gravity= Gravity.TOP;
            if(position==MARGIN_BOTTOM){
                windowParams.y=y-marginBottom;
            } else {
                windowParams.y+=y+marginBottom;
            }

        } else {
            params.bottomMargin=marginBottom;
            params.topMargin=marginTop;
        }

        return this;
    }

    /**
     * Get the Ribbon's top margin either as a percent or DIP, depending how it was set
     *
     * @return The Ribbon's top margin
     */
    public int getRibbonMarginTop(){
        if(ribbonData.marginIsPercentage) {
            return ribbonData.ribbonMarginTop;
        } else {
            return Math.round(ribbonData.ribbonMarginTop / getResources().getDisplayMetrics().density);
        }
    }

    /**
     * Get the Ribbon's bototm margin either as a percent or DIP, depending how it was set
     *
     * @return The rRibbon's bottom margin
     */
    public int getRibbonMarginBottom(){
        if(ribbonData.marginIsPercentage) {
            return ribbonData.ribbonMarginBottom;
        } else {
            return Math.round (ribbonData.ribbonMarginBottom / getResources().getDisplayMetrics().density);
        }
    }


    /**
     * Returns true when margins are set as a percentage, false when not
     * @return boolean that answers the question
     */
    public boolean marginIsSetAsPercentage(){
        return ribbonData.marginIsPercentage;
    }


    /**
     * Set whether the Ribbon should be shown in dialogue mode. Dialogue mode means that the
     * Ribbon does not extend ot the screen edges and side borders are also visible, identical in colour ot the top and bottom borders.
     * In dialogue mode, there is also an option to control the corner radius
     *
     * @param isInDialogueMode boolean to determine whether the Ribbon should be drawn in dialogue mode
     */
    public ProgressRibbon setRibbonInDialogueMode(boolean isInDialogueMode){
        ribbonData.isInDialogueMode=isInDialogueMode;
        return setRibbonBorders();
    }

    /**
     * Whether the Ribbon is in dialogue mode or not
     *
     * @return boolean value showing whether the Ribbon is in dialogue mode
     */
    public boolean isInDialogueMode(){ return ribbonData.isInDialogueMode;}


    /**
     * This controls the Ribbon'Űs border thickness in DIP
     *
     * @param borderSize the Ribbon's border thickness in DIP
     */
    public ProgressRibbon setRibbonBorderSize(@IntRange(from=0) int borderSize){
        ribbonData.ribbonBorderSize=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, borderSize, getResources().getDisplayMetrics()));
        return setRibbonBorderSize();
    }

    /**
     * Set the Ribbon's border thickness in a unit of your choice (as a complex unit form from TypedValue)
     *
     * @param unit Accepted values are TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM
     * @param borderSize the actual value of the border thickness
     */

    public ProgressRibbon setRibbonBorderSize(@ValidSizeUnit int unit, @IntRange(from=0) int borderSize){
        ribbonData.ribbonBorderSize=Math.round(TypedValue.applyDimension(unit, borderSize, getResources().getDisplayMetrics()));
        return setRibbonBorderSize();
    }

    /**
     * Internal method to set the border size for real. Non-dialogue Ribbons have a top and Bottom view that act as borders, their height will determine border thickness.
     * These are switched on and off when dialogue mode is toggled. In dialoguie mode, a background drawable uiis used, and that is updated here
     */
    private ProgressRibbon setRibbonBorderSize(){

        int sidePadding=0;
        if(ribbonData.isInDialogueMode){
            ((GradientDrawable) ribbonContainer.getBackground()).setStroke(ribbonData.ribbonBorderSize, ribbonData.ribbonBorderColor);
            sidePadding=ribbonData.ribbonBorderSize;
        } else {
            /**
             * Need to recreate this, otherwise "hot-swappnig" size has no effect. It's not a realistic situation, but still, i shold work.
             */
            LayerDrawable bg= (LayerDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ribbon_default_bg);
            bg.setLayerInset(1, 0,ribbonData.ribbonBorderSize,0,ribbonData.ribbonBorderSize);
            ribbonContainer.setBackground(bg);
            sidePadding=0;

        }

        if(ribbonData.ribbonBorderSize==0){
            /**
             * Using the internal method so that the stored border colour does not get overridden
             * Applying backgroundColour rather than Color.TRANSPARENT for the elevation effect to still work
             *
             * The colour change is necessary because the drawable might still show even if stroke is set to 0 width
             * */
            setBorderColor(ribbonData.backgroundColor);
        }

        ribbonContainer.setPadding(sidePadding, ribbonData.ribbonPaddingTop, sidePadding, ribbonData.ribbonPaddingBottom);

        return this;
    }

    /**
     * Get the Ribbon1s border size in DIP
     *
     * @return the Ribbon1s border size in DIP
     */
    public int getRibbonBorderSize(){
        return Math.round(ribbonData.ribbonBorderSize / getResources().getDisplayMetrics().density);
    }

    /**
     * Set the color of the Ribbon's borders (as a Colorint)
     *
     * @param color ColorInt, value of the Ribbon's border color
     */
    public ProgressRibbon setRibbonBorderColor(@ColorInt int color){
        ribbonData.ribbonBorderColor =color;
        return setBorderColor(ribbonData.ribbonBorderColor);
    }

    /**
     * Internal method to set the ribbons border color for real. Don1t ask why this is needed, I have no idea.
     * Might have had plans for multiple public methods, dunno.
     */
    private ProgressRibbon setBorderColor(@ColorInt int color){
        if(ribbonData.isInDialogueMode){
            GradientDrawable bg = (GradientDrawable) ribbonContainer.getBackground();
            bg.setStroke(ribbonData.ribbonBorderSize, color);
        } else {
            LayerDrawable bg= (LayerDrawable) ribbonContainer.getBackground();
            ((GradientDrawable)bg.getDrawable(0)).setColor(color);
        }

        return this;
    }

    /**
     * Get the border colour as a ColorInt
     *
     * @return Colorint of the border color
     */
    @ColorInt
    public int getRibbonBorderColor(){
        return ribbonData.ribbonBorderColor;
    }

    /**
     * The overlay must remain transparent. This will prevent colour changes.
     * The reason why this is ignored and not pasoed on to set the visible part's background colour,
     * is that we actually need to use it t make the blocking overlay transparent.
     * Hack-is. but it works
     *
     * @param color Dummy parameter, it will be ignored.
     */
    @Override
    public void setBackgroundColor(@ColorInt int color) {
        super.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * Now we are talking. this will set any supplied backlground drawable on the visible part of the ribbon
     * (skipping the transparent overlay)
     *
     * @param background Drawable to set as background
     */
    @Override
    public void setBackground(Drawable background) {
        ribbonContainer.setBackground(background);
    }

    /**
     * The proper way to set a solid colour as a Ribbon backgrond
     *
     * @param color ColorInt to set as ribbon background colour
     */
    public ProgressRibbon setRibbonBackgroundColor(@ColorInt int color){
        ribbonData.backgroundColor=color;
        return setRibbonBackgroundColor();
    }

    /**
     * Internal method to set Ribbon1s background color. Dialogue mode needs the custom drawable ot be altered,
     * while non-dialogue mode will set the top and bottom border view's color
     */
    private ProgressRibbon setRibbonBackgroundColor(){
        Drawable bg= ribbonContainer.getBackground();
        if(ribbonData.isInDialogueMode) {
            ((GradientDrawable)bg).setColor(ribbonData.backgroundColor);
            ribbonContainer.setBackground(bg);
        } else {
            /**
             * That's one ugly casting there, but at least we don't have to create two extra Objects just to change colour.
             */
            ((GradientDrawable) ((InsetDrawable) ((LayerDrawable) bg).getDrawable(1)).getDrawable()).setColor(ribbonData.backgroundColor);

        }

        return this;
    }

    /**
     * Get the Ribbon1s background color
     *
     * @return ColorINt of the Ribbon's background color
     */
    @ColorInt
    public int getRibbonBackgroundColor(){
        return ribbonData.backgroundColor;
    }

    /**
     * Set the Ribbon's corner radius in DIP, as a non-zero integer. only works for dialogue mode Ribbons, otherwise ignored
     *
     * @param radius the corner radius in DIP, as a non-zero integer
     */
    public ProgressRibbon setRibbonBorderRadius(@FloatRange(from=0.0f) float radius){

        ribbonData.ribbonBorderRadius= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radius, getResources().getDisplayMetrics());

        if(ribbonData.isInDialogueMode) {
            setRibbonBorderRadius();
        }
        return this;
    }

    /**
     * Get the Ribbon's current corner radius. Meaningful only for dialoge-mode Ribbons
     *
     * @return Corner radius in DIP
     */
    public float getRibbonBorderRadius(){
        return ribbonData.ribbonBorderRadius;
    }

    /**
     * Internal method to set the Ribbon's corner radius. Only makes sese in dialogue mode, where the custom drawable
     * background will be updated accordingly
     */
    private ProgressRibbon setRibbonBorderRadius(){
        if(ribbonData.isInDialogueMode){
            GradientDrawable bg = (GradientDrawable) ribbonContainer.getBackground();
            bg.setCornerRadius(ribbonData.ribbonBorderRadius);
            //      ribbonContainer.setBackground(bg);
            /**
             * On pre-Lollipop devices too big radius can cause overlap with bar style view, a larger top padding is advisable
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ribbonContainer.setClipToOutline(true);
            }
        } else {
            Log.e("ProgressRibbon", "Setting border radius on a non-dialogue-mode Ribbon will have no effect.");
        }

        return this;
    }

    /**
     * internal method to change Ribbon borders from Bordering Views to custom drawable (used in dialogue mode).
     * Called when Ribbon nis initially set or when dialogue mode is toggled.
     */
    private ProgressRibbon setRibbonBorders(){
        int currentPaddingLeft=getPaddingLeft();
        int currentPaddingRight=getPaddingRight();

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) ribbonContainer.getLayoutParams();

        if(ribbonData.isInDialogueMode){
            GradientDrawable bg = (GradientDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ribbon_dialogue_bg);
            ribbonContainer.setBackground(bg);
            int sideMargin=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            params.leftMargin=sideMargin;
            params.rightMargin=sideMargin;
            ribbonContainer.setPadding(ribbonData.ribbonBorderSize, ribbonContainer.getPaddingTop(), ribbonData.ribbonBorderSize, ribbonContainer.getPaddingBottom());
        } else {
            LayerDrawable bg= (LayerDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ribbon_default_bg);
            ribbonContainer.setBackground(bg);
            params.leftMargin=0;
            params.rightMargin=0;
            ribbonContainer.setPadding(0, ribbonContainer.getPaddingTop(), 0, ribbonContainer.getPaddingBottom());
        }

        setBorderColor(ribbonData.ribbonBorderColor);
        setRibbonBackgroundColor();
        setRibbonBorderSize();

        /**
         * Side effect of changing background: All paddings are lost. Need to restore to whatever they were just before
         */
        ribbonContainer.setPadding(currentPaddingLeft, ribbonData.ribbonPaddingTop, currentPaddingRight, ribbonData.ribbonPaddingBottom);


        if(ribbonData.isInDialogueMode) {
            setRibbonBorderRadius();
        }

        return this;
    }


    /**
     * A shortcut function to hide all the background and borders without having to set their individual colours
     */
    public ProgressRibbon setRibbonTransparent(){
        ribbonData.backgroundColor= Color.TRANSPARENT;
        ribbonData.ribbonBorderColor =Color.TRANSPARENT;
        setBorderColor(Color.TRANSPARENT);
        setRibbonBackgroundColor();
        return setRibbonBorderSize();
    }

    /**
     * Convenience method to set text colour for the progress text without havng to access the textView manually
     *
     * @param color ColorInt of the text colour to be set
     */
    public ProgressRibbon setProgressTextColour(@ColorInt int color){
        ribbonData.progressTextColor = color;
        progressTextView.setTextColor(color);
        return this;
    }

    /**
     * Get the current progress text colour as a simple integer (ColorInt).
     * if you need proepr ColorStateList, access the component TextView manually via getProgressTextView(),
     * and interrogate the Widget as you see fit
     *
     * @return @ ColorInt of the progress text colour.
     */
    @ColorInt
    public int getProgressTextColor(){
        return ribbonData.progressTextColor;
    }

    /**
     * Convenience method to set text size in SP for the progress text without having to access the textView manually
     *
     * @param textSize int of the text site to be set in SP
     */
    public ProgressRibbon setProgressTextSize(int textSize){

        ribbonData.progressTextSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, getResources().getDisplayMetrics()));
        progressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        return this;
    }

    /**
     * Convenience method to set text size in any valid unit (use Typedvalue units) for the progress text without having to access the textView manually
     *
     * @param unit Accepted values are TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP, TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM
     * @param textSize float of the text site to be set
     */
    public ProgressRibbon setProgressTextSize(@ValidSizeUnit int unit, float textSize){
        progressTextView.setTextSize(unit, textSize);
        ribbonData.progressTextSize=Math.round(progressTextView.getTextSize());
        return this;
    }

    /**
     * Get the size of the progress text in SP
     * @return The raw size of the progress text
     */
    public float getProgressTextSize(){
        return ribbonData.progressTextSize;
    }

    /**
     * Freezes the progress at the current point, any progress updates to it will not be applied after this
     */
    public ProgressRibbon freezeProgress(){
        return setFrozenState(FREEZE_PROGRESS);
    }

    /**
     * This will unfreeze the Ribbon's progress, allowing for its progressbar to be updated again
     */
    public ProgressRibbon allowProgress(){
        return setFrozenState(UNFREEZE_PROGRESS);
    }

    /**
     * Set the frozen/unfrozen state of the Ribbon. When frozen, any updated to the Ribbon are not applied,
     * while unfreezing it will allow updates again
     *
     * @param frozenState
     */
    public ProgressRibbon setFrozenState(@ProgressFrozen int frozenState){
        this.isProgressFrozen = (frozenState>0);
        if(ribbonStateChangeListener !=null) {
            if (isProgressFrozen) {
                if (ribbonData.progress < ribbonData.max) {
                    ribbonStateChangeListener.onRibbonProgressStopped(ribbonData.progress);
                }
            } else if (ribbonData.progress >= ribbonData.min) {
                ribbonStateChangeListener.onRibbonProgressStarted(ribbonData.progress);
            }
        }
        return this;
    }

    /**
     * Check if the Ribbon is in frozen state
     *
     * @return The frozen state as a boolean
     */
    public boolean isProgresFrozen(){
        return isProgressFrozen;
    }

    /**
     * Checks if the Ribbon is in orphan mode, meaning it has no parent view it's attached to.
     * Orphan Ribbons are attached directly ot the Window, otherwise they are attached to a ViewGroup
     *
     * @return The fact of orphaneness, or not
     */
    public boolean isInOrphanMode(){
        return ribbonData.isInOrphanMode;
    }

    /**
     * Set the ribbon's progress text as a String
     *
     * @param progressText The text to set
     */
    public ProgressRibbon setProgressText(String progressText){
        return setText(progressText);
    }

    /**
     * Set the Ribbon's progress text as a string resource ID.
     * It will find the String and apply it, so you don't have to
     *
     * @param resId the ID of the string resource
     */
    public ProgressRibbon setProgressText(@StringRes int resId){
        return setText(getContext().getString(resId));
    }

    /**
     * Internal funtion to actually set the text on the Ribbon, called from the public functions.
     * If you see this text from another class, something either went horribly wrong, or you/someone did not
     * rewrite the JavaDoc comments when forking this project...
     *
     * @param text Text to set on the Ribbon
     */
    private ProgressRibbon setText(String text){
        progressText=text;
        progressTextView.setText(progressText);
        invalidate();
        return this;
    }

    /**
     * Get the currently set progress text from the Ribbon as a String
     *
     * @return the text from underneath the progressbar
     */
    public String getProgressText(){
        return progressText;
    }

    /**
     * Here we simply set the blockUnderLying flag from the outside world.
     * The supplied boolean will control what the overridden onTouchEvent(...) will return (true means it intercepts touches,
     * blocking what's underneath). If we are in orphan mode, meaning the ribbon is attached to the window with no parent view,
     * it will bock the underlying views by simply expanding our Ribbon (or rather its trasparent overlay view on the top and
     * bottom bit) to the full size of the screen. Or not (if we are in non-blocking mode). Basically only the height ever varies.
     * (The type and flags of the WindowManager.LayoutParams ensure that the view can block others and is touchable so it can
     * intercept motion events correctly)
     *
     * @param blocksUnderlyingViews - This one speaks for itself.
     */
    public ProgressRibbon setViewBlocking(boolean blocksUnderlyingViews){
        ribbonData.blocksUnderlying = blocksUnderlyingViews;

        if(ribbonData.isInOrphanMode) {

            WindowManager.LayoutParams params;
            int height;

            if(ribbonData.blocksUnderlying) {
                height=WindowManager.LayoutParams.MATCH_PARENT;
            } else {
                height=WindowManager.LayoutParams.WRAP_CONTENT;
            }

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    height,
                    WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);

            if(getWindowToken() != null) {
                activity.getWindowManager().updateViewLayout(this, params);
            } else {
                setLayoutParams(params);
            }

        }
        return this;
    }

    /**
     * Check if the Ribbon is set to block underlying views
     * @return a boolean value whether it1s blocking or not
     */
    public boolean willBlockUnderlyingViews(){
        return ribbonData.blocksUnderlying;
    }




    /**
     * This one will summon the dark... Oh wait that1s not it. This one will, your guess was right, set the Ribbons elevation. Magic.
     * Oh wait, there is more. It will actually work on *all* SDK levels, checking against the version, and deciding whether is
     * should apply proper elevation, or the AppCompat version. So it really IS magic.  You know, sort of how elevation should have been
     * implemented by Android devs in the first place...
     *
     * @param elevation Elevation value in DP to set, in an SDK version independent manner. Ha!
     */
    @Override
    public void setElevation(float elevation) {
        setRibbonElevationInternal(elevation);
    }

    /**
     * Same as setElevation (float), but with the option to chain it with other setters
     * @param elevation
     * @return
     */
    public ProgressRibbon setRibbonElevation(float elevation) {
        return setRibbonElevationInternal(elevation);
    }


    /**
     * Internal to actually do the heavy lifting, when setting elevation level. This is where the magic happens.
     * It will check whether it can apply proper elevation (according to SDK version), and if not, applies the AppCompat
     * way instead. Also, if you see this text from another class, somebody somewhere forgot to update JavaDoc when
     * changing stuff...
     *
     * @param elevation the elevaton value in DP
     */
    public ProgressRibbon setRibbonElevationInternal(@FloatRange(from=0) float elevation){

        ribbonData.ribbonElevation=elevation;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ribbonContainer.setElevation(ribbonData.ribbonElevation);
        } else {
            ViewCompat.setElevation(ribbonContainer, ribbonData.ribbonElevation);
        }

        requestLayout();
        return this;
    }


    /**
     * Gets the currently set elevation of the Ribbon
     *
     * @return the elevation value
     */
    @Override
    public float getElevation() {
        return ribbonData.ribbonElevation;
    }

    /**
     * Simmply read  the default layout configuration from the system. and
     * @return the default layout configuration from the system.
     */
    private int getConfigLayoutDirection(){
        final Configuration config = getResources().getConfiguration();
        int configLayoutDirection= config.getLayoutDirection();
        return configLayoutDirection;
    }

    /**
     * Sets the layout direction same as with every wiew, LRT? RTL? INHERIT? OR LOCALE. In the latter two
     * cases it works differently, as it will not really traverse the view-tree to find one that is explicitly set
     * (as in INHERIT), but checks the layout direction in the configuraton (i.e. what is the basic system setting), and apply
     * it to the view. This breaks inheritence, but since we are not necessarily attached yet, needed a sane default.
     *
     * @param layoutDirection the layoutDirection as ni android. only LTR and RTL do anyting effieftively, otherwise system config value will apply
     */
    @Override
    public void setLayoutDirection(int layoutDirection) {
        super.setLayoutDirection(layoutDirection);

        if(layoutDirection==LAYOUT_DIRECTION_INHERIT || layoutDirection==LAYOUT_DIRECTION_LOCALE){
            layoutDirection = getConfigLayoutDirection();
        }

        if(layoutDirection==LAYOUT_DIRECTION_RTL){
            layoutIsRTL=true;
            progressBar.setLayoutDirection(LAYOUT_DIRECTION_RTL);
            progressTextView.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        } else {
            layoutIsRTL=false;
            progressBar.setLayoutDirection(LAYOUT_DIRECTION_LTR);
            progressTextView.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        }
    }

    /**
     * Shortcut that tells you whether the layout of the ribbon is set to RTL in a convenient boolean format
     * @return whether layout is RTL (boolean)
     */
    public boolean getLayoutIsRTL(){
        return layoutIsRTL;

    }

    /**
     * Sets the min value of the Ribbon in an SDK independent manner. On Android's standard ProgressBar widgets
     * min can only be set from upwards of SDK v26, *and* in a not very sensible manner either, i.e. the bar gets
     * a minimum value, but the progress drawable does not reflect this value.
     * In ProgressRibbon, the min value gets applied to the drawable as well.
     * (It will start from there, instead of starting from an empty bar.)
     * Any progress applied to the ribbon that is below min, will be ignored.
     *
     * @param min the minimum value for the determinate ProgressRibbon
     */
    public ProgressRibbon setMin(@IntRange(from=0) int min){
        ribbonData.min=min;
        ribbonData.progress=min;
        progressBar.setProgress(min);
        return this;
    }

    /**
     * Returns the current value of the Ribbons minimum value
     *
     * @return the current value of min
     */
    public int getMin(){
        return ribbonData.min;
    }

    /**
     * Sets the maximum value of the ProgressRibbon (in determinate mode). Works the same as in the standard
     * ProgressBar widget
     *
     * @param max the maximum value of the ProgressRibbon in determinate mode
     */
    public ProgressRibbon setMax(@IntRange(from=0)int max){
        if(max>=ribbonData.min) {
            ribbonData.max = max;
        } else {
            ribbonData.max=ribbonData.min;
        }
        progressBar.setMax(ribbonData.max);
        return this;
    }

    /**
     * Returns the maximum value of a determinate ProgressRibbon as an int
     *
     * @return The maximum value of a determinate ProgressRibbon as an int
     */
    public int getMax(){
        return ribbonData.max;
    }

    /**
     * Sets the progress of the determinate progress widgets to the specified value. Yes, there are
     * two widgets and they will both be set, regardless of which one is showing to avoid complexity elsewhere.)
     * It will check if the progress exceeds the set minimum, and sets the progress value to it if yes.
     * It will then check if the progress is frozen before applying the value to the animated widgets too.
     * Also calls the appropriate listeners, if they exist.
     *
     * @param progress the value to set as progress
     */
    public void setProgress(@IntRange(from=0) int progress){

        if(ribbonStateChangeListener !=null && (progress==ribbonData.min)){
            ribbonStateChangeListener.onRibbonProgressStarted(progress);
        }

        if(progress>ribbonData.min) {
            ribbonData.progress = progress;

            if(!isProgressFrozen) {

                //Make it look nice if the device is new enough
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(progress, true);
                } else {
                    progressBar.setProgress(progress);
                }

                if (ribbonStateChangeListener != null) {
                    ribbonStateChangeListener.onRibbonProgressChange(ribbonData.progress);
                }
            }

        } else {
            ribbonData.progress = ribbonData.min;
            progressBar.setProgress(ribbonData.progress);
        }

        if(ribbonStateChangeListener !=null  && ribbonData.progress==ribbonData.max){
            ribbonStateChangeListener.onRibbonProgressStopped(ribbonData.progress);
        }
    }


    /**
     * Set the seconrady progress for the Ribbon,
     * @param secondaryProgress the secondary progress value
     */
    public void setSecondaryProgress(@IntRange(from=0) int secondaryProgress){

        if (secondaryProgress > ribbonData.min) {
            ribbonData.secondaryProgress = secondaryProgress;
        } else {
            ribbonData.secondaryProgress = ribbonData.min;
        }

        if (!isProgressFrozen) {
            if (ribbonData.progressBarStyle == BAR_HORIZONTAL) {
                progressBar.setSecondaryProgress(ribbonData.secondaryProgress);

            } else {
                // ProgressRibbon does not know out special drawable and it will not
                // recognise the levels set in it even with the right ids, so we need
                // to get creative
                updateCircularSecondaryProgress();
            }
        }
    }

    /**
     * This method combines those from the super class that dot he same job,only corrrectly applying the
     * secondary progress to our special round drawable
     */
    private synchronized void updateCircularSecondaryProgress(){
        Drawable d = progressBar.getProgressDrawable();

        int range = ribbonData.max - ribbonData.min;
        final float scale = range > 0 ? (ribbonData.secondaryProgress - ribbonData.min) / (float) range : 0;

        if (d instanceof LayerDrawable) {
            d = ((LayerDrawable) d).findDrawableByLayerId(R.id.secondaryProgress);
        }

        if (d != null) {
            final int level = (int) (scale * MAX_LEVEL);
            d.setLevel(level);
        } else {
            invalidate();
        }

    }

    /**
     * Same as ProgressBar incrementProgressby. It will  increments progress by
     * @param diff int to increment progress by
     */
    public void incrementProgressBy(@IntRange(from=0) int diff){
        progressBar.incrementProgressBy(diff);
    }

    /**
     * Same as ProgressBar incrementSecondaryProgressby. It will  increments secondary progress by
     * @param diff int to increment progress by
     */
    public void incrementSecondaryProgressBy(@IntRange(from=0) int diff){
        ribbonData.secondaryProgress+=diff;
        updateCircularSecondaryProgress();
    }

    /**
     * Can set whether to return progress as a percent of the currrent max value, or as a raw number
     * @param value boolean flagh
     */
    public ProgressRibbon setReportProgressAsMaxPercent(boolean value){
        ribbonData.reportProgressAsMaxPercent=value;
        return this;
    }

    /**
     * Tells you whether the Ribbon will return progress as a percent of the currrent max value, or as a raw number
     */
    public boolean willReportProgressAsMaxPercent(){
        return ribbonData.reportProgressAsMaxPercent;
    }

    /**
     * Gets the actual value of the currently displayed of the determinate widgets as an int, or, if the flag is set,
     * is value as a percentage of the currently set max value (useful if other than 100)
     *
     * @return Current progress value as int.
     */
    public int getProgress(){

        if(ribbonData.reportProgressAsMaxPercent){
            return getProgressPercentage();
        } else {
            return progressBar.getProgress();
        }
    }

    /**
     * Returns secondary progress value, if there is any
     *
     * @return the secondary progress value as an integer
     */
    public int getSecondaryProgress(){
        return ribbonData.secondaryProgress;
    }

    /**
     * To see (in code) whether the Ribbon is showing ort not
     * @return a boolean that tells you the same
     */
    public boolean isShowing(){
        return isShowing;
    }

    /**
     * internal method to returns the progress value as a percentage of the maximum progress. Useful, when maximum is set to
     * anything other than 100, but an accurate percentage is still needed. Needs reportProgressAsMaxPercent to be set
     *
     * @return the calculated percentage of the current progress, in relation to the max value
     */
    private int getProgressPercentage(){
        return (progressBar.getProgress()*100)/ribbonData.max;
    }

    /**
     * Set a delay in milliseconds (int) before showing the Ribbon. Useful for operations that might finish very quickly
     * (no progressbar needed) or might take some time 8progressbar should be shown.
     * This wil allow not to show the ribbon at all, when the operation finishes withn the delay treshold.
     *
     * @param showDelay dDelay value in milliseconds (int)
     */
    public ProgressRibbon setShowDelay(@IntRange(from=0) int showDelay){
        ribbonData.showDelay=showDelay;
        return this;
    }

    /**
     * Returns the currently set show delay value in milliseconds, as an int
     *
     * @return The currently set show delay value in milliseconds, as an int
     */
    public int getShowDelay(){
        return ribbonData.showDelay;
    }

    /**
     * Sets a hide delay on the Ribbon, in milliseconds (int). hide delay works in conjunction with show delay,
     * and is meant to ensure that the Ribbon will not just "flash" when the operation calling it finishes shortly after the Ribbon appeared.
     * hide delay can be thought of as a minimum time a Ribbon should be visible to avoid ugly fragment-of-a-second flashes
     *
     * @param hideDelay hide delay value in milliseconds (int)
     */
    public ProgressRibbon setHideDelay(@IntRange(from=0) int hideDelay) {
        ribbonData.hideDelay = hideDelay;
        return this;
    }

    /**
     * Returns the currently set hide delay value in milliseconds, as an int
     *
     * @return The currently set hide delay value in milliseconds, as an int
     */
    public int getHideDelay(){
        return ribbonData.hideDelay;
    }

    /**
     * Choose between circular and flat bars. Can use static constants PROGRESSBAR_ROUND_DETERMINATE (0) or PROGRESS_BAR (1)
     * Everything else will be ignored, and the circular style applied
     *
     * @param style int value of the progress bar style (see static constants)
     */
    public ProgressRibbon setProgressBarStyle(@ProgressStyle int style){
        if(style!=ribbonData.progressBarStyle) {
            ribbonData.progressBarStyle = style;
            initProgressbar(ribbonData.progressBarStyle, ribbonData.isIndeterminate);
        }
        return this;
    }

    /**
     * Get the current Ribbon bar style as an int. Evaluate against static constants PROGRESSBAR_ROUND_DETERMINATE or PROGRESS_BAR
     *
     * @return Current ribbon style as an in
     */
    public int getProgressBarStyle(){
        return ribbonData.progressBarStyle;
    }

    /**
     * The progress text can appear either besides r underneath the progress bar IF it's set to circular. Flat
     * bars can only display text underneath, so that si what they will do. Call with a false value to set the ttext under the bar
     *
     * Foolproofing: If called on a flat style bar with a `true` value, it will be ignored
     * and the text et underneath anyway
     *
     * @param textPosition Whether the text should be displayed besides or underneath the bar (boolean)
     */
    public ProgressRibbon setRibbonTextPosition(@TextPosition int textPosition){
        ribbonData.textBesideBar = (textPosition>0);
        LinearLayout.LayoutParams barParams = (LinearLayout.LayoutParams) progressBar.getLayoutParams();
        int textPaddingTop, textPaddingEnd;

        if(!ribbonData.textBesideBar || ribbonData.progressBarStyle ==BAR_HORIZONTAL) {
            ribbonData.textBesideBar =false;
            ribbonContainer.setOrientation(LinearLayout.VERTICAL);
            barParams.gravity=Gravity.CENTER;
            textPaddingTop=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            textPaddingEnd=0;
        } else {
            ribbonData.textBesideBar =true;
            ribbonContainer.setOrientation(LinearLayout.HORIZONTAL);
            barParams.gravity=Gravity.START;
            textPaddingTop=0;
            textPaddingEnd=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());;
        }

        progressTextView.setPaddingRelative(0, textPaddingTop, textPaddingEnd,0);
        return this;
    }

    /**
     * Returns a boolean stating whether the text is set to be displayed beside or underneath the bar
     *
     * @return boolean, text is displaying beside the bar. Or not.
     */
    public boolean isRibbonTextBesideProgressBar(){
        return ribbonData.textBesideBar;
    }

    /**
     * Set the Ribbon as an indeterminate loader, with no progress values to set/show.
     * Works with both circular and flat bars. If set ot indeterminate, the bars will spin indeterminately,
     * otherwise they will show the currently set progress in an animated fashion (yes, even the circular one)
     *
     * @param indeterminateState Whether the progress is indeterminate (boolean)
     */
    public ProgressRibbon setIndeterminateState(@IndetermiateState int indeterminateState){

        ribbonData.isIndeterminate = (indeterminateState>0);
        progressBar.setIndeterminate(ribbonData.isIndeterminate);

        if(!ribbonData.isIndeterminate){
            progressBar.setProgress(ribbonData.progress);
        }

        if(ribbonData.progressBarStyle==BAR_ROUND){
            initProgressbar(ribbonData.progressBarStyle, ribbonData.isIndeterminate);
        }

        if(ribbonStateChangeListener !=null ) {
            ribbonStateChangeListener.onRibbonIndeterminateStatusChanged(ribbonData.isIndeterminate);
        }
        return this;
    }

    public boolean isIndeterminate(){
        return ribbonData.isIndeterminate;
    }

    /**
     * Check whether the Ribbon's progressbar is determinate or indeterminate.
     * Evaluate against DETERMINATE or INDETERMINATE member constants.
     *
     * @return DETERMINATE, or INDETERMINATE or rather their values
     */
    @IndetermiateState
    public int getIndeterminateState(){
        if(ribbonData.isIndeterminate) {
            return INDETERMINATE;
        } else {
            return  DETERMINATE;
        }
    }

    /**
     * Retruns whether the Ribbon is set to animate its showing hiding
     *
     * @return What it says
     */
    public boolean willAnimateAppearance(){
        return ribbonData.animationType!=DO_NOT_ANIMATE;
    }

    /**
     * Sets the duration of show/hide animation (if any) in milliseconds (int)
     *  Will be ignored during animations
     *
     * @param value the duration of the show/hide animatin in milliseconds
     */
    public ProgressRibbon setAnimationDuration(@IntRange(from=0) int value){
        if(!animationInProgress) {
            ribbonData.animationDuration = value;
        }
        return this;
    }

    /**
     * gets the duration of show/hide animation (if any) in milliseconds (int).
     * @return the duration of the show/hide animation in milliseconds
     */
    public int getAnimationDuration(){
        return ribbonData.animationDuration;
    }

    /**
     * Supports three types of animations, which are accessible as static constants for easy access:
     * - Simple fade in/out (ANIMATE_FADE)
     * - Simple scale in/out (ANIMATE_SCALE)
     * - Scale with fade in/out (ANIMATE_SCALE_FADE)
     *
     * It will set both the show and hide animations at the same tme, but it can be changed on they fly
     * while the Ribbon is showing, although not while animating (there is a failsafe for this)
     **
     * @param animationType the type of animation to use
     */
    public ProgressRibbon setAnimationType(@AnimationType int animationType){
        if(!animationInProgress) {
            if (animationType < DO_NOT_ANIMATE) {
                ribbonData.animationType = DO_NOT_ANIMATE;
            } else {
                ribbonData.animationType = animationType;
            }
        }
        return this;
    }

    /**
     * Get the current animation type as an int, evaluate against the static constants ANIMATE_FADE, ANIMATE_SCALE and ANIMATE_SCALE_FADE
     *
     * @return the current animation type as an int
     */
    public int getAnimationType(){
        return ribbonData.animationType;
    }


    /**********************************Set up animations*******************************************/

    /**
     * Internal function to handle the animation of showing/hiding the ribbon. Thwhether it's showing otr hiding
     * shodl be set by using the two constants ANIMATION_SHOW and ANIMATION_HIDE.
     * It uses the member vaurable animationType to know what type of animation to do
     * If you see this text from another class, somebody forgot to update the JavaDoc
     *
     * @param animationDirection Meaning showing or hiding. see static constants
     */
    private ProgressRibbon animateShowHide(@AnimationDirection final int animationDirection){

        float fromXY, toXY, fromAlpha, toAplha;

        if(animationDirection==ANIMATION_SHOW){
            fromXY=0f;
            toXY=1.0f;
            fromAlpha=0;
            toAplha=1.0f;
        } else {
            fromXY = 1.0f;
            toXY = 0.0f;
            fromAlpha=1.0f;
            toAplha=0.0f;
        }

        final AnimatorSet animatorSet = new AnimatorSet();

        if(ribbonData.animationType== ANIMATE_SCALE){
            ObjectAnimator animatorX = ObjectAnimator.ofFloat(this, "scaleX", fromXY, toXY);
            ObjectAnimator animatorY = ObjectAnimator.ofFloat(this, "scaleY", fromXY, toXY);
            animatorSet.play(animatorX).with(animatorY);
        } else if (ribbonData.animationType== ANIMATE_SCALE_FADE){
            ObjectAnimator animatorAlpha = ObjectAnimator.ofFloat(this, "alpha", fromAlpha, toAplha);
            ObjectAnimator animatorX = ObjectAnimator.ofFloat(this, "scaleX", fromXY, toXY);
            ObjectAnimator animatorY = ObjectAnimator.ofFloat(this, "scaleY", fromXY, toXY);
            animatorSet.play(animatorX).with(animatorY);
            animatorSet.play(animatorX).with(animatorAlpha);
        } else {
            ObjectAnimator  animatorAlpha = ObjectAnimator.ofFloat(this, "alpha", fromAlpha, toAplha);
            animatorSet.play(animatorAlpha).with(animatorAlpha);
        }

        animatorSet.setDuration(ribbonData.animationDuration);
        animatorSet.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {
                setVisibility(View.VISIBLE);
                animationInProgress=true;
            }

            @Override
            public void onAnimationEnd(Animator animatior) {

                if(animationDirection==ANIMATION_HIDE){
                    isShowing=false;
                    ProgressRibbon.this.setVisibility(View.GONE);
                    if(ribbonStateChangeListener !=null) {
                        ribbonStateChangeListener.onRibbonHide();
                    }
                } else {
                    isShowing=true;
                    if(ribbonStateChangeListener !=null) {
                        ribbonStateChangeListener.onRibbonShow();
                    }
                }

                /*Leave everything as we found it, for the next guy*/
                ProgressRibbon.this.setAlpha(1.0f);
                ProgressRibbon.this.setScaleX(1.0f);
                ProgressRibbon.this.setScaleY(1.0f);

                animatior.cancel();
                animationInProgress=false;
            }

            @Override
            public void onAnimationRepeat(Animator animatior) {}

            @Override
            public void onAnimationCancel(Animator animatior) {}
        });
        animatorSet.start();
        return this;
    }


    /**********************************Handle (delayed) show/hide*********************************/

    private Handler showHandler;
    private Runnable runner;
    private Handler hideHandler;

    /**
     * Combined internal function to make the ribbon appear (with delay if a delay is set), and if not yet done,
     * attach to to either a parent view if one is supplied or the Window directly, if not. the skipAnimaton parameter
     * allows for temporarily overriding willAnimateAppearance, when animation shoudl momentarily be ignored, such as when restoring view after layout changes
     *
     * if you see this text from another class, somebody forgot to update the javaDoc
     *
     * @param skipAnimation temporarily override animation setting in specific cases
     */
    private void attachAndShowView(boolean skipAnimation){

        setVisibility(View.GONE);

        if(isDynamicallyCreated) {

            /**
             * Static instancs tend to stick around and we'll need to remove them in certain cases. Since we don't know what's
             * happening in the ViewController, we best do this here
             */
            if(this.getParent()!=null) {
                if (this.getParent() instanceof ViewGroup) {
                    ((ViewGroup) this.getParent()).removeView(this);
                } else {
                    ((WindowManager) activity.getSystemService(Service.WINDOW_SERVICE)).removeView(this);
                }
            }

            if(viewParent!=null) {

                viewParent.addView(ProgressRibbon.this);

                if(ribbonStateChangeListener !=null) {
                    ribbonStateChangeListener.onRibbonAttached(true);
                }

            } else {
                WindowManager windowManager = activity.getWindowManager();
                windowManager.addView(ProgressRibbon.this, getLayoutParams());

                if(ribbonStateChangeListener !=null) {
                    ribbonStateChangeListener.onRibbonAttached(false);
                }
            }
        }

        if(ribbonData.animationType!=DO_NOT_ANIMATE && !skipAnimation) {
            animateShowHide(ANIMATION_SHOW);
        } else {
            isShowing=true;
            setVisibility(View.VISIBLE);
            if(ribbonStateChangeListener !=null) {
                ribbonStateChangeListener.onRibbonShow();
            }
        }
    }

    /**
     * Will hide the Ribbon, with a delay , if one is set
     */
    private void hideView(){
        if(ribbonData.animationType!=DO_NOT_ANIMATE ){
            animateShowHide(ANIMATION_HIDE);
        } else {
            isShowing=false;
            ProgressRibbon.this.setVisibility(View.GONE);
            if(ribbonStateChangeListener !=null) {
                ribbonStateChangeListener.onRibbonHide();
            }
        }
    }

    /**
     * Will show the Ribbon, with the set showdelay, or immediately, if none is set
     */

    public void show(){
        show(ribbonData.showDelay);
    }

    /**
     * Will show the ribbon with the ad-hoc declared showDelay in milliseconds, even if no permanent showDelay is set
     * @param delay showDealy in millisec
     */

    public void show(@IntRange(from=0) int delay){

        setVisibility(View.GONE);

        if(delay>0) {
            showHandler = new Handler();
            runner = new Runnable() {
                public void run() {
                    attachAndShowView(false);
                }
            };
            showHandler.postDelayed(runner, delay);

        } else {
            attachAndShowView(false);
        }
        if(ribbonStateChangeListener !=null) {
            ribbonStateChangeListener.onRibbonSignalledToShow(delay);
        }

    }

    /**
     * Show the ribbon immediately, ignoring any showDelay, even if one is set
     */
    public void showNoDelay() {

        attachAndShowView(true);

        if(ribbonStateChangeListener !=null) {
            ribbonStateChangeListener.onRibbonSignalledToShow(0);
        }
    }

    /**
     * Will hide the Ribbon, with the set hideDelay, or immediately, if none is set
     */
    public void hide(){
        hide(ribbonData.hideDelay);
    }

    /**
     * Will hide the ribbon with the ad-hoc supplied hideDelay even without a permanent hideDelay set
     * @param delay Hide delay in millisec
     */
    public void hide(@IntRange(from=0) int delay) {

        if (delay > 0) {

            hideHandler = new Handler();
            hideHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (showHandler != null) {
                        showHandler.removeCallbacks(runner);
                    }
                    hideView();
                }

            }, delay);

        } else {

            if (showHandler != null) {
                showHandler.removeCallbacks(runner);
            }

            hideView();
        }

        if(ribbonStateChangeListener !=null) {
            ribbonStateChangeListener.onRibbonSignalledToHide(delay);
        }
    }

    /**
     * Hide the ribbon immediately, ignoring any hideDelay, even if one is set
     */
    public void hideNoDelay(){

        if (showHandler != null) {
            showHandler.removeCallbacks(runner);
        }

        if(ribbonStateChangeListener !=null) {
            ribbonStateChangeListener.onRibbonSignalledToHide(0);
        }

        hideView();
    }


    /**
     * Internal function to completely detach the possibly dynaically created) Ribbon from the parent view,
     * if there is one, or the window, if there isn't.
     *
     * As usual, if you can see this text from another class, somebody has forgotten to update the javaDoc
     */
    private void detachView(){

        if(viewParent!=null) {
            if(getParent()==viewParent) {
                viewParent.removeView(ProgressRibbon.this);
            }
        } else {
            if(getWindowToken() != null){
                activity.getWindowManager().removeView(ProgressRibbon.this);
            }
        }

        isShowing=false;

        if(ribbonStateChangeListener !=null) {
            ribbonStateChangeListener.onRibbonRemoved();
        }
    }


    /**
     * Used (mainly) for completely removing a dynamically created Ribbon, ignoring any hideDelay,
     * but respecting huide animation settings
     */
    public void removeDynamicRibbon() {
        if (isDynamicallyCreated) {
            hideView();
            detachView();
        }
    }

    /*******************************Save & Restore State kinda stuff******************************/

    /**
     * Identifier for the state of the super class.
     */
    private static String STATE_SUPER_CLASS = "SuperClass";

    /**
     * The usual business. Put everything in a bundle
     * @return the bundle with everything in it
     */
    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();


        Log.e("PR", "saving instance state");


        bundle.putParcelable(STATE_SUPER_CLASS,
                super.onSaveInstanceState());

        bundle.putString("progressText", progressText);
        bundle.putInt("ribbonBorderColor", ribbonData.ribbonBorderColor);
        bundle.putInt("backgroundColor", ribbonData.backgroundColor);
        bundle.putInt("showDelay", ribbonData.showDelay);
        bundle.putInt("hideDelay", ribbonData.hideDelay);
        bundle.putInt("min", ribbonData.min);
        bundle.putInt("max", ribbonData.max);
        bundle.putInt("progress", ribbonData.progress);
        bundle.putInt("secondaryProgress", ribbonData.secondaryProgress);
        bundle.putInt("ribbonMarginTop", ribbonData.ribbonMarginTop);
        bundle.putInt("ribbonMarginBottom", ribbonData.ribbonMarginBottom);
        bundle.putInt("ribbonBorderSize", ribbonData.ribbonBorderSize);
        bundle.putInt("progressTextColor", ribbonData.progressTextColor);
        bundle.putInt("progressBarStyle", ribbonData.progressBarStyle);
        bundle.putInt("animationDuration", ribbonData.animationDuration);
        bundle.putInt("animationType", ribbonData.animationType);
        bundle.putInt("ribbonPaddingTop", ribbonData.ribbonPaddingTop);
        bundle.putInt("ribbonPaddingBottom", ribbonData.ribbonPaddingBottom);
        bundle.putInt("progressTextSize", ribbonData.progressTextSize);
        bundle.putFloat("ribbonElevation", ribbonData.ribbonElevation);
        bundle.putFloat("ribbonBorderRadius", ribbonData.ribbonBorderRadius);
        bundle.putBoolean("isIndeterminate", ribbonData.isIndeterminate);
        bundle.putBoolean("xmlMarginTopPercentIsSet", xmlMarginTopPercentIsSet);
        bundle.putBoolean("xmlMarginBottomPercentIsSet", xmlMarginBottomPercentIsSet);
        bundle.putBoolean("isInDialogueMode", ribbonData.isInDialogueMode);
        bundle.putBoolean("isDynamicallyCreated", isDynamicallyCreated);
        bundle.putBoolean("blocksUnderlying", ribbonData.blocksUnderlying);
        bundle.putBoolean("isInOrphanMode", ribbonData.isInOrphanMode);
        bundle.putBoolean("isProgressFrozen", isProgressFrozen);
        bundle.putBoolean("textBesideBar", ribbonData.textBesideBar);
        bundle.putBoolean("marginIsPercentage", ribbonData.marginIsPercentage);
        bundle.putBoolean("animationInProgress", animationInProgress);
        bundle.putBoolean("layoutIsRTL", layoutIsRTL);
        bundle.putBoolean("isShowing", isShowing);
        bundle.putBoolean("doNotShowOnAttachFromXML", doNotShowOnAttachFromXML);

        return bundle;
    }

    /**
     * The usual business. get everything from a bundle
     * @return the bundle with everything in it
     */
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle)state;


            Log.e("PR", "restoring instance state");

            super.onRestoreInstanceState(bundle
                    .getParcelable(STATE_SUPER_CLASS));

            progressText=bundle.getString("progressText");
            ribbonData.ribbonBorderColor =bundle.getInt("ribbonBorderColor");
            ribbonData.backgroundColor=bundle.getInt("backgroundColor");
            ribbonData.showDelay=bundle.getInt("showDelay");
            ribbonData.hideDelay=bundle.getInt("hideDelay");
            ribbonData.min=bundle.getInt("min");
            ribbonData.max=bundle.getInt("max");
            ribbonData.progress=bundle.getInt("progress");
            ribbonData.secondaryProgress=bundle.getInt("secondaryProgress");
            ribbonData.progressBarStyle =bundle.getInt("progressBarStyle");
            ribbonData.animationDuration=bundle.getInt("animationDuration");
            ribbonData.animationType=bundle.getInt("animationType");
            ribbonData.ribbonPaddingTop=bundle.getInt("ribbonPaddingTop");
            ribbonData.ribbonPaddingBottom=bundle.getInt("ribbonPaddingBottom");
            ribbonData.ribbonMarginTop=bundle.getInt("ribbonMarginTop");
            ribbonData.ribbonMarginBottom=bundle.getInt("ribbonMarginBottom");
            ribbonData.ribbonBorderSize=bundle.getInt("ribbonBorderSize");
            ribbonData.progressTextColor=bundle.getInt("progressTextColor");
            ribbonData.progressTextSize=bundle.getInt("progressTextSize");
            ribbonData.ribbonBorderRadius=bundle.getFloat("ribbonBorderRadius");
            ribbonData.ribbonElevation=bundle.getFloat("ribbonElevation");
            xmlMarginTopPercentIsSet=bundle.getBoolean("xmlMarginTopPercentIsSet");
            xmlMarginBottomPercentIsSet=bundle.getBoolean("xmlMarginBottomPercentIsSet");
            ribbonData.isIndeterminate=bundle.getBoolean("isIndeterminate");
            isDynamicallyCreated=bundle.getBoolean("isDynamicallyCreated");
            ribbonData.blocksUnderlying=bundle.getBoolean("blocksUnderlying");
            ribbonData.isInOrphanMode=bundle.getBoolean("isInOrphanMode");
            ribbonData.isInDialogueMode=bundle.getBoolean("isInDialogueMode");
            ribbonData.marginIsPercentage=bundle.getBoolean("marginIsPercentage");
            isProgressFrozen=bundle.getBoolean("isProgressFrozen");
            ribbonData.textBesideBar =bundle.getBoolean("textBesideBar");
            animationInProgress=bundle.getBoolean("animationInProgress");
            layoutIsRTL=bundle.getBoolean("layoutIsRTL");
            isShowing=bundle.getBoolean("isShowing");
            doNotShowOnAttachFromXML=bundle.getBoolean("doNotShowOnAttachFromXML");

            setRibbonAttributes(true);

        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * He who saves the state of child views with dispatchSaveInstanceState has forgotten the face of his father
     * I do not save the state of child views with dispatchSaveInstanceState
     * I save the state of child views with my heart (or in onRestoreInstanceState, manually)
     *
     * @param container If you want to mess with this, you should know what it is. otherwise, don't touch it
     */
    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchFreezeSelfOnly(container);
    }

    /**
     * He who restores the state of child view with dispatchSaveInstanceState has forgotten the face of his father
     * I do not restore the state of child views with dispatchSaveInstanceState
     * I restore the state of child views with my heart (or in onRestoreInstanceState, manually)
     *
     * @param container If you want to mess with this, you should know what it is. otherwise, don't touch it
     */
    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchThawSelfOnly(container);
    }




    /*********************************Interfaces interfacing**************************************/

    public interface RibbonStateChangeListener {
        void onRibbonSignalledToShow(int showDelay);
        void onRibbonShow();
        void onRibbonIndeterminateStatusChanged(boolean ribbonIsIndeternimate);
        void onRibbonProgressStarted(int startValue);
        void onRibbonProgressStopped(int stopValue);
        void onRibbonProgressChange(int currentValue);
        void onRibbonSignalledToHide(int hideDelay);
        void onRibbonHide();
        void onRibbonAttached(boolean hasViewParent);
        void onRibbonRemoved();
    }

    public interface OnIndeterminateStatusChangeListener {
        void onRibbonIndeterminateStatusChanged(boolean ribbonIsIndeternimate);
    }

    public interface OnStartListener {
        void onRibbonProgressStarted(int startValue);
    }

    public interface OnStopListener {
        void onRibbonProgressStopped(int stopValue);
    }

    public interface OnRibbonAttachDetachListener {
        void onRibbonAttached(boolean hasViewParent);
        void onRibbonRemoved();
    }

    public interface OnRibbonShowListener{
        void onRibbonSignalledToShow(int showDelay);
        void onRibbonShow();
    }

    public interface OnRibbonHideListener{
        void onRibbonSignalledToHide(int hideDelay);
        void onRibbonHide();
    }

    public interface OnRibbonProgressUpdateListener{
        void onRibbonProgressChange(int currentValue);
    }


    /*************************Interface getters and setters*****************************************/

    public void setOnIndeterminateStatusChangeListener(OnIndeterminateStatusChangeListener listener){
        onIndeterminateStatusChangeListener =listener;
    }

    public OnIndeterminateStatusChangeListener getOnIndeterminateStatusChangeListener(){
        return onIndeterminateStatusChangeListener;
    }

    public void setOnStartListener(OnStartListener listener){
        onStartListener =listener;
    }

    public OnStartListener getOnStartListener(){
        return onStartListener;
    }

    public void setOnStopListener(OnStopListener listener){
        onStopListener =listener;
    }

    public OnStopListener getOnStopListener(){
        return onStopListener;
    }

    public void OnRibbonAttachDetachListener(OnRibbonAttachDetachListener listener){
        onRibbonAttachDetachListener =listener;
    }

    public OnRibbonAttachDetachListener getOnRibbonAttachDetachListener(){
        return onRibbonAttachDetachListener;
    }

    public void setOnRibbonShowListener(OnRibbonShowListener listener){
        onRibbonShowListener =listener;
    }

    public OnRibbonShowListener getRibbonShowListener(){
        return onRibbonShowListener;
    }

    public void setOnRibbonHideListener(OnRibbonHideListener listener){
        onRibbonHideListener = listener;
    }

    public OnRibbonHideListener getRibbonHideListener(){
        return onRibbonHideListener;
    }

    public void setOnRibbonProgressUpdateListener(OnRibbonProgressUpdateListener listener){
        onRibbonProgressUpdateListener =listener;
    }

    public OnRibbonProgressUpdateListener getOnRibbonProgressUpdateListener(){
        return onRibbonProgressUpdateListener;
    }

    public void setOnRibbonStateChangedListener(RibbonStateChangeListener listener){
        ribbonStateChangeListener =listener;
    }

    public RibbonStateChangeListener getRibbonStateChangeListener(){
        return ribbonStateChangeListener;
    }


    /*RibbonData class representing, surprisingly, the Ribbon's data. Wow. Such obvious. So smooth*/

    public class RibbonData {


        /**
         * Default values, without any values (some need ot be calculated, so they get assigned their
         * values in the constructor
         */
        public final int DEFAULT_RIBBON_MIN,
                DEFAULT_RIBBON_MAX,
                DEFAULT_ANIMATION_DURATION,
                DEFAULT_RIBBON_PADDING,
                DEFAULT_RIBBON_BORDER_SIZE,
                DEFAULT_RIBBON_ELEVATION,
                DEFAULT_RIBBON_BORDER_COLOR,
                DEFAULT_RIBBON_TEXT_COLOR,
                DEFAULT_RIBBON_TEXT_SIZE,
                DEFAULT_RIBBON_BG_COLOR,
                DEFAULT_RIBBON_MARGIN,
                DEFAULT_RIBBON_HIDE_DELAY,
                DEFAULT_RIBBON_SHOW_DELAY;
        public final float DEFAULT_RIBBON_BORDER_RADIUS;


        /**
         * Members representing actual values
         */
        private int ribbonBorderColor;
        private int backgroundColor;
        private int showDelay;
        private int hideDelay;
        private int min;
        private int max;
        private int progress;
        private int secondaryProgress;
        private int progressBarStyle;
        private int animationDuration;
        private int animationType;
        private int ribbonPaddingTop;
        private int ribbonPaddingBottom;
        private int ribbonMarginTop;
        private int ribbonMarginBottom;
        private int ribbonBorderSize;
        private int progressTextColor;
        private int progressTextSize;
        private float ribbonBorderRadius;
        private float ribbonElevation;
        private boolean isIndeterminate;
        private boolean blocksUnderlying;
        private boolean isInOrphanMode;
        private boolean isInDialogueMode;
        private boolean textBesideBar;
        private boolean marginIsPercentage;
        private boolean reportProgressAsMaxPercent;

        public RibbonData(Context context, boolean setDefaults){

            DEFAULT_RIBBON_MIN=0; //You don't say...
            DEFAULT_RIBBON_MAX=100; //Makes no sense, but why not (I mean that's the default of defaults anyway, ain't it?)
            DEFAULT_ANIMATION_DURATION=300; //This looks like a reasonable default animation time
            DEFAULT_RIBBON_PADDING=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics()));//A decent enough default
            DEFAULT_RIBBON_BORDER_SIZE=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics()));//Default border is 1dp thick
            DEFAULT_RIBBON_ELEVATION=24;//A decent enough default. Probably
            DEFAULT_RIBBON_BORDER_COLOR = resolveAppThemeColour(android.R.attr.colorPrimary); //Should pull it in frrom the host app, if non found, use our own default
            DEFAULT_RIBBON_TEXT_COLOR = resolveAppThemeColour(android.R.attr.textColorPrimary); //Pretty much the same across devices, but shoudl conform to the app's settings if differen
            DEFAULT_RIBBON_TEXT_SIZE = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, context.getResources().getDisplayMetrics())); //Decent enough text size
            DEFAULT_RIBBON_BG_COLOR = context.getResources().getColor(android.R.color.white); //White default is limited in scope. An isue is open to change this
            DEFAULT_RIBBON_MARGIN=0; //No margin for old man
            DEFAULT_RIBBON_HIDE_DELAY=0; //Delays should be set when needed
            DEFAULT_RIBBON_SHOW_DELAY=0; //Delays should be set when needed
            DEFAULT_RIBBON_BORDER_RADIUS=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8.0f, context.getResources().getDisplayMetrics());//A decent enough default

            /**
             * It's conditional, because... Well, because we are not sure we really want everything ot be defaulted, are we?
             */
            if(setDefaults) {
                applyDefaults();
            }
        }

        public void applyDefaults(){

            min = DEFAULT_RIBBON_MIN;
            progress = min;
            secondaryProgress=min;
            max = DEFAULT_RIBBON_MAX;
            backgroundColor= DEFAULT_RIBBON_BG_COLOR;
            progressBarStyle = BAR_ROUND;
            animationDuration=DEFAULT_ANIMATION_DURATION;
            animationType= DO_NOT_ANIMATE;
            ribbonPaddingTop=DEFAULT_RIBBON_PADDING;
            ribbonPaddingBottom=DEFAULT_RIBBON_PADDING;
            ribbonMarginTop=DEFAULT_RIBBON_MARGIN;
            ribbonMarginBottom=DEFAULT_RIBBON_MARGIN;
            ribbonBorderSize=DEFAULT_RIBBON_BORDER_SIZE;
            ribbonBorderRadius=DEFAULT_RIBBON_BORDER_RADIUS;
            ribbonBorderColor = DEFAULT_RIBBON_BORDER_COLOR;
            ribbonElevation=DEFAULT_RIBBON_ELEVATION;
            progressTextColor=DEFAULT_RIBBON_TEXT_COLOR;
            showDelay=DEFAULT_RIBBON_SHOW_DELAY;
            hideDelay=DEFAULT_RIBBON_HIDE_DELAY;
            progressTextSize=DEFAULT_RIBBON_TEXT_SIZE;
            textBesideBar =false;
            blocksUnderlying =true;
            isInOrphanMode =true;
            isIndeterminate = true;
            isInDialogueMode=false;
            marginIsPercentage=false;
            reportProgressAsMaxPercent=false;
        }
    }
}