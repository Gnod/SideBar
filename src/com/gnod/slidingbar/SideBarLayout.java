package com.gnod.slidingbar;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ListView;
import android.widget.Toast;

public class SideBarLayout extends ViewGroup{

	public static final int SIDEBAR_SIDE_LEFT = 0;
	public static final int SIDEBAR_SIDE_RIGHT = 1;
	public static final int SIDEBAR_SIDE_BOTH = 2;
	private static final int PRESSED_MOVE_HORIZANTAL = 0;
	private static final int PRESSED_DOWN = 1;
	private static final int PRESSED_DONE = 2;
	private static final int PRESSED_MOVE_VERTICAL = 3;
	
	protected View leftSideBar = null;
	protected View rightSideBar = null;
	protected View mainView = null;
	protected ListView sideBarList = null;
	private int sideBarWidth = 200;
	private boolean isSideBarOpen = false;
	private int sideBarSide = SIDEBAR_SIDE_LEFT;
	private OnOpenListener openListener;
	private OnCloseListener closeListener;
	private int xOffset;
	private int mainViewLeft;
	private int mainViewTop;
	private int mainViewBottom;
	private int state = PRESSED_DOWN;
	private PointF origPoint = new PointF();
	
	public SideBarLayout(Context context) {
        super(context);
    }
	
	public SideBarLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		leftSideBar = findViewById(R.id.sidebar_layout_sidebar);
		mainView = findViewById(R.id.sidebar_layout_content);
		if(leftSideBar == null) {
			throw new NullPointerException("View SideBar not initialized.");
		}
		
		if(mainView == null ) {
			throw new NullPointerException("View mainView not initialized");
		}
		
		openListener = new OnOpenListener();
		closeListener = new OnCloseListener();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		super.measureChildren(widthMeasureSpec, heightMeasureSpec);
		sideBarWidth = leftSideBar.getMeasuredWidth();
		
		mainViewLeft = mainView.getLeft();
		mainViewTop = mainView.getTop();
		mainViewBottom = mainView.getBottom();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
		int sideBarLeft = l;
		if(sideBarSide == SIDEBAR_SIDE_RIGHT){
			sideBarLeft = r - sideBarWidth;
		}
		leftSideBar.layout(sideBarLeft, t, sideBarLeft + sideBarWidth, leftSideBar.getMeasuredHeight());
		
