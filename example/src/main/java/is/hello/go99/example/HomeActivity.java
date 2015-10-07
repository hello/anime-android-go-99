/*
 * Copyright 2015 Hello, Inc
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
package is.hello.go99.example;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.go99.example.adapter.HomeFragmentsAdapter;
import is.hello.go99.example.view.ViewPagerAnimatorContextAdapter;

public class HomeActivity extends AppCompatActivity implements AnimatorContext.Scene, ViewPager.OnPageChangeListener {
    private static final int NUMBER_FRAGMENTS = 7;
    private static final String SAVED_ENABLE_LONG_ANIMATIONS = HomeActivity.class.getName() + ".SAVED_ENABLE_LONG_ANIMATIONS";

    private AnimatorContext animatorContext;

    private ViewPager viewPager;
    private HomeFragmentsAdapter adapter;

    private boolean enableLongAnimations = false;

    private MenuItem longAnimationsMenuItem;
    private MenuItem clearMenuItem;


    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.animatorContext = new AnimatorContext(getClass().getSimpleName());
        animatorContext.setTransactionTemplate(new AnimatorTemplate(new FastOutSlowInInterpolator()));

        this.viewPager = (ViewPager) findViewById(R.id.activity_home_view_pager);

        final ViewPagerAnimatorContextAdapter animatorContextAdapter
                = new ViewPagerAnimatorContextAdapter(animatorContext);
        viewPager.addOnPageChangeListener(animatorContextAdapter);
        viewPager.addOnPageChangeListener(this);

        this.adapter = new HomeFragmentsAdapter(getSupportFragmentManager(), NUMBER_FRAGMENTS);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(NUMBER_FRAGMENTS - 1, false);

        if (savedInstanceState != null) {
            final boolean enableLongAnimations =
                    savedInstanceState.getBoolean(SAVED_ENABLE_LONG_ANIMATIONS);
            setEnableLongAnimations(enableLongAnimations);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVED_ENABLE_LONG_ANIMATIONS, enableLongAnimations);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        viewPager.clearOnPageChangeListeners();
    }

    //endregion


    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_amplitudes, menu);

        this.longAnimationsMenuItem = menu.findItem(R.id.action_long_animations);
        this.clearMenuItem = menu.findItem(R.id.action_clear);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        longAnimationsMenuItem.setChecked(enableLongAnimations);
        final AmplitudesFragment currentFragment =
                (AmplitudesFragment) adapter.getCurrentFragment();
        if (currentFragment != null) {
            clearMenuItem.setEnabled(currentFragment.isClearEnabled());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_long_animations: {
                final boolean newValue = !enableLongAnimations;
                setEnableLongAnimations(newValue);
                item.setChecked(newValue);
                return true;
            }

            case R.id.action_clear: {
                final AmplitudesFragment currentFragment =
                        (AmplitudesFragment) adapter.getCurrentFragment();
                if (currentFragment != null) {
                    currentFragment.clear();
                }
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //endregion


    //region Animations

    public void setEnableLongAnimations(boolean enableLongAnimations) {
        if (enableLongAnimations != this.enableLongAnimations) {
            final AnimatorTemplate oldTemplate = getAnimatorContext().getTransactionTemplate();
            final AnimatorTemplate newTemplate;
            if (enableLongAnimations) {
                newTemplate = oldTemplate.withDuration(oldTemplate.duration * 2L);
            } else {
                newTemplate = oldTemplate.withDuration(oldTemplate.duration / 2L);
            }
            getAnimatorContext().setTransactionTemplate(newTemplate);

            final AmplitudesFragment currentFragment =
                    (AmplitudesFragment) adapter.getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.setEnableLongAnimations(enableLongAnimations);
            }

            this.enableLongAnimations = enableLongAnimations;
        }
    }

    @NonNull
    @Override
    public AnimatorContext getAnimatorContext() {
        return animatorContext;
    }

    //endregion


    //region Scrolling

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        final AmplitudesFragment currentFragment = (AmplitudesFragment) adapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.setEnableLongAnimations(enableLongAnimations);
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    //endregion
}
