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
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.go99.animators.MultiAnimator;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.go99.example.AmplitudesFragment;
import is.hello.go99.example.adapter.AmplitudeAdapter;

/**
 * Demonstrates coordinating a very large number of on-screen animations. This item animator
 * uses the {@link AnimatorContext} class to fade in and 'count up' a series of amplitudes.
 * <p>
 * All animations are coordinated through an {@link AnimatorContext.Transaction}, and the basic
 * characteristics of the animations are specified in via an {@link AnimatorTemplate} set
 * inside of {@link AmplitudesFragment#onCreate(Bundle)}.
 * <p>
 * The implementation of the animations are split between {@link Change} and
 * {@link AmplitudeView}. {@code Change} uses a {@link MultiAnimator} to efficiently
 * fade the views in and out, and {@code AmplitudeView} uses a classic {@code ValueAnimator}
 * to expand its amplitude bar from the left to the right.
 */
public class AmplitudeItemAnimator extends RecyclerView.ItemAnimator {
    private final AnimatorContext animatorContext;

    private boolean wantsLongDelayStep = false;
    private @Nullable Runnable runAfterNextAnimationComplete;

    private final List<Change> pending = new ArrayList<>();
    private final List<Change> running = new ArrayList<>();
    private @Nullable AnimatorContext.Transaction currentTransaction;


    //region Lifecycle

    /**
     * Constructs a new item animator.
     *
     * @param animatorContext The animator context to coordinate animations through.
     */
    public AmplitudeItemAnimator(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;

        // We don't care about this, so we'll avoid
        // a lot of extra work by turning it off.
        setSupportsChangeAnimations(false);
    }

    public void setWantsLongDelayStep(boolean wantsLongDelayStep) {
        this.wantsLongDelayStep = wantsLongDelayStep;
    }

    public boolean getWantsLongDelayStep() {
        return wantsLongDelayStep;
    }

    public long getDelayStep() {
        if (wantsLongDelayStep) {
            return 40;
        } else {
            return 20;
        }
    }

    //endregion


    //region Running Animations

    @Override
    public void runPendingAnimations() {
        Collections.sort(pending);

        this.currentTransaction = animatorContext.transaction(new AnimatorContext.TransactionConsumer() {
            @Override
            public void consume(@NonNull AnimatorContext.Transaction transaction) {
                long delay = 0;
                for (final Change change : pending) {
                    final AmplitudeAdapter.ViewHolder viewHolder = change.viewHolder;
                    change.animation.animate(AmplitudeItemAnimator.this,
                                             viewHolder,
                                             delay,
                                             transaction);
                    running.add(change);

                    delay += getDelayStep();
                }

                pending.clear();
            }
        }, new OnAnimationCompleted() {
            @Override
            public void onAnimationCompleted(boolean finished) {
                for (final Change change : running) {
                    change.animation.completed(AmplitudeItemAnimator.this,
                                               change.viewHolder);
                }
                running.clear();

                if (finished) {
                    dispatchAnimationsFinished();
                    if (runAfterNextAnimationComplete != null) {
                        runAfterNextAnimationComplete.run();
                    }
                    AmplitudeItemAnimator.this.runAfterNextAnimationComplete = null;
                    AmplitudeItemAnimator.this.currentTransaction = null;
                }
            }
        });
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        // See AmplitudeView#clearAnimation() for more info.
        item.itemView.clearAnimation();
    }

    @Override
    public void endAnimations() {
        if (currentTransaction != null) {
            currentTransaction.cancel();
        }
    }

    @Override
    public boolean isRunning() {
        return (!pending.isEmpty() || currentTransaction != null && currentTransaction.isRunning());
    }

    public void runAfterAnimationsDone(@NonNull Runnable runnable) {
        this.runAfterNextAnimationComplete = runnable;
    }

    //endregion


    //region Hooks

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        final AmplitudeAdapter.ViewHolder amplitudeHolder = (AmplitudeAdapter.ViewHolder) holder;
        amplitudeHolder.amplitudeView.setAlpha(0f);
        amplitudeHolder.amplitudeView.setAmplitude(0f);
        pending.add(new Change(Change.Animation.ADD, amplitudeHolder));
        return true;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        pending.add(new Change(Change.Animation.REMOVE, (AmplitudeAdapter.ViewHolder) holder));
        return true;
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY,
                               int toX, int toY) {
        dispatchMoveFinished(holder);
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
                                 RecyclerView.ViewHolder newHolder,
                                 int fromLeft, int fromTop, int toLeft, int toTop) {
        dispatchChangeFinished(oldHolder, true);
        return false;
    }

    //endregion


    static class Change implements Comparable<Change> {
        final Animation animation;
        final AmplitudeAdapter.ViewHolder viewHolder;

        Change(@NonNull Animation animation, @NonNull AmplitudeAdapter.ViewHolder viewHolder) {
            this.animation = animation;
            this.viewHolder = viewHolder;
        }

        @Override
        public int compareTo(@NonNull Change other) {
            return this.viewHolder.getAdapterPosition() - other.viewHolder.getAdapterPosition();
        }

        enum Animation {
            ADD {
                @Override
                void animate(@NonNull AmplitudeItemAnimator animator,
                             @NonNull AmplitudeAdapter.ViewHolder viewHolder,
                             long animationDelay,
                             @NonNull AnimatorContext.Transaction transaction) {
                    animator.dispatchAddStarting(viewHolder);

                    transaction.animatorFor(viewHolder.amplitudeView)
                               .withStartDelay(animationDelay)
                               .alpha(1f);

                    viewHolder.amplitudeView.animateToAmplitude(viewHolder.getAmplitudeValue(),
                                                                animationDelay,
                                                                transaction);
                }

                @Override
                void completed(@NonNull AmplitudeItemAnimator animator,
                               @NonNull AmplitudeAdapter.ViewHolder viewHolder) {
                    animator.dispatchAddFinished(viewHolder);
                }
            },
            REMOVE {
                @Override
                void animate(@NonNull AmplitudeItemAnimator animator,
                             @NonNull AmplitudeAdapter.ViewHolder viewHolder,
                             long animationDelay,
                             @NonNull AnimatorContext.Transaction transaction) {
                    animator.dispatchRemoveStarting(viewHolder);

                    transaction.animatorFor(viewHolder.amplitudeView)
                               .withStartDelay(animationDelay)
                               .alpha(0f);

                    viewHolder.amplitudeView.animateToAmplitude(0f,
                                                                animationDelay,
                                                                transaction);
                }

                @Override
                void completed(@NonNull AmplitudeItemAnimator animator,
                               @NonNull AmplitudeAdapter.ViewHolder viewHolder) {
                    animator.dispatchRemoveFinished(viewHolder);
                }
            };

            abstract void animate(@NonNull AmplitudeItemAnimator animator,
                                  @NonNull AmplitudeAdapter.ViewHolder viewHolder,
                                  long animationDelay,
                                  @NonNull AnimatorContext.Transaction transaction);
            abstract void completed(@NonNull AmplitudeItemAnimator animator,
                                    @NonNull AmplitudeAdapter.ViewHolder viewHolder);
        }
    }
}
