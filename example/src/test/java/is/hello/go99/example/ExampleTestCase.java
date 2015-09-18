package is.hello.go99.example;

import android.content.Context;
import android.content.res.Resources;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        sdk = 21)
public abstract class ExampleTestCase {
    protected Context getContext() {
        return RuntimeEnvironment.application.getApplicationContext();
    }

    protected Resources getResources() {
        return getContext().getResources();
    }
}
