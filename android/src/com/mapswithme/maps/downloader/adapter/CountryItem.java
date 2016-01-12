package com.mapswithme.maps.downloader.adapter;

public class CountryItem
{
  public final String id;
  public final String name;
  public final long size;
  public final int childrenCount;
  public final String parentName;
  public final int status;

  public CountryItem(String id, String name, int size, int childrenCount, String parentName, int status)
  {
    this.id = id;
    this.name = name;
    this.size = size;
    this.childrenCount = childrenCount;
    this.parentName = parentName;
    this.status = status;
  }
}
