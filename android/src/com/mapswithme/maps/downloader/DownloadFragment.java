package com.mapswithme.maps.downloader;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.mapswithme.maps.R;
import com.mapswithme.maps.base.BaseMwmRecyclerFragment;
import com.mapswithme.maps.downloader.adapter.BaseDownloadAdapter;
import com.mapswithme.maps.widget.SearchToolbarController;
import com.mapswithme.util.UiUtils;

public class DownloadFragment extends BaseMwmRecyclerFragment
{
  class DownloadToolbarController extends SearchToolbarController
  {
    public DownloadToolbarController(View root, Activity activity)
    {
      super(root, activity);
    }

    public void showQuery(boolean show)
    {
      UiUtils.showIf(show, mQuery);
    }
  }

  private DownloadToolbarController mToolbarController;
  private View mPanel;
  private TextView mPanelAction;
  private TextView mPanelText;

  private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState)
    {
      if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
        mToolbarController.deactivate();
    }
  };

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);

    // TODO if map was migrated or postponedto small mwms
    boolean migrated = false;
    boolean postponed = true;
    if (!migrated && !postponed)
      getMwmActivity().replaceFragment(MigrateSmallMwmFragment.class, null, null);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    mToolbarController = new DownloadToolbarController(view, getActivity());
    mPanel = view.findViewById(R.id.bottom_panel);
    mPanelAction = (TextView) mPanel.findViewById(R.id.tv__action);
    mPanelText = (TextView) mPanel.findViewById(R.id.tv__text);
    getRecyclerView().addOnScrollListener(mScrollListener);
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
    getRecyclerView().removeOnScrollListener(mScrollListener);
  }

  @Override
  protected int getLayoutRes()
  {
    return R.layout.fragment_downloader;
  }

  @Override
  protected RecyclerView.Adapter createAdapter()
  {
    return new BaseDownloadAdapter(getActivity());
  }
}
