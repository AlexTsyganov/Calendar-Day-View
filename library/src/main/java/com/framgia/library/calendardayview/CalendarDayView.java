package com.framgia.library.calendardayview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.framgia.library.calendardayview.data.IEvent;
import com.framgia.library.calendardayview.data.IPopup;
import com.framgia.library.calendardayview.data.ITimeDuration;
import com.framgia.library.calendardayview.decoration.CdvDecoration;
import com.framgia.library.calendardayview.decoration.CdvDecorationDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by FRAMGIA\pham.van.khac on 07/07/2016.
 */
public class CalendarDayView extends FrameLayout {

    private int mDayHeight = 0;

    private int mEventMarginLeft = 0;

    private int mHourWidth = 120;

    private int mTimeHeight = 120;

    private int mSeparateHourHeight = 0;

    private int mStartHour = 0;

    private int mEndHour = 24;

    private LinearLayout mLayoutDayView;

    private FrameLayout mLayoutEvent;

    private FrameLayout mLayoutPopup;

    private CdvDecoration mDecoration;

    private List<? extends IEvent> mEvents;

    private List<? extends IPopup> mPopups;

    private OnTimeLineLongClickListener timeLineLongClickListener;

    public CalendarDayView(Context context) {
        super(context);
        init(null);
    }

    public CalendarDayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CalendarDayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        LayoutInflater.from(getContext()).inflate(R.layout.view_day_calendar, this, true);

        mLayoutDayView = (LinearLayout) findViewById(R.id.dayview_container);
        mLayoutEvent = (FrameLayout) findViewById(R.id.event_container);
        mLayoutPopup = (FrameLayout) findViewById(R.id.popup_container);

        mDayHeight = getResources().getDimensionPixelSize(R.dimen.dayHeight);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CalendarDayView);
            try {
                mEventMarginLeft =
                    a.getDimensionPixelSize(R.styleable.CalendarDayView_eventMarginLeft,
                        mEventMarginLeft);
                mDayHeight =
                    a.getDimensionPixelSize(R.styleable.CalendarDayView_dayHeight, mDayHeight);
                mStartHour = a.getInt(R.styleable.CalendarDayView_startHour, mStartHour);
                mEndHour = a.getInt(R.styleable.CalendarDayView_endHour, mEndHour);
            } finally {
                a.recycle();
            }
        }

        mEvents = new ArrayList<>();
        mPopups = new ArrayList<>();
        mDecoration = new CdvDecorationDefault(getContext());

        refresh();
        setupCustomLongClickListener();
    }

    public void refresh() {
        drawDayViews();

        drawEvents();

        drawPopups();
    }

    private void drawDayViews() {
        mLayoutDayView.removeAllViews();
        DayView dayView = null;
        for (int i = mStartHour; i <= mEndHour; i++) {
            dayView = getDecoration().getDayView(i);
            mLayoutDayView.addView(dayView);
        }
        mHourWidth = (int) dayView.getHourTextWidth();
        mTimeHeight = (int) dayView.getHourTextHeight();
        mSeparateHourHeight = (int) dayView.getSeparateHeight();
    }

    private void drawEvents() {
        mLayoutEvent.removeAllViews();

        for (IEvent event : mEvents) {
            Rect rect = getTimeBound(event);

            // add event view
            EventView eventView =
                getDecoration().getEventView(event, rect, mTimeHeight, mSeparateHourHeight);
            if (eventView != null) {
                mLayoutEvent.addView(eventView, eventView.getLayoutParams());
            }
        }
    }

    private void drawPopups() {
        mLayoutPopup.removeAllViews();

        for (IPopup popup : mPopups) {
            Rect rect = getTimeBound(popup);

            // add popup views
            PopupView view =
                getDecoration().getPopupView(popup, rect, mTimeHeight, mSeparateHourHeight);
            if (popup != null) {
                mLayoutPopup.addView(view, view.getLayoutParams());
            }
        }
    }

    private void setupCustomLongClickListener() {
        final List<Float> y = new ArrayList<>();
        mLayoutDayView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    y.clear();
                    y.add(event.getY());
                }
                return false;
            }
        });
        mLayoutDayView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (timeLineLongClickListener != null && y.size() == 1) {
                    timeLineLongClickListener.timeLineSelect(getTimeLine(y.get(0)));
                    return true;
                }
                return false;
            }
        });
    }

    private Calendar getTimeLine(float y) {
        Calendar calendar = Calendar.getInstance();
        float d = 120 * (y - mTimeHeight / 2 - mSeparateHourHeight) / (mDayHeight + mDayHeight) + 60*mStartHour;
        int minute = (int) (d % 60);
        int hour = (int) (d / 60);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Rect getTimeBound(ITimeDuration event) {
        Rect rect = new Rect();
        rect.top = getPositionOfTime(event.getStartTime()) + mTimeHeight / 2 + mSeparateHourHeight;
        rect.bottom = getPositionOfTime(event.getEndTime()) + mTimeHeight / 2 + mSeparateHourHeight;
        rect.left = mHourWidth + mEventMarginLeft;
        rect.right = getWidth();
        return rect;
    }

    private int getPositionOfTime(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY) - mStartHour;
        int minute = calendar.get(Calendar.MINUTE);
        return hour * mDayHeight + minute * mDayHeight / 60;
    }

    public void setEvents(List<? extends IEvent> events) {
        this.mEvents = events;
        refresh();
    }

    public void setPopups(List<? extends IPopup> popups) {
        this.mPopups = popups;
        refresh();
    }

    public void setTimeLineLongClickListener(OnTimeLineLongClickListener timeLineLongClickListener) {
        this.timeLineLongClickListener = timeLineLongClickListener;
    }

    public void setLimitTime(int startHour, int endHour) {
        if (startHour >= endHour) {
            throw new IllegalArgumentException("start hour must before end hour");
        }
        mStartHour = startHour;
        mEndHour = endHour;
        refresh();
    }

    /**
     * @param decorator decoration for draw event, popup, time
     */
    public void setDecorator(@NonNull CdvDecoration decorator) {
        this.mDecoration = decorator;
        refresh();
    }

    public CdvDecoration getDecoration() {
        return mDecoration;
    }

    public interface OnTimeLineLongClickListener {
        void timeLineSelect(Calendar start);
    }
}
