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
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.go99.example.adapter.AmplitudeAdapter;
import is.hello.go99.example.data.Amplitude;
import is.hello.go99.example.data.AmplitudeSource;
import is.hello.go99.example.data.RandomAmplitudeSource;
import is.hello.go99.example.view.AmplitudeItemAnimator;
import is.hello.go99.example.view.InfoTooltipView;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class AmplitudesFragment extends Fragment
        implements AnimatorContext.Scene, SwipeRefreshLayout.OnRefreshListener,
        AmplitudeSource.Consumer, AmplitudeAdapter.OnClickListener, InfoTooltipView.OnDismissListener {
    private static final float TARGET_DIMMED_ALPHA = 0.25f;

    private static final String SAVED_SOURCE_STATE = AmplitudesFragment.class.getName() + ".SAVED_SOURCE_STATE";
    private static final String SAVED_WANTS_LONG_DELAY_STEP = AmplitudesFragment.class.getName() + ".SAVED_WANTS_LONG_DELAY_STEP";

    private AmplitudeSource amplitudeSource;

    private FrameLayout root;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private AmplitudeAdapter adapter;
    private Button generateData;

    private @Nullable InfoTooltipView infoTooltipView;
    private AmplitudeItemAnimator itemAnimator;


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
        if (savedInstanceState != null) {
            final boolean wantsLongDelayStep = savedInstanceState.getBoolean(SAVED_WANTS_LONG_DELAY_STEP);
            itemAnimator.setWantsLongDelayStep(wantsLongDelayStep);
        }
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (getView() != null && isVisibleToUser) {
            if (adapter.getItemCount() == 0) {
                onRefresh();
            } else {
                generateData.setVisibility(View.INVISIBLE);
                generateData.setScaleX(1f);
                generateData.setScaleY(1f);
                generateData.setAlpha(1f);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBundle(SAVED_SOURCE_STATE, amplitudeSource.saveState());
        outState.putBoolean(SAVED_WANTS_LONG_DELAY_STEP, itemAnimator.getWantsLongDelayStep());
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

    public boolean isClearEnabled() {
        return (adapter.getItemCount() > 0 && !swipeRefreshLayout.isRefreshing());
    }

    public void clear() {
        if (isClearEnabled()) {
            adapter.clear();
            itemAnimator.runAfterAnimationsDone(new Runnable() {
                @Override
                public void run() {
                    showGenerateData();
                }
            });
        }
    }

    //endregion


    //region Callbacks

    @NonNull
    @Override
    public AnimatorContext getAnimatorContext() {
        final AnimatorContext.Scene parentScene = (AnimatorContext.Scene) getActivity();
        return parentScene.getAnimatorContext();
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
    public void onAmplitudesReady(@NonNull final List<Amplitude> amplitudes) {
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
        itemAnimator.setWantsLongDelayStep(enableLongAnimations);
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

                    delay += itemAnimator.getDelayStep();
                }

                transaction.animatorFor(viewHolder.itemView)
                           .alpha(1f);

                delay = 0;
                for (int i = childIndex + 1, count = recyclerView.getChildCount(); i < count; i++) {
                    transaction.animatorFor(recyclerView.getChildAt(i))
                               .withStartDelay(delay)
                               .alpha(TARGET_DIMMED_ALPHA);

                    delay += itemAnimator.getDelayStep();
                }

                if (infoTooltipView != null) {
                    infoTooltipView.dismiss(true);
                }

                AmplitudesFragment.this.infoTooltipView = new InfoTooltipView(getActivity());
                infoTooltipView.setText(getString(R.string.amplitude_tooltip_fmt,
                                                  viewHolder.getAmplitudeValue() * 100f));
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
