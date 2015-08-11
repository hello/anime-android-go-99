package is.hello.go99.animators;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.animation.Animation;
import android.view.animation.Interpolator;

import is.hello.go99.Anime;
import is.hello.go99.evaluators.RectEvaluatorCompat;

/**
 * Read-only class that encapsulates the attributes common to different animation classes
 * in the Android SDK.
 */
public final class AnimatorTemplate {
    //region Attributes

    /**
     * The duration of an animation.
     */
    public final long duration;

    /**
     * The interpolator of an animation.
     */
    public final @NonNull Interpolator interpolator;

    //endregion


    //region Creation

    /**
     * The global default animator template. Uses {@link Anime#DURATION_NORMAL}
     * and {@link Anime#INTERPOLATOR_DEFAULT}.
     */
    public static final AnimatorTemplate DEFAULT = new AnimatorTemplate(Anime.DURATION_NORMAL,
            Anime.INTERPOLATOR_DEFAULT);

    public AnimatorTemplate(long duration,
                            @NonNull Interpolator interpolator) {
        this.duration = duration;
        this.interpolator = interpolator;
    }

    public AnimatorTemplate(long duration) {
        this(duration, Anime.INTERPOLATOR_DEFAULT);
    }

    public AnimatorTemplate(@NonNull Interpolator interpolator) {
        this(Anime.DURATION_NORMAL, interpolator);
    }

    //endregion


    //region Updating

    /**
     * Returns a new AnimatorTemplate with the specified
     * duration and the called object's interpolator.
     */
    public AnimatorTemplate withDuration(long newDuration) {
        return new AnimatorTemplate(newDuration, interpolator);
    }

    /**
     * Returns a new AnimatorTemplate with the specified
     * interpolator and the called object's duration.
     */
    public AnimatorTemplate withInterpolator(@NonNull Interpolator newInterpolator) {
        return new AnimatorTemplate(duration, newInterpolator);
    }

    //endregion


    //region Vending Animators

    /**
     * Creates and returns a configured ValueAnimator that will
     * transition between the specified array of colors.
     */
    public ValueAnimator createColorAnimator(@NonNull int... colors) {
        ValueAnimator colorAnimator = ValueAnimator.ofInt((int[]) colors);
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setInterpolator(interpolator);
        colorAnimator.setDuration(duration);
        return colorAnimator;
    }

    /**
     * Creates and returns a configured ValueAnimator that will
     * transition between the specified array of rectangles.
     *
     * @see RectEvaluatorCompat
     */
    public ValueAnimator createRectAnimator(@NonNull Rect... rectangles) {
        ValueAnimator rectAnimator = ValueAnimator.ofObject(new RectEvaluatorCompat(),
                                                            (Object[]) rectangles);
        rectAnimator.setInterpolator(interpolator);
        rectAnimator.setDuration(duration);
        return rectAnimator;
    }

    //endregion


    //region Applying

    /**
     * Applies the attributes of the template to a given animator, returning that animator.
     */
    public <T extends Animator> T apply(@NonNull T animator) {
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        return animator;
    }

    /**
     * Applies the applicable attributes of the template to a given layout transition,
     * returning that layout transition.
     * <p />
     * Does not update the interpolators on the layout transition.
     */
    public LayoutTransition apply(@NonNull LayoutTransition transition) {
        transition.setDuration(duration);
        return transition;
    }

    /**
     * Applies the attributes of the template to a given animation, returning that animation.
     */
    public Animation apply(@NonNull Animation animation) {
        animation.setDuration(duration);
        animation.setInterpolator(interpolator);
        return animation;
    }

    //endregion
}
