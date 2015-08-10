package is.hello.go99.animators;

import android.animation.Animator;
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

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class AnimatorContext implements Animator.AnimatorListener {
    private static final boolean DEBUG = false;

    private static final int MSG_IDLE = 0;

    private final String name;
    private final List<Runnable> pending = new ArrayList<>();

    private int activeAnimationCount = 0;

    private final Handler idleHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == MSG_IDLE) {
                for (Runnable runnable : pending) {
                    runnable.run();
                }
                pending.clear();

                return true;
            }

            return false;
        }
    });

    public AnimatorContext(@NonNull String name) {
        this.name = name;
    }


    //region Listeners

    /**
     * Posts a unit of work to run when the context is idle.
     * <p/>
     * The runnable will be immediately executed if
     * the animation context is currently idle.
     */
    public void runWhenIdle(@NonNull Runnable runnable) {
        if (DEBUG) {
            printTrace("runWhenIdle");
        }

        if (activeAnimationCount == 0) {
            runnable.run();
        } else {
            pending.add(runnable);
        }
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
        String partialTrace = TextUtils.join(" -> ", partialTraceComponents);
        Log.i(getClass().getSimpleName(), traceName + ": " + partialTrace);
    }

    public void beginAnimation() {
        idleHandler.removeMessages(MSG_IDLE);

        this.activeAnimationCount++;

        if (DEBUG) {
            printTrace("beginAnimation [" + activeAnimationCount + "]");
        }
    }

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
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }


    //endregion


    //region Transactions

    /**
     * Executes a series of animations within the animation context.
     * Example usage:
     * <pre>
     *     AnimatorContext context = ...;
     *     context.transaction(f -> {
     *         f.animate(oldView).fadeOut(View.GONE);
     *         f.animate(newView).fadeIn();
     *     });
     * </pre>
     * <p/>
     * The callback will be passed an instance of {@see #Facade}. This
     * facade should then be used to construct animators. The callback
     * <em>should not</em> call start on the animators, this will be
     * done automatically by the animator context.
     * @param config        An optional animator config to apply to each animator.
     * @param options       The options to apply to the transaction
     * @param consumer    A callback that describes the animations to run against a given facade.
     * @param onCompleted   An optional listener to invoke when the longest animation completes.
     */
    public void transaction(final @Nullable AnimatorConfig config,
                            final @TransactionOptions int options,
                            final @NonNull TransactionConsumer consumer,
                            final @Nullable OnAnimationCompleted onCompleted) {
        final List<Animator> animators = new ArrayList<>(2);

        Transaction transaction = new Transaction() {
            @Override
            public MultiAnimator animate(@NonNull View view) {
                MultiAnimator animator = animatorFor(view, AnimatorContext.this);
                if (config != null) {
                    config.apply(animator);
                }
                animators.add(animator);
                return animator;
            }

            @Override
            public <T extends Animator> T take(@NonNull T animator) {
                if (config != null) {
                    config.apply(animator);
                }
                animators.add(animator);
                return animator;
            }
        };
        consumer.consume(transaction);

        if ((options & OPTION_START_ON_IDLE) == OPTION_START_ON_IDLE) {
            runWhenIdle(new Runnable() {
                @Override
                public void run() {
                    startTransaction(animators, onCompleted);
                }
            });
        } else {
            startTransaction(animators, onCompleted);
        }
    }

    /**
     * Internal. Second half of {@link #transaction(AnimatorConfig, int, TransactionConsumer, OnAnimationCompleted)}.
     */
    private void startTransaction(@NonNull List<Animator> animators,
                                  @Nullable OnAnimationCompleted onCompleted) {
        Animator longestAnimator = null;
        long longestTotalDuration = 0;
        for (Animator animator : animators) {
            long totalDuration = animator.getStartDelay() + animator.getDuration();
            if (longestAnimator == null || totalDuration >= longestTotalDuration) {
                longestAnimator = animator;
                longestTotalDuration = totalDuration;
            }

            animator.start();
        }

        if (longestAnimator != null && onCompleted != null) {
            longestAnimator.addListener(new OnAnimationCompleted.Adapter(onCompleted));
        }
    }

    /**
     * Short-hand provided for common use-case.
     *
     * @see #transaction(AnimatorConfig, int, TransactionConsumer, OnAnimationCompleted)
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
     * @see #transaction(AnimatorConfig, int, TransactionConsumer, OnAnimationCompleted)
     */
    public interface Transaction {
        /**
         * Create a property animator proxy for a given view,
         * applying any properties provided, and queuing it
         * to be executed with any other animators in the
         * containing transaction.
         * <p/>
         * No external references to the returned animator should be made.
         */
        MultiAnimator animate(@NonNull View view);

        /**
         * Takes ownership of a given animator, applying
         * the transaction's configuration to it.
         * <p />
         * Use {@link #animate(View)} if you need a {@link MultiAnimator}.
         */
        <T extends Animator> T take(@NonNull T animator);
    }

    /**
     * The transaction should start when the animator context is idle.
     */
    public static final int OPTION_START_ON_IDLE = (1 << 1);

    /**
     * Use the default transaction options.
     */
    public static final int OPTIONS_DEFAULT = (OPTION_START_ON_IDLE);

    @IntDef(flag = true, value = {
            OPTION_START_ON_IDLE,
            OPTIONS_DEFAULT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransactionOptions {}


    public interface TransactionConsumer {
        void consume(@NonNull Transaction transaction);
    }

    public interface Scene {
        @NonNull AnimatorContext getAnimatorContext();
    }
}
