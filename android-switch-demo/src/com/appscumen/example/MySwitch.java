/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appscumen.example;


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.CompoundButton;


/**
 * A MySwitch is a two-state toggle switch widget that can select between two
 * options. The user may drag the "thumb" back and forth to choose the selected option,
 * or simply tap to toggle as if it were a checkbox. The {@link #setText(CharSequence) text}
 * property controls the text displayed in the label for the switch, whereas the
 * {@link #setTextOff(CharSequence) off} and {@link #setTextOn(CharSequence) on} text
 * controls the text on the thumb. Similarly, the
 * {@link #setTextAppearance(android.content.Context, int) textAppearance} and the related
 * setTypeface() methods control the typeface and style of label text, whereas the
 * {@link #setSwitchTextAppearance(android.content.Context, int) switchTextAppearance} and
 * the related seSwitchTypeface() methods control that of the thumb.
 *
 */
public class MySwitch extends CompoundButton {
    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;
    private static final String TAG="MySwitch";
    
    // Enum for the "typeface" XML parameter.
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    
    private OnChangeAttemptListener mOnChangeAttemptListener;
    private boolean mTextOutsideTrack;
    private boolean mTextOnThumb;
    private Drawable mThumbDrawable;
    private Drawable mTrackDrawable;
    private int mThumbTextPadding;
    private int mTrackTextPadding;
    private int mSwitchMinWidth;
    private int mSwitchMinHeight;
    private int mSwitchPadding;
    private CharSequence mTextOn;
    private CharSequence mTextOff;
    private boolean fixed=false;
    private boolean clickDisabled=false;
    private boolean onOrOff=true;

    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private int mMinFlingVelocity;

    private float mThumbPosition;
    private int mSwitchWidth;
    private int mSwitchHeight;
    private int mThumbWidth; // Does not include padding

    private int mSwitchLeft;
    private int mSwitchTop;
    private int mSwitchRight;
    private int mSwitchBottom;

    private TextPaint mTextPaint;
    private ColorStateList mTextColors;
    private Layout mOnLayout;
    private Layout mOffLayout;

    @SuppressWarnings("hiding")
    private final Rect mTrackPaddingRect = new Rect();
    private final Rect mThumbPaddingRect = new Rect();
	private final Rect canvasClipBounds = new Rect();

    private static final int[] CHECKED_STATE_SET = {
        android.R.attr.state_checked
    };

    /**
     * Construct a new MySwitch with default styling.
     *
     * @param context The Context that will determine this widget's theming.
     */
    public MySwitch(Context context) {
        this(context, null);
    }

