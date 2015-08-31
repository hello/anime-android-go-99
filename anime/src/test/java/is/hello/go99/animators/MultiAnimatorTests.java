package is.hello.go99.animators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.go99.Go99TestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MultiAnimatorTests extends Go99TestCase {
    private final FrameLayout fakeView = new FrameLayout(getContext());
    private MultiAnimator animator;

    @Before
    public void setUp() {
        this.animator = MultiAnimator.animatorFor(fakeView);
    }

    @Test
    public void startWithPreExistingAnimations() {
        Robolectric.getForegroundThreadScheduler().pause();

        MultiAnimator.animatorFor(fakeView)
                     .addOnAnimationCompleted(new OnAnimationCompleted() {
                         @Override
                         public void onAnimationCompleted(boolean finished) {
                             assertThat(finished, is(false));
                         }
                     })
                     .x(10f)
                     .start();

        fakeView.animate().start();

        final AtomicBoolean onAnimationCompletedCalled = new AtomicBoolean();
        animator.addOnAnimationCompleted(new OnAnimationCompleted() {
            @Override
            public void onAnimationCompleted(boolean finished) {
                onAnimationCompletedCalled.set(true);
            }
        });

        animator.x(0f);
        animator.start();

        assertThat(onAnimationCompletedCalled.get(), is(false));
    }

    @Test
    public void addOnAnimationCompletedOrder() {
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            final int expected = i;
            animator.addOnAnimationCompleted(new OnAnimationCompleted() {
                @Override
                public void onAnimationCompleted(boolean finished) {
                    assertThat(expected, is(equalTo(counter.getAndIncrement())));
                }
            });
        }
        animator.onAnimationEnd(animator);
    }

    @Test
    public void listenerRemoveDuringCallback() {
        for (int i = 0; i < 3; i++) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animator.removeListener(this);
                }
            });
        }
        animator.onAnimationEnd(animator);
    }
}
