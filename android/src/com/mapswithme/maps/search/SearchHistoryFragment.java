package com.mapswithme.maps.search;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.mapswithme.maps.R;
import com.mapswithme.maps.base.BaseMwmRecyclerFragment;
import com.mapswithme.maps.widget.SearchToolbarController;
import com.mapswithme.util.UiUtils;

public class SearchHistoryFragment extends BaseMwmRecyclerFragment
{
  private View mPlaceHolder;

  private void updatePlaceholder()
  {
    UiUtils.showIf(getAdapter().getItemCount() == 0, mPlaceHolder);
  }

  @Override
  protected RecyclerView.Adapter createAdapter()
  {
    return new SearchHistoryAdapter(((SearchToolbarController.Container) getParentFragment()).getController());
  }

  @Override
  protected @LayoutRes int getLayoutRes()
  {
    return R.layout.fragment_search_recent;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    mPlaceHolder = view.findViewById(R.id.placeholder);

    getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
    {
      @Override
      public void onChanged()
      {
        updatePlaceholder();
      }
    });
    updatePlaceholder();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    ((SearchFragment) getParentFragment()).setRecyclerScrollListener(getRecyclerView());
  }
}
