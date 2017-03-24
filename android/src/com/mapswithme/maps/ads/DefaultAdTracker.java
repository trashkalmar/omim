package com.mapswithme.maps.ads;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.mapswithme.util.log.Logger;
import com.mapswithme.util.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DefaultAdTracker implements AdTracker, OnAdCacheModifiedListener
{
  private final static Logger LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC);
  private final static String TAG = DefaultAdTracker.class.getSimpleName();
  private final static int GOOD_IMPRESSION_TIME_MS = 2000;
  private final Map<String, TrackInfo> mTracks = new HashMap<>();

  @Override
  public void onViewShown(@NonNull String bannerId)
  {
    LOGGER.d(TAG, "onViewShown bannerId = " + bannerId);
    TrackInfo info = mTracks.get(bannerId);
    if (info != null)
      info.setVisible(true);
  }

  @Override
  public void onViewHidden(@NonNull String bannerId)
  {
    LOGGER.d(TAG, "onViewHidden bannerId = " + bannerId);
    TrackInfo info = mTracks.get(bannerId);
    if (info != null)
      info.setVisible(false);
  }

  @Override
  public void onContentObtained(@NonNull String bannerId)
  {
    LOGGER.d(TAG, "onContentObtained bannerId = " + bannerId);
    TrackInfo info = mTracks.get(bannerId);
    if (info == null)
      throw new AssertionError("A track info must be put in a cache before a content is obtained");

    info.fill();
  }

  @Override
  public boolean isImpressionGood(@NonNull String bannerId)
  {
    TrackInfo info = mTracks.get(bannerId);
    return info != null && info.getShowTime() > GOOD_IMPRESSION_TIME_MS;
  }

  @Override
  public void onRemoved(@NonNull String id)
  {
    LOGGER.d(TAG, "onRemoved id = " + id);
    mTracks.remove(id);
  }

  @Override
  public void onPut(@NonNull String id)
  {
    LOGGER.d(TAG, "onPut id = " + id);
    mTracks.put(id, new TrackInfo());
  }

  private static class TrackInfo
  {
    private long mTimestamp;
    /**
     * Accumulates amount of time that ad is already shown.
     */
    private long mShowTime;
    /**
     * Indicates whether the ad view is visible or not.
     */
    private boolean mVisible;
    /**
     * Indicates whether the ad content is obtained or not.
     */
    private boolean mFilled;

    public void setVisible(boolean visible)
    {
      // No need tracking if the ad is not filled with a content
      if (!mFilled)
        return;

      // If ad becomes visible, and it's filled with a content the timestamp must be stored.
      if (visible && !mVisible)
      {
        mTimestamp = SystemClock.elapsedRealtime();
      }
      // If ad is hidden the show time must be accumulated.
      else if (!visible && mVisible)
      {
        mShowTime += SystemClock.elapsedRealtime() - mTimestamp;
        mTimestamp = 0;
      }
      mVisible = visible;
    }

    public void fill()
    {
      // If the visible ad is filled with the content the timestamp must be stored
      if (mVisible)
      {
        mTimestamp = SystemClock.elapsedRealtime();
      }
      mFilled = true;
    }

    private boolean isRealShow()
    {
      return mFilled && mVisible;
    }

    long getShowTime()
    {
      return mShowTime;
    }
  }
}