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
package is.hello.go99.animators;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

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
import static org.mockito.Mockito.never;
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
            animatorContext.endAnimation("Not real!");
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

        final Scheduler scheduler = Robolectric.getForegroundThreadScheduler();

        animatorContext.beginAnimation("Test animation");
        animatorContext.runWhenIdle(task);
        assertThat(taskWasRun.get(), is(false));

        // Otherwise the animator context's Handler will
        // execute anything sent to it immediately.
        scheduler.pause();
        animatorContext.endAnimation("Test animation");
        assertThat(taskWasRun.get(), is(false));

        animatorContext.beginAnimation("Test animation");
        assertThat(taskWasRun.get(), is(false));

        animatorContext.endAnimation("Test animation");
        scheduler.advanceToLastPostedRunnable();
        assertThat(taskWasRun.get(), is(true));
    }

    @Test
    public void runOnIdleConcurrentModification() {
        final AtomicBoolean reached = new AtomicBoolean(false);
        animatorContext.beginAnimation("Test animation");
        animatorContext.runWhenIdle(new Runnable() {
            @Override
            public void run() {
                animatorContext.beginAnimation("Nested test animation");
                animatorContext.runWhenIdle(new Runnable() {
                    @Override
                    public void run() {
                        reached.set(true);
                    }
                });
                animatorContext.endAnimation("Nested test animation");
            }
        });
        animatorContext.endAnimation("Test animation");

        assertThat(reached.get(), is(true));
    }

    @Test
    public void bind() throws Exception {
        final AnimatorContext animatorContext = spy(this.animatorContext);
        final AnimatorSet fake = spy(new AnimatorSet());
        animatorContext.bind(fake, "Test animation");

        fake.getListeners().get(0).onAnimationStart(fake);
        verify(animatorContext).beginAnimation("Test animation");

        fake.getListeners().get(0).onAnimationEnd(fake);
        verify(animatorContext).endAnimation("Test animation");
    }

    @Test(expected = IllegalArgumentException.class)
    public void bindBlocksNotBindable() throws Exception {
        final FrameLayout fakeView = new FrameLayout(getContext());
        animatorContext.bind(MultiAnimator.animatorFor(fakeView, animatorContext), "Test animator");
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static class TransactionTests extends Go99TestCase {
        private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());

        @Test
        public void toAnimatorSingle() throws Exception {
            final AnimatorTemplate template = new AnimatorTemplate(Anime.DURATION_SLOW,
                                                                   new AccelerateDecelerateInterpolator());

            final AnimatorContext.Transaction single = new AnimatorContext.Transaction(animatorContext,
                                                                                       template);
            final AnimatorSet testAnimator = new AnimatorSet();
            single.takeOwnership(testAnimator, "Test animation");

            final Animator animator1 = single.toAnimator();
            assertThat(animator1, is(sameInstance((Animator) testAnimator)));
            assertThat(animator1.getDuration(),
                       is(equalTo(template.duration)));
            assertThat(animator1.getInterpolator(),
                       is(equalTo((TimeInterpolator) template.interpolator)));
        }

        @Test
        public void toAnimatorMultiple() throws Exception {
            final AnimatorTemplate template = new AnimatorTemplate(Anime.DURATION_SLOW,
                                                                   new AccelerateDecelerateInterpolator());

            final AnimatorContext.Transaction multiple = new AnimatorContext.Transaction(animatorContext,
                                                                                         template);

            final AnimatorSet testAnimator1 = new AnimatorSet();
            final AnimatorSet testAnimator2 = new AnimatorSet();
            multiple.takeOwnership(testAnimator1, "Test animation 1");
            multiple.takeOwnership(testAnimator2, "Test animation 2");

            final Animator animator2 = multiple.toAnimator();
            assertThat(animator2, is(not(sameInstance((Animator) testAnimator1))));
            assertThat(animator2.getDuration(),
                       is(equalTo(template.duration)));
            assertThat(animator2.getInterpolator(),
                       is(equalTo((TimeInterpolator) template.interpolator)));
        }

        @Test
        public void toAnimatorResultConsistency() {
            final AnimatorTemplate template = new AnimatorTemplate(Anime.DURATION_SLOW,
                                                                   new AccelerateDecelerateInterpolator());


            final AnimatorContext.Transaction single = new AnimatorContext.Transaction(animatorContext,
                                                                                       template);
            final AnimatorSet testAnimator = new AnimatorSet();
            single.takeOwnership(testAnimator, "Test animation");

            assertThat(single.toAnimator(), is(sameInstance(single.toAnimator())));


            final AnimatorContext.Transaction multiple = new AnimatorContext.Transaction(animatorContext,
                                                                                         template);

            final AnimatorSet testAnimator1 = new AnimatorSet();
            multiple.takeOwnership(testAnimator1, "Test animation 1");
            final AnimatorSet testAnimator2 = new AnimatorSet();
            multiple.takeOwnership(testAnimator2, "Test animation 2");

            assertThat(multiple.toAnimator(), is(sameInstance(multiple.toAnimator())));
        }

        @Test
        public void cancelBeforeStart() {
            final AnimatorTemplate template = new AnimatorTemplate(Anime.DURATION_SLOW,
                                                                   new AccelerateDecelerateInterpolator());

            final AnimatorContext.Transaction single = new AnimatorContext.Transaction(animatorContext,
                                                                                       template);
            final AnimatorSet testAnimator = spy(new AnimatorSet());
            single.takeOwnership(testAnimator, "Test animation");

            single.cancel();
            verify(testAnimator, never()).cancel();

            single.start();
            verify(testAnimator, never()).start();
        }

        @Test
        public void cancelAfterStart() {
            final AnimatorTemplate template = new AnimatorTemplate(Anime.DURATION_SLOW,
                                                                   new AccelerateDecelerateInterpolator());

            final AnimatorContext.Transaction single = new AnimatorContext.Transaction(animatorContext,
                                                                                       template);
            final AnimatorSet testAnimator = spy(new AnimatorSet());
            single.takeOwnership(testAnimator, "Test animation");

            single.start();
            verify(testAnimator).start();

            single.cancel();
            verify(testAnimator).cancel();
        }
    }
}
