/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.modules.timepicker;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Build;

import android.content.DialogInterface;
import android.widget.NumberPicker;
import android.widget.TimePicker;

/**
 * <p>
 *   Certain versions of Android (Jellybean-KitKat) have a bug where when dismissed, the
 *   {@link TimePickerDialog} still calls the OnTimeSetListener. This class works around that issue
 *   by *not* calling super.onStop on KitKat on lower, as that would erroneously call the
 *   OnTimeSetListener when the dialog is dismissed, or call it twice when "OK" is pressed.
 * </p>
 *
 * <p>
 *   See: <a href="https://code.google.com/p/android/issues/detail?id=34833">Issue 34833</a>
 * </p>
 */
public class DismissableTimePickerDialog extends TimePickerDialog {

  private final static int TIME_PICKER_INTERVAL = 5;
  private TimePicker mTimePicker;
  private final OnTimeSetListener mTimeSetListener;

  public DismissableTimePickerDialog(
      Context context,
      @Nullable OnTimeSetListener callback,
      int hourOfDay,
      int minute,
      boolean is24HourView) {
    super(context, callback, hourOfDay, minute / TIME_PICKER_INTERVAL, is24HourView);
    mTimeSetListener = callback;
  }

  public DismissableTimePickerDialog(
      Context context,
      int theme,
      @Nullable OnTimeSetListener callback,
      int hourOfDay,
      int minute,
      boolean is24HourView) {
    super(context, theme, callback, hourOfDay, minute / TIME_PICKER_INTERVAL, is24HourView);
    mTimeSetListener = callback;
  }

  @Override
  protected void onStop() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
      super.onStop();
    }
  }

  @Override
  public void updateTime(int hourOfDay, int minuteOfHour) {
      mTimePicker.setCurrentHour(hourOfDay);
      mTimePicker.setCurrentMinute(minuteOfHour / TIME_PICKER_INTERVAL);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
      switch (which) {
          case BUTTON_POSITIVE:
              if (mTimeSetListener != null) {
                  mTimeSetListener.onTimeSet(mTimePicker, mTimePicker.getCurrentHour(),
                          mTimePicker.getCurrentMinute() * TIME_PICKER_INTERVAL);
              }
              break;
          case BUTTON_NEGATIVE:
              cancel();
              break;
      }
  }

  @Override
  public void onAttachedToWindow() {
      super.onAttachedToWindow();
      try {
          Class<?> classForid = Class.forName("com.android.internal.R$id");
          Field timePickerField = classForid.getField("timePicker");
          mTimePicker = (TimePicker) findViewById(timePickerField.getInt(null));
          Field field = classForid.getField("minute");

          NumberPicker minuteSpinner = (NumberPicker) mTimePicker
              .findViewById(field.getInt(null));
          minuteSpinner.setMinValue(0);
          minuteSpinner.setMaxValue((60 / TIME_PICKER_INTERVAL) - 1);
          List<String> displayedValues = new ArrayList<>();
          for (int i = 0; i < 60; i += TIME_PICKER_INTERVAL) {
              displayedValues.add(String.format("%02d", i));
          }
          minuteSpinner.setDisplayedValues(displayedValues
                  .toArray(new String[displayedValues.size()]));
      } catch (Exception e) {
          e.printStackTrace();
      }
  }

  @Override
  public void setTitle(CharSequence title) {
    super.setTitle(""); // Override title for uniformity across devices.
  }

}
