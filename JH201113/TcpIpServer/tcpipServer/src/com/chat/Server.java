package com.chat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.msg.Msg;

public class Server {

   int port;

   HashMap<String, ObjectOutputStream> maps;

   ServerSocket serverSocket;

//   Receiver receiver;
   

   public Server() {
   }

   public Server(int port) {
      this.port = port;
      maps = new HashMap<>();
   }

   public void startServer() throws Exception {
      serverSocket = new ServerSocket(port);
      System.out.println("Start Server...");

      Runnable r = new Runnable() { // 받으면서 다른 것도 할 수 있도록 Thread 사용

         @Override
         public void run() {
            while (true) {
               try {
                  Socket socket = null;
                  System.out.println("Ready Server ...");
                  socket = serverSocket.accept();
                  System.out.println("socket Join:"+socket.getInetAddress());
                  makeOut(socket);
                  new Receiver(socket).start();
               } catch (Exception e) {
            	   //Android Studio Error(Invalid Stream header)
                  //e.printStackTrace();
               }
            } // end while
         }
      };
      new Thread(r).start();

   }

   public void makeOut(Socket socket) throws IOException {
      ObjectOutputStream oo;
      oo = new ObjectOutputStream(socket.getOutputStream());
      maps.put(socket.getInetAddress().toString(), oo);
      System.out.println("접속자 수: " + maps.size());
   }

   class Receiver extends Thread {
      Socket socket;
      ObjectInputStream oi;

      public Receiver(Socket socket) throws IOException {
         this.socket = socket;
         oi = new ObjectInputStream(this.socket.getInputStream());
      }

      @Override
      public void run() {
         while (oi != null) {
            Msg msg = null;
            try {
               msg = (Msg) oi.readObject();
               System.out.println(msg.getId()+" "+msg.getMsg());
               
               if (msg.getMsg().equals("q")) {
                  throw new Exception();
               } else if(msg.getMsg().equals("1")) {
                  String ip = socket.getInetAddress().toString();
                  ArrayList<String> ips = new ArrayList<>();
                  ips.add(ip); // 1을 쓴 본인에게 ips를 보낸다
                  //msg.setIps(ips); // ips를 msg에 넣는다
                  Set<String> keys = maps.keySet(); //사용자들의 정보를 가져온다
                  HashMap<String, Msg> hm = new HashMap<>();
                  for(String k:keys) {
                     hm.put(k, null);
                  } 
                  //1을 보낸 client , 서버의 접속자 ip들
                  msg.setMaps(hm);
               }
               sendMsg(msg);
               System.out.println("send Msg!!!!!!");
            } catch (Exception e) {
               maps.remove(socket.getInetAddress().toString());
               System.out.println(socket.getInetAddress() + "..Exited");
               System.out.println("접속자수: " + maps.size());
               break;
            }   
         } // end while

         try {
            if(oi != null) {
               oi.close();
            }
            if (socket != null) {
               socket.close();
            }
         } catch (Exception e) {

         }

      }
   }

   public void sendMsg(Msg msg) {
      Sender sender = new Sender();
      sender.setMsg(msg);
      sender.start();
   }
   
   class Sender extends Thread {
      Msg msg;
      public void setMsg(Msg msg) {
         this.msg = msg;
      }
      @Override
      public void run() {
         Collection<ObjectOutputStream> cols =
               maps.values();
         Iterator<ObjectOutputStream> it = 
               cols.iterator();
         while(it.hasNext()) {
            try {
            if(msg.getIps() != null ) {
               for(String ip:msg.getIps()) {
            	  System.out.println("sendip:"+ip+"    sendMsg:"+msg.toString());
                  maps.get(ip).writeObject(msg);              
               }
               break; // break로 while을 끝내서 it.next().writeObject(msg); 가 작동되지 않도록 한다
               // broadcast하지 말라고 break;를 거는 것
            }
               it.next().writeObject(msg);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
      
   }

   public static void main(String[] args){
      Server server = new Server(5555);
      try {
         server.startServer();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}