/*
 * Copyright 2015 Hello Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package is.hello.go99.animators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewPropertyAnimator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import is.hello.go99.Anime;
import is.hello.go99.ViewVisibility;

/**
 * A wrapper around {@link ViewPropertyAnimator} that descends from {@link Animator}
 * to allow for generic treatment of all animations within an {@link AnimatorContext}.
 */
@NotBindable
public class MultiAnimator extends Animator implements Animator.AnimatorListener {
    private Map<Property, Float> properties = new HashMap<>();
    private boolean hasFiredEndListener = false;

    /**
     * The target of the animator. Can be {@code null}, but never will be in callbacks.
     */
    private View target;
    private @Nullable AnimatorContext animatorContext;
    private long duration = Anime.DURATION_NORMAL;
    private long startDelay = 0;
    private TimeInterpolator interpolator = Anime.INTERPOLATOR_DEFAULT;

    private List<WillRunListener> willStartListeners = new ArrayList<>();


    //region Lifecycle

    /**
     * Creates a multi-animator for a given view, unbound to any animator context.
     * <p>
     * <em>Important:</em> <code>MultiAnimator</code> does not cache view animators,
     * every call to <code>#animatorFor(View)</code> returns a new object. Additionally,
     * attempting to run multiple multi-animators for a single view at the same time will
     * result in undefined behavior.
     *
     * @param view  The view to create an animator for.
     * @return  A new multi-animator object.
     */
    public static MultiAnimator animatorFor(@NonNull View view) {
        return new MultiAnimator(view, null);
    }

    /**
     * Creates a multi-animator for a given view, and binds it to a given animator context.
     * <p>
     * The transaction template of the animator context will be applied to the multi-animator.
     * <p>
     * <em>Important:</em> <code>MultiAnimator</code> does not cache view animators,
     * every call to <code>#animatorFor(View)</code> returns a new object. Additionally,
     * attempting to run multiple multi-animators for a single view at the same time will
     * result in undefined behavior.
     *
     * @param view  The view to create an animator for.
     * @param animatorContext The animator context to bind to.
     * @return  A new multi-animator object.
     */
    public static MultiAnimator animatorFor(@NonNull View view,
                                            @Nullable AnimatorContext animatorContext) {
        return new MultiAnimator(view, animatorContext);
    }

    /**
     * Creates a multi-animator with no target or animator context to use as a template
     * with other classes that {@code clone()} animators like {@code LayoutTransition}.
     * @return  A new multi-animator object.
     */
    public static MultiAnimator empty() {
        return new MultiAnimator(null, null);
    }

    private MultiAnimator(@Nullable View target,
                          @Nullable AnimatorContext animatorContext) {
        this.target = target;
        this.animatorContext = animatorContext;

        if (animatorContext != null) {
            animatorContext.getTransactionTemplate().apply(this);
        }
    }

    //endregion


    //region Attributes

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public MultiAnimator setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    /**
     * Updates the duration of the multi-animator's animation.
     * @param duration  The new duration.
     * @return  The multi-animator.
     */
    public MultiAnimator withDuration(long duration) {
        return setDuration(duration);
    }

    @Override
    public long getStartDelay() {
        return startDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        this.startDelay = startDelay;
    }

    /**
     * Updates the start delay to wait before the multi-animator starts animating.
     * @param startDelay    The start delay.
     * @return  The multi-animator
     */
    public MultiAnimator withStartDelay(long startDelay) {
        setStartDelay(startDelay);
        return this;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public TimeInterpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    /**
     * Updates the interpolator the multi-animator will use during animation.
     * @param interpolator  The new interpolator.
     * @return  The multi-animator.
     */
    public MultiAnimator withInterpolator(@NonNull TimeInterpolator interpolator) {
        setInterpolator(interpolator);
        return this;
    }

    /**
     * Updates the animator context the multi-animator is tied to.
     * @param animatorContext   The animator context.
     * @return  The multi-animator.
     */
    public MultiAnimator withAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
        return this;
    }

    /**
     * Sets the view that this multi-animator will operate on. This value is automatically set
     * by the {@link #animatorFor(View)} and {@link #animatorFor(View, AnimatorContext)} methods.
     *
     * @param target    The target.
     * @throws IllegalArgumentException if {@code target} is not an instance of {@code View}.
     */
    @Override
    public void setTarget(@Nullable Object target) {
        if (target != null && !(target instanceof View)) {
            throw new IllegalArgumentException("Target must be a View");
        }

        this.target = (View) target;
    }

    public View getTarget() {
        return target;
    }

    @Override
    public boolean isRunning() {
        return (target != null && Anime.isAnimating(target));
    }

    //endregion


    //region Animations

    public MultiAnimator x(float value) {
        properties.put(Property.X, value);
        return this;
    }

    public MultiAnimator y(float value) {
        properties.put(Property.Y, value);
        return this;
    }

    public MultiAnimator translationX(float value) {
        properties.put(Property.TRANSLATION_X, value);
        return this;
    }

