package org.gitzian.neurogranny.model;

/** Simple Java callback; deliberately avoids java.util.function (not present on Android API 23). */
public interface Callback<T> {
    void accept(T value);
}
