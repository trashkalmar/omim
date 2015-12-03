package com.mapswithme.maps.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.preference.PreferenceManager;

import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.maps.background.AppBackgroundTracker;

public enum TrackRecorder
{
  INSTANCE;

  private final AlarmManager mAlarmManager = (AlarmManager)MwmApplication.get().getSystemService(Context.ALARM_SERVICE);

  private final LocationHelper.LocationListener mLocationListener = new LocationHelper.LocationListener()
  {
    @Override
    public void onLocationUpdated(Location location)
    {
      LocationHelper.onLocationUpdated(location);
      TrackRecorderWakeService.stop();

      if (nativeIsEnabled())
        scheduleAlarm();
    }

    @Override
    public void onLocationError(int errorCode)
    {
      stop();
    }

    @Override
    public void onCompassUpdated(long time, double magneticNorth, double trueNorth, double accuracy) {}
  };

  public void init()
  {
    MwmApplication.backgroundTracker().addListener(new AppBackgroundTracker.OnTransitionListener()
    {
      @Override
      public void onTransit(boolean foreground)
      {
        if (foreground)
          stopInternal();
        else
          startIfEnabled();
      }
    });
  }

  private static PendingIntent getAlarmIntent()
  {
    return PendingIntent.getBroadcast(MwmApplication.get(), 0, new Intent("com.mapswithme.maps.TRACK_RECORDER_ALARM"), 0);
  }

  public void scheduleAlarm()
  {
    if (!MwmApplication.backgroundTracker().isForeground())
      mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + LocationHelper.INSTANCE.getUpdateInterval(), getAlarmIntent());
  }

  public void startIfEnabled()
  {
    if (PreferenceManager.getDefaultSharedPreferences(MwmApplication.get())
                         .getBoolean(MwmApplication.get().getString(R.string.pref_track_record_enabled), false))
      start();
  }

  public void start()
  {
    scheduleAlarm();
    nativeSetDuration(2);  // TODO: Move to settings
    nativeSetEnabled(true);
  }

  private void stopInternal()
  {
    mAlarmManager.cancel(getAlarmIntent());
    TrackRecorderWakeService.stop();
  }

  public void stop()
  {
    nativeSetEnabled(false);
    stopInternal();
  }

  public static void onWakeAlarm()
  {
    if (nativeIsEnabled())
      TrackRecorderWakeService.start();
  }

  void onServiceStarted()
  {
    LocationHelper.INSTANCE.addLocationListener(mLocationListener, false);
  }

  void onServiceStopped()
  {
    LocationHelper.INSTANCE.removeLocationListener(mLocationListener);
  }

  private static native void nativeSetEnabled(boolean enable);
  private static native boolean nativeIsEnabled();
  private static native void nativeSetDuration(int hours);
  private static native int nativeGetDuration();
}