    public MultiAnimator translationY(float value) {
        properties.put(Property.TRANSLATION_Y, value);
        return this;
    }

    /**
     * Convenience method calls both {@link #scaleX(float)} and {@link #scaleY(float)}.
     * @param value The scale value to animate to.
     * @return The multi-animator.
     */
    public MultiAnimator scale(float value) {
        return this.scaleX(value)
                   .scaleY(value);
    }

    public MultiAnimator scaleX(float value) {
        properties.put(Property.SCALE_X, value);
        return this;
    }

    public MultiAnimator scaleY(float value) {
        properties.put(Property.SCALE_Y, value);
        return this;
    }

    public MultiAnimator alpha(float value) {
        properties.put(Property.ALPHA, value);
        return this;
    }

    public MultiAnimator rotation(float value) {
        properties.put(Property.ROTATION, value);
        return this;
    }

    public MultiAnimator rotationX(float value) {
        properties.put(Property.ROTATION_X, value);
        return this;
    }

    public MultiAnimator rotationY(float value) {
        properties.put(Property.ROTATION_Y, value);
        return this;
    }

    //endregion


    //region Forwarding

    @Override
    public void onAnimationStart(Animator animation) {
        final ArrayList<AnimatorListener> listeners = getListeners();
        if (listeners != null) {
            // The Animator contract requires that removing listeners
            // always works. Unfortunately, iterating backwards causes
            // some of the canned animations to fail. So we make a local
            // copy of the listeners array and work with that.
            final AnimatorListener[] listenersCopy =
                    listeners.toArray(new AnimatorListener[listeners.size()]);
            for (AnimatorListener listener : listenersCopy) {
                listener.onAnimationStart(this);
            }
        }

        Anime.addAnimatingView(target);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (hasFiredEndListener) {
            return;
        }

        final ArrayList<AnimatorListener> listeners = getListeners();
        if (listeners != null) {
            final AnimatorListener[] listenersCopy =
                    listeners.toArray(new AnimatorListener[listeners.size()]);
            for (AnimatorListener listener : listenersCopy) {
                listener.onAnimationEnd(this);
            }
        }

        if (animatorContext != null) {
            animatorContext.endAnimation(toString());
        }

        Anime.removeAnimatingView(target);

        this.hasFiredEndListener = true;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        final ArrayList<AnimatorListener> listeners = getListeners();
        if (listeners != null) {
            final AnimatorListener[] listenersCopy =
                    listeners.toArray(new AnimatorListener[listeners.size()]);
            for (AnimatorListener listener : listenersCopy) {
                listener.onAnimationCancel(this);
            }
        }

        Anime.removeAnimatingView(target);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        // Not supported
    }

    //endregion


    //region Running

    /**
     * Configures the underlying {@code ViewPropertyAnimator} and
     * starts animating the multi-animators target view.
     *
     * @throws IllegalStateException if no target has been set on the multi-animator.
     */
    @Override
    public void start() {
        if (target == null) {
            throw new IllegalStateException("Cannot start a MultiAnimator without setting a target");
        }

        for (final WillRunListener willStart : willStartListeners) {
            willStart.onMultiAnimatorWillRun(this);
        }

        final ViewPropertyAnimator propertyAnimator = target.animate();
        propertyAnimator.cancel();
        propertyAnimator.setListener(this);
        propertyAnimator.setDuration(duration);
        propertyAnimator.setStartDelay(startDelay);
        propertyAnimator.setInterpolator(interpolator);

        for (final Map.Entry<Property, Float> entry : properties.entrySet()) {
            final Property property = entry.getKey();
            final float value = entry.getValue();
            switch (property) {
                case X:
                    propertyAnimator.x(value);
                    break;
                case Y:
                    propertyAnimator.y(value);
                    break;
                case TRANSLATION_X:
                    propertyAnimator.translationX(value);
                    break;
                case TRANSLATION_Y:
                    propertyAnimator.translationY(value);
                    break;
                case SCALE_X:
                    propertyAnimator.scaleX(value);
                    break;
                case SCALE_Y:
                    propertyAnimator.scaleY(value);
                    break;
                case ALPHA:
                    propertyAnimator.alpha(value);
                    break;
                case ROTATION:
                    propertyAnimator.rotation(value);
                    break;
                case ROTATION_X:
                    propertyAnimator.rotationX(value);
                    break;
                case ROTATION_Y:
                    propertyAnimator.rotationY(value);
                    break;
            }
        }

        this.hasFiredEndListener = false;
        propertyAnimator.start();

        if (animatorContext != null) {
            animatorContext.beginAnimation(toString());
        }
    }

    public void postStart() {
        if (target == null) {
            throw new IllegalStateException("Cannot postStart a MultiAnimator without setting a target");
        }

        target.post(new Runnable() {
            @Override
            public void run() {
                start();
            }
        });
    }

    @Override
    public void cancel() {
        if (target != null) {
            target.animate().cancel();
        }
    }

