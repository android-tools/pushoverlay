package ru.busylee.pushoverlay;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by busylee on 07.10.16.
 */

public class PushOverlay {

  private static final boolean DEBUG = false;
  private static Queue<Pushable> pushables = new ArrayDeque<>();
  private static final String TAG = "PushOverlay";
  
  static long MOVE_UP_DURATION = 300L;
  static long PUSH_DURATION = 2000L;
  static long FADE_OUT_DURATION = 1000L;
  static long FADE_OUT_DELAY = 4000L;

  static WeakReference<Activity> activityWeakReference = new WeakReference<>(null);

  public static void attach(Activity activity) {
    log("attach");
    activityWeakReference = new WeakReference<>(activity);
    Window window = activity.getWindow();
    if (window.findViewById(R.id.push_overlay_layout) == null) {
      FrameLayout frameLayout = new FrameLayout(activity);
      frameLayout.setClickable(false);
      frameLayout.setId(R.id.push_overlay_layout);
      window.addContentView(frameLayout, getParamsMatchParent());
    }
    Pushable pushable = pushables.poll();
    while (pushable != null) {
      push(pushable);
      pushable = pushables.poll();
    }
  }

  public static void push(Pushable pushable) {
    log("push = [value=" + pushable.value + "]");
    Activity activity = activityWeakReference.get();
    if (activity != null) {
      final View view = createPushView(pushable, activity);
      final Window window = activity.getWindow();
      if (addView(view, window)) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            startPushAndFadeOutAnimation(view, window);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
              view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
          }
        });
      }
    } else {
      pushables.add(pushable);
    }
  }

  private static void startPushAndFadeOutAnimation(final View view, Window window) {
    Animator pushAnimator = createPushAnimator(view, window);
    AnimatorSet setSecond = createFadeOutAnimator(view);
    pushAnimator.start();
    setSecond.start();
  }

  @NonNull
  private static AnimatorSet createFadeOutAnimator(final View view) {
    AnimatorSet setSecond = new AnimatorSet();
    setSecond.play(ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f));
    setSecond.setStartDelay(FADE_OUT_DELAY);
    setSecond.setDuration(FADE_OUT_DURATION);
    setSecond.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {

      }

      @Override
      public void onAnimationEnd(Animator animation) {
        log("Animator.onAnimationEnd");
        ViewParent parent = view.getParent();
        if(parent instanceof ViewGroup) {
          final ViewGroup viewGroup = (ViewGroup) parent;
          viewGroup.removeView(view);
          recalculateYPositions(viewGroup);
        }
      }

      @Override
      public void onAnimationCancel(Animator animation) {
      }


      @Override
      public void onAnimationRepeat(Animator animation) {

      }
    });
    return setSecond;
  }

  private static void recalculateYPositions(ViewGroup containerView) {
    log("recalculateYPositions");
    int childCount = containerView.getChildCount();
    for(int indexInContainer = 0; indexInContainer < childCount; indexInContainer++) {
      final View view = containerView.getChildAt(indexInContainer);
      SmartProperty smartProperty = (SmartProperty) view.getTag();
      float finalYPosition = getFinalYPosition(view, containerView);
      if(smartProperty != null) {
        log("recalculateYPositions [change] exists SmartProperty");
        smartProperty.changeToPosition(finalYPosition);
      } else {
        log("recalculateYPositions [move up] new SmartProperty");
        final float fromPosition = view.getY();
        log("recalculateYPositions [move up] " +
          "from = " + fromPosition + " to = " + finalYPosition);
        smartProperty = SmartProperty.of(view, View.Y, fromPosition, finalYPosition);
        view.setTag(smartProperty);
        Animator set = ObjectAnimator.ofFloat(smartProperty, SmartProperty.PROPERTY, 0f, 1f);
        set.setDuration(MOVE_UP_DURATION);
        set.addListener(RemoveTagOnEndListener.of(view));
        set.start();
      }
    }
  }

  @NonNull
  private static Animator createPushAnimator(final View view, Window window) {
    log("createPushAnimator");
    Rect windowRect = getWindowRect(window);
    ViewGroup containerView = getContainerView(window);
    float finalYPosition = getFinalYPosition(view, containerView);
    SmartProperty smartProperty = SmartProperty.of(view, View.Y, windowRect.height(), finalYPosition);
    view.setTag(smartProperty);
    AnimatorSet set = new AnimatorSet();
    set
      .play(ObjectAnimator.ofFloat(view, View.X, windowRect.width(),
        0f))
      .with(ObjectAnimator.ofFloat(smartProperty, SmartProperty.PROPERTY, 0f,
        1f))
      .with(ObjectAnimator.ofFloat(view, View.SCALE_X, 0.2f, 1f))
      .with(ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.2f, 1f));
    set.setDuration(PUSH_DURATION);
    set.addListener(RemoveTagOnEndListener.of(view));
    return set;
  }

  private static float getFinalYPosition(View view, ViewGroup containerView) {
    float finalYPosition = 0f;
    int childCount = containerView.getChildCount();
    int viewIndex = containerView.indexOfChild(view);
    log("createPushAnimator() childCount = " + childCount);
    for (int index = 0; index < viewIndex; index++) {
      View childAt = containerView.getChildAt(index);
      int height = childAt.getMeasuredHeight();
      log("createPushAnimator() height = " + height);
      finalYPosition += height;
      log("createPushAnimator() finalYPosition = " + finalYPosition);
    }
    return finalYPosition;
  }

  @NonNull
  private static TextView createPushView(Pushable pushable, Activity activity) {
    final TextView textView = new TextView(activity);
    textView.setText(pushable.value);
    textView.setTextColor(pushable.textColor);
    textView.setBackgroundResource(R.drawable.push_overlay_pushable_background);
    GradientDrawable drawable = (GradientDrawable) textView.getBackground();
    drawable.setColor(pushable.color);
    return textView;
  }

  @NonNull
  private static Rect getWindowRect(Window window) {
    Rect windowRect = new Rect();
    window.getDecorView().getGlobalVisibleRect(windowRect);
    return windowRect;
  }

  private static boolean addView(View pushView, Window window) {
    FrameLayout pushMessagesContainer = getContainerView(window);
    if(pushMessagesContainer != null) {
      pushMessagesContainer.addView(pushView, getParamsWrapContents());
      return true;
    }
    return false;
  }

  private static FrameLayout getContainerView(Window window) {
    return (FrameLayout) window.getDecorView().findViewById(R.id.push_overlay_layout);
  }

  @NonNull
  private static LinearLayout.LayoutParams getParamsWrapContents() {
    return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  @NonNull
  private static LinearLayout.LayoutParams getParamsMatchParent() {
    return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
  }

  private static void log(String message) {
    if(DEBUG) {
      Log.d(TAG, message);
    }
  }
  
  private static class SmartProperty {

    static Property<SmartProperty, Float> PROPERTY = new Property<SmartProperty, Float>(Float.class, "X") {
      @Override
      public java.lang.Float get(SmartProperty object) {
        return object.get();
      }

      @Override
      public void set(SmartProperty object, Float value) {
        object.set(value);
      }
    };

    public static SmartProperty of(View view, Property property, float from, float to) {
      SmartProperty smartProperty = new SmartProperty();
      smartProperty.fromPosition = from;
      smartProperty.toPosition = to;
      smartProperty.view = view;
      smartProperty.property = property;
      return smartProperty;
    }

    private View view;
    private Property property;

    private float fromPosition;
    private float toPosition;

    private float currentValue;

    public void set(Float value) {
      currentValue = value;
      property.set(view, fromPosition + (toPosition - fromPosition) * currentValue);
    }

    public Float get() {
      return currentValue;
    }

    private void changeToPosition(float finalYPosition) {
      toPosition = finalYPosition;
    }
  }

  private static class RemoveTagOnEndListener implements Animator.AnimatorListener {

    private WeakReference<View> weakViewReference;

    public static RemoveTagOnEndListener of(View view) {
      RemoveTagOnEndListener removeTagOnEndListener = new RemoveTagOnEndListener();
      removeTagOnEndListener.weakViewReference = new WeakReference<View>(view);
      return removeTagOnEndListener;
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
      View view = weakViewReference.get();
      if(view != null) {
        view.setTag(null);
      }
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
  }

}
