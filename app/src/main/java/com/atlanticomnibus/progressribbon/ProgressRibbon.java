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

import androidx.annotation.RequiresApi;
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

    /**************************************Constants***********************************************/

    /**
     * <p>This value is taken form the original ProgressBar, it's used to calculate secondary progress
     * for the round variant. No touchy-touchy.</p>
     */
    private static final int MAX_LEVEL = 10000;

    /**
     * <p>Plain ole' integers holding values described by their names</p>
     */
    public static final int BAR_ROUND = 0,
            BAR_HORIZONTAL=1,
            DO_NOT_ANIMATE = 0,
            ANIMATE_FADE =1,
            ANIMATE_SCALE=2,
            ANIMATE_SCALE_FADE = 3,
            PARENT_HEIGHT_PERCENT=999;

    /**
     * <p>Boolean replacements. 1=true, 0=false, obviously</p>
     */
    public static final int FREEZE_PROGRESS=1,
            UNFREEZE_PROGRESS=0,
            TEXT_BESIDE_BAR=1,
            TEXT_UNDER_BAR=0,
            INDETERMINATE=1,
            DETERMINATE =0;

    /**
     * <p>Internal stuff. Stop poking your nose here</p>
     */
    private static final int PROGRESS_BAR_ID=666,
            ANIMATION_HIDE=0,
            ANIMATION_SHOW=1,
            MARGIN_TOP=0,
            MARGIN_BOTTOM=1,
            PADDING_TOP=0,
            PADDING_BOTTOM=1;

    /**********************************Enforcing enforcements**************************************/

    /**
     * These will ensure that you don't use incorrect values for stuff, coz the linter will lint you
     */

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
    @IntDef({TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP,
            TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ValidSizeUnit{}

    /** @hide */
    @IntDef({TypedValue.COMPLEX_UNIT_PX, TypedValue.COMPLEX_UNIT_DIP, TypedValue.COMPLEX_UNIT_SP,
            TypedValue.COMPLEX_UNIT_PT, TypedValue.COMPLEX_UNIT_IN, TypedValue.COMPLEX_UNIT_MM,
            PARENT_HEIGHT_PERCENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RibbonMarginUnit{}

    /** @hide */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @IntDef({android.R.attr.colorPrimary, android.R.attr.colorAccent, android.R.attr.textColorPrimary, android.R.attr.windowBackground})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ValidColorAttrIds{}


    /*************************************Viewable views*******************************************/

    /**
     * <p>A {@link LinearLayout} that contains the {@link ProgressBar} and the {@link TextView} of the
     * compound widget</p>
     */
    private LinearLayout ribbonContainer;

    /**
     * <p>{@link TextView} to display the progress text</p>
     */
    private TextView progressTextView;

    /**
     * <p>Will hold a dynamically created {@link ProgressBar} to display progress
     */
    private ProgressBar progressBar;

    /**
     * <p>The parent parent {@link ViewGroup} the XML inflated, or dynamically created (non-orphan)
     * ProgressRibbon is attached to</p>
     */
    private ViewGroup viewParent;


    /**********************************Various Variables*******************************************/

    private Activity activity; //This is self explanatory
    private String progressText; //The text of the progress

    /**
     * <p>The {@link RibbonData} class separates the progressRibbon's data from its representation.
     * It's an internal class for convenience and to allow for a self-contained main class</p>
     */
    private RibbonData ribbonData;
    //Whether the {@link ProgressRibbon} was added from XML or Java (true if from Java)
    private boolean isDynamicallyCreated;
    //Whether the progress is "frozen (i.e. no visual updates allowed). not sure f it even makes sense ot have this
    private boolean isProgressFrozen;
    //Just to be able to check whether it had changed. This is used when top/bottom margin are set as a %
    private int parentHeight;

    //ToDo: the below two booleans need to be sanitiesd, this was a quick fix and not a permanent solution
    //The top margin set from XML is of percentage value
    private boolean xmlMarginTopPercentIsSet;
    //The bottom margin set from XML is of percentage value
    private boolean xmlMarginBottomPercentIsSet;

    //Whether the {@link ProgressRibbon} is being shown
    private boolean isShowing;
    //Failsafe to prevent changing animation settings when it's being shown
    private boolean animationInProgress;
    //To know when layout is right to left, so that cmponents can be added in the right order
    private boolean layoutIsRTL;

    //if, for any reason, you1d not want to XML-declared {@link ProgressRibbon} to appear until you explicitly show it...
    private boolean doNotShowOnAttachFromXML;


    /*******************************Listeners that listen******************************************/

    /**
     * Various liteners the {@link ProgressRibbon} can use. Their names should be self-explanatory
     */
    private RibbonStateChangeListener ribbonStateChangeListener;
    private OnIndeterminateStatusChangeListener onIndeterminateStatusChangeListener;
    private OnStartListener onStartListener;
    private OnStopListener onStopListener;
    private OnRibbonAttachDetachListener onRibbonAttachDetachListener;
    private OnRibbonShowListener onRibbonShowListener;
    private OnRibbonHideListener onRibbonHideListener;
    private OnRibbonProgressUpdateListener onRibbonProgressUpdateListener;


    /**
     * <p>Chainable static solution for one-liners. Depending on whether a parent {@link ViewGroup} is supplied
     * it will create an orphan or an attached {@link ProgressRibbon}. Orphan means it has no parent (poor little orphan)
     * and is attached to the Window instead, much like a modal dialogue.</p>
     *
     * @param activity The activity for context
     * @param parentView Optional parent {@link ViewGroup
     * @return A new {@link ProgressRibbon} instance
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
     * <p>Creates a {@link ProgressRibbon} attached to the Window directly, with no parent {@lnk ViewGroup}, i.e. in  orphan mode.
     * It's called an orphan, for it has no parent :,(</p>
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
     * <p>A dynamic constructor that allows for creating a {@link ProgressRibbon} with a few settable values.</p>
     *
     * ToDo: Consider deprecating this before v1.0 is out, since the "standard" constructor and {@link ProgressRibbon#newInstance}
     *       can now be used ni a chain style
     *
     * @param activity - Need an activity in case it's an orphan view
     * @param parentView - Optional. If null, it will be in orphan mode, otherwise attached to the {@link ViewGroup} supplied
     * @param blocksUnderlyingView - Whether the {@link ProgressRibbon} allows touches to views underneath it in the same {@link ViewGroup},
     *                               or the whole window (in orphan mode)
     * @param animationType - Whether to animate appearing/disappearing and how
     * @param progressBarStyle - Flat or round {@link ProgressBar}
     * @param indeterminateState - {@link ProgressRibbon#INDETERMINATE} ({@value ProgressRibbon#INDETERMINATE}) or
     *                             {@link ProgressRibbon#DETERMINATE} ({@value ProgressRibbon#INDETERMINATE})
     *                             style {@link ProgressBar} (round determinate also available)
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
     * Creates {@link ProgressRibbon} with default values, attached to a parent {@link ViewGroup}
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
     * <p>Gets the attributes from an XML defined {@link ProgressRibbon} </p>
     *
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
            ribbonData.progress=a.getInteger(R.styleable.ProgressRibbon_ribbonProgress, 0);
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
     * <p>Overridden so that measured height of the parent can be used ot determine correct percentage of margins if
     * margin is set as a percentage</p>
     *
     * @param widthMeasureSpec See super method
     * @param heightMeasureSpec See super method
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
     * <p>Inflates {@link ProgressRibbon} compound {@link View}s from their default layout file {@link com.atlanticomnibus.progressribbon.R.layout#progress_ribbon}</p>
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
     * <p>Called automatically when {@link ProgressRibbon} is added form XLM. has to be explicitly called if
     * constructor was called from Java code</p>
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
     * <p>As a {@link View}'s style cannot be dynamically changed, the {@link ProgressBar} element needs to be recreated
     * when changing between flat and round styles. That being the case, it's better dynamically create them in the first place.</p>
     *
     * @param style Style of the {@link ProgressBar} {@link ProgressRibbon#BAR_ROUND} ({@value ProgressRibbon#BAR_ROUND}), or
     * {@link ProgressRibbon#BAR_HORIZONTAL} ({@value ProgressRibbon#BAR_HORIZONTAL}) are the only accepted alues
     * @param indeterminate boolean switch, marking whether the {@link ProgressBar} should be indeterminate
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
            //Horizontal bars are straighforward
            progressBar= new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            params= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity=Gravity.CENTER;
        } else {

            if(indeterminate){
                progressBar= new ProgressBar(getContext(), null, android.R.attr.progressBarStyle);
            } else {
                //Curcular determinate bars will be set as horizontal with a special drawable applied to them
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
     * <p>One interface to rule them all. This is an internal listener by default, which handles all
     * the smaller ones, if they are attached. If overridden, the smaller ones never get called,
     * but all the functions will get exposed in one central location to the caller.</p>
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
     * <p>Set up all sorts of attributes of the {@link ProgressRibbon}. This can get called independently from {@link ProgressRibbon#onFinishInflate()} (as in restoring state)</p>
     * <br />
     * <p>If called form {@link ProgressRibbon#onRestoreInstanceState(Parcelable)}, the {@link ProgressRibbon} has to be shown again, so we need to know when it's called from that method.</p>
     *
     * @param fromRestoreState Whether it's called from {@link ProgressRibbon#onRestoreInstanceState(Parcelable)}
     */
    private void setRibbonAttributes(boolean fromRestoreState){

        //Laíyout parameters will be set first
        if(isDynamicallyCreated){
            setLayoutParamsInternal();
        }

        //Create a ProgressBar
        initProgressbar(ribbonData.progressBarStyle, ribbonData.isIndeterminate);

        //Set the background and text attributes
        setBackgroundColor(getResources().getColor(android.R.color.transparent));
        setProgressTextColour(ribbonData.progressTextColor);
        setProgressTextSize(TypedValue.COMPLEX_UNIT_PX, ribbonData.progressTextSize);
        setRibbonTextPosition(ribbonData.textBesideBar ? TEXT_BESIDE_BAR:TEXT_UNDER_BAR);

        //Min and max values for the progressbar, obviously
        setMin(ribbonData.min);
        setMax(ribbonData.max);

        //Does what it says, but does it in a version agnostic manner (works on pre-lollipop too)
        setElevation(ribbonData.ribbonElevation);

        /**
         * Set the ribbon to be either a real ribbon, or to resemble a dialogue
         * This will also set the border colour and thickness, and border radius if in dialogue mode,
         * so those do not mneed to be called separately
         */
        setRibbonInDialogueMode(ribbonData.isInDialogueMode);



        //Set margins and paddings
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


        //Only works after initialising progressbar, because it also sets layoutDirection on that
        if(getLayoutDirection()!=LAYOUT_DIRECTION_LTR) {
            setLayoutDirection(getLayoutDirection());
        } else {
            setLayoutDirection(getConfigLayoutDirection());
        }

        //Set various progress related things, like progress value, text and text colour
        progressBar.setProgress(ribbonData.progress);
        progressTextView.setText(progressText);
        setProgressTextColour(ribbonData.progressTextColor);


        //The Ribbon itself always blocks touches
        ribbonContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        //If coming from state restore, we need to re-show the ribbon, without any applicable delay
        if(fromRestoreState && isShowing){
            showNoDelay();
        } else if(!isDynamicallyCreated && !doNotShowOnAttachFromXML) {
            /**
             * This is so that XML added ribbon can be automatically shown after setup is complete
             * If {@link ProgressRibbon#doNotShowOnAttachFromXML} is true, the ribbon never ghets aut-shown,
             * and must be shown from code
             */
            show();
        } else if (!isDynamicallyCreated && doNotShowOnAttachFromXML){
            //XML added ribbon should be hidden under all circumstances when {@link ProgressRibbon#doNotShowOnAttachFromXML} is true
            hideNoDelay();
        }
    }

    /**
     * <p>An attempt to determine the type of LayoutParams needed
     * Probably needs some work/attention, there might be a more efficient solution to do this</p>
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
     * <p>The transparent overly only blocks touches when it's set to do so</p>
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return ribbonData.blocksUnderlying;
    }


    /***************************************Getters and setters************************************/


    /**
     * <p>Get the raw {@link RibbonData} object, containing all the... wait for it... RIBBON DATA! Yay!
     * Individual bits of the {@link RibbonData} object, including default constants are not accessible
     * from proper getter methods</p>
     *
     * @return the {@link RibbonData} object
     */
    public RibbonData getRibbonData(){
        return ribbonData;
    }


    /**
     * <p>Exposes the internal {@link ProgressBar} widget, to set or whatever is not exposed explicitly in the main widget</p>
     *
     * @return The dynamically created {@link ProgressBar} widget that is part of the compound layout
     */
    public ProgressBar getProgressbar(){
        return progressBar;
    }

    /**
     * <p>Exposes the internal {@link TextView} holding the progress text to allow fine settings manipulation</p>
     *
     * @return The {@link TextView} widget that is part of the compound layout
     */
    public TextView getProgressTextView(){
        return progressTextView;
    }

    /**
     * <p>Set the {@link ProgressRibbon}'s top padding (the distance between it's top border and the topmost element(s))
     * in {@link TypedValue#COMPLEX_UNIT_DIP}</p>
     *
     * @param paddingTop Padding value as {@link TypedValue#COMPLEX_UNIT_DIP}. It must be a non-negative integer
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonPaddingTop(@IntRange(from=0) int paddingTop){
        return setRibbonPadding(PADDING_TOP, TypedValue.COMPLEX_UNIT_DIP, paddingTop);
    }

    /**
     * <p>Set the {@link ProgressRibbon}'s top padding (the distance between it's top border and the topmost element(s))
     * in the unit of your choice (use {@link TypedValue} constants for units) </p>
     *
     * @param unit <p>Accepted units are:
     *              <ul>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *              </ul>
     *             </p>
     * @param paddingTop The padding value, which must be a non-negative integer
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonPaddingTop(@ValidSizeUnit int unit, @IntRange(from=0) int paddingTop){
        return setRibbonPadding(PADDING_TOP, unit, paddingTop);
    }

    /**
     * <p>Set the {@link ProgressRibbon}'s bottom padding (the distance between it's bottom border and the bottommost element(s))
     * in {@link TypedValue#COMPLEX_UNIT_DIP}</p>
     *
     * @param paddingBottom Padding value as {@link TypedValue#COMPLEX_UNIT_DIP}, must be a non-negative integer
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonPaddingBottom(@IntRange(from=0) int paddingBottom){
        return setRibbonPadding(PADDING_BOTTOM, TypedValue.COMPLEX_UNIT_DIP, paddingBottom);
    }

    /**
     * <p>Set the {@link ProgressRibbon}'s bottom padding (the distance between it's bottom border and the bottommost element(s))
     * in the unit of your choice (use {@link TypedValue} constants for units) </p>
     *
     * @param unit <p>Accepted units are:
     *              <ul>
     *                 <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                 <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                 <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                 <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                 <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                 <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *             </p>
     * @param paddingBottom The padding value, which must be a non-negative integer
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonPaddingBottom(@ValidSizeUnit int unit, @IntRange(from=0) int paddingBottom){
        return setRibbonPadding(PADDING_BOTTOM, unit, paddingBottom);
    }

    /**
     * <p>Internal method to actually set the {@link ProgressRibbon}'s paddings</p>
     *
     * @param position Accepted values are {@link ProgressRibbon#PADDING_BOTTOM} ({@value ProgressRibbon#PADDING_BOTTOM}) or {@link ProgressRibbon#PADDING_TOP} ({@link ProgressRibbon#PADDING_TOP})
     * @param unit <p>Accepted units are:
     *              <ul>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *              </ul>
     *             </p>
     * @param padding Non-negative integer value of the padding to be set
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Get the {@link ProgressRibbon}'s internal paddings, both bottom and top in {@link TypedValue#COMPLEX_UNIT_DIP}, as an integer array</p>
     *
     * @return <p></p>int array of padding with <code>[0]</code> representing {@link RibbonData#ribbonPaddingTop} and <code>[1]</code> representing {@link RibbonData#ribbonPaddingBottom} in {@link TypedValue#COMPLEX_UNIT_DIP}</p>
     */
    public int[] getRibbonPadding(){
        int[] result= {Math.round(ribbonData.ribbonPaddingTop / getResources().getDisplayMetrics().density),  Math.round(ribbonData.ribbonPaddingBottom/ getResources().getDisplayMetrics().density)};
        return result;
    }

    /**
     * <p>Get the {@link ProgressRibbon}'s internal top padding in {@link TypedValue#COMPLEX_UNIT_DIP}</p>
     *
     * @return the top padding in {@link TypedValue#COMPLEX_UNIT_DIP}
     */
    public int getRibbonTopPadding(){
        return Math.round(ribbonData.ribbonPaddingTop / getResources().getDisplayMetrics().density);
    }

    /**
     * <p>Get the {@link ProgressRibbon}'s internal top padding in {@link TypedValue#COMPLEX_UNIT_DIP}</p>
     *
     * @return the bottom padding in in {@link TypedValue#COMPLEX_UNIT_DIP}
     */
    public int getRibbonBottomPadding(){
        return Math.round(ribbonData.ribbonPaddingBottom / getResources().getDisplayMetrics().density);

    }

    /**
     * <p>>Set a top margin on the {@link ProgressRibbon} in in {@link TypedValue#COMPLEX_UNIT_DIP}, which will push the {@link ProgressRibbon} downwards</p
     *
     * @param marginTop The top margin value in in {@link TypedValue#COMPLEX_UNIT_DIP}
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonMarginTop(@IntRange(from=0) int marginTop){
        return setRibbonVerticalMargin(MARGIN_TOP, TypedValue.COMPLEX_UNIT_DIP, marginTop);
    }

    /**
     * <p>Set a top margin on the {@link ProgressRibbon} in any valid unit, which will push the {@link ProgressRibbon} downwards.</p>
     * <br />
     * <p>As a convenience, the margin can be set as a percentage of the containing layout's height,
     * which will be automatically calculated and applied. Use {@link TypedValue} unit constants or or {@link ProgressRibbon#PARENT_HEIGHT_PERCENT}</p>
     *
     * @param unit <p>Accepted values are:
     *                <ul>
     *                    <li>{@link ProgressRibbon#PARENT_HEIGHT_PERCENT}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *                </ul>
     *             </p>
     * @param marginTop the value of the top margin as a non-negative integer
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonMarginTop(@RibbonMarginUnit int unit, @IntRange(from=0) int marginTop){
        return setRibbonVerticalMargin(MARGIN_TOP, unit, marginTop);
    }

    /**
     * <p>Set a bottom margin on the {@link ProgressRibbon} in {@link TypedValue#COMPLEX_UNIT_DIP}, which will push the {@link ProgressRibbon} upwards</p>
     *
     * @param marginBottom The bottom margin value in {@link TypedValue#COMPLEX_UNIT_DIP}
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonMarginBottom(@IntRange(from=0) int marginBottom){
        return setRibbonVerticalMargin(MARGIN_BOTTOM, TypedValue.COMPLEX_UNIT_DIP, marginBottom);
    }


    /**
     * <p>Set a bottom margin on the {@link ProgressRibbon} in any valid unit, which will push the {@link ProgressRibbon} upwards.</p>
     * <br />
     * <p>As a convenience, the margin can be set as a percentage of the containing layout's height,
     * which will be automatically calculated and applied. Use {@link TypedValue} unit constants or or {@link ProgressRibbon#PARENT_HEIGHT_PERCENT}</p>
     *
     * @param unit <p>Accepted values are:
     *                <ul>
     *                    <li>{@link ProgressRibbon#PARENT_HEIGHT_PERCENT}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *                </ul>
     *             </p>
     * @param marginBottom the value of the top margin as a non-negative integer
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonMarginBottom(@RibbonMarginUnit int unit, @IntRange(from=0) int marginBottom){
        return setRibbonVerticalMargin(MARGIN_BOTTOM, unit, marginBottom);
    }

    /**
     * <p>Internal method to set the {@link ProgressRibbon}'s vertical margins for real.</p>
     *
     * @param position Accepted values are {@link ProgressRibbon#MARGIN_BOTTOM} ({@value ProgressRibbon#MARGIN_BOTTOM}), or {@link ProgressRibbon#MARGIN_TOP} ({@value ProgressRibbon#MARGIN_TOP})
     * @param unit <p>Accepted values are:
     *                <ul>
     *                    <li>{@link ProgressRibbon#PARENT_HEIGHT_PERCENT}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                    <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *                </ul>
     *             </p>
     * @param margin Non-negative integer value of the margin to be set
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Get the {@link ProgressRibbon}'s top margin either as {@link ProgressRibbon#PARENT_HEIGHT_PERCENT} or {@link TypedValue#COMPLEX_UNIT_DIP}, depending how it was set</p>
     * <br />
     * <p>Note: This method will tell you nothing about the unit in use, it only returns the stored value.</p>
     *
     * @return The {@link ProgressRibbon}'s top margin
     */
    public int getRibbonMarginTop(){
        if(ribbonData.marginIsPercentage) {
            return ribbonData.ribbonMarginTop;
        } else {
            return Math.round(ribbonData.ribbonMarginTop / getResources().getDisplayMetrics().density);
        }
    }

    /**
     * <p>Get the {@link ProgressRibbon}'s bottom margin either as {@link ProgressRibbon#PARENT_HEIGHT_PERCENT} or {@link TypedValue#COMPLEX_UNIT_DIP}, depending how it was set</p>
     * <br />
     * <p>Note: This method will tell you nothing about the unit in use, it only returns the stored value.</p>
     *
     * @return The {@link ProgressRibbon}'s bottom margin
     */
    public int getRibbonMarginBottom(){
        if(ribbonData.marginIsPercentage) {
            return ribbonData.ribbonMarginBottom;
        } else {
            return Math.round (ribbonData.ribbonMarginBottom / getResources().getDisplayMetrics().density);
        }
    }


    /**
     * <p>Returns true when margins are set as a percentage, false when not</p>
     *
     * @return A boolean that answers the question
     */
    public boolean marginIsSetAsPercentage(){
        return ribbonData.marginIsPercentage;
    }


    /**
     * <p>Set whether the {@link ProgressRibbon} should be shown in dialogue mode.<p>
     * <p>Dialogue mode means that the {@link ProgressRibbon} does not extend ot the screen edges and side
     * borders are also visible, identical in colour ot the top and bottom borders.<br />
     * In dialogue mode, there is also an option to control the corner radius</p>
     * <br />
     * <p>Note: This will also set the call the method to set the {@link ProgressRibbon}'s borders,
     * for they will be different int eh two modes</p>
     *
     * @param isInDialogueMode boolean to determine whether the {@link ProgressRibbon} should be drawn in dialogue mode
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonInDialogueMode(boolean isInDialogueMode){
        ribbonData.isInDialogueMode=isInDialogueMode;
        return setRibbonBorders();
    }

    /**
     * <p>Check whether the {@link ProgressRibbon} is in dialogue mode or not</p>
     *
     * @return boolean value showing whether the {@link ProgressRibbon} is in dialogue mode
     */
    public boolean isInDialogueMode(){ return ribbonData.isInDialogueMode;}


    /**
     * <p>This controls the {@link ProgressRibbon}'s border thickness in {@link TypedValue#COMPLEX_UNIT_DIP}</p>
     *
     * @param borderSize the {@link ProgressRibbon}'s border thickness in {@link TypedValue#COMPLEX_UNIT_DIP}
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonBorderSize(@IntRange(from=0) int borderSize){
        ribbonData.ribbonBorderSize=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, borderSize, getResources().getDisplayMetrics()));
        return setRibbonBorderSize();
    }

    /**
     * <p>Set the Ribbon's border thickness in a {@link TypedValue} constant unit of your choice.</p>
     *
     * @param unit <p>Accepted units are:
     *              <ul>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *              </ul>
     *             </p>
     * @param borderSize the actual value of the border thickness
     * @return The {@link ProgressRibbon} object, for method chaining
     */
     public ProgressRibbon setRibbonBorderSize(@ValidSizeUnit int unit, @IntRange(from=0) int borderSize){
        ribbonData.ribbonBorderSize=Math.round(TypedValue.applyDimension(unit, borderSize, getResources().getDisplayMetrics()));
        return setRibbonBorderSize();
     }

    /**
     * <p>Internal method to set the border size for real. Non-dialogue mode {@link ProgressRibbon} use a different drawable than dialogue mode ones</p>
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Get the {@link ProgressRibbon}'s border size in {@link TypedValue#COMPLEX_UNIT_DIP}</p>
     *
     * @return the {@link ProgressRibbon}'s border size in {@link TypedValue#COMPLEX_UNIT_DIP}
     */
    public int getRibbonBorderSize(){
        return Math.round(ribbonData.ribbonBorderSize / getResources().getDisplayMetrics().density);
    }

    /**
     * <p>Set the color of the {@link ProgressRibbon}'s borders as a {@link ColorInt}</p>
     *
     * @param color {@link ColorInt}, value of the {@link ProgressRibbon}'s border color
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonBorderColor(@ColorInt int color){
        ribbonData.ribbonBorderColor =color;
        return setBorderColor(ribbonData.ribbonBorderColor);
    }

    /**
     * <p>Internal method to set the ribbons border color for real. Don't ask why this is needed, I have no idea.
     * Might have had plans for multiple public methods, dunno.</p>
     *
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Get the border colour as a {@link ColorInt}</p>
     *
     * @return {@link ColorInt} of the border color
     */
    @ColorInt
    public int getRibbonBorderColor(){
        return ribbonData.ribbonBorderColor;
    }

    /**
     * <p><em>Warning:</em> This method will do nothing. To set background colour, use {@link ProgressRibbon#setRibbonBackgroundColor(int)} instead.</p>
     *
     * @param color Dummy parameter, it will be ignored (override needs it).
     */
    @Override
    public void setBackgroundColor(@ColorInt int color) {
        /**
         * The overlay must remain transparent. This will prevent colour changes.
         * The reason why this is ignored and not pasoed on to set the visible part's background colour,
         * is that we actually need to use it t make the blocking overlay transparent.
         * Might be hack-ish, but it works
         */
        super.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * <p>Properly set any supplied background {@link Drawable} on the visible part of the {@link ProgressRibbon}
     *
     * @param background {@link Drawable} to set as background
     */
    @Override
    public void setBackground(Drawable background) {
        ribbonContainer.setBackground(background);
    }

    /**
     * <p>Set a solid colour as a {@link ProgressRibbon} background</p>
     * <br />
     * <p>This method replaces {@link ProgressRibbon#setBackgroundColor(int)}</p>
     *
     * @param color {@link ColorInt} to set as {@link ProgressRibbon} background colour
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonBackgroundColor(@ColorInt int color){
        ribbonData.backgroundColor=color;
        return setRibbonBackgroundColor();
    }

    /**
     * <p>Internal method to set {@link ProgressRibbon}'s background color</p>
     *
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    private ProgressRibbon setRibbonBackgroundColor(){
        Drawable bg= ribbonContainer.getBackground();
        if(ribbonData.isInDialogueMode) {
            ((GradientDrawable)bg).setColor(ribbonData.backgroundColor);
            ribbonContainer.setBackground(bg);
        } else {
            /**
             * That's one ugly casting there, but at least we don't have to create two extra Objects just to change colour.
             * maybe that would have been better, i know, but I wanted to show off my casting skillz, hehe.
             */
            ((GradientDrawable) ((InsetDrawable) ((LayerDrawable) bg).getDrawable(1)).getDrawable()).setColor(ribbonData.backgroundColor);

        }

        return this;
    }

    /**
     * <p>Get the {@link ProgressRibbon}'s background color as a {@link ColorInt}</p>
     *
     * @return {@link ColorInt} of the {@link ProgressRibbon}'s background color
     */
    @ColorInt
    public int getRibbonBackgroundColor(){
        return ribbonData.backgroundColor;
    }

    /**
     * Set the {@link ProgressRibbon}'s corner radius in {@link TypedValue#COMPLEX_UNIT_DIP}, as a non-zero integer.</p>
     * <br />
     * <p>Only works for dialogue mode {@link ProgressRibbon}s, otherwise ignored</p>
     *
     * @param radius the corner radius in {@link TypedValue#COMPLEX_UNIT_DIP}, as a non-zero integer
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonBorderRadius(@FloatRange(from=0.0f) float radius){

        ribbonData.ribbonBorderRadius= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radius, getResources().getDisplayMetrics());

        if(ribbonData.isInDialogueMode) {
            setRibbonBorderRadius();
        }
        return this;
    }

    /**
     * <p>Get the {@link ProgressRibbon}'s current corner radius. Meaningful only for dialoge-mode {@link ProgressRibbon}</p>
     *
     * @return Corner radius in {@link TypedValue#COMPLEX_UNIT_DIP}
     */
    public float getRibbonBorderRadius(){
        return ribbonData.ribbonBorderRadius;
    }

    /**
     * <p>Internal method to set the Ribbon's corner radius. Only makes sense in dialogue mode, where the custom drawable
     * background will be updated accordingly</p>
     *
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    private ProgressRibbon setRibbonBorderRadius(){
        if(ribbonData.isInDialogueMode){
            GradientDrawable bg = (GradientDrawable) ribbonContainer.getBackground();
            bg.setCornerRadius(ribbonData.ribbonBorderRadius);
            //ribbonContainer.setBackground(bg);
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
     * <p>Internal method to change Ribbon's borders {@link Drawable}s, applicable paddings and margins</p>
     * <br />
     * <p>Called when {@link ProgressRibbon} is initially set or when dialogue mode is toggled.</p>
     *
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    private ProgressRibbon setRibbonBorders(){

        int ribbonSidePadding=0;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) ribbonContainer.getLayoutParams();

        if(ribbonData.isInDialogueMode){
            GradientDrawable bg = (GradientDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ribbon_dialogue_bg);
            ribbonContainer.setBackground(bg);
            int sideMargin=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            params.leftMargin=sideMargin;
            params.rightMargin=sideMargin;
            ribbonSidePadding=ribbonData.ribbonBorderSize;
            ribbonContainer.invalidate();
        } else {
            LayerDrawable bg= (LayerDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ribbon_default_bg);
            ribbonContainer.setBackground(bg);
            params.leftMargin=0;
            params.rightMargin=0;
        }

        ribbonContainer.setPadding(ribbonSidePadding, ribbonContainer.getPaddingTop(), ribbonSidePadding, ribbonContainer.getPaddingBottom());
        setBorderColor(ribbonData.ribbonBorderColor);
        setRibbonBackgroundColor();
        setRibbonBorderSize();

        if(ribbonData.isInDialogueMode) {
            setRibbonBorderRadius();
        }

        return this;
    }


    /**
     * <p>Shortcut method to hide all the background and borders without having to set their individual colours</p>
     *
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setRibbonTransparent(){
        ribbonData.backgroundColor= Color.TRANSPARENT;
        ribbonData.ribbonBorderColor =Color.TRANSPARENT;
        setBorderColor(Color.TRANSPARENT);
        setRibbonBackgroundColor();
        return setRibbonBorderSize();
    }

    /**
     * <p>Set text colour for the progress text as a {@link ColorInt}</p>
     *
     * @param color {@link ColorInt} of the text colour to be set
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setProgressTextColour(@ColorInt int color){
        ribbonData.progressTextColor = color;
        progressTextView.setTextColor(color);
        return this;
    }

    /**
     * <p>Get the current progress text colour as a simple integer as a {@link ColorInt}.</p>
     * <br />
     * <p>If you need a {@link android.content.res.ColorStateList}, you can access the component {@link TextView} manually via {@link ProgressRibbon#getProgressTextView()}
     * and interrogate the widget as you see fit</p>
     *
     * @return @ {@link ColorInt} of the progress text colour.
     */
    @ColorInt
    public int getProgressTextColor(){
        return ribbonData.progressTextColor;
    }

    /**
     * <p>Set text size of the progress text in {@link TypedValue#COMPLEX_UNIT_SP}</p>
     *
     * @param textSize Text size to be set in {@link TypedValue#COMPLEX_UNIT_SP}
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setProgressTextSize(int textSize){
        ribbonData.progressTextSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, getResources().getDisplayMetrics()));
        progressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        return this;
    }

    /**
     * <p>Set text size in any valid unit as a {@link TypedValue} constant for the progress text</p>
     *
     * @param unit <p>Accepted units are:
     *              <ul>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PX}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_DIP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_SP}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_PT}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_IN}</li>
     *                  <li>{@link TypedValue#COMPLEX_UNIT_MM}</li>
     *              </ul>
     * @param textSize float of the text site to be set
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setProgressTextSize(@ValidSizeUnit int unit, float textSize){
        progressTextView.setTextSize(unit, textSize);
        ribbonData.progressTextSize=Math.round(progressTextView.getTextSize());
        return this;
    }

    /**
     * <p>Get the size of the progress text in raw {@link TypedValue#COMPLEX_UNIT_PX}</p>
     * @return <p>The raw {@link TypedValue#COMPLEX_UNIT_PX} size of the progress text</p>
     */
    public float getProgressTextSize(){
        return ribbonData.progressTextSize;
    }

    /**
     * <p>Freeze the progress at the current point, any progress updates will not be visually applied after this</p>
     *
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon freezeProgress(){
        return setFrozenState(FREEZE_PROGRESS);
    }

    /**
     * <p>Unfreeze the progress at the current point, progress updates will be visually applied again after this</p>
     *
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon allowProgress(){
        return setFrozenState(UNFREEZE_PROGRESS);
    }

    /**
     * <p>Set the frozen/unfrozen state of the {@link ProgressRibbon}. When frozen, any updated to the {@link ProgressRibbon}
     * are not applied visually to the {@link ProgressBar}, while unfreezing it will allow updates again</p>
     *
     * @param frozenState Use either {@link ProgressRibbon#FREEZE_PROGRESS} ({@value ProgressRibbon#FREEZE_PROGRESS})
     *                    or {@link ProgressRibbon#UNFREEZE_PROGRESS} ({@value ProgressRibbon#FREEZE_PROGRESS})
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Check if the {@link ProgressRibbon} is in frozen state</p>
     *
     * @return The frozen state as a boolean
     */
    public boolean isProgresFrozen(){
        return isProgressFrozen;
    }

    /**
     * <p>Checks if the {@link ProgressRibbon} is in orphan mode, meaning it has no parent view it's attached to.</p>
     * <br />
     * <p>Orphan {@link ProgressRibbon} are attached directly ot the window, otherwise they are attached to
     * parenta {@link ViewGroup}</p>
     *
     * @return The fact of orphaneness, or not
     */
    public boolean isInOrphanMode(){
        return ribbonData.isInOrphanMode;
    }

    /**
     * <p>Set the {@link ProgressRibbon}'s progress text as a {@link String}</p>
     *
     * @param progressText The text {@link String} to be set
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setProgressText(String progressText){
        return setText(progressText);
    }

    /**
     * <p>Set the {@link ProgressRibbon}'s progress text as a {@link String} resource ID.
     * It will find the {@link String} and apply it, so you don't have to</p>
     *
     * @param resId the resID int of the string resource
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setProgressText(@StringRes int resId){
        return setText(getContext().getString(resId));
    }

    /**
     * <p>Internal funtion to actually set the text on the {@link ProgressRibbon}, called from the public functions.</p>
     * <br />
     * <p>If you see this text from another class, something either went horribly wrong, or you/someone did not
     * rewrite the {@JavaDoc} comments when forking this project...</p>
     *
     * @param text Text to set on the Ribbon
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    private ProgressRibbon setText(String text){
        progressText=text;
        progressTextView.setText(progressText);
        invalidate();
        return this;
    }

    /**
     * <p>Get the currently set progress text from the {@link ProgressRibbon} as a {@link String} </p>
     *
     * @return the progress text from underneath the {@link ProgressBar} as a {@link String}
     */
    public String getProgressText(){
        return progressText;
    }


    /**
     * <p>Sets whether the {@link ProgressRibbon} will block the views/window behind it (true), or allows touches (false)</p>
     *
     * @param blocksUnderlyingViews - Basically true or false (yes or no)
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setViewBlocking(boolean blocksUnderlyingViews){
        /**
         * Here we simply set the blockUnderLying flag from the outside world.
         * The supplied boolean will control what the overridden onTouchEvent(...) will return (true means it intercepts touches,
         * blocking what's underneath). If we are in orphan mode, meaning the ribbon is attached to the window with no parent view,
         * it will bock the underlying views by simply expanding our Ribbon (or rather its trasparent overlay view on the top and
         * bottom bit) to the full size of the screen. Or not (if we are in non-blocking mode). Basically only the height ever varies.
         * (The type and flags of the WindowManager.LayoutParams ensure that the view can block others and is touchable so it can
         * intercept motion events correctly)
         * */
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
     * <p>Check if the {@link ProgressRibbon} is set to block underlying views</p>
     *
     * @return a boolean value telling us whether the {@link ProgressRibbon} is blocking or not
     */
    public boolean willBlockUnderlyingViews(){
        return ribbonData.blocksUnderlying;
    }


    /**
     * <p>This one will summon the dark... Oh wait that's not it This one will, your guess was right, set the Ribbons elevation. Magic.</p>
     * <br />
     * <p>Oh wait, there is more. It will actually work on <em>all</em> SDK levels, checking against the version, and deciding whether is
     * should apply proper elevation, or the AppCompat version. So it really IS magic.  You know, sort of how elevation should have been
     * implemented by Android devs in the first place...</p>
     *
     * @param elevation Elevation value in {@link TypedValue#COMPLEX_UNIT_DIP} to set, in an SDK version independent manner. Ha!
     */
    @Override
    public void setElevation(float elevation) {
        setRibbonElevationInternal(elevation);
    }

    /**
     * <p>Same as setElevation (float), but with the option to chain it with other setters</p>
     *
     * @param elevation Elevation value in {@link TypedValue#COMPLEX_UNIT_DIP} to set, in an SDK version independent manner. Ha!
     * @return {@link ProgressRibbon} so you can continue horading these setters on one line
     */
    public ProgressRibbon setRibbonElevation(float elevation) {
        return setRibbonElevationInternal(elevation);
    }


    /**
     * <p>Internal method to actually do the heavy lifting, when setting elevation level. This is where the magic happens.
     * It will check whether it can apply proper elevation (according to SDK version), and if not, applies the AppCompat
     * way instead. Also, if you see this text from another class, somebody somewhere forgot to update JavaDoc when
     * changing stuff...</p>
     *
     * @param elevation the elevaton value in {@link TypedValue#COMPLEX_UNIT_DIP}
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Gets the currently set elevation of the {@link ProgressRibbon}</p>
     *
     * @return the elevation
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
     * <p>Sets the layout direction same as with every wiew, {@link View#LAYOUT_DIRECTION_LTR}, {@link View#LAYOUT_DIRECTION_RTL},
     * {@link View#LAYOUT_DIRECTION_INHERIT}, or {@link View#LAYOUT_DIRECTION_LOCALE}. In the latter two
     * cases it works differently, as it will not really traverse the view-tree to find one that is explicitly set
     * (as in {@link View#LAYOUT_DIRECTION_INHERIT}), but checks the layout direction in the configuraton (i.e. what is the basic system setting), and apply
     * it to the view. This breaks proper inheritence, but since we are not necessarily attached yet, and needed a sane default, it's what it is for now.</p>
     *
     * @param layoutDirection the layout direction. Only {@link View#LAYOUT_DIRECTION_LTR} and {@link View#LAYOUT_DIRECTION_RTL} do anything meaningful for now
     */
    @Override
    public void setLayoutDirection(int layoutDirection) {
        //ToDo this needs attention
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
     * <p>Shortcut that tells you whether the layout of the ribbon is set to RTL in a convenient boolean format</p>
     * @return <code>true</code> layout is {@link View#LAYOUT_DIRECTION_RTL}, false otherwise
     */
    public boolean getLayoutIsRTL(){
        return layoutIsRTL;

    }

    /**
     * <p>Sets the {@link RibbonData#min} value of the Ribbon in an SDK independent manner, that also shows
     * on the {@link ProgressBar}'s status.</p>
     * <br />
     * <p>{@link ProgressBar#setMin(int)} can only be used from upwards of SDK{@value Build.VERSION_CODES#O},
     * <em>and</em> in a not very sensible manner either (i.e. the bar gets a minimum value, but the progress
     * drawable does not reflect this value).</p>
     * <br />
     * <p>In {@link ProgressRibbon}, the {@link RibbonData#min} value gets applied to the {@link ProgressBar}'s {@klink Drawable} as well.
     * Progress will start from the visually represented {@link RibbonData#min}, instead of starting from an empty bar.)
     * Any progress applied to the {@link ProgressRibbon} that is below {@link RibbonData#min}, will be ignored.</p>
     *
     * @param min the minimum value for the determinate {@link ProgressRibbon}
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setMin(@IntRange(from=0) int min){
        ribbonData.min=min;
        ribbonData.progress=min;
        progressBar.setProgress(min);
        return this;
    }

    /**
     * <p>Returns the current value of the {@link ProgressRibbon} minimum value</p>
     *
     * @return the current value of min
     */
    public int getMin(){
        return ribbonData.min;
    }

    /**
     * <p>Sets the maximum value of the {@link ProgressRibbon} (in determinate mode). Works the same as
     * {@link ProgressBar#setMax(int)}</p>
     *
     * @param max the maximum value of the {@link ProgressRibbon} in determinate mode
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Returns the maximum value of a determinate {@link ProgressRibbon} as an int</p>
     *
     * @return The maximum value of a determinate {@link ProgressRibbon} as an int
     */
    public int getMax(){
        return ribbonData.max;
    }

    /**
     * <p>Sets the progress of the determinate {@link ProgressBar} to the specified value.</p>
     * <br />
     * <p>It will check if the progress exceeds {@link RibbonData#min} and sets the progress value it does.
     * (If the progress value is below the minimum,it will be ignored.) It will then check if the progress
     * is frozen before visually the value to the widget too.</p>
     * <br />
     * <p>Also calls the appropriate listeners, if they exist.</p>
     *
     * @param progress the int value to set as progress
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
     * <p>Set the seconrady progress for the {@link ProgressRibbon},</p>
     *
     * @param secondaryProgress the secondary progress value as an int
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
                /**
                 * The {@link ProgressRibbon} class does not know about our special {@link Drawable}
                 * and will not recognise the levels set in it even with the right ids, so we need
                 * to get creative here
                 */
                updateCircularSecondaryProgress();
            }
        }
    }

    /**
     * <p>This internal method will correctly apply the secondary progress to our special round drawable</p>
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
     * <p>Same as {@link ProgressBar#incrementProgressBy(int)}  It will  increments progress by.</p>
     *
     * @param diff int to increment progress by
     */
    public void incrementProgressBy(@IntRange(from=0) int diff){
        progressBar.incrementProgressBy(diff);
    }

    /**
     * <p>Same as {@link ProgressBar#incrementSecondaryProgressBy(int)}.  It will  increments secondary progress by.</p>
     *
     * @param diff int to increment secondary progress by
     */
    public void incrementSecondaryProgressBy(@IntRange(from=0) int diff){
        setSecondaryProgress(ribbonData.secondaryProgress+diff);
    }

    /**
     * <p>Set whether to return progress as a percentage of the current max value, or as a raw number</p>
     *
     * @param value boolean flagh to do what's advertised
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setReportProgressAsMaxPercent(boolean value){
        ribbonData.reportProgressAsMaxPercent=value;
        return this;
    }


    /**
     * <p>Tells you whether the {@link ProgressRibbon} will return progress as a percent of the currrent max value, or as a raw number</p>
     *
     * @return <code>true</code> or <code>false</code>
     */
    public boolean willReportProgressAsMaxPercent(){
        return ribbonData.reportProgressAsMaxPercent;
    }

    /**
     * <p>Get the actual value of the currently displayed progress of the determinate {@link ProgressBar} widgets as an int, or
     * its value as a percentage of the currently set {@link RibbonData#max} value (useful if other than 100), depending on
     * {@link RibbonData#reportProgressAsMaxPercent}</p>
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
     * <p>Returns secondary progress value, if there is any</p>
     *
     * @return the secondary progress value as an integer
     */
    public int getSecondaryProgress(){
        return ribbonData.secondaryProgress;
    }

    /**
     * <p>See whether the {@link ProgressRibbon} is showing ort not</p>
     *
     * @return a boolean that tells you exactly that
     */
    public boolean isShowing(){
        return isShowing;
    }

    /**
     * <p>Internal method to calculate the progress value as a percentage of the {@link RibbonData#max} progress.</p>
     * <br />
     * <p>Useful, when maximum is set to anything other than 100, but an accurate percentage is still needed.
     * Needs {@link RibbonData#reportProgressAsMaxPercent} to be set to <code>true</code></p>
     *
     * @return the calculated percentage of the current progress, in relation to the {@link RibbonData#max} value
     */
    private int getProgressPercentage(){
        return (progressBar.getProgress()*100)/ribbonData.max;
    }

    /**
     * <p>Set a delay in milliseconds (int) before showing the {@link ProgressRibbon}.</p>
      <br />
     * <p>Useful for operations that might finish very quickly
     * (so quickly that no {@link ProgressBar} is needed). If the operation takes more time then expected (i.e. longer then {@link RibbonData#showDelay}),
     * the {@link ProgressRibbon} will appear.</p>
     * <br />
     * <p>This will allow to never show the {@link ProgressRibbon} at all, when the operation finishes within the delay {@link RibbonData#showDelay} threshold.</p>
     * <br />
     * <p>See also {@link ProgressRibbon#setHideDelay(int)}, to make sure the {@link ProgressRibbon} remains showing for a minimum amount of time (to avoid "flashing" it)</p>
     *
     * @param showDelay Delay value in milliseconds (int)
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setShowDelay(@IntRange(from=0) int showDelay){
        ribbonData.showDelay=showDelay;
        return this;
    }

    /**
     * <p>Returns the currently set show delay value in milliseconds, as an int</p>
     *
     * @return The currently set show delay value in milliseconds, as an int
     */
    public int getShowDelay(){
        return ribbonData.showDelay;
    }

    /**
     * <p>Sets a hide delay on the {@link ProgressRibbon}, in milliseconds (int).</p>
     * <br />
     * <p>Hide delay works in conjunction with
     * show delay (see {@link ProgressRibbon#setShowDelay(int)}) and is meant to ensure that the {@link ProgressRibbon}
     * will not just "flash" when the operation calling it finishes very shortly after the {@link ProgressRibbon} appeared.</p>
     * <br />
     * <p>In practice this means avoiding to show the {@link ProgressRibbon} for a fraction of a second only. Hide delay
     * can be thought of as a minimum time a {@link ProgressRibbon} should be visible to avoid such "flashes".</p>
     *
     * @param hideDelay hide delay value in milliseconds (int)
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setHideDelay(@IntRange(from=0) int hideDelay) {
        ribbonData.hideDelay = hideDelay;
        return this;
    }

    /**
     * <p>Returns the currently set hide delay value in milliseconds, as an int</p>
     *
     * @return The currently set hide delay value in milliseconds, as an int
     */
    public int getHideDelay(){
        return ribbonData.hideDelay;
    }

    /**
     * <p>Choose between circular and flat {@link ProgressBar}. Use static constants {@link ProgressRibbon#BAR_ROUND} ({@value ProgressRibbon#BAR_ROUND})
     * or {@link ProgressRibbon#BAR_HORIZONTAL} ({@value ProgressRibbon#BAR_HORIZONTAL}).
     *
     * @param style int value of the progress bar style as either {@link ProgressRibbon#BAR_ROUND} or {@link ProgressRibbon#BAR_HORIZONTAL}
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setProgressBarStyle(@ProgressStyle int style){
        if(style!=ribbonData.progressBarStyle) {
            ribbonData.progressBarStyle = style;
            initProgressbar(ribbonData.progressBarStyle, ribbonData.isIndeterminate);
        }
        return this;
    }

    /**
     * <p>Get the current Ribbon bar style as an int. Evaluate against static constants
     * {@link ProgressRibbon#BAR_ROUND} ({@value ProgressRibbon#BAR_ROUND})
     * or {@link ProgressRibbon#BAR_HORIZONTAL} ({@value ProgressRibbon#BAR_HORIZONTAL})</p>
     *
     * @return Current ribbon style as either {@link ProgressRibbon#BAR_ROUND} ({@value ProgressRibbon#BAR_ROUND})
     *         or {@link ProgressRibbon#BAR_HORIZONTAL} ({@value ProgressRibbon#BAR_HORIZONTAL}
     */
    public int getProgressBarStyle(){
        return ribbonData.progressBarStyle;
    }

    /**
     * <p>When the progressbar style is set to {@link ProgressRibbon#BAR_ROUND} (see {@link ProgressRibbon#setProgressBarStyle(int)}),
     * the progress text can appear either besides or underneath the progress bar. Set to either
     * {@link ProgressRibbon#TEXT_BESIDE_BAR} {@value ProgressRibbon#TEXT_BESIDE_BAR}
     * or {@link ProgressRibbon#TEXT_UNDER_BAR} {@value ProgressRibbon#TEXT_UNDER_BAR}</p>
     * <br />
     * <p>If the {@link ProgressBar is set to {@link ProgressRibbon#BAR_HORIZONTAL}}, it can only display text underneath the bar,
     * so that is what they will do, regardless of this setting</p>
     *
     * @param textPosition Whether the text should be displayed besides or underneath the bar (boolean)
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Returns whether the text is set to be displayed beside the bar (<code>true</code>) or underneath it (<code>false</code>).
     * <br />
     * <p>Only meaningful when {@link ProgressRibbon} is et to {@link ProgressRibbon#BAR_ROUND}</p>
     *
     * @return boolean stating whether text is displaying beside the bar. Or not.
     */
    public boolean isRibbonTextBesideProgressBar(){
        return ribbonData.textBesideBar;
    }

    /**
     * <p>Set the {@link ProgressBar} as either {@link ProgressRibbon#INDETERMINATE} ({@value ProgressRibbon#INDETERMINATE}),
     * with no progress values to set/show, or {@link ProgressRibbon#DETERMINATE} ({@value ProgressRibbon#DETERMINATE})
     * Works with both circular and flat bars. If set ot indeterminate, the {@link ProgressBar} will spin indeterminately,
     * otherwise they will show the currently set progress in an animated fashion (yes, even the circular one)</p>
     *
     * @param indeterminateState Whether the {@link ProgressBar} is indeterminate (boolean)
     * @return The {@link ProgressRibbon} object, for method chaining
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

    /**
     * Check whether the {@link ProgressBar} is set to {@link ProgressRibbon#INDETERMINATE} (<code>true</code>)
     * or {@link ProgressRibbon#DETERMINATE (<code>false</code>)
     * @return <code>true</code> for {@link ProgressRibbon#INDETERMINATE}, <code>false</code> for {@link ProgressRibbon#INDETERMINATE}
     */
    public boolean isIndeterminate(){
        return ribbonData.isIndeterminate;
    }

    /**
     * <p>Get the indeterminate state of the {@link ProgressBar}</p>
     * <br />
     * <p>Evaluate against constants {@link ProgressRibbon#INDETERMINATE} ({@value ProgressRibbon#INDETERMINATE})
     * or {@link ProgressRibbon#DETERMINATE} ({@value ProgressRibbon#DETERMINATE})</p>
     *
     * @return Either {@link ProgressRibbon#INDETERMINATE} ({@value ProgressRibbon#INDETERMINATE})
     *         or {@link ProgressRibbon#DETERMINATE} ({@value ProgressRibbon#DETERMINATE})
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
     * <p>Retruns whether the {@link ProgressRibbon} is set to animate its showing and hiding</p>
     *
     * @return What it says on the tin
     */
    public boolean willAnimateAppearance(){
        return ribbonData.animationType!=DO_NOT_ANIMATE;
    }

    /**
     * <p>Sets the duration of the {@link ProgressRibbon}'s show/hide animation (if any) in milliseconds (int).</p>
     * <br />
     * <p>Will be ignored when used during animations</p>
     *
     * @param value the duration of the show/hide animatin in milliseconds
     * @return The {@link ProgressRibbon} object, for method chaining
     */
    public ProgressRibbon setAnimationDuration(@IntRange(from=0) int value){
        if(!animationInProgress) {
            ribbonData.animationDuration = value;
        }
        return this;
    }

    /**
     * <p>Get the duration of the {@link ProgressRibbon}'s show/hide animation (if any) in milliseconds (int).</p>
     *
     * @return the duration of the show/hide animation in milliseconds
     */
    public int getAnimationDuration(){
        return ribbonData.animationDuration;
    }

    /**
     * <p>Set the {@link ProgressRibbon}'s animation type to one of four built-in animation types</p>
     * <p>
     *     <ul>
     *         <li>{@link ProgressRibbon#DO_NOT_ANIMATE} means the widget will not be animated when showing or hiding</li>
     *         <li>{@link ProgressRibbon#ANIMATE_FADE} means the widget will fade in and out when showing or hiding</li>
     *         <li>{@link ProgressRibbon#ANIMATE_SCALE} means the widget will scale in and out when showing or hiding</li>
     *         <li>{@link ProgressRibbon#ANIMATE_SCALE_FADE} means the widget will scale <em>and</em> fade in and out when showing or hiding</li>
     *     </ul>
     *
     * </p>
     * <br />
     * <p>It will set both the show and hide animations at the same tme, but it can be changed on they fly
     * while the Ribbon is showing, although not while animating (there is a failsafe in place for this)</p>
     **
     * @param animationType <p>The type of animation to use. Accepted values are</p>
     *                      <p>
     *                        <ul>
     *                           <li>{@link ProgressRibbon#DO_NOT_ANIMATE}</li>
     *                           <li>{@link ProgressRibbon#ANIMATE_FADE}</li>
     *                           <li>{@link ProgressRibbon#ANIMATE_SCALE}</li>
     *                           <li>{@link ProgressRibbon#ANIMATE_SCALE_FADE}</li>
     *                       </ul>
     *                      </p>
     * @return The {@link ProgressRibbon} object, for method chaining
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
     * <p>Get the current animation type as an int, evaluate against the static constants
     * {@link ProgressRibbon#DO_NOT_ANIMATE} {{@value ProgressRibbon#DO_NOT_ANIMATE}},
     * {@link ProgressRibbon#ANIMATE_FADE} {{@value ProgressRibbon#ANIMATE_FADE}},
     * {@link ProgressRibbon#ANIMATE_SCALE} {{@value ProgressRibbon#ANIMATE_SCALE}},
     * {@link ProgressRibbon#ANIMATE_SCALE_FADE} {{@value ProgressRibbon#ANIMATE_SCALE_FADE}},
     * </p>
     *
     * @return the current animation type as an int
     */
    public int getAnimationType(){
        return ribbonData.animationType;
    }


    /**********************************Set up animations*******************************************/

    /**
     * <p>Internal method to handle the animation of showing/hiding the {@link ProgressRibbon}. Whether
     * it's showing or hiding should be set by using the two constants {@link ProgressRibbon#ANIMATION_SHOW}
     * and {@link ProgressRibbon#ANIMATION_HIDE}. It uses  {@link RibbonData#animationType} to know
     * what type of animation to do</p>
     * <br />
     * <p>If you see this text from another class, somebody forgot to update the JavaDoc</p>
     *
     * @param animationDirection Meaning showing or hiding. see static constants {@link ProgressRibbon#ANIMATION_SHOW} and
     *                           {@link ProgressRibbon#ANIMATION_HIDE}
     * @return The {@link ProgressRibbon} object, for method chaining
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

    /**
     * <p>These thigns will be timed, yeah</p>
     */
    private Handler showHandler;
    private Runnable runner;
    private Handler hideHandler;

    /**
     * <p>Combined internal function to make the {@link ProgressRibbon} appear (with a delay if a delay is set), and if not yet attached,
     * attach it to to either a parent view if one is supplied or the window directly, if not. The boolean parameter
     * allows for temporarily overriding {@link RibbonData willAnimateAppearance}, when animation should temporarily be ignored,
     * such as when restoring view after layout changes.</p>
     * <br />
     * <p>if you see this text from another class, somebody forgot to update the javaDoc</p>
     *
     * @param skipAnimation temporarily override animation setting in specific cases, like e.g. restoring layourt state
     */
    private void attachAndShowView(boolean skipAnimation){

        setVisibility(View.GONE);

        if(isDynamicallyCreated) {

            /**
             * Static instances tend to stick around and we'll need to remove them in certain cases. Since we don't know what's
             * happening in the {@link ViewController}, we best do this here
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
     * <p>Will hide the {@linnk ProgressRibbon}, with any applicable delay</p>
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
     * <p>Show the {@linnk ProgressRibbon}, applying {@link RibbonData#showDelay}, or immediately, if it1s not set</p>
     */
    public void show(){
        show(ribbonData.showDelay);
    }

    /**
     * <p>Show the {@linnk ProgressRibbon} with the declared delay value (in milliseconds), ignoring {@link RibbonData#showDelay}</p>
     *
     * @param delay show delay in millisec
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
     * <p>Show the {@linnk ProgressRibbon} immediately, ignoring ignoring {@link RibbonData#showDelay}, even if it is set</p>
     */
    public void showNoDelay() {

        attachAndShowView(true);

        if(ribbonStateChangeListener !=null) {
            ribbonStateChangeListener.onRibbonSignalledToShow(0);
        }
    }

    /**
     * <p>Hide the {@linnk ProgressRibbon}, applying {@link @RibbonData#hideDelay}, or immediately, if it's not set</p>
     */
    public void hide(){
        hide(ribbonData.hideDelay);
    }

    /**
     * <p>Hide the {@linnk ProgressRibbon} with the declared delay value (in milliseconds), ignoring {@link RibbonData#hideDelay}</p>
     *
     * @param delay hide delay in millisec
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
     * <p>Hide the {@linnk ProgressRibbon} immediately, ignoring ignoring {@link RibbonData#hideDelay}, even if it is set</p>
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
     * <p>Internal function to completely detach the {@link ProgressRibbon} from the parent {@link ViewGroup},
     * if there is one, or the window, if the widget is an orphan.</p>
     * <br />
     * <p>As usual, if you can see this text from another class, somebody has forgotten to update the JavaDoc</p>
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
     * <p>Completely remove a dynamically created {@link ProgressRibbon}, ignoring any {@RibbonData#hideDelay},
     * but respecting {@link RibbonData#animationType} settings</p>
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
     * The usual business. Get everything from a bundle
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
     * <p>I do not saves the state of child {@link View}s with {@link ProgressRibbon#dispatchSaveInstanceState}.<br />
     * He who saves the state of child views with {@link ProgressRibbon#dispatchSaveInstanceState}. has forgotten the
     * face of his father.<br />
     * I save the state of child views with my heart</p>
     * <br />
     * <p>(Or, rather, in {@link ProgressRibbon#onSaveInstanceState}, manually)</p>
     *
     * @param container If you want to mess with this, you should know what it is. otherwise, don't touch it
     */
    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchFreezeSelfOnly(container);
    }

    /**
     * <p>I do not restores the state of child {@link View}s with {@link ProgressRibbon#dispatchRestoreInstanceState}.<br />
     * He who restores the state of child views with {@link ProgressRibbon#dispatchRestoreInstanceState}. has forgotten the
     * face of his father.<br />
     * I save the state of child views with my heart</p>
     * <br />
     * <p>(Or, rather, in {@link ProgressRibbon#onRestoreInstanceState}, manually)</p>
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


    /**
     * Internal class representing, surprisingly, the {@link ProgressRibbon}'s data. Wow. Such obvious. So smooth
     */
    public class RibbonData {

        /**
         * Constants for default values, without any values (some need ot be calculated,
         * so they get assigned their values in the constructor
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

            //You don't say...
            DEFAULT_RIBBON_MIN=0;
            //Makes no sense, but why not (I mean that's the default of defaults anyway, ain't it?)
            DEFAULT_RIBBON_MAX=100;
            //This looks like a reasonable default animation time
            DEFAULT_ANIMATION_DURATION=300;
            //A decent enough default
            DEFAULT_RIBBON_PADDING=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics()));
            //Default border is 1dp thick
            DEFAULT_RIBBON_BORDER_SIZE=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics()));
            //A decent enough default. Probably
            DEFAULT_RIBBON_ELEVATION=24;

            /**
             * Now there is a difference before and after SDK{@value Build.VERSION_CODES.LOLLIPOP}
             * It's because of the resolver method used (I have no idea how to do this pre-Lollipop)
             */
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {

                //Should pull it in from the host app, if non found, use our own default
                DEFAULT_RIBBON_BORDER_COLOR = resolveAppThemeColour(android.R.attr.colorPrimary);
                //Pretty much the same across devices, but shoudl conform to the app's settings if differen
                DEFAULT_RIBBON_TEXT_COLOR = resolveAppThemeColour(android.R.attr.textColorPrimary);
                //Default app background colour, or white, if all else fails
                DEFAULT_RIBBON_BG_COLOR = resolveAppThemeColour(android.R.attr.windowBackground);
            } else{
                //Should pull it in frrom the built in resource
                DEFAULT_RIBBON_BORDER_COLOR = ContextCompat.getColor(getContext(), R.color.colorPrimary);
                //Should pull it in frrom the built in resource
                DEFAULT_RIBBON_TEXT_COLOR = ContextCompat.getColor(getContext(), R.color.textColorPrimary);
                //White's as good as anything
                DEFAULT_RIBBON_BG_COLOR = ContextCompat.getColor(getContext(), android.R.color.white);
            }

            //Decent enough text size
            DEFAULT_RIBBON_TEXT_SIZE = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, context.getResources().getDisplayMetrics()));
            //No margin for old men
            DEFAULT_RIBBON_MARGIN=0;
            //Delays should be set when needed
            DEFAULT_RIBBON_HIDE_DELAY=0;
            //Delays should be set when needed
            DEFAULT_RIBBON_SHOW_DELAY=0;
            //A decent enough default
            DEFAULT_RIBBON_BORDER_RADIUS=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8.0f, context.getResources().getDisplayMetrics());

            /**
             * It's conditional, because... Well, because we are not sure we really want everything ot be defaulted, are we?
             */
            if(setDefaults) {
                applyDefaults();
            }
        }


        /**
         * <p>An attempt to resolve some of the application's colours in a failsafe way. If nothing helps, use the declared default</p>
         * <br />
         * <p>Credit for this solution goes to <https://mbcdev.com/2017/01/16/resolving-android-theme-colours-programmatically/></p>
         *
         * @param attrId <p>The color id to resolve. Accepted values are
         *                  <ul>
         *                      <li>{@link android.R.attr#colorPrimary}</li>
         *                      <li>{@link android.R.attr#colorAccent}</li>
         *                      <li>{@link android.R.attr#textColorPrimary}</li>
         *                      <li>{@link android.R.attr#windowBackground}</li>
         *                  </ul>
         *                  These require min. SDK{@value android.os.Build.VERSION_CODES#LOLLIPOP}, so on earlier versions a more
         *                  direct approach should be used. ToDo: Figure out how to do this pre-Lollipop
         *                </p>
         *               <br />
         *               <p>Note {@link android.R.attr#colorAccent} is not currently used and is only included for future convenience</p>
         *
         * @return The resolved {@link  ColorInt}
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private int resolveAppThemeColour(@ValidColorAttrIds int attrId){

            TypedValue outValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            boolean wasResolved =
                    theme.resolveAttribute(
                            attrId, outValue, true);
            if (wasResolved) {
                return outValue.resourceId == 0 ? outValue.data : ContextCompat.getColor(getContext(), outValue.resourceId);
            } else {
                //Fallback colour handling
                switch(attrId){
                    case android.R.attr.colorPrimary: {
                        return ContextCompat.getColor(getContext(), R.color.colorPrimary);
                    }
                    case android.R.attr.colorAccent: {
                        return ContextCompat.getColor(getContext(), R.color.colorAccent);
                    }
                    case android.R.attr.textColorPrimary: {
                        return ContextCompat.getColor(getContext(), R.color.textColorPrimary);
                    }
                    default: {
                        if (attrId >= TypedValue.TYPE_FIRST_COLOR_INT && attrId <= TypedValue.TYPE_LAST_COLOR_INT) {
                            return ContextCompat.getColor(getContext(), attrId);
                        } else {
                            //The absolute last resource
                            return ContextCompat.getColor(getContext(), android.R.color.white);
                        }
                    }
                }
            }
        }


        /**
         * Set those defaults, where defaults need setting
         */
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