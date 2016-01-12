package com.mapswithme.maps.downloader.adapter;

import java.util.ArrayList;
import java.util.List;

public class GroupItem
{
  public final String id;
  public final String name;
  public final List<CountryItem> children = new ArrayList<>();

  public GroupItem(String id, String name)
  {
    this.id = id;
    this.name = name;
  }
}
