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

import java.io.Serializable;

import is.hello.go99.example.AmplitudesFragment;

/**
 * Plain-old-data class that represents a single amplitude value
 * in a series displayed within {@link AmplitudesFragment}.
 */
public final class Amplitude implements Serializable {
    /**
     * The amplitude value. Range is {0.0, 1.0} inclusive.
     */
    public final float value;

    /**
     * The height of the amplitude value. Range is {0.0, 1.0} inclusive.
     */
    public final float height;

    /**
     * Constructs an amplitude value with the given value and height.
     *
     * @param value     Range is {0.0, 1.0} inclusive.
     * @param height    Range is {0.0, 1.0} inclusive.
     */
    public Amplitude(float value, float height) {
        this.value = value;
        this.height = height;
    }

    @Override
    public String toString() {
        return "Amplitude{" +
                "value=" + value +
                ", height=" + height +
                '}';
    }
}