		if(isSideBarOpen) {
			if(sideBarSide == SIDEBAR_SIDE_LEFT) {
				mainView.layout(l + sideBarWidth, t, r + sideBarWidth, b);
			} else {
				mainView.layout(l - sideBarWidth, t, r - sideBarWidth, b);
			}
		} else {
			mainView.layout(l, t, r, b);
		}
	}

	@Override
	protected void measureChild(View child, int parentWidthMeasureSpec,
			int parentHeightMeasureSpec) {
		if(child == leftSideBar) {
			int mode = MeasureSpec.getMode(parentWidthMeasureSpec);
			int width = (int)(parentWidthMeasureSpec * 0.8);
			super.measureChild(child, MeasureSpec.makeMeasureSpec(width, mode), parentHeightMeasureSpec);
		} else {
			super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec);
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(mainView.getAnimation() != null) {
			return false;
		}
		
		int x = (int) ev.getX();
		int y = (int) ev.getY();
		if(!(mainView.getLeft() < x &&
			mainView.getRight() > x &&
			mainView.getTop() < y &&
			mainView.getBottom() > y)) {
			return false;
		}
		
		switch(ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			state = PRESSED_DOWN;
			if(sideBarSide == SIDEBAR_SIDE_LEFT)
				leftSideBar.setVisibility(View.VISIBLE);
			if(sideBarSide == SIDEBAR_SIDE_RIGHT)
				rightSideBar.setVisibility(View.VISIBLE);
			origPoint.set(ev.getX(), ev.getY());
			mainViewLeft = mainView.getLeft();
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if(state == PRESSED_MOVE_HORIZANTAL)
				toggleSideBar();
			state = PRESSED_DONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if(state  != PRESSED_DOWN && state != PRESSED_MOVE_HORIZANTAL)
				break;
			xOffset = (int) (ev.getX() - origPoint.x);
			int yOffset = (int)(ev.getY() - origPoint.y);
			if(state == PRESSED_DOWN) {
				if(yOffset > 10 || yOffset < -10) {
					state = PRESSED_MOVE_VERTICAL;
					break;
				}
				if(xOffset < -50 || xOffset > 50) {
					state = PRESSED_MOVE_HORIZANTAL;
					ev.setAction(MotionEvent.ACTION_CANCEL);
				}
			} else if(state == PRESSED_MOVE_HORIZANTAL) {
				meansureManiViewLayout();
				return true;
			}
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	public void meansureManiViewLayout(){
		int left = mainViewLeft + xOffset;

		if(sideBarSide == SIDEBAR_SIDE_LEFT && left < 0) {
			return;
		}else if(sideBarSide == SIDEBAR_SIDE_RIGHT && left > 0){
			return;
		}else if(sideBarSide == SIDEBAR_SIDE_BOTH) {
			if(left > 0 && leftSideBar.getVisibility() != View.VISIBLE){
				leftSideBar.setVisibility(View.VISIBLE);
				rightSideBar.setVisibility(View.INVISIBLE);
			}
			if(left < 0 && rightSideBar.getVisibility() != View.VISIBLE){
				rightSideBar.setVisibility(View.VISIBLE);
				leftSideBar.setVisibility(View.INVISIBLE);
			}
		}
		
		if(left > sideBarWidth){
			left = sideBarWidth;
			xOffset = left - mainViewLeft;
		}else if(left < -sideBarWidth) {
			left = - sideBarWidth;
			xOffset = left - mainViewLeft;
		}

		mainView.layout(left, mainViewTop, 
				left + mainView.getWidth(), mainViewBottom);
	}

	public boolean isSideBarOpen() {
		return isSideBarOpen;
	}
	public void openSideBar(){
		if(!isSideBarOpen) {
			toggleSideBar();
		}
	}
	
	public void closeSideBar() {
		if(isSideBarOpen) {
			toggleSideBar();
		}
	}
	
	public void toggleSideBar() {
		if(mainView.getAnimation() != null) {
			return;
		}
		
		int time;
		Animation anim = null;
		if(isSideBarOpen) {
			anim = new TranslateAnimation(0, - mainView.getLeft(), 0, 0);
			time = 500 * Math.abs(mainView.getLeft())/sideBarWidth;
			anim.setAnimationListener(closeListener);
		} else {
			if(sideBarSide == SIDEBAR_SIDE_LEFT || mainView.getLeft() > 0) {
				anim = new TranslateAnimation(0, sideBarWidth - mainView.getLeft(), 0, 0);
			} else {
				anim = new TranslateAnimation(0, - sideBarWidth - mainView.getLeft(), 0, 0);
			}
			time = 500 * ( sideBarWidth - Math.abs(mainView.getLeft()))/sideBarWidth;
			anim.setAnimationListener(openListener);
		}
		anim.setDuration(time);
		anim.setFillAfter(true);
		anim.setFillEnabled(true);
		mainView.startAnimation(anim);
	}
	
	public void setSideBarSide(int b) {
		this.sideBarSide = b;
	}
	
	private class OnOpenListener implements Animation.AnimationListener {
		@Override
		public void onAnimationStart(Animation animation){
			View sideBar = null;
			if(sideBarSide == SIDEBAR_SIDE_LEFT)
				sideBar = leftSideBar;
			else if(sideBarSide == SIDEBAR_SIDE_RIGHT)
				sideBar = rightSideBar;
			else {
				if(mainView.getLeft() < 0)
					sideBar = leftSideBar;
				else 
					sideBar = rightSideBar;
			}
				
			if(sideBar.getVisibility() != View.VISIBLE)
				sideBar.setVisibility(View.VISIBLE);
			
		}
		@Override
		public void onAnimationEnd(Animation animation) {
			isSideBarOpen = true;
			mainView.clearAnimation();
			requestLayout();
		}
		@Override
		public void onAnimationRepeat(Animation animation) {
		}
	}
	
	private class OnCloseListener implements Animation.AnimationListener {
		@Override
		public void onAnimationStart(Animation animation) {
		}
		@Override
		public void onAnimationEnd(Animation animation) {
			isSideBarOpen = false;
			mainView.clearAnimation();
			if(leftSideBar != null && leftSideBar.getVisibility() == View.VISIBLE)
				leftSideBar.setVisibility(View.INVISIBLE);
			if(rightSideBar != null && rightSideBar.getVisibility() == View.VISIBLE)
				rightSideBar.setVisibility(View.INVISIBLE);
			requestLayout();
		}
		@Override
		public void onAnimationRepeat(Animation animation) {
		}
	}
	
	
	
}
