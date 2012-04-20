package com.shellware.CarHome;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class GaugeNeedle extends ImageView {

	private final int NOT_SET_YET = Integer.MAX_VALUE;
	
	private int offsetCenterInDegrees = NOT_SET_YET;
	private float lastValue; 
	
	private int minValue = 0;
	private int maxValue = 360;
	private int minDegrees = -180;
	private int maxDegrees = 180;
	private float pivotPoint = .5f;

	public GaugeNeedle(Context context) {
		super(context);
	}
	public GaugeNeedle(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public GaugeNeedle(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setValue(final float value) {
		
		// calculate our center point if it has not yet been calculated
		if (offsetCenterInDegrees == NOT_SET_YET) {
			offsetCenterInDegrees = maxDegrees - (maxDegrees - minDegrees) / 2 ;
		}
		
		final float scale = (float) (maxDegrees - minDegrees) / (maxValue - minValue) ;
		final float newValue = (value - minValue) * scale - ((maxDegrees - minDegrees) / 2) + offsetCenterInDegrees;
		
		// bail if our values are significantly beyond our borders (20+ degrees)
		if (newValue > maxDegrees + 20) return;
		if (newValue < minDegrees - 20) return;

		RotateAnimation rotateAnimation = new RotateAnimation(lastValue, newValue, 
			  Animation.RELATIVE_TO_SELF, 0.5f, 
			  Animation.RELATIVE_TO_SELF, pivotPoint);

		rotateAnimation.setInterpolator(new LinearInterpolator());
		rotateAnimation.setDuration(250);
		rotateAnimation.setFillAfter(true);	
	
		startAnimation(rotateAnimation);
		
		lastValue = newValue;
		//Log.d("Tester", String.valueOf(value) + " - " + String.valueOf(newValue) + " - " + String.valueOf(scale));
	}
	

	public int getMinValue() {
		return minValue;
	}
	public void setMinValue(int minValue) {
		this.minValue = minValue;
	}
	public int getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}
	public int getMinDegrees() {
		return minDegrees;
	}
	public void setMinDegrees(int minDegrees) {
		this.minDegrees = minDegrees;
	}
	public int getMaxDegrees() {
		return maxDegrees;
	}
	public void setMaxDegrees(int maxDegrees) {
		this.maxDegrees = maxDegrees;
	}
	public float getPivotPoint() {
		return pivotPoint;
	}
	public void setPivotPoint(float pivotPoint) {
		this.pivotPoint = pivotPoint;
	}

}
