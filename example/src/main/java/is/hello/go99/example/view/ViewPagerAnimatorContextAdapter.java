/*
 * Copyright 2015 Hello Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package is.hello.go99.example.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;

import is.hello.go99.animators.AnimatorContext;

/**
 * Implements {@code ViewPager.OnPageChangeListener} to provide a bridge between
 * {@code ViewPager} and {@link AnimatorContext}. With this adapter, both interactive
 * and non-interactive page changes are counted as animation within the given animator
 * context. This allows tying animation timing to page transitions via
 * {@link AnimatorContext#runWhenIdle(Runnable)}.
 *
 * @see is.hello.go99.example.HomeActivity#onCreate(Bundle) for usage.
 */
public class ViewPagerAnimatorContextAdapter implements ViewPager.OnPageChangeListener {
    private final AnimatorContext animatorContext;
    private int lastPagerScrollState = ViewPager.SCROLL_STATE_IDLE;

    
    /**
     * Constructs an adapter that will notify a given animator
     * context when page change transitions are in progress.
     *
     * @param animatorContext The animator context to notify.
     */
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
