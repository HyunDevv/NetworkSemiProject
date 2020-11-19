package com.example.tcpip;

import androidx.annotation.NonNull;
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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.df.DataFrame;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.msg.Msg;

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

public class FcmActivity extends AppCompatActivity {
    Switch sw_power, sw_door; // 스위치를 통한 on/off
    TextView tx_temp, tx_setTemp, tx_log; // 현재 온도, 희망 온도
    EditText etx_setTemp; // 온도 설정
    Button bt_temp; // 설정 후 확인 버튼

    int port;
    String address;
    String id;
    Socket socket;
    Sender sender;

    NotificationManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcm);
        sw_power = findViewById(R.id.sw_power);
        sw_power.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread() {
                        public void run() {
                            send("power", "s"); // send 함수로 input을 보낸다
                        }
                    }.start();
                    sw_power.setText("시동 ON");
                } else {
                    new Thread() {
                        public void run() {
                            send("power", "t"); // send 함수로 input을 보낸다
                        }
                    }.start();
                    sw_power.setText("시동 OFF");
                }
            }
        });
        sw_door = findViewById(R.id.sw_door);
        sw_door.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread() {
                        public void run() {
                            send("door", "0"); // send 함수로 input을 보낸다
                        }
                    }.start();
                    sw_door.setText("문 잠김");
                } else {
                    new Thread() {
                        public void run() {
                            send("door", "1"); // send 함수로 input을 보낸다
                        }
                    }.start();
                    sw_door.setText("문 열림");
                }
            }
        });
        tx_setTemp = findViewById(R.id.tx_setTemp);
        tx_temp = findViewById(R.id.tx_temp);
        tx_log = findViewById(R.id.tx_log);
        etx_setTemp = findViewById(R.id.etx_setTemp);
        bt_temp = findViewById(R.id.bt_temp);
        bt_temp.setOnClickListener(new View.OnClickListener() { // 확인 버튼 클릭 시
            @Override
            public void onClick(View v) {
                final String input = etx_setTemp.getText().toString(); // input = 내가 원하는 온도
                if (input.equals("")) {
                    Toast.makeText(FcmActivity.this,"온도를 입력해주세요.",Toast.LENGTH_SHORT).show();
                } else {
                    new Thread() {
                        public void run() {
                            send("temp", input); // send 함수로 input을 보낸다
                        }
                    }.start();
                }
            }
        });

        port = 5558;
        address = "192.168.0.60"; // 192.168.0.60 server로 간다
        id = "MobileKH"; // 내 아이디

        new Thread(con).start(); // 풀면 tcpip 사용


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

    class HttpAsync extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String url = strings[0];
            String result = HttpConnect.getString(url); //result는 JSON
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            tx_log.append(s);
        }

    }

    @Override
    public void onBackPressed() { // 뒤로가기 눌렀을 때 q를 보내 tcp/ip 통신 종료
        super.onBackPressed();
        try {
            DataFrame df = new DataFrame(null, id, "q");
            sender.setDf(df);
            new Thread(sender).start();
            if (socket != null) {
                socket.close();
            }
            finish();
            onDestroy();

        } catch (Exception e) {

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
            socket = new Socket(address, port);
        } catch (Exception e) {
            while (true) {
                try {
                    Thread.sleep(2000);
                    socket = new Socket(address, port);
                    break;
                } catch (Exception e1) {
                    System.out.println("Retry...");
                }
            }
        }

        System.out.println("Connected Server:" + address);

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
            // 수신 inputStream이 비어 있지 않은 경우 실행!
            while(oi != null) {
                DataFrame df = null;
                // 수신 시도
                try {
                    System.out.println("[Client Receiver Thread] 수신 대기");
                    df = (DataFrame)oi.readObject();
                    System.out.println("[Client Receiver Thread] 수신 완료"); // 11/19에 이 부분에 setText 추가하기
                    System.out.println(df.getSender()+": "+df.getContents());
                } catch (Exception e) {
                    System.out.println("[Client Receiver Thread] 수신 실패");
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
        ObjectOutputStream outstream;
        DataFrame df;

        public Sender(Socket socket) throws IOException {
            this.socket = socket;
            outstream = new ObjectOutputStream(socket.getOutputStream());
        }

        public void setDf(DataFrame df) {
            this.df = df;
        }

        @Override
        public void run() {
            //전송 outputStream이 비어 있지 않은 경우 실행!
            if(outstream != null) {
                // 전송 시도
                try {
                    System.out.println("[Client Sender Thread] 데이터 전송 시도: "+df.getIp()+"으로 "+df.getContents()+" 전송");
                    outstream.writeObject(df);
                    Log.d("[test]",df.toString());
                    outstream.flush();
                    System.out.println("[Client Sender Thread] 데이터 전송 시도: "+df.getIp()+"으로 "+df.getContents()+" 전송 완료");
                } catch (IOException e) {
                    System.out.println("[Client Sender Thread] 전송 실패");
                    // 전송 실패시 소켓이 열려 있다면 소켓 닫아버리고 다시 서버와 연결을 시도
                    try{
                        if(socket != null) {
                            System.out.println("[Client Sender Thread] 전송 실패, 소켓 닫음");
                            socket.close();
                        }
                        // 소켓을 닫을 수 없음
                    }catch(Exception e1) {
                        e1.printStackTrace();
                    }
                    // 다시 서버와 연결 시도
                    try {
                        Thread.sleep(2000);
                        connect();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    // FCM 받아오는 곳
    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String title = intent.getStringExtra("title");
                String control = intent.getStringExtra("control");
                String data = intent.getStringExtra("data");

                if (control.equals("temp")) { // control이 temp면, data(온도값)을 set해라
                    if (Integer.parseInt(data) > 30) {
                        Toast.makeText(FcmActivity.this,
                                "30도 이하의 온도로 설정해주세요.", Toast.LENGTH_LONG).show();
                    } else if (Integer.parseInt(data) < 18) {
                        Toast.makeText(FcmActivity.this,
                                "18도 이상의 온도로 설정해 주세요.", Toast.LENGTH_LONG).show();
                    } else if (data.equals(tx_setTemp.getText())) {
                        Toast.makeText(FcmActivity.this,
                                "바꿀 온도를 입력해주세요.", Toast.LENGTH_LONG).show();

                    } else {
                        tx_log.append("희망 온도가 " + tx_setTemp.getText() + "℃에서 " + data + "℃로 변경되었습니다." + "\n");
                        tx_setTemp.setText(data);
                        Toast.makeText(FcmActivity.this,
                                "온도가 변경되었습니다.", Toast.LENGTH_LONG).show();
                    }
                // 반대 핸드폰에서 희망 온도가 바뀌지 않는 경우에도 FCM이 가는걸 막으려면 if문을 밖으로 빼준다.
                } else if (control.equals("door")) { // 문 제어
                    if (data.equals("0")) {
                        tx_log.append("문이 잠겼습니다." + "\n");
                        sw_door.setChecked(true);
                    } else if (data.equals("1")) {
                        tx_log.append("문이 열렸습니다." + "\n");
                        sw_door.setChecked(false);
                    }

                } else if (control.equals("power")) { // 시동 제어
                    if (data.equals("s")) {
                        tx_log.append("시동이 켜졌습니다." + "\n");
                        sw_power.setChecked(true);
                    } else if (data.equals("t")) {
                        tx_log.append("시동이 꺼졌습니다." + "\n");
                        sw_power.setChecked(false);
                    }
                } // 추가로 제어할 것이 있으면 이곳에 else if 추가

                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); // 진동 없애려면 삭제
                if (Build.VERSION.SDK_INT >= 26) { //버전 체크를 해줘야 작동하도록 한다
                    vibrator.vibrate(VibrationEffect.createOneShot(1000, 10));
                } else {
                    vibrator.vibrate(1000);
                }

                manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = null;
                if (Build.VERSION.SDK_INT >= 26) {
                    if (manager.getNotificationChannel("ch1") == null) {
                        manager.createNotificationChannel(
                                new NotificationChannel("ch1", "chname", NotificationManager.IMPORTANCE_DEFAULT));
                    }
                    builder = new NotificationCompat.Builder(context, "ch1");
                } else {
                    builder = new NotificationCompat.Builder(context);
                }

                Intent intent1 = new Intent(context, FcmActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 101, intent1, PendingIntent.FLAG_UPDATE_CURRENT
                );
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                builder.setAutoCancel(true);
                builder.setContentIntent(pendingIntent);

                builder.setContentTitle(title);
                if (data.equals("0")) {
                    builder.setContentText(control + " 이(가) LOCK 상태로 변경되었습니다.");
                } else if (data.equals("1")) {
                    builder.setContentText(control + " 이(가) UNLOCK 상태로 변경되었습니다.");
                } else if (data.equals("s")){
                    builder.setContentText(control + " 이(가) ON 상태로 변경되었습니다.");
                } else if (data.equals("t")){
                    builder.setContentText(control + " 이(가) OFF 상태로 변경되었습니다.");
                } else {
                    builder.setContentText(control + " 이(가)" + data + " ℃로 변경되었습니다.");
                }
                builder.setSmallIcon(R.drawable.a1);
                Notification noti = builder.build();
                manager.notify(1, noti); // 상단 알림을 없애려면 이곳 주석 처리
            }
        }
    };


    // 데이터를 Push Message에 넣어서 보내는 send 함수
    public void send(String control, String input) { // String control, String input 으로 변경하기 !
        System.out.println("phone Send Start...");
        URL url = null;
        try {
            url = new URL("https://fcm.googleapis.com/fcm/send");
        } catch (MalformedURLException e) {
            System.out.println("Error while creating Firebase URL | MalformedURLException");
            e.printStackTrace();
        }
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
                + "AAAAK89FyMY:APA91bGxNwkQC6S_QQAKbn3COepWgndhyyjynT8ZvIEarTaGpEfMA1SPFo-ReN8b9uO21R1OfSOpNhfYbQaeohKP_sKzsgVTxu7K5tmzcjEfHzlgXRFrB1r0uqhfxLp4p836lbKw_iaN");

        // create notification message into JSON format
        JSONObject message = new JSONObject();
        try {
            message.put("to", "/topics/car");
            message.put("priority", "high");

            JSONObject notification = new JSONObject();
            notification.put("title", "HyunDai");
            notification.put("body", "자동차 상태 변경");
            message.put("notification", notification);

            JSONObject data = new JSONObject();
            data.put("control", control); // 이 부분 변경하며 temp, door, power 등 조절
            data.put("data", input);
            message.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            System.out.println("FCM 전송:" + message.toString());
            out.write(message.toString());
            out.flush();
            conn.getInputStream();
            System.out.println("OK...............");

        } catch (IOException e) {
            System.out.println("Error while writing outputstream to firebase sending to ManageApp | IOException");
            e.printStackTrace();
        }

        System.out.println("phone Send End...");
    }
}