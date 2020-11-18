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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.df.DataFrame;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    TextView tx_list, tx_msg,tx_sen;
    EditText et_ip, et_msg;

    int port;
    String address;
    String id;
    Socket socket;

    Sender sender;

    NotificationManager manager;

    HttpAsync httpAsync;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tx_list = findViewById(R.id.tx_list);
        tx_msg = findViewById(R.id.tx_msg);
        tx_sen = findViewById(R.id.tx_sen);
        et_ip = findViewById(R.id.et_ip);
        et_msg = findViewById(R.id.et_msg);
        port = 5558;
        address = "15.165.195.250";
        id="MobileJH";

        new Thread(con).start();

        // FCM사용 (앱이 중단되어 있을 때 기본적으로 title,body값으로 푸시!!)
        FirebaseMessaging.getInstance().subscribeToTopic("car").
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

//        getSensor();

//        while(true){
//            getSensor();
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }



    } // end OnCreate


//    // 웹서버에서 센서값을 받아오자!
//    public void getSensor(){
//        String url = "http://192.168.0.19/tcpip/car.jsp";
//        //url += "?id="+id+"&pwd="+pwd;
//        //String result = HttpConnect.getString(url); <- 서브스레드 안에서 해야한다!!
//        httpAsync = new HttpAsync();
//        httpAsync.execute(url);
//    }

    class HttpAsync extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String url = strings[0];
            String result = HttpConnect.getString(url); //result는 JSON
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            tx_sen.setText(s);
        }

    }



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



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try{
            DataFrame df = new DataFrame(null,id,"q");
            sender.setDf(df);
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

        //getList();

        //sendMsg();
    }

//    private void getList() {
//        Msg msg = new Msg(null,"[JaeHyun]","1");
//        sender.setMsg(msg);
//        new Thread(sender).start();
//    }


    // ==============MSG버전==========================
//    public void clickBt(View v){
//        ArrayList<String> ips = new ArrayList<>();
//        String ip = et_ip.getText().toString();
//        String ms = et_msg.getText().toString();
//        Msg msg = null;
//        if(ip.equals("") || ip == null){
//            msg = new Msg(id,ms);
//        }else{
//            ips.add(ip);
//            msg = new Msg(ips,id,ms);
//        }
//        // send하고 찍는다
//        final Msg finalMsg = msg;
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                String tx = tx_msg.getText().toString();
//                tx_msg.setText("---"+finalMsg.getId()+finalMsg.getMsg()+"\n"+tx);
//            }
//        });
//
//        Log.d("[TAG]",ips.toString());
//        sender.setMsg(msg);
//        new Thread(sender).start();
//
//        et_msg.setText("");
//        //getList();
//    }

    public void clickBt(View v){
        ArrayList<String> ips = new ArrayList<>();
        String ip = et_ip.getText().toString();
        String ms = et_msg.getText().toString();
        DataFrame df = null;
        if(!ip.equals("") && ip != null){
            df = new DataFrame(ip,id,ms);
        }
        // send하고 찍는다
        final DataFrame finalDf = df;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String tx = tx_msg.getText().toString();
                tx_msg.setText("==="+finalDf.getSender()+" "+finalDf.getContents()+"\n"+tx);
            }
        });

        //Log.d("[TAG]",ips.toString());
        sender.setDf(df);
        new Thread(sender).start();

        et_msg.setText("");
        //getList();
    }


//    ================================MSG버전=================================
//    class Receiver extends Thread{
//        ObjectInputStream oi;
//        public Receiver(Socket socket) throws IOException {
//            oi = new ObjectInputStream(socket.getInputStream());
//        }
//        @Override
//        public void run() {
//            while(oi != null) {
//                Msg msg = null;
//                try {
//                    msg = (Msg) oi.readObject();
//                    // 접속되어 있는 IP주소 찍는다
//                    if(msg.getMaps() != null) {
//                        HashMap<String,Msg> hm = msg.getMaps();
//                        Set<String> keys = hm.keySet();
//                        for(final String k : keys) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    String tx = tx_list.getText().toString();
//                                    tx_list.setText(tx+k+"\n");
//                                }
//                            });
////                            System.out.println(k);
//                        }
//                        continue;
//                    }
//                    final Msg finalMsg = msg;
//                    Log.d("------------------",finalMsg.getMsg());
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            String tx = tx_msg.getText().toString();
//                            tx_msg.setText("==="+finalMsg.getId()+finalMsg.getMsg()+"\n"+tx);
//                        }
//                    });
//                    System.out.println(msg.getId()+msg.getMsg());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    break;
//                }
//
//            } // end while
//            try {
//                if(oi != null) {
//                    oi.close();
//                }
//                if(socket != null) {
//                    socket.close();
//                }
//            }catch(Exception e){
//
//            }
//            // 서버가 끊기면 connect를 한다!
//            try {
//                Thread.sleep(2000);
//                System.out.println("test2");
//                connect();
//                //sendMsg();
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//
//        }
//
//    }

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
                    System.out.println("[Client Receiver Thread] 수신 완료");
                    System.out.println(df.getSender()+": "+df.getContents());
                    final DataFrame finalDf = df;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String tx = tx_msg.getText().toString();
                            tx_msg.setText("==="+finalDf.getSender()+finalDf.getContents()+"\n"+tx);
                        }
                    });

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

//    =============================MSG버전=======================
//    class Sender implements Runnable{
//        Socket socket;
//        ObjectOutputStream oo;
//        Msg msg;
//
//        public Sender(Socket socket) throws IOException {
//            this.socket = socket;
//            oo = new ObjectOutputStream(socket.getOutputStream());
//        }
//
//        public void setMsg(Msg msg) {
//            this.msg = msg;
//        }
//
//        @Override
//        public void run() {
//            if(oo != null) {
//                try {
//                    oo.writeObject(msg);
//                } catch (IOException e) {
//                    //e.printStackTrace();
//                    try {
//                        if(socket != null) {
//                            socket.close();
//                        }
//                    }catch(Exception e1) {
//                        e1.printStackTrace();
//
//                    }
//                    // 서버가 끊기면 connect를 한다!
//                    try {
//                        Thread.sleep(2000);
//                        connect();
//                        //sendMsg();
//                        System.out.println("test1");
//                    } catch (Exception e1) {
//                        e1.printStackTrace();
//                    }
//
//                }
//            }
//        }
//
//    }


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



}