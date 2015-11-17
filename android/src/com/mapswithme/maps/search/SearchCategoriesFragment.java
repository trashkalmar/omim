package com.mapswithme.maps.search;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import com.mapswithme.maps.base.BaseMwmRecyclerFragment;

public class SearchCategoriesFragment extends BaseMwmRecyclerFragment
                                   implements CategoriesAdapter.OnCategorySelectedListener
{
  @Override
  protected RecyclerView.Adapter createAdapter()
  {
    return new CategoriesAdapter(this);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    ((SearchFragment) getParentFragment()).setRecyclerScrollListener(getRecyclerView());
  }

  @Override
  public void onCategorySelected(String category)
  {
    if (!passCategory(getParentFragment(), category))
      passCategory(getActivity(), category);
  }

  private static boolean passCategory(Object listener, String category)
  {
    if (!(listener instanceof CategoriesAdapter.OnCategorySelectedListener))
      return false;

    ((CategoriesAdapter.OnCategorySelectedListener)listener).onCategorySelected(category);
    return true;
  }
}
