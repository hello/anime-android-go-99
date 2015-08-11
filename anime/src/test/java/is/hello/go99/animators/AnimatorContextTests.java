package is.hello.go99.animators;

import android.animation.AnimatorSet;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.util.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.go99.Go99TestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AnimatorContextTests extends Go99TestCase {
    private AnimatorContext animatorContext;

    @Before
    public void setUp() {
        this.animatorContext = new AnimatorContext(getClass().getSimpleName());
    }

    @Test
    public void negativeCounterIsIllegal() throws Exception {
        try {
            animatorContext.endAnimation();
        } catch (IllegalStateException ignored) {
            return;
        }
        fail("Counter was able to decrement into negative space");
    }

    @Test
    public void idleTasks() throws Exception {
        final AtomicBoolean taskWasRun = new AtomicBoolean(false);
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                taskWasRun.set(true);
            }
        };

        animatorContext.runWhenIdle(task);
        assertThat(taskWasRun.get(), is(true));

        taskWasRun.set(false);

        Scheduler scheduler = Robolectric.getForegroundThreadScheduler();

        animatorContext.beginAnimation();
        animatorContext.runWhenIdle(task);
        assertThat(taskWasRun.get(), is(false));

        // Otherwise the animator context's Handler will
        // execute anything sent to it immediately.
        scheduler.pause();
        animatorContext.endAnimation();
        assertThat(taskWasRun.get(), is(false));

        animatorContext.beginAnimation();
        assertThat(taskWasRun.get(), is(false));

        animatorContext.endAnimation();
        scheduler.advanceToLastPostedRunnable();
        assertThat(taskWasRun.get(), is(true));
    }

    @Test
    public void listenerFunctionality() throws Exception {
        AnimatorContext animatorContext = spy(this.animatorContext);
        AnimatorSet fake = spy(new AnimatorSet());
        fake.addListener(animatorContext);

        animatorContext.onAnimationStart(fake);
        verify(animatorContext).beginAnimation();

        animatorContext.onAnimationEnd(fake);
        verify(animatorContext).endAnimation();
        verify(fake).removeListener(animatorContext);
    }
}