    /**
     * Construct a new MySwitch with default styling, overriding specific style
     * attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs Specification of attributes that should deviate from default styling.
     */
    public MySwitch(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.mySwitchStyleAttr);
    }

    /**
     * Construct a new MySwitch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs Specification of attributes that should deviate from the default styling.
     * @param defStyle An attribute ID within the active theme containing a reference to the
     *                 default style for this widget. e.g. android.R.attr.switchStyle.
     */
    public MySwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Resources res = getResources();
        mTextPaint.density = res.getDisplayMetrics().density;
        mTextPaint.setShadowLayer(0.5f, 1.0f, 1.0f, Color.BLACK);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MySwitch, defStyle, 0);

        mThumbDrawable = a.getDrawable(R.styleable.MySwitch_thumb);
        mTrackDrawable = a.getDrawable(R.styleable.MySwitch_track);
        mTextOn = a.getText(R.styleable.MySwitch_textOn);
        mTextOff = a.getText(R.styleable.MySwitch_textOff);
        mTextOutsideTrack = a.getBoolean(R.styleable.MySwitch_textOutsideTrack, false);
        mTextOnThumb = a.getBoolean(R.styleable.MySwitch_textOnThumb, false);
        mThumbTextPadding = a.getDimensionPixelSize( R.styleable.MySwitch_thumbTextPadding, 0);
        mTrackTextPadding = a.getDimensionPixelSize( R.styleable.MySwitch_trackTextPadding, 0);
        mSwitchMinWidth = a.getDimensionPixelSize( R.styleable.MySwitch_switchMinWidth, 0);
        mSwitchMinHeight = a.getDimensionPixelSize( R.styleable.MySwitch_switchMinHeight, 0);
        mSwitchPadding =  a.getDimensionPixelSize( R.styleable.MySwitch_switchPadding, 0);

        mTrackDrawable.getPadding(mTrackPaddingRect) ;
        //Log.d(TAG, "mTrackPaddingRect=" + mTrackPaddingRect);
        mThumbDrawable.getPadding(mThumbPaddingRect);
        //Log.d(TAG, "mThumbPaddingRect=" + mTrackPaddingRect);
        
        
        int appearance = a.getResourceId(R.styleable.MySwitch_switchTextAppearanceAttrib, 0);
        if (appearance != 0) {
            setSwitchTextAppearance(context, appearance);
        }
        a.recycle();

        ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        // Refresh display with current params
        refreshDrawableState();
        setChecked(isChecked());
        this.setClickable(true);
        //this.setOnClickListener(clickListener);
    }

    /**
     * Sets the switch text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     */
    public void setSwitchTextAppearance(Context context, int resid) {
        TypedArray appearance =
                context.obtainStyledAttributes(resid, R.styleable.mySwitchTextAppearanceAttrib);

        ColorStateList colors;
        int ts;

        colors = appearance.getColorStateList(R.styleable.mySwitchTextAppearanceAttrib_textColor);
        if (colors != null) {
            mTextColors = colors;
        } else {
            // If no color set in TextAppearance, default to the view's textColor
            mTextColors = getTextColors();
        }

        ts = appearance.getDimensionPixelSize(R.styleable.mySwitchTextAppearanceAttrib_textSize, 0);
        if (ts != 0) {
            if (ts != mTextPaint.getTextSize()) {
                mTextPaint.setTextSize(ts);
                requestLayout();
            }
        }

        int typefaceIndex, styleIndex;

        typefaceIndex = appearance.getInt(R.styleable.mySwitchTextAppearanceAttrib_typeface, -1);
        styleIndex = appearance.getInt(R.styleable.mySwitchTextAppearanceAttrib_textStyle, -1);

        setSwitchTypefaceByIndex(typefaceIndex, styleIndex);

        appearance.recycle();
    }

    private void setSwitchTypefaceByIndex(int typefaceIndex, int styleIndex) {
        Typeface tf = null;
        switch (typefaceIndex) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;

            case SERIF:
                tf = Typeface.SERIF;
                break;

            case MONOSPACE:
                tf = Typeface.MONOSPACE;
                break;
        }

        setSwitchTypeface(tf, styleIndex);
    }

    /**
     * Sets the typeface and style in which the text should be displayed on the
     * switch, and turns on the fake bold and italic bits in the Paint if the
     * Typeface that you provided does not have all the bits in the
     * style that you specified.
     */
    public void setSwitchTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setSwitchTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setSwitchTypeface(tf);
        }
    }

    /**
     * Sets the typeface in which the text should be displayed on the switch.
     * Note that not all Typeface families actually have bold and italic
     * variants, so you may need to use
     * {@link #setSwitchTypeface(Typeface, int)} to get the appearance
     * that you actually want.
     *
     * @attr ref android.R.styleable#TextView_typeface
     * @attr ref android.R.styleable#TextView_textStyle
     */
    public void setSwitchTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);

            requestLayout();
            invalidate();
        }
    }

    /**
     * Returns the text displayed when the button is in the checked state.
     */
    public CharSequence getTextOn() {
        return mTextOn;
    }

    /**
     * Sets the text displayed when the button is in the checked state.
     */
    public void setTextOn(CharSequence textOn) {
        mTextOn = textOn;
        this.mOnLayout=null;
        requestLayout();
    }

    /**
     * Returns the text displayed when the button is not in the checked state.
     */
    public CharSequence getTextOff() {
        return mTextOff;
    }

    /**
     * Sets the text displayed when the button is not in the checked state.
     */
    public void setTextOff(CharSequence textOff) {
        mTextOff = textOff;
        this.mOffLayout=null;
        requestLayout();
    }
    
    
    /**
     * Interface definition for a callback to be invoked when the switch is
     * in a fixed state and there was an attempt to change its state either
     * via a click or drag
     */
    public static interface OnChangeAttemptListener {
        /**
         * Called when an attempt was made to change the checked state of the 
         * switch while the switch was in a fixed state.
         *
         * @param isChecked  The current state of switch.
         */
        void onChangeAttempted(boolean isChecked);
    }

    /**
     * Register a callback to be invoked when there is an attempt to change the
     * state of the switch when its in fixated
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnChangeAttemptListener(OnChangeAttemptListener listener) {
        mOnChangeAttemptListener = listener;
    }
    
    
    /**
     * fixates the switch on one of the positions ON or OFF.
     * if the switch is fixated, then it cannot be switched to the other position
     * @param fixed   If true, sets the switch to fixed mode. 
     *                If false, sets the switch to switched mode. 
     * @param onOrOff The switch position to which it will be fixed.
     *                If it is true then the switch is fixed on ON.
     *                If it is false then the switch is fixed on OFF
     * @Note The position is only fixed from the user interface. It can still be
     *       changed through program by using {@link #setChecked(boolean) setChecked} 
     */
    public void fixate(boolean fixed, boolean onOrOff) {
        fixate(fixed);
    	this.onOrOff = onOrOff;
    	if (onOrOff)
    		this.setChecked(true);
    }

    /**
     * fixates the switch on one of the positions ON or OFF.
     * if the switch is fixated, then it cannot be switched to the other position
     * @param fixed   if true, sets the switch to fixed mode. 
     *                if false, sets the switch to switched mode. 
     */
    public void fixate(boolean fixed) {
    	this.fixed = fixed;
    }

    /**
     * returns if the switch is fixed to one of its positions
     */
    public boolean isFixed() {
    	return fixed;
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);


        if (mOnLayout == null) {
            mOnLayout = makeLayout(mTextOn);
        }
        if (mOffLayout == null) {
            mOffLayout = makeLayout(mTextOff);
        }

        //mTrackDrawable.getPadding(mTrackPaddingRect);
        final int maxTextWidth = Math.max(mOnLayout.getWidth(), mOffLayout.getWidth());
        final int maxTextHeight = Math.max(mOnLayout.getHeight(), mOffLayout.getHeight());
        mThumbWidth = maxTextWidth + mThumbTextPadding * 2;
        if (mTextOnThumb == false) {
            mThumbWidth = mThumbDrawable.getIntrinsicWidth() ;
            if (mThumbWidth <15) {
                //TODO: change this to something gueessed based on the other parameters.
            	mThumbWidth = 15;
            }
        }

        int switchWidth = Math.max(mSwitchMinWidth, maxTextWidth * 2 +
        		                                    mThumbTextPadding * 2 +
        		                                    mTrackTextPadding * 2 +
        		                                    mTrackPaddingRect.left +
        		                                    mTrackPaddingRect.right);
        if (mTextOnThumb == false) {
            switchWidth = Math.max(  maxTextWidth
            		               + mThumbWidth + mTrackTextPadding * 2
            		               + mTrackPaddingRect.right
            		               + mTrackPaddingRect.left
            		               , mSwitchMinWidth); 
        	                      
        }
        	
        final int trackHeight = mTrackDrawable.getIntrinsicHeight();
        final int thumbHeight = mThumbDrawable.getIntrinsicHeight();
        int switchHeight = Math.max(mSwitchMinHeight, maxTextHeight);
            switchHeight = Math.max(trackHeight, switchHeight);
            switchHeight = Math.max(switchHeight, thumbHeight);


        switch (widthMode) {
            case MeasureSpec.AT_MOST:
                widthSize = Math.min(widthSize, switchWidth);
                break;

            case MeasureSpec.UNSPECIFIED:
                widthSize = switchWidth;
                break;

            case MeasureSpec.EXACTLY:
                // Just use what we were given
                break;
        }

        switch (heightMode) {
            case MeasureSpec.AT_MOST:
                heightSize = Math.min(heightSize, switchHeight);
                break;

            case MeasureSpec.UNSPECIFIED:
                heightSize = switchHeight;
                break;

            case MeasureSpec.EXACTLY:
                // Just use what we were given
                break;
        }

        mSwitchWidth = switchWidth;
        mSwitchHeight = switchHeight;

        //Log.d(TAG, "mSwitchWidth="+mSwitchWidth+" mSwitchHeight="+mSwitchHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int measuredHeight = getMeasuredHeight();
        final int measuredWidth = getMeasuredWidth();
        if (measuredHeight < switchHeight) {
            setMeasuredDimension(getMeasuredWidth(), switchHeight);
        }
        if (measuredWidth < switchWidth) {
            setMeasuredDimension(switchWidth, getMeasuredHeight());
        } 
    }

  
    private Layout makeLayout(CharSequence text) {
        return new StaticLayout(text, mTextPaint,
                (int) Math.ceil(Layout.getDesiredWidth(text, mTextPaint)),
                Layout.Alignment.ALIGN_NORMAL, 1.f, 0, true);
    }

    /**
     * @return true if (x, y) is within the target area of the switch thumb
     */
    private boolean hitThumb(float x, float y) {
        //mThumbDrawable.getPadding(mThumbPaddingRect);
        final int thumbTop = mSwitchTop - mTouchSlop;
        final int thumbLeft = mSwitchLeft + (int) (mThumbPosition + 0.5f) - mTouchSlop;
        final int thumbRight = thumbLeft + mThumbWidth +
                mThumbPaddingRect.left + mThumbPaddingRect.right + mTouchSlop;
        final int thumbBottom = mSwitchBottom + mTouchSlop;
        return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
    }

    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	//if (fixed) {
    		//Log.d(TAG, "the switch position is fixed to " + (onOrOff ? "On":"Off") + "position.");
    		//return true;
    	//}
        mVelocityTracker.addMovement(ev);
        //Log.d(TAG, "onTouchEvent(ev="+ev.toString()+")");
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    mTouchMode = TOUCH_MODE_DOWN;
                    mTouchX = x;
                    mTouchY = y;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                switch (mTouchMode) {
                    case TOUCH_MODE_IDLE:
                        // Didn't target the thumb, treat normally.
                        break;

                    case TOUCH_MODE_DOWN: {
                        final float x = ev.getX();
                        final float y = ev.getY();
                        if (Math.abs(x - mTouchX) > mTouchSlop ||
                                Math.abs(y - mTouchY) > mTouchSlop) {
                            mTouchMode = TOUCH_MODE_DRAGGING;
            			    if (getParent() != null) {
            				    getParent().requestDisallowInterceptTouchEvent(true);
            			    }
                            mTouchX = x;
                            mTouchY = y;
                            return true;
                        }
                        break;
                    }

                    case TOUCH_MODE_DRAGGING: {
                        final float x = ev.getX();
                        final float dx = x - mTouchX;
                        float newPos = Math.max(0,
                                Math.min(mThumbPosition + dx, getThumbScrollRange()));
                        if (newPos != mThumbPosition) {
                            mThumbPosition = newPos;
                            mTouchX = x;
                            invalidate();
                        }
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    stopDrag(ev);
                    return true;
                }
                mTouchMode = TOUCH_MODE_IDLE;
                mVelocityTracker.clear();
                break;
            }
        }

        boolean flag = super.onTouchEvent(ev);
        //Log.d(TAG, "super.onTouchEvent(ev) returned="+flag);
        return flag;
    }
    
    /*   
	OnClickListener clickListener = new OnClickListener() {
		public void onClick(View v) {
            Log.d(TAG, "onClick()");
            animateThumbToCheckedState(isChecked());
		}
	}
	*/
	
	@Override
	public boolean performClick() {
		if (!clickDisabled) {
			// Log.d(TAG, "performClick(). current Value="+isChecked());
			if (!fixed) {
				boolean flag = super.performClick();
				// Log.d(TAG,
				// "after super.performClick().  Value="+isChecked());
				return flag;
			} else {
				if (this.mOnChangeAttemptListener != null)
					this.mOnChangeAttemptListener
							.onChangeAttempted(isChecked());
				return false;
			}
		} else {
			return false;
		}
	}

    public void disableClick() {
    	clickDisabled=true;
    }


    
    public void enableClick() {
    	clickDisabled=false;
    }

    /*
        public  void toggleWithAnimation(boolean animate) {
    	toggle();
    	if (animate) {
            animateThumbToCheckedState(isChecked());
    	}
    }
    
    //@Override
    public boolean isChecked1() {
    	Log.d(TAG, "isChecked()-mTextOnThumb="+mTextOnThumb);
        if (mTextOnThumb) {
    	    Log.d(TAG, "returning = "+super.isChecked());
        	return super.isChecked();
        }
   	    Log.d(TAG, "returning="+(!(super.isChecked())));
       	return !(super.isChecked());
    }
    */
    
    public CharSequence getCurrentText() {
    	if (isChecked())
    		return mTextOn;
    	
    	return mTextOff;
    }
    
    public CharSequence getText(boolean checkedState) {
    	if (checkedState) 
    		return mTextOn;
    	else
    		return mTextOff;
    }
    
	

    private void cancelSuperTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    /**
     * Called from onTouchEvent to end a drag operation.
     *
     * @param ev Event that triggered the end of drag mode - ACTION_UP or ACTION_CANCEL
     */
    private void stopDrag(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_IDLE;
        // Up and not canceled, also checks the switch has not been disabled during the drag
        boolean commitChange = ev.getAction() == MotionEvent.ACTION_UP && isEnabled();
        
        //check if the swtich is fixed to a position
        commitChange = commitChange && (!fixed);
        Log.d(TAG,"commitChange="+commitChange);

        cancelSuperTouch(ev);

        if (commitChange) {
            boolean newState;
            mVelocityTracker.computeCurrentVelocity(1000);
            float xvel = mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > mMinFlingVelocity) {
                newState = xvel > 0;
            } else {
                newState = getTargetCheckedState();
            }
            if (mTextOnThumb)
                animateThumbToCheckedState(newState);
            else
                animateThumbToCheckedState(!newState);
        } else {
            animateThumbToCheckedState(isChecked());
            if (fixed) 
			if (this.mOnChangeAttemptListener != null)
				this.mOnChangeAttemptListener.onChangeAttempted(isChecked());
        }
    }

    private void animateThumbToCheckedState(boolean newCheckedState) {
        // TODO animate!
        //float targetPos = newCheckedState ? 0 : getThumbScrollRange();
        //mThumbPosition = targetPos;
        setChecked(newCheckedState);
    }

    private boolean getTargetCheckedState() {
        return mThumbPosition >= getThumbScrollRange() / 2;
    }

    @Override
    public void setChecked(boolean checked) {
        //Log.d(TAG, "setChecked("+checked+")");
    	boolean lc = checked;
        if (!mTextOnThumb) {
    	    lc = !checked;
        }
        super.setChecked(checked);
        mThumbPosition = lc ? getThumbScrollRange() : 0;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		//LoggingUtility.d(TAG, "onLayout()-left=" + left + ",top="+top+",right="+right+",bottom="+bottom);
        super.onLayout(changed, left, top, right, bottom);
        

        int switchTop = 0;
        int switchBottom = 0;
        switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
            default:
            case Gravity.TOP:
                switchTop = getPaddingTop();
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.CENTER_VERTICAL:
                switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 -
                        mSwitchHeight / 2;
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.BOTTOM:
                switchBottom = getHeight() - getPaddingBottom();
                switchTop = switchBottom - mSwitchHeight;
                break;
        }

        mSwitchWidth = right - left;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = mSwitchWidth-getPaddingRight();
        mSwitchLeft = mSwitchRight - mSwitchWidth;
        mSwitchHeight = switchBottom - switchTop;
        if (this.mTextOnThumb) {
            mThumbPosition = isChecked() ? getThumbScrollRange() : 0;
        } else  {
            mThumbPosition = isChecked() ? 0 : getThumbScrollRange();
        }
        
        //Log.d(TAG, "getWidth()="+getWidth()+" getHeight()="+getHeight());
        //Log.d(TAG, "getPaddingLeft()="+getPaddingLeft()+" getPaddingRight()="+getPaddingRight());
        //Log.d(TAG, "getPaddingTop()="+getPaddingTop()+" getPaddingBottom()="+getPaddingBottom());
        
        //Log.d(TAG, "mSwitchLeft="+mSwitchLeft+" mSwitchRight="+mSwitchRight);
        //Log.d(TAG, "mSwitchTop="+mSwitchTop+" mSwitchBottom="+mSwitchBottom);
    }
 
    protected void onLayout_orig(boolean changed, int left, int top, int right, int bottom) {
		Log.d(TAG, "left=" + left + ",top="+top+",right="+right+",bottom="+bottom);
        super.onLayout(changed, left, top, right, bottom);

        mThumbPosition = isChecked() ? getThumbScrollRange() : 0;

        int switchRight = getWidth() - getPaddingRight();
        int switchLeft = switchRight - mSwitchWidth;
        int switchTop = 0;
        int switchBottom = 0;
        switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
            default:
            case Gravity.TOP:
                switchTop = getPaddingTop();
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.CENTER_VERTICAL:
                switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 -
                        mSwitchHeight / 2;
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.BOTTOM:
                switchBottom = getHeight() - getPaddingBottom();
                switchTop = switchBottom - mSwitchHeight;
                break;
        }

        mSwitchLeft = switchLeft;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = switchRight;
        Log.d(TAG, "mSwitchLeft="+mSwitchLeft+" mSwitchRight="+mSwitchRight);
        Log.d(TAG, "mSwitchTop="+mSwitchTop+" mSwitchBottom="+mSwitchBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Log.d(TAG, "onDraw()");
        // Draw the switch
        int switchLeft = mSwitchLeft;
        int switchTop = mSwitchTop;
        int switchRight = mSwitchRight;
        int switchBottom = mSwitchBottom;

        //draw the track
        mTrackDrawable.setBounds(switchLeft, switchTop, switchRight, switchBottom);
        mTrackDrawable.draw(canvas);


        // evaluate the cordinates for drawing the Thumb and Text
        //mTrackDrawable.getPadding(mTrackPaddingRect);
        int switchInnerLeft = switchLeft + mTrackPaddingRect.left;
        int switchInnerTop = switchTop + mTrackPaddingRect.top;
        int switchInnerRight = switchRight - mTrackPaddingRect.right;
        int switchInnerBottom = switchBottom - mTrackPaddingRect.bottom;
        canvas.clipRect(switchInnerLeft, switchTop, switchInnerRight, switchBottom);
        //Log.d(TAG, "switchInnerLeft="+switchInnerLeft+" switchInnerRight="+switchInnerRight);
        //Log.d(TAG, "switchInnerTop="+switchInnerTop+" switchInnerBottom="+switchInnerBottom);

        // mTextColors should not be null, but just in case
        if (mTextColors != null) {
            mTextPaint.setColor(mTextColors
            		.getColorForState(getDrawableState(), mTextColors.getDefaultColor()));
        }
        mTextPaint.drawableState = getDrawableState();
        
        // draw the texts for On/Off in reduced alpha mode.
        //mThumbDrawable.getPadding(mThumbPaddingRect);
        int alpha = mTextPaint.getAlpha();
        mTextPaint.setAlpha(alpha/3);
        int thumbL = switchInnerLeft + mThumbPaddingRect.left;
        int thumbR = switchInnerLeft + mThumbWidth - mThumbPaddingRect.right;
        int dxOffText = mTextOnThumb ?   (thumbL + thumbR)/2
        		                         - mOffLayout.getWidth()/2
        		                         - mThumbTextPadding //(thumbL+thumbR)/2 already has 2*mThumbTextPadding
        		                                             // so we have to subtract it
        		                         + mTrackTextPadding
        		                       : switchInnerLeft
        		                         + mTrackTextPadding;

        int thumbPos =  getThumbScrollRange();
        thumbL = thumbL + thumbPos;
        thumbR = thumbR + thumbPos;
        int dxOnText = mTextOnThumb ?   (thumbL + thumbR)/2 //(thumbL + thumbR)/2 already has the ThumbTextPadding
        		                                             // so we dont have to add it
        		                        - mOnLayout.getWidth()/2
        		                      : switchInnerRight
        		                        - mOnLayout.getWidth()
       		                            - mTrackTextPadding;
        int switchVerticalMid = (switchInnerTop + switchInnerBottom) / 2;

        if (getTargetCheckedState()) {
           	canvas.save();
           	canvas.translate(dxOnText, switchVerticalMid - mOnLayout.getHeight() / 2);
	        if (canvas.getClipBounds(canvasClipBounds)) {
	            canvasClipBounds.left += (mThumbPosition+ mThumbWidth/2);
           	    canvas.clipRect(canvasClipBounds);
	        }
            mOnLayout.draw(canvas);
            canvas.restore();
            
            if (mTextOnThumb == false)
                mTextPaint.setAlpha(alpha);
            canvas.save();
            canvas.translate(dxOffText, switchVerticalMid - mOffLayout.getHeight() / 2);
	        if (canvas.getClipBounds(canvasClipBounds)) {
	            canvasClipBounds.right -= (thumbPos- mThumbPosition + mThumbWidth/2);
           	    canvas.clipRect(canvasClipBounds);
	        }
            mOffLayout.draw(canvas);
            canvas.restore();
        }
        else {
            canvas.save();
            canvas.translate(dxOffText, switchVerticalMid - mOffLayout.getHeight() / 2);
	        if (canvas.getClipBounds(canvasClipBounds)) {
	            canvasClipBounds.right -= (thumbPos - mThumbPosition + mThumbWidth/2);
           	    canvas.clipRect(canvasClipBounds);
	        }
            mOffLayout.draw(canvas);
            canvas.restore();
            
            if (mTextOnThumb == false)
                mTextPaint.setAlpha(alpha);
          	canvas.save();
           	canvas.translate(dxOnText, switchVerticalMid - mOnLayout.getHeight() / 2);
	        if (canvas.getClipBounds(canvasClipBounds)) {
	            canvasClipBounds.left += (mThumbPosition+ mThumbWidth/2);
           	    canvas.clipRect(canvasClipBounds);
	        }
            mOnLayout.draw(canvas);
            canvas.restore();
        }
            	
        //Draw the Thumb
        thumbPos = (int) (mThumbPosition + 0.5f);
        thumbL = switchInnerLeft + mThumbPaddingRect.left + thumbPos;
        thumbR = switchInnerLeft + thumbPos + mThumbWidth - mThumbPaddingRect.right;
        mThumbDrawable.setBounds(thumbL, switchTop, thumbR, switchBottom);
        mThumbDrawable.draw(canvas);
        	
        //Draw the text on the Thumb
        mTextPaint.setAlpha(alpha);
        Layout onSwitchText = getTargetCheckedState() ? mOnLayout : mOffLayout;
        if (mTextOnThumb) {
            canvas.save();
            canvas.translate((thumbL + thumbR) / 2 - onSwitchText.getWidth() / 2,
                    (switchInnerTop + switchInnerBottom) / 2 - onSwitchText.getHeight() / 2);
            onSwitchText.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    public int getCompoundPaddingRight() {
        int padding = super.getCompoundPaddingRight() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    private int getThumbScrollRange() {
        if (mTrackDrawable == null) {
            return 0;
        }
        //mTrackDrawable.getPadding(mTrackPaddingRect);
        int range = mSwitchWidth - mThumbWidth - mTrackPaddingRect.left - mTrackPaddingRect.right;
        //Log.d(TAG,"getThumbScrollRange() = "+ range);
        return range;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        int[] myDrawableState = getDrawableState();

        // Set the state of the Drawable
        // Drawable may be null when checked state is set from XML, from super constructor
        if (mThumbDrawable != null) mThumbDrawable.setState(myDrawableState);
        if (mTrackDrawable != null) mTrackDrawable.setState(myDrawableState);

        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrackDrawable;
    }
}
