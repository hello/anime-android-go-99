/*
 * Copyright 2015 Hello Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package is.hello.go99.example.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.example.R;

/**
 * A simple view that renders a colored bar that takes up a percentage of the view's visual bounds.
 */
public class AmplitudeView extends View {
    private static final String CHANGE_ANIMATOR_NAME = AmplitudeView.class.getSimpleName() + "#changeAnimator";

    private final Paint fillPaint = new Paint();
    private final Colors colors;

    private @Nullable ValueAnimator changeAnimator;


    /**
     * The amplitude to display. A value in the range {@code 0..1} inclusive.
     */
    private float amplitude = 0f;


    //region Lifecycle

    public AmplitudeView(@NonNull Context context) {
        this(context, null);
    }

    public AmplitudeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmplitudeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        fillPaint.setColor(Color.RED);
        this.colors = new Colors(getResources());
    }

    /**
     * Extends the default implementation to call {@link #clearAnimation()} whenever
     * the view visibility becomes something other than {@code View.VISIBLE}. This
     * automatically cancels the animation in many circumstances.
     */
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            clearAnimation();
        }
    }

    /**
     * Extends the default implementation to stop any
     * custom animation currently running within the view.
     * <p>
     * Overriding this method allows anime-android-go-99 to
     * generically terminate animation on any {@code View} instance.
     *
     * @see Anime#cancelAll(View...) for more info.
     */
    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (changeAnimator != null) {
            changeAnimator.cancel();
        }
    }

    //endregion


    //region Drawing

    /**
     * Provides a more efficient implementation of setting alpha for devices
     * that aren't hardware accelerated to improve the efficiency of rendering
     * and animations in this view.
     */
    @Override
    protected boolean onSetAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (amplitude > 0f) {
            final int width = canvas.getWidth(),
                      height = canvas.getHeight();

            final float fillWidth = (width * amplitude);
            canvas.drawRect(0f, 0f, fillWidth, height, fillPaint);
        }
    }

    //endregion


    //region Attributes

    public void setAmplitude(final float amplitude) {
        final float normalizedAmplitude;
        if (amplitude < 0f) {
            normalizedAmplitude = 0f;
        } else if (amplitude > 1f) {
            normalizedAmplitude = 1f;
        } else {
            normalizedAmplitude = amplitude;
        }

        this.amplitude = normalizedAmplitude;
        fillPaint.setColor(colors.getColor(amplitude, fillPaint.getAlpha()));
        invalidate();
    }

    /**
     * Animates the amplitude of the view to a new value from its present state.
     * <p>
     * The change animation will take on the template of the given transaction,
     * and will fire when the transaction fires. The view is not responsible
     * for any coordination or configuration besides delay.
     *
     * @param newAmplitude      The new amplitude to animate to.
     * @param animationDelay    The amount of time to delay before firing the animation.
     * @param transaction       The transaction to bind the ampltiude change animator to.
     */
    public void animateToAmplitude(final float newAmplitude,
                                   long animationDelay,
                                   @NonNull AnimatorContext.Transaction transaction) {
        if (changeAnimator != null) {
            changeAnimator.cancel();
        }

        final @ColorInt float oldAmplitude = this.amplitude;
        final @ColorInt int[] colors = this.colors.getAnimatorColors(oldAmplitude, newAmplitude);
        this.changeAnimator = transaction.template.createColorAnimator(colors);
        changeAnimator.setStartDelay(animationDelay);
        changeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                AmplitudeView.this.amplitude = Anime.interpolateFloats(animator.getAnimatedFraction(),
                                                                       oldAmplitude,
                                                                       newAmplitude);
                final @ColorInt int rawColor = (int) animator.getAnimatedValue();
                fillPaint.setColor(Colors.withAlpha(rawColor, fillPaint.getAlpha()));
                invalidate();
            }
        });

        changeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation == changeAnimator) {
                    AmplitudeView.this.changeAnimator = null;
                }
            }
        });

        transaction.takeOwnership(changeAnimator, CHANGE_ANIMATOR_NAME);
    }

    //endregion


    /**
     * Deals with the intricacies of vending the correct colors for the correct ranges of values.
     */
    @VisibleForTesting
    static class Colors {
        private final @ColorInt int[] colorTable;

        @SuppressWarnings("deprecation") // android-support hasn't caught up with Marshmallow yet
        Colors(@NonNull Resources resources) {
            this.colorTable = new int[] {
                    resources.getColor(R.color.amplitude_light),
                    resources.getColor(R.color.amplitude_medium),
                    resources.getColor(R.color.amplitude_dark),
            };
        }

        static @ColorInt int withAlpha(@ColorInt final int color, final int alpha) {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }

        static int getColorIndex(final float forAmplitude) {
            if (forAmplitude <= 0.2f) {
                return 0;
            } else if (forAmplitude <= 0.6f) {
                return 1;
            } else {
                return 2;
            }
        }

        @ColorInt int getColor(final float forAmplitude,
                               final int alpha) {
            final int colorIndex = getColorIndex(forAmplitude);
            return withAlpha(colorTable[colorIndex], alpha);
        }

        @ColorInt int[] getAnimatorColors(final float fromAmplitude,
                                          final float toAmplitude) {
            final int startIndex = getColorIndex(fromAmplitude);
            final int toIndex = getColorIndex(toAmplitude);

            final @ColorInt int[] colors;
            if (toIndex < startIndex) {
                colors = Arrays.copyOfRange(colorTable, toIndex, startIndex + 1);
                for (int i = 0, length = colors.length; i < length / 2; i++) {
                    final int temp = colors[i];
                    colors[i] = colors[length - 1 - i];
                    colors[length - 1 - i] = temp;
                }
            } else {
                colors = Arrays.copyOfRange(colorTable, startIndex, toIndex + 1);
            }

            if (colors.length == 1) {
                return new int[] { colors[0], colors[0] };
            } else {
                return colors;
            }
        }
    }
}
