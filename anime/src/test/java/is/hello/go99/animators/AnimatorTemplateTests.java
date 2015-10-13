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

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import org.junit.Test;

import is.hello.go99.Anime;
import is.hello.go99.Go99TestCase;
import is.hello.go99.R;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class AnimatorTemplateTests extends Go99TestCase {
    @Test
    public void sensibleDefaults() throws Exception {
        assertThat(AnimatorTemplate.DEFAULT.duration, is((long) Anime.DURATION_NORMAL));
        assertThat(AnimatorTemplate.DEFAULT.interpolator, is(Anime.INTERPOLATOR_DEFAULT));

        AnimatorTemplate template1 = new AnimatorTemplate(Anime.DURATION_FAST);
        assertThat(template1.duration, is((long) Anime.DURATION_FAST));
        assertThat(template1.interpolator, is(Anime.INTERPOLATOR_DEFAULT));

        Interpolator interpolator = new AccelerateInterpolator();
        AnimatorTemplate template2 = new AnimatorTemplate(interpolator);
        assertThat(template2.duration, is((long) Anime.DURATION_NORMAL));
        assertThat(template2.interpolator, is(interpolator));

        AnimatorTemplate template3 = new AnimatorTemplate(Anime.DURATION_VERY_FAST, interpolator);
        assertThat(template3.duration, is((long) Anime.DURATION_VERY_FAST));
        assertThat(template3.interpolator, is(interpolator));
    }

    @Test
    public void updates() throws Exception {
        AnimatorTemplate template1 = AnimatorTemplate.DEFAULT;

        AnimatorTemplate template2 = template1.withDuration(Anime.DURATION_VERY_FAST);
        assertThat(template1, is(not(sameInstance(template2))));
        assertThat(template1.duration, not(equalTo(template2.duration)));
        assertThat(template2.duration, is((long) Anime.DURATION_VERY_FAST));

        Interpolator newInterpolator = new AccelerateInterpolator();
        AnimatorTemplate template3 = template1.withInterpolator(newInterpolator);
        assertThat(template1, is(not(sameInstance(template3))));
        assertThat(template1.interpolator, is(not(equalTo(template3.interpolator))));
        assertThat(template3.interpolator, is(equalTo(newInterpolator)));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void apply() throws Exception {
        AnimatorSet animator = new AnimatorSet();
        AnimatorTemplate.DEFAULT.apply(animator);

        assertThat(animator.getDuration(),
                   is(AnimatorTemplate.DEFAULT.duration));
        assertThat(animator.getInterpolator(),
                   is((TimeInterpolator) AnimatorTemplate.DEFAULT.interpolator));


        LayoutTransition transition = new LayoutTransition();
        AnimatorTemplate.DEFAULT.apply(transition);
        assertThat(transition.getDuration(LayoutTransition.APPEARING),
                   is(AnimatorTemplate.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.CHANGING),
                   is(AnimatorTemplate.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.DISAPPEARING),
                   is(AnimatorTemplate.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.CHANGE_APPEARING),
                   is(AnimatorTemplate.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.CHANGE_DISAPPEARING),
                   is(AnimatorTemplate.DEFAULT.duration));


        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.anime_fade_in);
        AnimatorTemplate.DEFAULT.apply(animation);

        assertThat(animation.getDuration(),
                   is(AnimatorTemplate.DEFAULT.duration));
        assertThat(animation.getInterpolator(),
                   is(AnimatorTemplate.DEFAULT.interpolator));
    }

    @Test
    public void createColorAnimator() throws Exception {
        AnimatorTemplate template = AnimatorTemplate.DEFAULT;
        ValueAnimator colorAnimator = template.createColorAnimator(Color.BLACK, Color.WHITE);
        assertThat(colorAnimator.getInterpolator(),
                   is(equalTo((TimeInterpolator) template.interpolator)));
        assertThat(colorAnimator.getDuration(),
                   is(equalTo(template.duration)));
    }

    @Test
    public void createRectAnimator() throws Exception {
        AnimatorTemplate template = AnimatorTemplate.DEFAULT;
        ValueAnimator colorAnimator = template.createRectAnimator(new Rect(0, 0, 250, 250),
                                                                  new Rect(250, 250, 500, 500));
        assertThat(colorAnimator.getInterpolator(),
                   is(equalTo((TimeInterpolator) template.interpolator)));
        assertThat(colorAnimator.getDuration(),
                   is(equalTo(template.duration)));
    }
}
