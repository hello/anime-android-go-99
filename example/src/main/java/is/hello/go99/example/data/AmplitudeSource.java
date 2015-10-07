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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import is.hello.go99.example.AmplitudesFragment;

/**
 * Provides a series of amplitudes for display in {@link AmplitudesFragment}. The current
 * default implementation of this interface is {@link RandomAmplitudeSource}.
 */
public interface AmplitudeSource {
    /**
     * Saves the current state of the amplitude source into a bundle, if applicable.
     *
     * @return  The state of the amplitude source, represented as a bundle.
     */
    @Nullable Bundle saveState();

    /**
     * Restores the current state of the amplitude source from a bundle.
     * <p>
     * This method is only called if {@link #saveState()} returns a non-null value.
     *
     * @param state The state to restore.
     */
    void restoreState(@NonNull Bundle state);


    /**
     * Causes the amplitude source to update its contents
     * from whatever its backing data source is.
     */
    void update();


    /**
     * Adds an amplitude data consumer to the source.
     * <p>
     * Implementations do not have to guard against duplicate consumers.
     * <p>
     * If data is currently cached in the amplitude source, the
     * consumer should be immediately provided with this data.
     *
     * @param consumer  The consumer to add.
     */
    void addConsumer(@NonNull Consumer consumer);

    /**
     * Removes an amplitude data consumer from the source.
     * <p>
     * Implementations must support redundant calls to this method.
     *
     * @param consumer  The consumer to remove.
     */
    void removeConsumer(@NonNull Consumer consumer);


    /**
     * An object interested in consuming amplitude data.
     */
    interface Consumer {
        /**
         * Notifies the consumer that a series of amplitudes is now available.
         *
         * @param amplitudes    The amplitudes to consume.
         */
        void onAmplitudesReady(@NonNull List<Amplitude> amplitudes);

        /**
         * Notifies the consumer that the source cannot provide amplitudes.
         *
         * @param reason    The underlying reason for the failure to provide amplitudes.
         */
        void onAmplitudesUnavailable(@NonNull Throwable reason);
    }
}
