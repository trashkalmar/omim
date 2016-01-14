package com.mapswithme.maps.downloader.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.cocosw.bottomsheet.BottomSheet;
import com.mapswithme.maps.MapStorage;
import com.mapswithme.maps.R;
import com.mapswithme.maps.widget.WheelProgressView;
import com.mapswithme.util.BottomSheetHelper;
import com.mapswithme.util.StringUtils;
import com.mapswithme.util.UiUtils;

public class BaseDownloadAdapter extends RecyclerView.Adapter<BaseDownloadAdapter.BaseViewHolder>
{
  private final static int MENU_CANCEL = 0;
  private final static int MENU_DOWNLOAD = 1;
  private final static int MENU_UPDATE = 2;
  private final static int MENU_SHOW = 3;
  private final static int MENU_DELETE = 4;
  private final static int MENU_RETRY = 5;

  private Activity mActivity;

  protected static final int TYPE_HEADER = 0;
  protected static final int TYPE_ITEM = 1;

  static List<GroupItem> mGroups = new ArrayList<>();

  // TODO get actual data
  static
  {
    GroupItem nearMe = new GroupItem("1", "Near me");
    nearMe.children.add(new CountryItem("1", "Russia", 1024503, 15, "", MapStorage.DOWNLOAD_FAILED));
    nearMe.children.add(new CountryItem("2", "Belarus", 13424543, 1, "", MapStorage.ON_DISK));
    nearMe.children.add(new CountryItem("3", "Poland", 13543, 1, "", MapStorage.ON_DISK_OUT_OF_DATE));
    mGroups.add(nearMe);
    GroupItem downloaded = new GroupItem("2", "Downloaded");
    downloaded.children.add(new CountryItem("1", "Russia", 64503, 15, "", MapStorage.DOWNLOAD_FAILED));
    downloaded.children.add(new CountryItem("2", "Belarus", 3524543, 1, "", MapStorage.ON_DISK));
    downloaded.children.add(new CountryItem("3", "Poland", 3543, 1, "", MapStorage.ON_DISK_OUT_OF_DATE));
    mGroups.add(downloaded);
    GroupItem list = new GroupItem("3", "List");
    list.children.add(new CountryItem("1", "US", 123024503, 15, "", MapStorage.DOWNLOAD_FAILED));
    list.children.add(new CountryItem("2", "Trololo", 513424543, 1, "", MapStorage.ON_DISK));
    mGroups.add(list);
  }

  protected abstract class BaseViewHolder extends RecyclerView.ViewHolder
  {
    public BaseViewHolder(View itemView)
    {
      super(itemView);
    }

    public abstract void onBind(int position);
  }

  protected class GroupViewHolder extends BaseViewHolder
  {
    public TextView title;
    public ImageView arrow;

    public GroupViewHolder(View itemView)
    {
      super(itemView);
      itemView.setEnabled(false);
      title = (TextView) itemView.findViewById(R.id.tv__title);
      arrow = (ImageView) itemView.findViewById(R.id.iv__expand_indicator);
      // TODO remove? no more expandable layouts?
      UiUtils.hide(arrow);
    }

    @Override
    public void onBind(int position)
    {
      final GroupItem group = getGroupItem(position);
      title.setText(group.name);
    }
  }

  protected class ChildViewHolder extends BaseViewHolder
  {
    public TextView title;
    public TextView subtitle;
    public ImageView status;
    public WheelProgressView progress;
    public TextView size;

    public ChildViewHolder(View itemView)
    {
      super(itemView);
      title = (TextView) itemView.findViewById(R.id.tv__title);
      subtitle = (TextView) itemView.findViewById(R.id.tv__subtitle);
      size = (TextView) itemView.findViewById(R.id.tv__size);
      status = (ImageView) itemView.findViewById(R.id.iv__status);
      progress = (WheelProgressView) itemView.findViewById(R.id.wpv__download_progress);
    }

