package is.hello.go99.animators;

import android.animation.Animator;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.go99.Go99TestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OnAnimationCompletedTests extends Go99TestCase {
    @Test
    public void adapterFinished() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicBoolean value = new AtomicBoolean();
        final OnAnimationCompleted onCompleted = new OnAnimationCompleted() {
            @Override
            public void onAnimationCompleted(boolean finished) {
                called.set(true);
                value.set(finished);
            }
        };

        Animator.AnimatorListener listener = new OnAnimationCompleted.Adapter(onCompleted);
        listener.onAnimationEnd(null);

        assertThat(called.get(), is(true));
        assertThat(value.get(), is(true));
    }

    @Test
    public void adapterNotFinished() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicBoolean value = new AtomicBoolean(true);
        final OnAnimationCompleted onCompleted = new OnAnimationCompleted() {
            @Override
            public void onAnimationCompleted(boolean finished) {
                called.set(true);
                value.set(finished);
            }
        };

        Animator.AnimatorListener listener = new OnAnimationCompleted.Adapter(onCompleted);
        listener.onAnimationCancel(null);
        listener.onAnimationEnd(null);

        assertThat(called.get(), is(true));
        assertThat(value.get(), is(false));
    }
}
