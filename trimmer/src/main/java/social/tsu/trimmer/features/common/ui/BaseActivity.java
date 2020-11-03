package social.tsu.trimmer.features.common.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("Registered")
public abstract class BaseActivity extends AppCompatActivity {

  protected abstract void initUI();

  protected void loadData() {
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      render();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void render() {
    initUI();
    loadData();
  }
}
