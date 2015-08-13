/*
 * Copyright 2014-2015 Hello, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package is.hello.go99.animators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class AnimatorContext implements Animator.AnimatorListener {
    /**
     * Whether or not stack-traces should be printed when {@link #beginAnimation()}
     * and {@link #endAnimation()} are called. Provided for debugging dangling animations.
     */
    public static boolean DEBUG = false;

    private static final int MSG_IDLE = 0;

    private final String name;
    private final List<Runnable> runOnIdle = new ArrayList<>();
    private final List<Transaction> runningTransactions = new ArrayList<>();

    private @Nullable AnimatorContext parent;
    private int activeAnimationCount = 0;

    private final Handler idleHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == MSG_IDLE) {
                for (Runnable task : runOnIdle) {
                    task.run();
                }
                runOnIdle.clear();

                return true;
            }

            return false;
        }
    });

    /**
     * Constructs an animator context with a given name.
     * @param name  The name used for {@link #toString()}.
     */
    public AnimatorContext(@NonNull String name) {
        this.name = name;
    }


    //region Listeners

    /**
     * Posts a unit of work to run when the context is idle.
     * <p/>
     * The task will be immediately executed if
     * the animation context is currently idle.
     */
    public void runWhenIdle(@NonNull Runnable task) {
        if (DEBUG) {
            printTrace("runWhenIdle");
        }

        if (activeAnimationCount == 0) {
            task.run();
        } else {
            runOnIdle.add(task);
        }
    }

    public void startWhenIdle(final @NonNull Animator animator) {
        runWhenIdle(new Runnable() {
            @Override
            public void run() {
                animator.start();
            }
        });
    }

    //endregion


    //region Active Animations

    private void printTrace(@NonNull String traceName) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int size = Math.min(stackTrace.length - 3, 4);
        String[] partialTraceComponents = new String[size];
        for (int i = 0; i < size; i++) {
            StackTraceElement stackTraceElement = stackTrace[i + 3];
            partialTraceComponents[i] = stackTraceElement.getClassName() + "#" + stackTraceElement.getMethodName();
        }
        String partialTrace = TextUtils.join("\n", partialTraceComponents);
        Log.i(getClass().getSimpleName(), traceName + ": " + partialTrace);
    }

    /**
     * Increment the active animation counter, potentially
     * bringing the animator context into the active state.
     * <p />
     * Animation tracking is implemented as a counter instead
     * of tracking animator instances to allow interactive
     * transitions to be treated as active animations within
     * a context. E.g. this can be used to prevent unwanted
     * animations from running when a user is swiping between
     * elements on a screen.
     */
    public void beginAnimation() {
        idleHandler.removeMessages(MSG_IDLE);

        if (parent != null && activeAnimationCount == 0) {
            parent.beginAnimation();
        }
        this.activeAnimationCount++;

        if (DEBUG) {
            printTrace("beginAnimation [" + activeAnimationCount + "]");
        }
    }

    /**
     * Decrement the active animation counter, potentially
     * bringing the animator context out of the active state.
     * If the counter reaches zero with this call, on the next
     * looper cycle any queued idle tasks will be executed.
     * <p />
     * Calling {@link #beginAnimation()} before the next looper
     * cycle will cause those tasks to remain queued.
     *
     * @see #beginAnimation() for rationale behind this API design.
     */
    public void endAnimation() {
        if (activeAnimationCount == 0) {
            throw new IllegalStateException("No active animations to end in " + toString());
        }

        this.activeAnimationCount--;

        if (DEBUG) {
            printTrace("endAnimation [" + activeAnimationCount + "]");
        }

        if (activeAnimationCount == 0) {
            if (parent != null) {
                parent.endAnimation();
            }

            idleHandler.removeMessages(MSG_IDLE);
            idleHandler.sendEmptyMessage(MSG_IDLE);
        }
    }

    /**
     * Sets the parent for this animator context.
     * <p />
     * An active child animator counts as one active
     * animation on its parent. Child contexts do not
     * inherit their parent's active state.
     */
    public void setParent(@Nullable AnimatorContext parent) {
        if (this.parent != null && activeAnimationCount > 0) {
            this.parent.endAnimation();
        }

        this.parent = parent;

        if (parent != null && activeAnimationCount > 0) {
            parent.beginAnimation();
        }
    }

    /**
     * Returns the parent context, if any.
     */
    public @Nullable AnimatorContext getParent() {
        return parent;
    }

    //endregion


    //region Listener

    @Override
    public void onAnimationStart(Animator animation) {
        beginAnimation();
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        endAnimation();
        animation.removeListener(this);
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        // Don't care
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        // Don't care
    }


    //endregion


    //region Transactions

    /**
     * Executes a series of animations within the animation context.
     * <code><pre>
     *     AnimatorContext context = ...;
     *     context.transaction(t -> {
     *         t.animatorFor(oldView).fadeOut(View.GONE);
     *         t.animatorFor(newView).fadeIn();
     *     });
     * </pre></code>
     *
     * @param template      An optional template to apply to animators involved in the transaction.
     * @param options       The options to apply to the transaction.
     * @param consumer      A consumer that will add animators to the transaction.
     * @param onCompleted   An optional listener to invoke when the animators all complete.
     * @return The transaction.
     *
     * @see TransactionOptions  For possible options.
     * @see Transaction         For more information on working with a transaction.
     */
    public Transaction transaction(final @Nullable AnimatorTemplate template,
                                   final @TransactionOptions int options,
                                   final @NonNull TransactionConsumer consumer,
                                   final @Nullable OnAnimationCompleted onCompleted) {
        final Transaction transaction = new Transaction(this, template);
        consumer.consume(transaction);
        if (transaction.isCanceled()) {
            return transaction;
        }

        Animator animator = transaction.toAnimator();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                runningTransactions.add(transaction);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                runningTransactions.remove(transaction);
            }
        });
        if (onCompleted != null) {
            animator.addListener(new OnAnimationCompleted.Adapter(onCompleted));
        }
        if ((options & OPTION_START_ON_IDLE) == OPTION_START_ON_IDLE) {
            startWhenIdle(animator);
        } else {
            animator.start();
        }

        return transaction;
    }

    /**
     * Short-hand provided for common use-case.
     *
     * @see #transaction(AnimatorTemplate, int, TransactionConsumer, OnAnimationCompleted)
     */
    public Transaction transaction(@NonNull TransactionConsumer consumer,
                                   @Nullable OnAnimationCompleted onCompleted) {
        return transaction(null, AnimatorContext.OPTIONS_DEFAULT, consumer, onCompleted);
    }

    /**
     * Stops any transactions that are currently running in the animator context.
     */
    public void cancelTransactions() {
        for (int i = runningTransactions.size() - 1; i >= 0; i--) {
            runningTransactions.get(i).cancel();
        }
        runningTransactions.clear();
    }

    //endregion


    @Override
    public String toString() {
        return "AnimationSystem{" +
                "name='" + name + '\'' +
                '}';
    }


    /**
     * A pending collection of animators that will be run
     * together within a containing animator context.
     * <p />
     * Transactions cannot be added to once they are committed,
     * which happens implicitly when your {@link TransactionConsumer}
     * returns. After a transaction has been committed, you may safely
     * retain a reference to it for as long as your circumstances require.
     *
     * @see #transaction(AnimatorTemplate, int, TransactionConsumer, OnAnimationCompleted)
     */
    public static class Transaction {
        /**
         * The animator context the transaction belongs to.
         */
        public final AnimatorContext animatorContext;

        /**
         * The template to apply to transaction animators, if non-null.
         */
        public final @Nullable AnimatorTemplate template;

        private final List<Animator> pending = new ArrayList<>(2);
        private @Nullable Animator animator;
        private boolean canceled = false;

        /**
         * Construct a transaction with an animator context and template.
         * <p />
         * Should not be called directly unless creating a new subclass.
         *
         * @see #transaction(AnimatorTemplate, int, TransactionConsumer, OnAnimationCompleted)
         */
        public Transaction(@NonNull AnimatorContext animatorContext,
                           @Nullable AnimatorTemplate template) {
            this.animatorContext = animatorContext;
            this.template = template;
        }

        /**
         * Creates a {@link MultiAnimator} for a given view, configuring it
         * according to the transaction's template (if applicable), and
         * starting it together with all other animations contained in
         * the transaction.
         * <p/>
         * The returned animator belongs to the transaction,
         * making modifications to it after the transaction
         * consumer returns is undefined.
         */
        public MultiAnimator animatorFor(@NonNull View view) {
            if (this.animator != null) {
                throw new IllegalStateException("Cannot modify a transaction after it has been committed");
            }

            MultiAnimator multiAnimator = MultiAnimator.animatorFor(view, animatorContext);
            pending.add(multiAnimator);
            return multiAnimator;
        }

        /**
         * Takes ownership of a given animator, configuring it
         * according to the transaction's template (if applicable)
         * and starting it together with all other animations
         * contained in the transaction.
         * <p />
         * The passed in animator belongs to the transaction,
         * making modifications to it after the transaction
         * consumer returns is undefined.
         *
         * @see #animatorFor(View) if you need a {@link MultiAnimator}.
         */
        public <T extends Animator> T takeOwnership(@NonNull T animator) {
            if (this.animator != null) {
                throw new IllegalStateException("Cannot modify a transaction after it has been committed");
            }

            pending.add(animator);
            animator.addListener(animatorContext);
            return animator;
        }

        /**
         * Converts the transaction into a compound animator
         * that will play all transaction animations together.
         * <p />
         * If the transaction contains only one animation, that
         * animation will be configured against the template (if
         * specified) and returned by this method.
         * <p />
         * This method has the side-effect of placing the transaction
         * into the committed state where no new animators may be added.
         * <p />
         * Guaranteed to return the same instance for multiple calls.
         */
        public Animator toAnimator() {
            if (animator == null) {
                if (pending.size() == 1) {
                    Animator single = pending.get(0);
                    if (template != null) {
                        template.apply(single);
                    }
                    this.animator = single;
                } else {
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(pending);
                    if (template != null) {
                        template.apply(set);
                    }
                    this.animator = set;
                }
                pending.clear();
            }
            return animator;
        }

        /**
         * Cancels the transaction.
         */
        public void cancel() {
            if (this.animator != null) {
                this.animator.cancel();
            }
            this.canceled = true;
        }

        /**
         * Returns whether or not the transaction has been canceled.
         */
        public boolean isCanceled() {
            return canceled;
        }
    }


    /**
     * A functor that takes a {@link Transaction} instance,
     * and adds animators to it.
     */
    public interface TransactionConsumer {
        void consume(@NonNull Transaction transaction);
    }

    /**
     * A scene owns an animator context and should contain a collection
     * of related views where tracking and coordinating animation life-cycles
     * is useful. A typical implementor of this method would be a Fragment,
     * an Activity, or a View depending on the structure of your application.
     */
    public interface Scene {
        /**
         * Returns the animator context associated with the scene.
         * <p />
         * Implementors must always return the same instance.
         */
        @NonNull AnimatorContext getAnimatorContext();
    }


    /**
     * The transaction should start when the animator context is idle.
     */
    public static final int OPTION_START_ON_IDLE = (1 << 1);

    /**
     * Use the default transaction options.
     */
    public static final int OPTIONS_DEFAULT = (OPTION_START_ON_IDLE);

    /**
     * @see AnimatorContext#OPTION_START_ON_IDLE
     * @see AnimatorContext#OPTIONS_DEFAULT
     */
    @IntDef(flag = true, value = {
            OPTION_START_ON_IDLE,
            OPTIONS_DEFAULT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransactionOptions {}
}
