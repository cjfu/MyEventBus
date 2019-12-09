package com.cjf.myeventbus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.cjf.eventbus.MyEventBus;
import com.cjf.eventbus.EventReceiver;
import com.cjf.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    Button btn_post;
    Button btn_jump;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_post = findViewById(R.id.btn_post);
        btn_jump = findViewById(R.id.btn_jump);
        MyEventBus.getInstance().register(this);
        btn_post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postMessage();
            }
        });

        btn_jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SecondActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyEventBus.getInstance().unRegister(this);
    }

    public void postMessage() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                MyEventBus.getInstance().post("111");
                User user = new User();
                user.setName("123");
                MyEventBus.getInstance().post(user);
            }
        }.start();
    }

    @EventReceiver()
    public void getMessage1(String str){
        Log.e("cjfu MainActivity",str + Thread.currentThread().getName());
    }

    @EventReceiver(ThreadMode.MAIN)
    public void getMessage2(User user){
        Log.e("cjfu MainActivity",user.getName() +  Thread.currentThread().getName());
    }
}
