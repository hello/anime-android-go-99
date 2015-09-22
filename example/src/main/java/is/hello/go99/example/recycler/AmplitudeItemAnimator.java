package is.hello.go99.example.recycler;

import android.animation.Animator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.OnAnimationCompleted;

public class AmplitudeItemAnimator extends RecyclerView.ItemAnimator {
    private final AnimatorContext animatorContext;

    private long delayStep = 20;
    private @Nullable Runnable runAfterNextAnimationComplete;

    private final List<Change> pending = new ArrayList<>();
    private final List<Change> running = new ArrayList<>();
    private @Nullable Animator currentTransaction;


    //region Lifecycle

    public AmplitudeItemAnimator(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
        setSupportsChangeAnimations(false);
    }

    public long getDelayStep() {
        return delayStep;
    }

    public void setDelayStep(long delayStep) {
        this.delayStep = delayStep;
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

                    delay += delayStep;
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

                    viewHolder.amplitudeView.animateToAmplitude(viewHolder.getTargetAmplitude(),
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
