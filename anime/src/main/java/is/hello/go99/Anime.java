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
package is.hello.go99;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.HashSet;
import java.util.Set;

public class Anime {
    /**
     * For animations that will be run in the middle of a user interaction
     * where just snapping an element off the screen would look bad.
     * <p>
     * Also available as {@code @integer/anime_duration_very_fast}.
     */
    public static final int DURATION_VERY_FAST = 50;

    /**
     * The fastest speed used for a regular animation.
     * <p>
     * Also available as {@code @integer/anime_duration_fast}.
     */
    public static final int DURATION_FAST = 150;

    /**
     * The slowest speed used for a regular animation.
     * <p>
     * Also available as {@code @integer/anime_duration_slow}.
     */
    public static final int DURATION_SLOW = 350;

    /**
     * Typical duration for animations in the Sense app. The
     * original duration constant used by iOS before version 7.
     * <p>
     * Also available as {@code @integer/anime_duration_normal}.
     */
    public static final int DURATION_NORMAL = 250;

    /**
     * The default interpolator used by the {@code animation} package.
     */
    public static final Interpolator INTERPOLATOR_DEFAULT = new DecelerateInterpolator();

    /**
     * The views that are currently animating.
     */
    private static final Set<View> animatingViews = new HashSet<>();


    //region Velocities

    /**
     * Calculate an animation duration for a given velocity over a given range of pixels.
     *
     * @param velocity  The velocity. Pixels/ms
     * @param area      The area the movement was contained within. Pixels.
     * @return A duration for use with an Animator.
     */
    public static long calculateDuration(float velocity, float area) {
        long rawDuration = (long) (area / velocity) * 1000 / 2;
        return Math.max(Anime.DURATION_FAST, Math.min(Anime.DURATION_SLOW, rawDuration));
    }

    //endregion


    //region Interpolation

    /**
     * Same as the logic from {@link android.animation.FloatEvaluator}
     * without the overhead of auto-boxing operations.
     *
     * @param fraction The fraction from the starting to the ending values.
     * @param start The start value.
     * @param end The end value.
     * @return A linear interpolation between the start and end values, given the
     *         {@code fraction} parameter.
     */
    public static float interpolateFloats(float fraction, float start, float end) {
        return start + fraction * (end - start);
    }

    /**
     * Same as the logic from {@link android.animation.ArgbEvaluator}
     * without the costs of auto-boxing associated with it.
     *
     * @param fraction The fraction from the starting to the ending colors.
     * @param startColor The start color.
     * @param endColor The end color.
     * @return A linear interpolation between the start and end colors, given the
     *         {@code fraction} parameter.
     */
    public static int interpolateColors(float fraction, int startColor, int endColor) {
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        return (startA + (int)(fraction * (endA - startA))) << 24 |
                (startR + (int)(fraction * (endR - startR))) << 16 |
                (startG + (int)(fraction * (endG - startG))) << 8 |
                (startB + (int)(fraction * (endB - startB)));
    }

    //endregion


    //region Animating Views

    /**
     * Stops any running animation on a given array of views.
     * <p>
     * If your view implements custom animations outside of {@link View#animate()},
     * and {@link is.hello.go99.animators.MultiAnimator}, and you want to support
     * this method, you should override {@link View#clearAnimation()}.
     *
     * @param forViews The views to cancel running animations on.
     */
    public static void cancelAll(@NonNull View... forViews) {
        for (View forView : forViews) {
            forView.animate().cancel();
            forView.clearAnimation();
        }
    }

    /**
     * Returns whether or not a given view is known to be animating.
     *
     * @param view The view to find the animating status for.
     * @return true if the view is animating; false otherwise.
     */
    public static boolean isAnimating(@NonNull View view) {
        return animatingViews.contains(view);
    }

    /**
     * Adds a view to the currently animating set.
     *
     * @param view The view that is animating.
     */
    public static void addAnimatingView(@NonNull View view) {
        animatingViews.add(view);
    }

    /**
     * Removes a view from the currently animating set.
     *
     * @param view The view that was animating.
     */
    public static void removeAnimatingView(@NonNull View view) {
        animatingViews.remove(view);
    }

    //endregion


    /**
     * Not meant to be instantiated.
     */
    private Anime() {
    }
}
