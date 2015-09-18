package is.hello.go99.example.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.go99.animators.AnimatorContext;

public class AmplitudeView extends View {
    private final Paint fillPaint = new Paint();

    private float amplitude = 0f;
    private @Nullable AnimatorContext animatorContext;

    private @Nullable ValueAnimator valueAnimator;

    //region Lifecycle

    public AmplitudeView(@NonNull Context context) {
        this(context, null);
    }

    public AmplitudeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmplitudeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setMinimumHeight(100);

        setBackgroundColor(Color.YELLOW);
        fillPaint.setColor(Color.RED);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            clearAnimation();
        }
    }

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    //endregion


    //region Drawing

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
        invalidate();
    }

    public void animateToAmplitude(final float newAmplitude,
                                   @NonNull AnimatorContext.Transaction transaction) {
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }

        this.valueAnimator = ValueAnimator.ofFloat(amplitude, newAmplitude);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                AmplitudeView.this.amplitude = (float) animator.getAnimatedValue();
                invalidate();
            }
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation == valueAnimator) {
                    AmplitudeView.this.valueAnimator = null;
                }
            }
        });

        transaction.takeOwnership(valueAnimator);
    }

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    //endregion
}
