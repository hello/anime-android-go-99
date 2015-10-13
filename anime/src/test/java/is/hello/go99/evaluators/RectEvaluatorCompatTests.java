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
package is.hello.go99.evaluators;

import android.graphics.Rect;

import org.junit.Test;

import is.hello.go99.Go99TestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class RectEvaluatorCompatTests extends Go99TestCase {
    private final Rect START = new Rect(0, 0, 500, 500);
    private final Rect END = new Rect(250, 250, 750, 750);

    @Test
    public void reusesRect() throws Exception {
        Rect source = new Rect();
        RectEvaluatorCompat evaluator1 = new RectEvaluatorCompat(source);

        assertThat(evaluator1.evaluate(0.00f, START, END), sameInstance(source));
        assertThat(evaluator1.evaluate(0.25f, START, END), sameInstance(source));
        assertThat(evaluator1.evaluate(0.50f, START, END), sameInstance(source));
        assertThat(evaluator1.evaluate(0.75f, START, END), sameInstance(source));
        assertThat(evaluator1.evaluate(1.00f, START, END), sameInstance(source));


        RectEvaluatorCompat evaluator2 = new RectEvaluatorCompat();
        assertThat(evaluator2.evaluate(0.00f, START, END),
                   sameInstance(evaluator2.evaluate(0.00f, START, END)));
    }

    @Test
    public void interpolates() throws Exception {
        RectEvaluatorCompat evaluator = new RectEvaluatorCompat();

        assertThat(evaluator.evaluate(0.00f, START, END), is(new Rect(0, 0, 500, 500)));
        assertThat(evaluator.evaluate(0.25f, START, END), is(new Rect(62, 62, 562, 562)));
        assertThat(evaluator.evaluate(0.50f, START, END), is(new Rect(125, 125, 625, 625)));
        assertThat(evaluator.evaluate(0.75f, START, END), is(new Rect(187, 187, 687, 687)));
        assertThat(evaluator.evaluate(1.00f, START, END), is(new Rect(250, 250, 750, 750)));
    }
}
