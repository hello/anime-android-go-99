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
package is.hello.go99.example.adapter;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import is.hello.go99.example.AmplitudesFragment;

/**
 * Provides an invariant number of amplitude fragments for a view pager.
 */
public class AmplitudeFragmentsAdapter extends FragmentPagerAdapter {
    private final int count;

    public AmplitudeFragmentsAdapter(@NonNull FragmentManager fm,
                                     int count) {
        super(fm);

        this.count = count;
    }

    @Override
    public int getCount() {
        return count;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return new AmplitudesFragment();
    }
}
