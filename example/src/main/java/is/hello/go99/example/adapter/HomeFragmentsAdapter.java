package is.hello.go99.example.adapter;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import is.hello.go99.example.AmplitudesFragment;

public class HomeFragmentsAdapter extends FragmentPagerAdapter {
    private final int count;

    public HomeFragmentsAdapter(@NonNull FragmentManager fm, int count) {
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
