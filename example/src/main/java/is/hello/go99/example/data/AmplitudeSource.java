package is.hello.go99.example.data;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface AmplitudeSource {
    @Nullable Bundle saveState();
    void restoreState(@NonNull Bundle state);

    void update();

    void addConsumer(@NonNull Consumer consumer);
    void removeConsumer(@NonNull Consumer consumer);

    interface Consumer {
        void onAmplitudesReady(@NonNull float[] amplitudes);
        void onAmplitudesUnavailable(@NonNull Throwable reason);
    }
}
