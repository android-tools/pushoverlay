package ru.busylee.pushoverlay.testapp;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import ru.busylee.pushoverlay.PushOverlay;
import ru.busylee.pushoverlay.Pushable;

public class TestActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test_app);
    /**
     * Just attach PushOverlay after setContentView line was invoked
     */
    PushOverlay.attach(this);

    findViewById(R.id.tv_click_me).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        /**
         * Push any overlay messages you want here
         */
        PushOverlay.push(new Pushable("Hi from push overlay").color(Color.WHITE));
      }
    });
  }
}
