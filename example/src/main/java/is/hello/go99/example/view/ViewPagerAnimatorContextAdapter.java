package is.hello.go99.example.view;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;

import is.hello.go99.animators.AnimatorContext;

public class ViewPagerAnimatorContextAdapter implements ViewPager.OnPageChangeListener {
    private final AnimatorContext animatorContext;
    private int lastPagerScrollState = ViewPager.SCROLL_STATE_IDLE;

    public ViewPagerAnimatorContextAdapter(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (lastPagerScrollState == ViewPager.SCROLL_STATE_IDLE &&
                state != ViewPager.SCROLL_STATE_IDLE) {
            animatorContext.beginAnimation("View pager scroll");
        } else if (lastPagerScrollState != ViewPager.SCROLL_STATE_IDLE &&
                state == ViewPager.SCROLL_STATE_IDLE) {
            animatorContext.endAnimation("View pager scroll");
        }
        this.lastPagerScrollState = state;
    }
}
