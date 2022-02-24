package com.dd.eventbusdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        textView =findViewById(R.id.tv_text);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),TestActivity.class);
                startActivity(intent);
            }
        });

    }


    @Subscribe(threadMode = ThreadMode.MAIN,priority = 50,sticky = true)
    public void message1(String msg){

        textView.setText(msg);


    }


    @Subscribe(threadMode = ThreadMode.MAIN,priority = 100,sticky = true)
    public void message2(String msg){

        textView.setText(msg);

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}