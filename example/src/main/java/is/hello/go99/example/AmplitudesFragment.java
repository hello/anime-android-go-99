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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.go99.example.data.AmplitudeSource;
import is.hello.go99.example.data.RandomAmplitudeSource;
import is.hello.go99.example.recycler.AmplitudeAdapter;
import is.hello.go99.example.recycler.AmplitudeItemAnimator;
import is.hello.go99.example.view.InfoTooltipView;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class AmplitudesFragment extends Fragment
        implements AnimatorContext.Scene, SwipeRefreshLayout.OnRefreshListener,
        AmplitudeSource.Consumer, AmplitudeAdapter.OnClickListener, InfoTooltipView.OnDismissListener {
    private static final float TARGET_DIMMED_ALPHA = 0.25f;

    private static final String SAVED_SOURCE_STATE = AmplitudesFragment.class.getName() + "#SAVED_SOURCE_STATE";

    private AnimatorContext animatorContext;
    private boolean enableLongAnimations = false;
    private long cascadeDelayStep = 10L;

    private AmplitudeSource amplitudeSource;

    private FrameLayout root;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private AmplitudeAdapter adapter;
    private Button generateData;

    private MenuItem longAnimationsItem;
    private MenuItem clearItem;

    private @Nullable InfoTooltipView infoTooltipView;
    private AmplitudeItemAnimator itemAnimator;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.animatorContext = new AnimatorContext(getClass().getSimpleName());
        animatorContext.setTransactionTemplate(new AnimatorTemplate(new FastOutSlowInInterpolator()));

        this.amplitudeSource = new RandomAmplitudeSource();
        if (savedInstanceState != null) {
            final Bundle savedState = savedInstanceState.getBundle(SAVED_SOURCE_STATE);
            if (savedState != null) {
                amplitudeSource.restoreState(savedState);
            }
        }

        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_amplitudes, container, false);

        this.root = (FrameLayout) view.findViewById(R.id.fragment_amplitudes_root);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_amplitudes_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary_dark,
                                                   R.color.accent_dark, R.color.primary);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_amplitudes_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new InfoTooltipDismissScrollListener());

        this.itemAnimator = new AmplitudeItemAnimator(getAnimatorContext());
        recyclerView.setItemAnimator(itemAnimator);

        this.adapter = new AmplitudeAdapter(getResources(), this);
        recyclerView.setAdapter(adapter);

        this.generateData = (Button) view.findViewById(R.id.fragment_amplitudes_generate_data);
        generateData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View ignored) {
                onRefresh();
            }
        });

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
        recyclerView.clearOnScrollListeners();

        this.swipeRefreshLayout = null;
        this.recyclerView = null;
        this.adapter = null;
        this.itemAnimator = null;
    }

    //endregion


    //region Menu

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_amplitudes, menu);

        this.longAnimationsItem = menu.findItem(R.id.action_long_animations);
        this.clearItem = menu.findItem(R.id.action_clear);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        longAnimationsItem.setChecked(enableLongAnimations);
        clearItem.setEnabled(adapter.getItemCount() > 0 && !swipeRefreshLayout.isRefreshing());
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
                if (adapter.getItemCount() > 0 && !swipeRefreshLayout.isRefreshing()) {
                    adapter.clear();
                    itemAnimator.runAfterAnimationsDone(new Runnable() {
                        @Override
                        public void run() {
                            showGenerateData();
                        }
                    });
                }
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onDestroyOptionsMenu() {
        this.longAnimationsItem = null;
        this.clearItem = null;
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
        if (generateData.isShown()) {
            hideInitialCallToAction();
        }

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


    //region Animations

    public void setEnableLongAnimations(boolean enableLongAnimations) {
        if (enableLongAnimations != this.enableLongAnimations) {
            final AnimatorTemplate oldTemplate = animatorContext.getTransactionTemplate();
            final AnimatorTemplate newTemplate;
            if (enableLongAnimations) {
                newTemplate = oldTemplate.withDuration(oldTemplate.duration * 2L);
                itemAnimator.setDelayStep(itemAnimator.getDelayStep() * 2L);
                this.cascadeDelayStep *= 2L;
            } else {
                newTemplate = oldTemplate.withDuration(oldTemplate.duration / 2L);
                itemAnimator.setDelayStep(itemAnimator.getDelayStep() / 2L);
                this.cascadeDelayStep /= 2L;
            }
            animatorContext.setTransactionTemplate(newTemplate);

            this.enableLongAnimations = enableLongAnimations;
        }
    }

    private void hideInitialCallToAction() {
        animatorFor(generateData, getAnimatorContext())
                .withInterpolator(new AnticipateOvershootInterpolator())
                .alpha(0f)
                .scale(0f)
                .addOnAnimationCompleted(new OnAnimationCompleted() {
                    @Override
                    public void onAnimationCompleted(boolean finished) {
                        if (finished) {
                            generateData.setVisibility(View.INVISIBLE);
                            generateData.setScaleX(1f);
                            generateData.setScaleY(1f);
                            generateData.setAlpha(1f);
                        }
                    }
                })
                .start();
    }

    private void showGenerateData() {
        animatorFor(generateData, getAnimatorContext())
                .withInterpolator(new AnticipateOvershootInterpolator())
                .addOnAnimationWillStart(new Runnable() {
                    @Override
                    public void run() {
                        generateData.setAlpha(0f);
                        generateData.setScaleX(0f);
                        generateData.setScaleY(0f);
                        generateData.setVisibility(View.VISIBLE);
                    }
                })
                .alpha(1f)
                .scale(1f)
                .start();
    }

    @Override
    public void onItemClicked(@NonNull AmplitudeAdapter.ViewHolder viewHolder) {
        cascadeDimAmplitudesFromCenterItem(viewHolder);
    }

    private void cascadeDimAmplitudesFromCenterItem(@NonNull final AmplitudeAdapter.ViewHolder viewHolder) {
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

                    delay += cascadeDelayStep;
                }

                transaction.animatorFor(viewHolder.itemView)
                           .alpha(1f);

                delay = 0;
                for (int i = childIndex + 1, count = recyclerView.getChildCount(); i < count; i++) {
                    transaction.animatorFor(recyclerView.getChildAt(i))
                               .withStartDelay(delay)
                               .alpha(TARGET_DIMMED_ALPHA);

                    delay += cascadeDelayStep;
                }

                if (infoTooltipView != null) {
                    infoTooltipView.dismiss(true);
                }

                AmplitudesFragment.this.infoTooltipView = new InfoTooltipView(getActivity());
                infoTooltipView.setText(getString(R.string.amplitude_tooltip_fmt,
                                                  viewHolder.getTargetAmplitude() * 100f));
                infoTooltipView.setAnimatorContext(getAnimatorContext());
                infoTooltipView.showAboveView(root, viewHolder.itemView, AmplitudesFragment.this);
            }
        }, new OnAnimationCompleted() {
            @Override
            public void onAnimationCompleted(boolean finished) {
            }
        });
    }

    private void undimAmplitudes() {
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

    @Override
    public void onInfoTooltipDismissed() {
        undimAmplitudes();
        this.infoTooltipView = null;
    }

    private class InfoTooltipDismissScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (infoTooltipView != null) {
                infoTooltipView.dismiss(true);
                AmplitudesFragment.this.infoTooltipView = null;

                for (int i = 0, count = recyclerView.getChildCount(); i < count; i++) {
                    recyclerView.getChildAt(i).setAlpha(1f);
                }
            }
        }
    }

    //endregion
}