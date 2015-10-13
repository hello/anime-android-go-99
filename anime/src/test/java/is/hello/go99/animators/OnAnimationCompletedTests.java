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
