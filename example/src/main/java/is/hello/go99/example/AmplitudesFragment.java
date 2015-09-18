package is.hello.go99.example;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.example.data.AmplitudeSource;
import is.hello.go99.example.data.RandomAmplitudeSource;
import is.hello.go99.example.recycler.AmplitudeAdapter;
import is.hello.go99.example.recycler.AmplitudeItemAnimator;

public class AmplitudesFragment extends Fragment
        implements AnimatorContext.Scene, SwipeRefreshLayout.OnRefreshListener,
        AmplitudeSource.Consumer {
    private static final String SAVED_SOURCE_STATE = AmplitudesFragment.class.getName() + "#SAVED_SOURCE_STATE";

    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());

    private AmplitudeSource amplitudeSource;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private AmplitudeAdapter adapter;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.amplitudeSource = new RandomAmplitudeSource();
        if (savedInstanceState != null) {
            final Bundle savedState = savedInstanceState.getBundle(SAVED_SOURCE_STATE);
            if (savedState != null) {
                amplitudeSource.restoreState(savedState);
            }
        }

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_amplitudes, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_amplitudes_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary_dark,
                                                   R.color.accent_dark, R.color.primary);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_amplitudes_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);

        recyclerView.setItemAnimator(new AmplitudeItemAnimator(getAnimatorContext()));

        this.adapter = new AmplitudeAdapter(getResources());
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        amplitudeSource.addConsumer(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBundle(SAVED_SOURCE_STATE, amplitudeSource.saveState());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        amplitudeSource.removeConsumer(this);

        this.swipeRefreshLayout = null;
        this.recyclerView = null;
        this.adapter = null;
    }

    //endregion


    //region Callbacks

    @NonNull
    @Override
    public AnimatorContext getAnimatorContext() {
        return animatorContext;
    }

    @Override
    public void onRefresh() {
        adapter.clear();
        amplitudeSource.update();
    }

    @Override
    public void onAmplitudesReady(@NonNull final float[] amplitudes) {
        swipeRefreshLayout.setRefreshing(false);
        getAnimatorContext().runWhenIdle(new Runnable() {
            @Override
            public void run() {
                adapter.bindAmplitudes(amplitudes);
            }
        });
    }

    @Override
    public void onAmplitudesUnavailable(@NonNull Throwable reason) {
        if (!isAdded()) {
            Log.e(getClass().getSimpleName(), "Amplitudes unavailable", reason);
            return;
        }

        swipeRefreshLayout.setRefreshing(false);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_error);
        builder.setMessage(reason.getLocalizedMessage());
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setCancelable(true);
        builder.create().show();
    }

    //endregion
}
