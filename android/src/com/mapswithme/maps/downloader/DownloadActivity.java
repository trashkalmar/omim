package com.mapswithme.maps.downloader;

import android.os.Bundle;

import com.mapswithme.maps.base.BaseMwmFragmentActivity;

public class DownloadActivity extends BaseMwmFragmentActivity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    replaceFragment(DownloadFragment.class, null, null);
  }
}
