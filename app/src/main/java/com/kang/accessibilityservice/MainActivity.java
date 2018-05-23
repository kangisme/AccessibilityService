package com.kang.accessibilityservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.open_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ServiceUtils.isServiceEnabled(MainActivity.this)) {
                    //打开系统无障碍设置界面
                    Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(accessibleIntent);
                } else {
                    Toast.makeText(MainActivity.this, "服务已开启", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
