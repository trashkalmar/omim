package com.mapswithme.maps.routing;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.maps.widget.RotateDrawable;
import com.mapswithme.maps.widget.ToolbarController;
import com.mapswithme.maps.widget.WheelProgressView;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.Utils;
import com.mapswithme.util.statistics.AlohaHelper;

public class RoutingPlanController extends ToolbarController
{
  static final int ANIM_TOGGLE = MwmApplication.get().getResources().getInteger(R.integer.anim_slots_toggle);

  protected final View mFrame;
  private final ImageView mToggle;
  private final SlotFrame mSlotFrame;
  private final RadioGroup mRouterTypes;
  private final WheelProgressView mProgressVehicle;
  private final WheelProgressView mProgressPedestrian;
  private final View mPlanningLabel;
  private final View mErrorLabel;
  private final View mNumbersFrame;
  private final TextView mNumbersTime;
  private final TextView mNumbersDistance;

  private final RotateDrawable mToggleImage = new RotateDrawable(R.drawable.ic_down);
  private int mFrameHeight;
  private int mToolbarHeight;
  private boolean mOpen;

  public RoutingPlanController(View root, Activity activity)
  {
    super(root, activity);
    mFrame = root;

    mToggle = (ImageView) mToolbar.findViewById(R.id.toggle);
    mSlotFrame = (SlotFrame) root.findViewById(R.id.slots);
    mSlotFrame.setOnSlotClickListener(new SlotFrame.OnSlotClickListener()
    {
      @Override
      public void OnSlotClick(int slotId)
      {
        RoutingController.get().searchPoi(slotId);
      }
    });

    View planFrame = root.findViewById(R.id.planning_frame);

    mRouterTypes = (RadioGroup) planFrame.findViewById(R.id.route_type);
    mRouterTypes.findViewById(R.id.vehicle).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        AlohaHelper.logClick(AlohaHelper.ROUTING_VEHICLE_SET);
        RoutingController.get().setRouterType(Framework.ROUTER_TYPE_VEHICLE);
      }
    });

    mRouterTypes.findViewById(R.id.pedestrian).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        AlohaHelper.logClick(AlohaHelper.ROUTING_PEDESTRIAN_SET);
        RoutingController.get().setRouterType(Framework.ROUTER_TYPE_PEDESTRIAN);
      }
    });

    View progressFrame = planFrame.findViewById(R.id.progress_frame);
    mProgressVehicle = (WheelProgressView) progressFrame.findViewById(R.id.progress_vehicle);
    mProgressPedestrian = (WheelProgressView) progressFrame.findViewById(R.id.progress_pedestrian);

    mPlanningLabel = planFrame.findViewById(R.id.planning);
    mErrorLabel = planFrame.findViewById(R.id.error);
    mNumbersFrame = planFrame.findViewById(R.id.numbers);
    mNumbersTime = (TextView) mNumbersFrame.findViewById(R.id.time);
    mNumbersDistance = (TextView) mNumbersFrame.findViewById(R.id.distance);

    setTitle(R.string.route);

    mToggle.setImageDrawable(mToggleImage);
    mToggle.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        toggleSlots();
      }
    });
  }

  @Override
  public void onUpClick()
  {
    AlohaHelper.logClick(AlohaHelper.ROUTING_GO_CLOSE);
    RoutingController.get().cancelPlanning();
  }

  private boolean checkFrameHeight()
  {
    if (mFrameHeight > 0)
      return true;

    mFrameHeight = mSlotFrame.getHeight();
    mToolbarHeight = mToolbar.getHeight();
    return (mFrameHeight > 0);
  }

  private void animateSlotFrame(int offset)
  {
    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mSlotFrame.getLayoutParams();
    lp.topMargin = (mToolbarHeight - offset);
    mSlotFrame.setLayoutParams(lp);
  }

  public void updatePoints()
  {
    mSlotFrame.update();
  }

  private void updateProgressLabels()
  {
    UiUtils.showIf(RoutingController.get().isBuilding(), mPlanningLabel);

    RoutingController.BuildState buildState = RoutingController.get().getBuildState();
    UiUtils.showIf(buildState == RoutingController.BuildState.ERROR, mErrorLabel);

    boolean ready = (buildState == RoutingController.BuildState.BUILT);
    UiUtils.showIf(ready, mNumbersFrame);
    if (!ready)
      return;

    RoutingInfo rinfo = RoutingController.get().getCachedRoutingInfo();
    mNumbersTime.setText(RoutingController.formatRoutingTime(rinfo.totalTimeInSeconds));
    mNumbersDistance.setText(Utils.formatUnitsText(R.dimen.text_size_routing_number, R.dimen.text_size_routing_dimension,
                                                   rinfo.distToTarget, rinfo.targetUnits));
  }

  public void updateBuildProgress(int progress, int router)
  {
    updateProgressLabels();

    boolean vehicle = (router == Framework.ROUTER_TYPE_VEHICLE);
    mRouterTypes.check(vehicle ? R.id.vehicle : R.id.pedestrian);

    if (!RoutingController.get().isBuilding())
    {
      UiUtils.hide(mProgressVehicle, mProgressPedestrian);
      return;
    }

    UiUtils.visibleIf(vehicle, mProgressVehicle);
    UiUtils.visibleIf(!vehicle, mProgressPedestrian);

    if (vehicle)
      mProgressVehicle.setProgress(progress);
    else
      mProgressPedestrian.setProgress(progress);
  }

  private void toggleSlots()
  {
    showSlots(!mOpen, true);
  }

  protected void showSlots(final boolean show, final boolean animate)
  {
    if (!checkFrameHeight())
    {
      mFrame.post(new Runnable()
      {
        @Override
        public void run()
        {
          showSlots(show, animate);
        }
      });
      return;
    }

    mOpen = show;

    if (animate)
    {
      ValueAnimator animator = ValueAnimator.ofFloat(mOpen ? 1.0f : 0, mOpen ? 0 : 1.0f);
      animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
      {
        @Override
        public void onAnimationUpdate(ValueAnimator animation)
        {
          float fraction = (float)animation.getAnimatedValue();
          animateSlotFrame((int)(fraction * mFrameHeight));
          mToggleImage.setAngle(fraction * 180.0f);
        }
      });

      animator.setDuration(ANIM_TOGGLE);
      animator.start();
      mSlotFrame.fadeSlots(!mOpen);
    } else
    {
      animateSlotFrame(mOpen ? 0 : mFrameHeight);
      mToggleImage.setAngle(mOpen ? 0.0f : 180.0f);
      mSlotFrame.unfadeSlots();
    }
  }

  public void disableToggle()
  {
    UiUtils.hide(mToggle);
    showSlots(true, false);
  }
}