    @Override
    public void onBind(int position)
    {
      final CountryItem item = getChildItem(position);

      title.setText(item.name);
      UiUtils.setTextAndHideIfEmpty(subtitle, item.childrenCount < 2 ? item.parentName : item.childrenCount + " maps");
      size.setText(StringUtils.getFileSizeString(item.size));
      UiUtils.show(status);
      UiUtils.hide(progress);
      switch (item.status)
      {
      case MapStorage.ON_DISK:
        status.setImageResource(R.drawable.downloader_downloaded);
        break;
      case MapStorage.DOWNLOAD_FAILED:
      case MapStorage.DOWNLOAD_FAILED_OUF_OF_MEMORY:
        status.setImageResource(R.drawable.downloader_update);
        break;
      case MapStorage.ON_DISK_OUT_OF_DATE:
        status.setImageResource(R.drawable.downloader_update);
        break;
      case MapStorage.DOWNLOADING:
        UiUtils.hide(status);
        UiUtils.show(progress);
        // TODO display actual progress
      }

      final CountryItem countryItem = getChildItem(getAdapterPosition());
      itemView.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (countryItem.childrenCount > 1)
          {
            // TODO get child items for item.id, open list of childs
          }
          else
            showOptions(countryItem);
        }
      });
    }
  }

  @Override
  public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
  {
    if (viewType == TYPE_HEADER)
    {
      View groupView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_downloader_group, parent, false);
      return new GroupViewHolder(groupView);
    }
    else
    {
      View childView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_downloader, parent, false);
      return new ChildViewHolder(childView);
    }
  }

  public BaseDownloadAdapter(Activity host)
  {
    mActivity = host;
  }

  @Override
  public void onBindViewHolder(BaseViewHolder holder, int position)
  {
    holder.onBind(position);
  }

  @Override
  public int getItemCount()
  {
    int count = 0;
    for (GroupItem group : mGroups)
    {
      count++; // 1 group item
      count += group.children.size();
    }
    return count;
  }

  @Override
  public int getItemViewType(int position)
  {
    int count = 0;
    for (GroupItem group : mGroups)
    {
      if (position == count)
        return TYPE_HEADER;

      count += group.children.size();
      if (position <= count)
        return TYPE_ITEM;
      count++;
    }

    throw new IllegalStateException("Could not find correct view type. Count : " + getItemCount() + ", position : " + position);
  }

  private void showOptions(CountryItem countryItem)
  {
    BottomSheet.Builder bs = BottomSheetHelper.create(mActivity)
                                              .title(countryItem.name)
                                              .listener(new MenuItem.OnMenuItemClickListener()
                                              {
                                                @Override
                                                public boolean onMenuItemClick(MenuItem item)
                                                {
                                                  // TODO
                                                  return false;
                                                }
                                              });

    switch (countryItem.status)
    {
    case MapStorage.ON_DISK_OUT_OF_DATE:
      BottomSheetHelper.sheet(bs, MENU_UPDATE, R.drawable.ic_update,
                              mActivity.getString(R.string.downloader_update_map) + ", " +
                                  StringUtils.getFileSizeString(countryItem.size));
    case MapStorage.ON_DISK:
      bs.sheet(MENU_SHOW, R.drawable.ic_explore, R.string.zoom_to_country);
      bs.sheet(MENU_DELETE, R.drawable.ic_delete, R.string.downloader_delete_map);
      break;
    case MapStorage.IN_QUEUE:
    case MapStorage.DOWNLOADING:
      bs.sheet(MENU_CANCEL, R.drawable.ic_cancel, R.string.cancel_download);
      break;
    case MapStorage.DOWNLOAD_FAILED:
    case MapStorage.DOWNLOAD_FAILED_OUF_OF_MEMORY:
      bs.sheet(MENU_RETRY, R.drawable.ic_retry, R.string.downloader_retry);
      break;
    case MapStorage.NOT_DOWNLOADED:
      BottomSheetHelper.sheet(bs, MENU_DOWNLOAD, R.drawable.ic_download_map,
                              mActivity.getString(R.string.downloader_download_map) + ", " +
                                  StringUtils.getFileSizeString(countryItem.size));
      break;
    }

    bs.show();
  }

  private GroupItem getGroupItem(int adapterPosition)
  {
    int lastPosition = 0;
    for (int i = 0; i < mGroups.size(); i++)
    {
      if (lastPosition == adapterPosition)
        return mGroups.get(i);

      final GroupItem group = mGroups.get(i);
      lastPosition++; // 1 group item
      lastPosition += group.children.size();
    }

    throw new IllegalStateException("Could not find group from position. Position : " + adapterPosition);
  }

  private CountryItem getChildItem(int adapterPosition)
  {
    int lastPosition = 0;
    for (int i = 0; i < mGroups.size(); i++)
    {
      final GroupItem group = mGroups.get(i);
      lastPosition++; // 1 group item
      lastPosition += group.children.size();

      if (adapterPosition < lastPosition)
        return group.children.get(group.children.size() - (lastPosition - adapterPosition - 1) - 1);
    }

    throw new IllegalStateException("Could not find group index from position. Position : " + adapterPosition);
  }
}
