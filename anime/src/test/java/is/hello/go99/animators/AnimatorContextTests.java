/*
 * Copyright 2014-2015 Hello, Inc
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
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.util.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.go99.Anime;
import is.hello.go99.Go99TestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
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


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static class TransactionTests extends Go99TestCase {
        private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());

        @Test
        public void toAnimatorSingle() throws Exception {
            AnimatorTemplate template = new AnimatorTemplate(Anime.DURATION_SLOW,
                                                             new AccelerateDecelerateInterpolator());

            AnimatorContext.Transaction single = new AnimatorContext.Transaction(animatorContext,
                                                                                 template);
            AnimatorSet testAnimator = new AnimatorSet();
            single.takeOwnership(testAnimator);

            Animator animator1 = single.toAnimator();
            assertThat(animator1, is(sameInstance((Animator) testAnimator)));
            assertThat(animator1.getDuration(),
                       is(equalTo(template.duration)));
            assertThat(animator1.getInterpolator(),
                       is(equalTo((TimeInterpolator) template.interpolator)));
        }

        @Test
        public void toAnimatorMultiple() throws Exception {
            AnimatorTemplate template = new AnimatorTemplate(Anime.DURATION_SLOW,
                                                             new AccelerateDecelerateInterpolator());

            AnimatorContext.Transaction multiple = new AnimatorContext.Transaction(animatorContext,
                                                                                   template);

            AnimatorSet testAnimator1 = new AnimatorSet();
            AnimatorSet testAnimator2 = new AnimatorSet();
            multiple.takeOwnership(testAnimator1);
            multiple.takeOwnership(testAnimator2);

            Animator animator2 = multiple.toAnimator();
            assertThat(animator2, is(not(sameInstance((Animator) testAnimator1))));
            assertThat(animator2.getDuration(),
                       is(equalTo(template.duration)));
            assertThat(animator2.getInterpolator(),
                       is(equalTo((TimeInterpolator) template.interpolator)));
        }
    }
}
