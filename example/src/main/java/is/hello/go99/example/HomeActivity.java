package is.hello.go99.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Just hosts an instance of {@link AmplitudesFragment}.
 */
public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }
}
