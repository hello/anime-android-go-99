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
    private final @Nullable AnimatorContext animatorContext;
    private final Map<Property, Float> properties = new HashMap<>();
    private boolean hasFiredEndListener = false;

    /**
     * The target of the animator. Can be {@code null}, but never will be in callbacks.
     */
    private View target;
    private long duration = Anime.DURATION_NORMAL;
    private long startDelay = 0;
    private TimeInterpolator interpolator = Anime.INTERPOLATOR_DEFAULT;

    private final List<Runnable> willStartListeners = new ArrayList<>();


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

    public MultiAnimator withInterpolator(@NonNull TimeInterpolator interpolator) {
        setInterpolator(interpolator);
        return this;
    }

    @Override
    public void setTarget(@Nullable Object target) {
        if (target != null && !(target instanceof View)) {
            throw new IllegalArgumentException("Target must be a View");
        }

        this.target = (View) target;
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

    @Override
    public void start() {
        if (target == null) {
            throw new IllegalStateException("Cannot start a MultiAnimator without setting a target");
        }

        for (final Runnable willStart : willStartListeners) {
            willStart.run();
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
        throw new AssertionError("end not supported by MultiAnimator.");
    }

    //endregion


    //region Convenience

    public MultiAnimator addOnAnimationWillStart(@NonNull Runnable willStart) {
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
        return addOnAnimationWillStart(new Runnable() {
            @Override
            public void run() {
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
        return addOnAnimationWillStart(new Runnable() {
            @Override
            public void run() {
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
        return addOnAnimationWillStart(new Runnable() {
            @Override
            public void run() {
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
