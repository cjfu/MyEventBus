package com.cjf.myeventbus;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.cjf.eventbus.EventBus;
import com.cjf.eventbus.EventReceiver;
import com.cjf.eventbus.ThreadMode;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getInstance().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unRegister(this);
    }

    public void postMessage(View view) {
        EventBus.getInstance().post("111");
        User user = new User();
        user.setName("123");
        EventBus.getInstance().post(user);
    }

    @EventReceiver()
    public void getMessage1(String str){
        Log.e("cjfu SecondActivity",str + Thread.currentThread().getName());
    }

    @EventReceiver(ThreadMode.MAIN)
    public void getMessage2(User user){
        Log.e("cjfu SecondActivity",user.getName() + Thread.currentThread().getName());
    }
}
