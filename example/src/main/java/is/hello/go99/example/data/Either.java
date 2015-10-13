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
package is.hello.go99.example.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * <code>type Either 'l 'r = Left 'l | Right 'r</code>
 * <p/>
 * A pseudo-discriminated union which represents one of two possible values.
 * @param <Left>    The left value type. A successful value by convention.
 * @param <Right>   The right value type. An error value by convention.
 */
public final class Either<Left, Right> {
    private final boolean left;
    private final @Nullable Object value;


    //region Creation

    public static <Left, Right> Either<Left, Right> left(@NonNull Left value) {
        return new Either<>(true, value);
    }

    public static <Left, Right> Either<Left, Right> right(@NonNull Right value) {
        return new Either<>(false, value);
    }

    private Either(boolean isLeft, @Nullable Object value) {
        this.left = isLeft;
        this.value = value;
    }

    //endregion


    //region Introspection

    public boolean isLeft() {
        return left;
    }

    @SuppressWarnings("unchecked")
    public Left getLeft() {
        if (!isLeft()) {
            throw new NullPointerException();
        }

        return (Left) value;
    }

    @SuppressWarnings("unchecked")
    public Right getRight() {
        if (isLeft()) {
            throw new NullPointerException();
        }

        return (Right) value;
    }

    public void match(@NonNull Matcher<Left> onLeft,
                      @NonNull Matcher<Right> onRight) {
        if (isLeft()) {
            onLeft.match(getLeft());
        } else {
            onRight.match(getRight());
        }
    }

    public <R> R map(@NonNull Mapper<Left, R> onLeft,
                     @NonNull Mapper<Right, R> onRight) {
        if (isLeft()) {
            return onLeft.map(getLeft());
        } else {
            return onRight.map(getRight());
        }
    }

    //endregion


    //region Identity

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Either<?, ?> either = (Either<?, ?>) o;
        return (left == either.left &&
                !(value != null ? !value.equals(either.value) : either.value != null));
    }

    @Override
    public int hashCode() {
        int result = (left ? 1 : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (isLeft()) {
            return "{Either left=" + value + "}";
        } else {
            return "{Either right=" + value + "}";
        }
    }

    //endregion


    public interface Matcher<T> {
        void match(T value);
    }

    public interface Mapper<T, U> {
        U map(T value);
    }
}
