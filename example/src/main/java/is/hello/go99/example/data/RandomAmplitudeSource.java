package is.hello.go99.example.data;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomAmplitudeSource implements AmplitudeSource, AmplitudeSource.Consumer {
    private static final String SAVED_AMPLITUDES = RandomAmplitudeSource.class.getName() + "#SAVED_AMPLITUDES";

    private static final int MIN_COUNT = 8;
    private static final int MAX_COUNT = 32;

    private final List<Consumer> consumers = new ArrayList<>(1);
    private final Random random = new Random();
    private @Nullable Producer producer;
    private @Nullable Either<float[], Throwable> current;


    //region Overrides

    @Nullable
    @Override
    public Bundle saveState() {
        if (current != null && current.isLeft()) {
            final float[] amplitudes = current.getLeft();
            final Bundle state = new Bundle();
            state.putFloatArray(SAVED_AMPLITUDES, amplitudes);
            return state;
        } else {
            return null;
        }
    }

    @Override
    public void restoreState(@NonNull Bundle state) {
        final float[] amplitudes = state.getFloatArray(SAVED_AMPLITUDES);
        if (amplitudes != null) {
            onAmplitudesReady(amplitudes);
        }
    }


    @Override
    public void update() {
        Log.d(getClass().getSimpleName(), "update()");

        if (producer != null) {
            producer.cancel(true);
        }

        this.producer = new Producer(this);
        producer.execute(random);
    }


    @Override
    public void addConsumer(@NonNull final Consumer consumer) {
        consumers.add(consumer);

        if (current != null) {
            current.match(new Either.Matcher<float[]>() {
                @Override
                public void match(float[] amplitudes) {
                    consumer.onAmplitudesReady(amplitudes);
                }
            }, new Either.Matcher<Throwable>() {
                @Override
                public void match(Throwable reason) {
                    consumer.onAmplitudesUnavailable(reason);
                }
            });
        }
    }

    @Override
    public void removeConsumer(@NonNull Consumer consumer) {
        consumers.remove(consumer);
    }

    //endregion


    //region Dispatching

    @Override
    public void onAmplitudesReady(@NonNull float[] amplitudes) {
        Log.d(getClass().getSimpleName(), "onAmplitudesReady(" + Arrays.toString(amplitudes) + ")");

        for (int i = consumers.size() - 1; i >= 0; i--) {
            consumers.get(i).onAmplitudesReady(amplitudes);
        }

        this.current = Either.left(amplitudes);
        this.producer = null;
    }

    @Override
    public void onAmplitudesUnavailable(@NonNull Throwable reason) {
        Log.e(getClass().getSimpleName(), "onAmplitudesUnavailable()", reason);

        for (int i = consumers.size() - 1; i >= 0; i--) {
            consumers.get(i).onAmplitudesUnavailable(reason);
        }

        this.current = Either.right(reason);
        this.producer = null;
    }

    //endregion


    static class Producer extends AsyncTask<Random, Void, float[]> {
        private final AmplitudeSource.Consumer consumer;

        Producer(@NonNull Consumer consumer) {
            this.consumer = consumer;
        }

        @Override
        protected float[] doInBackground(Random... randoms) {
            final Random random = randoms[0];
            final int count = MIN_COUNT + random.nextInt(MAX_COUNT - MIN_COUNT + 1);
            final float[] amplitudes = new float[count];
            for (int i = 0; i < count; i++) {
                amplitudes[i] = random.nextFloat();
            }

            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                Log.e(getClass().getSimpleName(), "sleep interrupted", e);
            }

            return amplitudes;
        }

        @Override
        protected void onPostExecute(float[] amplitudes) {
            if (!isCancelled()) {
                consumer.onAmplitudesReady(amplitudes);
            }
        }
    }
}
