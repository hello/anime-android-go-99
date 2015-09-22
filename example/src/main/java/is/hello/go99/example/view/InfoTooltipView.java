package is.hello.go99.example.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.go99.example.R;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class InfoTooltipView extends FrameLayout {
    private static final int VISIBLE_DURATION = 1500;
    private final TextView text;
    private @Nullable AnimatorContext animatorContext;


    //region Lifecycle

    public InfoTooltipView(@NonNull Context context) {
        this(context, null);
    }

    public InfoTooltipView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoTooltipView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);


        this.text = new TextView(context);
        text.setTextAppearance(context, android.support.v7.appcompat.R.style.TextAppearance_AppCompat_Body2);
        text.setBackgroundResource(R.drawable.background_info_tooltip);

        final Resources resources = getResources();
        final int paddingHorizontal = resources.getDimensionPixelSize(R.dimen.view_info_tooltip_padding_horizontal),
                  paddingVertical = resources.getDimensionPixelSize(R.dimen.view_info_tooltip_padding_vertical);
        text.setPadding(text.getPaddingLeft() + paddingHorizontal,
                        text.getPaddingTop() + paddingVertical,
                        text.getPaddingRight() + paddingHorizontal,
                        text.getPaddingBottom() + paddingVertical);

        @SuppressLint("RtlHardcoded")
        final LayoutParams textLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                               LayoutParams.WRAP_CONTENT,
                                                               Gravity.BOTTOM | Gravity.LEFT);
        addView(text, textLayoutParams);
    }

    //endregion


    //region Attributes

    public void setText(@NonNull CharSequence text) {
        this.text.setText(text);
    }

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void showAboveView(final @NonNull ViewGroup parent,
                              final @NonNull View aboveView,
                              final @NonNull OnDismissListener onDismissListener) {
        text.setVisibility(INVISIBLE);
        parent.addView(this, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                                                        LayoutParams.MATCH_PARENT));

        final Resources resources = getResources();
        final int overlap = resources.getDimensionPixelSize(R.dimen.view_info_tooltip_overlap);

        final LayoutParams layoutParams = (LayoutParams) text.getLayoutParams();
        layoutParams.bottomMargin = (parent.getHeight() - aboveView.getTop()) - overlap;
        layoutParams.leftMargin = aboveView.getLeft();
        text.requestLayout();
        post(new Runnable() {
            @Override
            public void run() {
                animateInText(onDismissListener);
            }
        });
    }

    private void animateInText(final @NonNull OnDismissListener onDismissListener) {
        final int overlap = getResources().getDimensionPixelSize(R.dimen.view_info_tooltip_overlap);
        animatorFor(text, animatorContext)
                .withInterpolator(new FastOutLinearInInterpolator())
                .slideYAndFade(overlap, 0f, 0f, 1f)
                .addOnAnimationCompleted(new OnAnimationCompleted() {
                    @Override
                    public void onAnimationCompleted(boolean finished) {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (Anime.isAnimating(text) || !isShown()) {
                                    return;
                                }

                                onDismissListener.onInfoTooltipDismissed();
                                dismiss(false);
                            }
                        }, VISIBLE_DURATION);
                    }
                })
                .start();
    }

    public void dismiss(boolean immediate) {
        if (immediate) {
            dismissNow();
        } else {
            animatorFor(text, animatorContext)
                    .withInterpolator(new FastOutSlowInInterpolator())
                    .fadeOut(INVISIBLE)
                    .addOnAnimationCompleted(new OnAnimationCompleted() {
                        @Override
                        public void onAnimationCompleted(boolean finished) {
                            dismissNow();
                        }
                    })
                    .start();
        }
    }

    private void dismissNow() {
        final ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
    }

    //endregion


    public interface OnDismissListener {
        void onInfoTooltipDismissed();
    }
}
