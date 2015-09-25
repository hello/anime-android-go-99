package is.hello.go99.animators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.util.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.go99.Anime;
import is.hello.go99.Go99TestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    public void overlappingAnimationsWithContext() {
        final Scheduler scheduler = Robolectric.getForegroundThreadScheduler();
        scheduler.pause();

        final AnimatorContext context = spy(new AnimatorContext("Test"));

        final MultiAnimator animator1 = MultiAnimator.animatorFor(fakeView, context);
        final AtomicBoolean animator1Canceled = new AtomicBoolean(false);
        final AtomicBoolean animator1Ended = new AtomicBoolean(false);
        final AtomicBoolean animator1Started = new AtomicBoolean(false);
        animator1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                animator1Canceled.set(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animator1Ended.set(true);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                animator1Started.set(true);
            }
        });
        animator1.x(10f);
        animator1.start();

        verify(context, times(1)).beginAnimation(any(String.class));
        assertThat(animator1Canceled.get(), is(false));
        assertThat(animator1Ended.get(), is(false));
        assertThat(animator1Started.get(), is(true));

        MultiAnimator.animatorFor(fakeView, context)
                     .x(0f)
                     .start();

        assertThat(animator1Canceled.get(), is(true));
        assertThat(animator1Ended.get(), is(true));

        verify(context, times(1)).endAnimation(any(String.class));
        verify(context, times(2)).beginAnimation(any(String.class));

        scheduler.unPause();

        verify(context, times(2)).endAnimation(any(String.class));
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

    @Test
    public void cancelBeforeNextLooperCycle() {
        final Scheduler scheduler = Robolectric.getForegroundThreadScheduler();
        scheduler.pause();

        final AnimatorContext testContext = spy(new AnimatorContext("Test"));

        final MultiAnimator animator = MultiAnimator.animatorFor(fakeView, testContext);
        animator.translationY(0f);
        animator.start();

        verify(testContext).beginAnimation(any(String.class));

        animator.cancel();

        verify(testContext).endAnimation(any(String.class));
    }

    @Test
    public void cancelFromAnimeBeforeNextLooperCycle() {
        final Scheduler scheduler = Robolectric.getForegroundThreadScheduler();
        scheduler.pause();

        final AnimatorContext testContext = spy(new AnimatorContext("Test"));

        final MultiAnimator animator = MultiAnimator.animatorFor(fakeView, testContext);
        animator.translationY(0f);
        animator.start();

        verify(testContext).beginAnimation(any(String.class));

        Anime.cancelAll(fakeView);

        verify(testContext).endAnimation(any(String.class));
    }

    @Test
    public void toStringDoesNotInfiniteLoop() {
        assertThat(animator.toString(), is(notNullValue()));

        final AnimatorContext testAnimatorContext = new AnimatorContext("Test");
        final MultiAnimator withAnimatorContext = MultiAnimator.animatorFor(fakeView, testAnimatorContext);
        assertThat(withAnimatorContext, is(notNullValue()));
    }
}
