package is.hello.go99.animators;

import android.animation.Animator;
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

public final class AnimatorContext implements Animator.AnimatorListener {
    /**
     * Whether or not stack-traces should be printed when {@link #beginAnimation()}
     * and {@link #endAnimation()} are called. Provided for debugging dangling animations.
     */
    public static boolean DEBUG = false;

    private static final int MSG_IDLE = 0;

    private final String name;
    private final List<Runnable> runOnIdle = new ArrayList<>();

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
            idleHandler.removeMessages(MSG_IDLE);
            idleHandler.sendEmptyMessage(MSG_IDLE);
        }
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
     *
     * @see TransactionOptions  For possible options.
     * @see TransactionConsumer For more information on working with a transaction.
     */
    public void transaction(final @Nullable AnimatorTemplate template,
                            final @TransactionOptions int options,
                            final @NonNull TransactionConsumer consumer,
                            final @Nullable OnAnimationCompleted onCompleted) {
        final List<Animator> animators = new ArrayList<>(2);

        Transaction transaction = new Transaction() {
            @Override
            public MultiAnimator animatorFor(@NonNull View view) {
                MultiAnimator animator = MultiAnimator.animatorFor(view, AnimatorContext.this);
                animators.add(animator);
                return animator;
            }

            @Override
            public <T extends Animator> T take(@NonNull T animator) {
                animators.add(animator);
                return animator;
            }
        };
        consumer.consume(transaction);

        AnimatorSet animatorSet = new AnimatorSet();
        if (template != null) {
            template.apply(animatorSet);
        }
        if (onCompleted != null) {
            animatorSet.addListener(new OnAnimationCompleted.Adapter(onCompleted));
        }
        animatorSet.playTogether(animators);
        if ((options & OPTION_START_ON_IDLE) == OPTION_START_ON_IDLE) {
            startWhenIdle(animatorSet);
        } else {
            animatorSet.start();
        }
    }

    /**
     * Short-hand provided for common use-case.
     *
     * @see #transaction(AnimatorTemplate, int, TransactionConsumer, OnAnimationCompleted)
     */
    public void transaction(@NonNull TransactionConsumer consumer,
                            @Nullable OnAnimationCompleted onCompleted) {
        transaction(null, AnimatorContext.OPTIONS_DEFAULT, consumer, onCompleted);
    }

    //endregion


    @Override
    public String toString() {
        return "AnimationSystem{" +
                "name='" + name + '\'' +
                '}';
    }


    /**
     * Used for transaction callbacks to specify animations against views.
     *
     * @see #transaction(AnimatorTemplate, int, TransactionConsumer, OnAnimationCompleted)
     */
    public interface Transaction {
        /**
         * Creates a {@link MultiAnimator} instance for a given view,
         * applies the transaction's template if applicable, and returns
         * the value. The animator will then be started with the rest of
         * the transaction.
         * <p/>
         * No external references to the returned multi animator should be made.
         */
        MultiAnimator animatorFor(@NonNull View view);

        /**
         * Takes ownership of a given animator, applying
         * the transaction's template to it if applicable.
         * <p />
         * Use {@link #animatorFor(View)} if you need a {@link MultiAnimator}.
         */
        <T extends Animator> T take(@NonNull T animator);
    }


    /**
     * A functor that takes a {@link Transaction} instance,
     * and adds animators to it.
     */
    public interface TransactionConsumer {
        void consume(@NonNull Transaction transaction);
    }

    public interface Scene {
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
