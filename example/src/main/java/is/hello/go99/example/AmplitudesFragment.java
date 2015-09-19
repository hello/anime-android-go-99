package is.hello.go99.example;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.go99.example.data.AmplitudeSource;
import is.hello.go99.example.data.RandomAmplitudeSource;
import is.hello.go99.example.recycler.AmplitudeAdapter;
import is.hello.go99.example.recycler.AmplitudeItemAnimator;
import is.hello.go99.example.view.InfoTooltipPopup;

public class AmplitudesFragment extends Fragment
        implements AnimatorContext.Scene, SwipeRefreshLayout.OnRefreshListener,
        AmplitudeSource.Consumer, AmplitudeAdapter.OnClickListener {
    private static final long DELAY_STEP = 10;
    private static final float TARGET_DIMMED_ALPHA = 0.25f;

    private static final String SAVED_SOURCE_STATE = AmplitudesFragment.class.getName() + "#SAVED_SOURCE_STATE";

    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());

    private AmplitudeSource amplitudeSource;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private AmplitudeAdapter adapter;

    private @Nullable InfoTooltipPopup infoTooltipPopup;


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

        this.adapter = new AmplitudeAdapter(getResources(), this);
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


    //region Item Animations

    @Override
    public void onItemClicked(@NonNull AmplitudeAdapter.ViewHolder viewHolder) {
        cascadeFadeOutFromCenterItem(viewHolder);
    }

    private void cascadeFadeOutFromCenterItem(@NonNull final AmplitudeAdapter.ViewHolder viewHolder) {
        final int childIndex = recyclerView.indexOfChild(viewHolder.itemView);
        if (childIndex == -1) {
            Log.w(getClass().getSimpleName(), "Child view is missing?!");
            return;
        }

        final AnimatorTemplate animatorTemplate = new AnimatorTemplate(Anime.DURATION_FAST,
                                                                       new FastOutLinearInInterpolator());
        getAnimatorContext().transaction(animatorTemplate, AnimatorContext.OPTIONS_DEFAULT, new AnimatorContext.TransactionConsumer() {
            @Override
            public void consume(@NonNull AnimatorContext.Transaction transaction) {
                long delay = 0;
                for (int i = childIndex - 1; i >= 0; i--) {
                    transaction.animatorFor(recyclerView.getChildAt(i))
                               .withStartDelay(delay)
                               .alpha(TARGET_DIMMED_ALPHA);

                    delay += DELAY_STEP;
                }

                transaction.animatorFor(viewHolder.itemView)
                           .alpha(1f);

                delay = 0;
                for (int i = childIndex + 1, count = recyclerView.getChildCount(); i < count; i++) {
                    transaction.animatorFor(recyclerView.getChildAt(i))
                               .withStartDelay(delay)
                               .alpha(TARGET_DIMMED_ALPHA);
                    delay += DELAY_STEP;
                }

                if (infoTooltipPopup != null) {
                    infoTooltipPopup.dismiss();
                }

                AmplitudesFragment.this.infoTooltipPopup = new InfoTooltipPopup(getActivity(), new Runnable() {
                    @Override
                    public void run() {
                        cascadeFadeIn();
                    }
                });
                infoTooltipPopup.setText("Hello, world");
                infoTooltipPopup.show(viewHolder.itemView);
            }
        }, new OnAnimationCompleted() {
            @Override
            public void onAnimationCompleted(boolean finished) {
            }
        });
    }

    private void cascadeFadeIn() {
        final AnimatorTemplate animatorTemplate = new AnimatorTemplate(Anime.DURATION_FAST,
                                                                       new FastOutSlowInInterpolator());
        getAnimatorContext().transaction(animatorTemplate, AnimatorContext.OPTIONS_DEFAULT, new AnimatorContext.TransactionConsumer() {
            @Override
            public void consume(@NonNull AnimatorContext.Transaction transaction) {
                for (int i = 0, count = recyclerView.getChildCount(); i < count; i++) {
                    transaction.animatorFor(recyclerView.getChildAt(i))
                               .alpha(1f);
                }
            }
        }, new OnAnimationCompleted() {
            @Override
            public void onAnimationCompleted(boolean finished) {
            }
        });
    }

    //endregion
}
