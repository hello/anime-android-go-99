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
package is.hello.go99.example.data;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomAmplitudeSource implements AmplitudeSource {
    private static final String SAVED_AMPLITUDES = RandomAmplitudeSource.class.getName() + "#SAVED_AMPLITUDES";

    private static final int MIN_COUNT = 8;
    private static final int MAX_COUNT = 32;

    private final List<Consumer> consumers = new ArrayList<>(1);
    private final Random random = new Random();
    private @Nullable Producer producer;
    private @Nullable Either<ArrayList<Amplitude>, Throwable> current;


    //region Overrides

    @Nullable
    @Override
    public Bundle saveState() {
        if (current != null && current.isLeft()) {
            final ArrayList<Amplitude> amplitudes = current.getLeft();
            final Bundle state = new Bundle();
            state.putSerializable(SAVED_AMPLITUDES, amplitudes);
            return state;
        } else {
            return null;
        }
    }

    @Override
    public void restoreState(@NonNull Bundle state) {
        @SuppressWarnings("unchecked")
        final ArrayList<Amplitude> amplitudes =
                (ArrayList<Amplitude>) state.getSerializable(SAVED_AMPLITUDES);
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

        this.producer = new Producer();
        producer.execute(random);
    }


    @Override
    public void addConsumer(@NonNull final Consumer consumer) {
        consumers.add(consumer);

        if (current != null) {
            current.match(new Either.Matcher<ArrayList<Amplitude>>() {
                @Override
                public void match(ArrayList<Amplitude> amplitudes) {
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

    public void onAmplitudesReady(@NonNull ArrayList<Amplitude> amplitudes) {
        Log.d(getClass().getSimpleName(), "onAmplitudesReady(" + amplitudes + ")");

        for (int i = consumers.size() - 1; i >= 0; i--) {
            consumers.get(i).onAmplitudesReady(amplitudes);
        }

        this.current = Either.left(amplitudes);
        this.producer = null;
    }

    //endregion


    class Producer extends AsyncTask<Random, Void, ArrayList<Amplitude>> {
        @Override
        protected ArrayList<Amplitude> doInBackground(Random... randoms) {
            final Random random = randoms[0];
            final int count = MIN_COUNT + random.nextInt(MAX_COUNT - MIN_COUNT + 1);
            final ArrayList<Amplitude> amplitudes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                amplitudes.add(new Amplitude(random.nextFloat(), random.nextFloat()));
            }

            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                Log.e(getClass().getSimpleName(), "sleep interrupted", e);
            }

            return amplitudes;
        }

        @Override
        protected void onPostExecute(ArrayList<Amplitude> amplitudes) {
            if (!isCancelled()) {
                onAmplitudesReady(amplitudes);
            }
        }
    }
}
