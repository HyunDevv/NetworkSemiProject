package com.example.tcpip;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.msg.Msg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    NotificationManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FCM사용 (앱이 중단되어 있을 때 기본적으로 title,body값으로 푸시!!)
        FirebaseMessaging.getInstance().subscribeToTopic("car"). //구독, 이걸로 원하는 기능 설정하기(파이널 때, db 활용)
                addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "FCM Complete...";
                        if (!task.isSuccessful()) {
                            msg = "FCM Fail";
                        }
                        Log.d("[TAG]", msg);

                    }
                });
        // 여기서 부터는 앱 실행상태에서 상태바 설정!!
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this); // 브로드캐스트를 받을 준비
        lbm.registerReceiver(receiver, new IntentFilter("notification")); // notification이라는 이름의 정보를 받겠다

    } // end OnCreate

    // 웹서버에서 센서값을 받아오자! // http 통신
//    public void getSensor(){
//        String url = "http://192.168.0.60/tcpip1/car.jsp";
//        //url += "?id="+id+"&pwd="+pwd;
//        //String result = HttpConnect.getString(url); <- 서브스레드 안에서 해야한다!!
//        httpAsync = new HttpAsync();
//        httpAsync.execute(url);
//    }
//
//    class HttpAsync extends AsyncTask<String,Void,String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            String url = strings[0];
//            String result = HttpConnect.getString(url); //result는 JSON
//            return result;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            tx_sen.setText(s);
//        }
//
//    } // 여기까지 http 통신이라 주석

    // MyFService.java의 intent 정보를 BroadcastReceiver를 통해 받는다
    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String title = intent.getStringExtra("title");
                String control = intent.getStringExtra("control");
                String data = intent.getStringExtra("data");
                Toast.makeText(MainActivity.this, title + " " + control + " " + data, Toast.LENGTH_SHORT).show();

                // 상단알람 사용
                manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = null;
                if (Build.VERSION.SDK_INT >= 26) {
                    if (manager.getNotificationChannel("ch2") == null) {
                        manager.createNotificationChannel(
                                new NotificationChannel("ch2", "chname", NotificationManager.IMPORTANCE_DEFAULT));
                    }
                    builder = new NotificationCompat.Builder(context, "ch2");
                } else {
                    builder = new NotificationCompat.Builder(context);
                }

                Intent intent1 = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 101, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

                builder.setAutoCancel(true);
                builder.setContentIntent(pendingIntent);
                //상단바 타이틀 설정
                builder.setContentTitle(title);
                //상단바 내용 설정
                builder.setContentText(control + " " + data);
                builder.setSmallIcon(R.drawable.a1);
                Notification noti = builder.build();
                manager.notify(1, noti);
            }
        }
    };
}