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
package is.hello.go99;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewPropertyAnimator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AnimeTests extends Go99TestCase {
    @Test
    public void constantsMatch() throws Exception {
        Resources resources = getResources();
        assertThat(Anime.DURATION_FAST, is(resources.getInteger(R.integer.anime_duration_fast)));
        assertThat(Anime.DURATION_NORMAL, is(resources.getInteger(R.integer.anime_duration_normal)));
        assertThat(Anime.DURATION_SLOW, is(resources.getInteger(R.integer.anime_duration_slow)));
        assertThat(Anime.DURATION_VERY_FAST, is(resources.getInteger(R.integer.anime_duration_very_fast)));
    }

    @Test
    public void calculateDuration() throws Exception {
        assertThat((int) Anime.calculateDuration(0f, 1000f), is(Anime.DURATION_FAST));
        assertThat((int) Anime.calculateDuration(500f, 1000f), is(Anime.DURATION_SLOW));
    }

    @Test
    public void interpolateFloats() throws Exception {
        final float MIN = -1f,
                    MAX = 1f;

        assertThat(Anime.interpolateFloats(0.00f, MIN, MAX), is(-1f));
        assertThat(Anime.interpolateFloats(0.25f, MIN, MAX), is(-0.5f));
        assertThat(Anime.interpolateFloats(0.50f, MIN, MAX), is(0f));
        assertThat(Anime.interpolateFloats(0.75f, MIN, MAX), is(0.5f));
        assertThat(Anime.interpolateFloats(1.00f, MIN, MAX), is(1f));
    }

    @Test
    public void interpolateColors() throws Exception {
        final int MIN = 0xff000000,
                  MAX = 0xffffffff;

        assertThat(Anime.interpolateColors(0.00f, MIN, MAX), is(0xff000000));
        assertThat(Anime.interpolateColors(0.25f, MIN, MAX), is(0xff3F3F3F));
        assertThat(Anime.interpolateColors(0.50f, MIN, MAX), is(0xff7F7F7F));
        assertThat(Anime.interpolateColors(0.75f, MIN, MAX), is(0xffBFBFBF));
        assertThat(Anime.interpolateColors(1.00f, MIN, MAX), is(0xffFFFFFF));
    }

    @Test
    public void cancelAll() throws Exception {
        View view = spy(new View(getContext()));
        ViewPropertyAnimator animator = spy(view.animate());
        doReturn(animator).when(view).animate();

        Anime.cancelAll(view);

        verify(view).clearAnimation();
        verify(animator).cancel();
    }

    @Test
    public void trackingAnimatingViews() throws Exception {
        View test = new View(getContext());
        assertThat(Anime.isAnimating(test), is(false));

        Anime.addAnimatingView(test);
        assertThat(Anime.isAnimating(test), is(true));

        Anime.removeAnimatingView(test);
        assertThat(Anime.isAnimating(test), is(false));
    }
}
