package com.example.tcpip;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class FcmActivity extends AppCompatActivity {
    Switch sw_power, sw_door; // 스위치를 통한 on/off
    TextView tx_temp, tx_setTemp; // 현재 온도, 희망 온도
    EditText etx_setTemp; // 온도 설정
    Button bt_temp; // 설정 후 확인 버튼

    int port;
    String address;
    String id;
    Socket socket;
    Sender sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcm);

        sw_power = findViewById(R.id.sw_power);
        sw_door = findViewById(R.id.sw_door);
        tx_temp = findViewById(R.id.tx_temp);
        tx_setTemp = findViewById(R.id.tx_setTemp);
        etx_setTemp = findViewById(R.id.etx_setTemp);
        bt_temp = findViewById(R.id.bt_temp);
//        sw_power.setOnCheckedChangeListener(new sw_powerListener()); // sw_power 변화 감지
//        sw_door.setOnCheckedChangeListener(new sw_doorListener()); // sw_door 변화 감지

        port = 5555;
        address = "192.168.0.60"; // 192.168.0.60 server로 간다
        id="MobileCustomer"; // 내 아이디

        new Thread(con).start();

    } // end OnCreate

    @Override
    public void onBackPressed() { // 뒤로가기 눌렀을 때 q를 보내 tcp/ip 통신 종료
        super.onBackPressed();
        try{
            Msg msg = new Msg(null,id,"q");
            sender.setMsg(msg);
            new Thread(sender).start();
            if(socket != null) {
                socket.close();
            }
            finish();
            onDestroy();

        }catch(Exception e){

        }
    }

    Runnable con = new Runnable() {
        @Override
        public void run() {
            try {
                connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };



    private void connect() throws IOException {
        // 소켓이 만들어지는 구간
        try {
            socket = new Socket(address,port);
        } catch (Exception e) {
            while(true) {
                try {
                    Thread.sleep(2000);
                    socket = new Socket(address,port);
                    break;
                } catch (Exception e1) {
                    System.out.println("Retry...");
                }
            }
        }

        System.out.println("Connected Server:"+address);

        sender = new Sender(socket);
        new Receiver(socket).start();

        //sendMsg();
    }

    class Receiver extends Thread{
        ObjectInputStream oi;
        public Receiver(Socket socket) throws IOException {
            oi = new ObjectInputStream(socket.getInputStream());
        }
        @Override
        public void run() {
            while(oi != null) {
                Msg msg = null;
                try {
                    msg = (Msg) oi.readObject();
                    // 접속되어 있는 IP주소 찍는다
                    final Msg finalMsg = msg;
                    Log.d("------------------",finalMsg.getMsg());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { // 이 부분에 id 받아서 어디로 msg 보내줄지 정하기
                            if(finalMsg.getId().equals("temp")) { // id가 temp일 때만 현재 온도 부분이 바뀌도록 한다
                                tx_temp.setText(finalMsg.getMsg());
                            }
                            // 이 부분이 마지막 메시지를 현재 온도에 띄우는 거고, 아직 if문으로 거르지 않아서
                            // 온도 설정을 하면 현재 온도도 같이 바뀜
                        }
                    });
                    System.out.println(msg.getId()+msg.getMsg());
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

            } // end while
            try {
                if(oi != null) {
                    oi.close();
                }
                if(socket != null) {
                    socket.close();
                }
            }catch(Exception e){

            }
            // 서버가 끊기면 connect를 한다!
            try {
                Thread.sleep(2000);
                System.out.println("test2");
                connect();
                //sendMsg();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }

    }


    class Sender implements Runnable{
        Socket socket;
        ObjectOutputStream oo;
        Msg msg;

        public Sender(Socket socket) throws IOException {
            this.socket = socket;
            oo = new ObjectOutputStream(socket.getOutputStream());
        }

        public void setMsg(Msg msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            if(oo != null) {
                try {
                    oo.writeObject(msg);
                } catch (IOException e) {
                    //e.printStackTrace();
                    try {
                        if(socket != null) {
                            socket.close();
                        }
                    }catch(Exception e1) {
                        e1.printStackTrace();

                    }
                    // 서버가 끊기면 connect를 한다!
                    try {
                        Thread.sleep(2000);
                        connect();
                        //sendMsg();
                        System.out.println("test1");
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                }
            }
        }

    }




    // switch on/off에 따라 앞에 text 바꿔주기 추가할 것
    
//        if(powerOn(true)){
//            sw_power.setChecked(true);
//        } else {
//            sw_power.setChecked(false);
//        } // switch on/off할 때 설정해주기
//
//   class sw_powerListener implements CompoundButton.OnCheckedChangeListener{
//       @Override
//       public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//            if(isChecked) // 체크됐을 때
//                powerOn(true);
//            else
//                powerOn(false);
//       }
//   }
//
//
//        if(doorClosed(true)){
//            sw_door.setChecked(true);
//        }else {
//            sw_door.setChecked(false);
//        } // switch on/off할 때 설정해주기
//        class sw_doorListener implements CompoundButton.OnCheckedChangeListener{
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked) // 체크됐을 때
//                    doorClosed(true);
//                else
//                    powerClosed(false);
//            }
//        }

    public void bt_temp(View v){
//        tx_setTemp.setText(etx_setTemp.getText());  // 온도 설정하면 희망 온도를 바꿈
//        String ip = "192.168.0.60";
//        String ms = etx_setTemp.getText().toString();

//        Msg msg = new Msg(id, ms);
//        sender.setMsg(msg);
        try {
            FcmSend("temp",100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(sender).start();
        // 18~40 까지만 설정 가능하게 하는 기능 추가하기 !
    }

    public void FcmSend(String id, int msg) throws IOException, JSONException { // FCM을 보낸다
        URL url = null;
        try {
            url = new URL("https://fcm.googleapis.com/fcm/send");
        } catch (MalformedURLException e) {
            System.out.println("Error while creating Firebase URL | MalformedURLException");
            e.printStackTrace();
        }
//            System.out.println("Error while creating Firebase URL | MalformedURLException");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            System.out.println("Error while createing connection with Firebase URL | IOException");
            e.printStackTrace();
        }
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        // set my firebase server key
        conn.setRequestProperty("Authorization", "key="
                + "AAAANIrfv7c:APA91bHjA_pjrOjgfO-9ZiLoj1vu24Dza6GbAioGIGl0MkW0sc_Mzmy9Kit8C6xkRB6zLtI6GRZg5diuHzUgabgiGTU6Z2ejurHIWMdPXrN8vbax_Cr2--bsfD23R7JAEMa0mMIfdwPs" +
                ""); // 이 부분 key를 필요할 때 바꿀 것

        // create notification message into JSON format
            JSONObject message = new JSONObject();
            message.put("to", "/topics/car");
            message.put("priority", "high");

            JSONObject notification = new JSONObject();
            notification.put("title", "title1");
            notification.put("body", "body1");
            message.put("notification", notification);

            JSONObject data = new JSONObject();
            data.put("control", id);
            data.put("data", msg);
            message.put("data", data);

        try {
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            System.out.println("FCM 전송:"+message.toString());
            out.write(message.toString());
            out.flush();
            conn.getInputStream();
            System.out.println("OK...............");

        } catch (IOException e) {
            System.out.println("Error while writing outputstream to firebase sending to ManageApp | IOException");
            e.printStackTrace();
        }
    }


}