    @Override
    public void end() {
        final ViewPropertyAnimator animator = target.animate();
        animator.setListener(null); // Prevent unwanted cancel callback
        animator.cancel();

        for (final Map.Entry<Property, Float> entry : properties.entrySet()) {
            final Property property = entry.getKey();
            final float value = entry.getValue();
            switch (property) {
                case X:
                    target.setX(value);
                    break;
                case Y:
                    target.setY(value);
                    break;
                case TRANSLATION_X:
                    target.setTranslationX(value);
                    break;
                case TRANSLATION_Y:
                    target.setTranslationY(value);
                    break;
                case SCALE_X:
                    target.setScaleX(value);
                    break;
                case SCALE_Y:
                    target.setScaleY(value);
                    break;
                case ALPHA:
                    target.setAlpha(value);
                    break;
                case ROTATION:
                    target.setRotation(value);
                    break;
                case ROTATION_X:
                    target.setRotationX(value);
                    break;
                case ROTATION_Y:
                    target.setRotationY(value);
                    break;
            }
        }

        onAnimationEnd(this);
    }

    //endregion


    //region Convenience

    /**
     * Adds a new {@code Runnable} object to run before the multi-animator configures and starts
     * its underlying {@code ViewPropertyAnimator}. Any changes made to timing, or additions made
     * to the multi-animators list of animated properties will take effect immediately after all
     * will start listeners are run.
     * @param willStart The runnable.
     * @return  The multi-animator.
     */
    public MultiAnimator addOnAnimationWillStart(@NonNull WillRunListener willStart) {
        willStartListeners.add(willStart);
        return this;
    }

    public MultiAnimator addOnAnimationCompleted(final @NonNull OnAnimationCompleted onAnimationCompleted) {
        addListener(new AnimatorListenerAdapter() {
            boolean canceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimationCompleted.onAnimationCompleted(!canceled);
            }
        });
        return this;
    }

    //endregion


    //region Canned Animations

    public MultiAnimator fadeIn() {
        return addOnAnimationWillStart(new WillRunListener() {
            @Override
            public void onMultiAnimatorWillRun(@NonNull MultiAnimator animator) {
                target.setAlpha(0f);
                target.setVisibility(View.VISIBLE);
            }
        }).alpha(1f);
    }

    public MultiAnimator fadeOut(final @ViewVisibility int targetVisibility) {
        return alpha(0f)
                .addOnAnimationCompleted(new OnAnimationCompleted() {
                    @Override
                    public void onAnimationCompleted(boolean finished) {
                        if (finished) {
                            target.setVisibility(targetVisibility);
                        }
                    }
                });
    }

    public MultiAnimator slideYAndFade(final float startDeltaY, final float endDeltaY,
                                       final float startAlpha, final float endAlpha) {
        return addOnAnimationWillStart(new WillRunListener() {
            @Override
            public void onMultiAnimatorWillRun(@NonNull MultiAnimator animator) {
                float y = target.getY();
                float startY = y + startDeltaY;
                float endY = y + endDeltaY;

                target.setAlpha(startAlpha);
                target.setY(startY);
                target.setVisibility(View.VISIBLE);

                MultiAnimator.this.y(endY);
                MultiAnimator.this.alpha(endAlpha);
            }
        });
    }

    public MultiAnimator slideXAndFade(final float startDeltaX, final float endDeltaX,
                                       final float startAlpha, final float endAlpha) {
        return addOnAnimationWillStart(new WillRunListener() {
            @Override
            public void onMultiAnimatorWillRun(@NonNull MultiAnimator animator) {
                float x = target.getX();
                float startX = x + startDeltaX;
                float endX = x + endDeltaX;

                target.setAlpha(startAlpha);
                target.setX(startX);
                target.setVisibility(View.VISIBLE);

                MultiAnimator.this.x(endX);
                MultiAnimator.this.alpha(endAlpha);
            }
        });
    }

    //endregion


    //region Cloning

    @Override
    public MultiAnimator clone() {
        final MultiAnimator animator = (MultiAnimator) super.clone();

        animator.properties = new HashMap<>(properties);
        animator.hasFiredEndListener = hasFiredEndListener;

        animator.target = target;
        animator.animatorContext = animatorContext;
        animator.duration = duration;
        animator.startDelay = startDelay;
        animator.interpolator = interpolator;

        animator.willStartListeners = new ArrayList<>(willStartListeners);

        return animator;
    }


    //endregion


    @Override
    public String toString() {
        return "MultiAnimator{" +
                "target=" + target +
                ", animatorContext=" + animatorContext +
                ", properties=" + properties.keySet() +
                ", interpolator=" + interpolator +
                ", startDelay=" + startDelay +
                ", duration=" + duration +
                '}';
    }

    public interface WillRunListener {
        void onMultiAnimatorWillRun(@NonNull MultiAnimator animator);
    }

    private enum Property {
        X,
        Y,
        TRANSLATION_X,
        TRANSLATION_Y,
        SCALE_X,
        SCALE_Y,
        ALPHA,
        ROTATION,
        ROTATION_X,
        ROTATION_Y,
    }
}
