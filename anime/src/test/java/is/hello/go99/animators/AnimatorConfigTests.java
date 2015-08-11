package is.hello.go99.animators;

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import org.junit.Test;

import is.hello.go99.Anime;
import is.hello.go99.Go99TestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class AnimatorConfigTests extends Go99TestCase {
    @Test
    public void sensibleDefaults() throws Exception {
        assertThat(AnimatorConfig.DEFAULT.duration, is((long) Anime.DURATION_NORMAL));
        assertThat(AnimatorConfig.DEFAULT.interpolator, is(Anime.INTERPOLATOR_DEFAULT));

        AnimatorConfig config1 = new AnimatorConfig(Anime.DURATION_FAST);
        assertThat(config1.duration, is((long) Anime.DURATION_FAST));
        assertThat(config1.interpolator, is(Anime.INTERPOLATOR_DEFAULT));

        Interpolator interpolator = new AccelerateInterpolator();
        AnimatorConfig config2 = new AnimatorConfig(interpolator);
        assertThat(config2.duration, is((long) Anime.DURATION_NORMAL));
        assertThat(config2.interpolator, is(interpolator));

        AnimatorConfig config3 = new AnimatorConfig(Anime.DURATION_VERY_FAST, interpolator);
        assertThat(config3.duration, is((long) Anime.DURATION_VERY_FAST));
        assertThat(config3.interpolator, is(interpolator));
    }

    @Test
    public void updates() throws Exception {
        AnimatorConfig config1 = AnimatorConfig.DEFAULT;
        AnimatorConfig config2 = config1.withDuration(Anime.DURATION_VERY_FAST);
        assertThat(config1, not(sameInstance(config2)));
        assertThat(config1.duration, not(equalTo(config2.duration)));
        assertThat(config2.duration, is((long) Anime.DURATION_VERY_FAST));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void apply() throws Exception {
        AnimatorSet animator = new AnimatorSet();
        AnimatorConfig.DEFAULT.apply(animator);

        assertThat(animator.getDuration(), is(AnimatorConfig.DEFAULT.duration));
        assertThat(animator.getInterpolator(), is((TimeInterpolator) AnimatorConfig.DEFAULT.interpolator));


        LayoutTransition transition = new LayoutTransition();
        AnimatorConfig.DEFAULT.apply(transition);
        assertThat(transition.getDuration(LayoutTransition.APPEARING),
                   is(AnimatorConfig.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.CHANGING),
                   is(AnimatorConfig.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.DISAPPEARING),
                   is(AnimatorConfig.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.CHANGE_APPEARING),
                   is(AnimatorConfig.DEFAULT.duration));
        assertThat(transition.getDuration(LayoutTransition.CHANGE_DISAPPEARING),
                   is(AnimatorConfig.DEFAULT.duration));
    }
}
