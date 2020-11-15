package com.tcpip;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.chat.Client;

@Controller
public class MainController {
	
	Client client;
	public MainController() {
		client = new Client("15.165.195.250",5555,"[WEB]");
		try {
			client.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping("/main.mc")
	public ModelAndView main() {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		return mv;
	}
	
	@RequestMapping("/iot.mc")
	public void iot() {
		System.out.println("IoT Send Start...");
		client.sendTarget("/15.165.195.250", "100");
	}
	
//	@RequestMapping("/phone.mc")
//	public void phone() {
//		System.out.println("phone Send Start...");
//		
//		URL url = null;
//		try {
//			url = new URL("https://fcm.googleapis.com/fcm/send");
//		} catch (MalformedURLException e) {
//			System.out.println("Error while creating Firebase URL | MalformedURLException");
//			e.printStackTrace();
//		}
//		HttpURLConnection conn = null;
//		try {
//			conn = (HttpURLConnection) url.openConnection();
//		} catch (IOException e) {
//			System.out.println("Error while createing connection with Firebase URL | IOException");
//			e.printStackTrace();
//		}
//		conn.setUseCaches(false);
//		conn.setDoInput(true);
//		conn.setDoOutput(true);
//		conn.setRequestProperty("Content-Type", "application/json");
//
//		// set my firebase server key
//		conn.setRequestProperty("Authorization", "key="
//				+ "AAAAK89FyMY:APA91bGxNwkQC6S_QQAKbn3COepWgndhyyjynT8ZvIEarTaGpEfMA1SPFo-ReN8b9uO21R1OfSOpNhfYbQaeohKP_sKzsgVTxu7K5tmzcjEfHzlgXRFrB1r0uqhfxLp4p836lbKw_iaN");
//
//		// create notification message into JSON format
//		JSONObject message = new JSONObject();
//		message.put("to", "/topics/car");
//		message.put("priority", "high");
//		
//		JSONObject notification = new JSONObject();
//		notification.put("title", "title1");
//		notification.put("body", "body1");
//		message.put("notification", notification);
//		
//		JSONObject data = new JSONObject();
//		data.put("control", "control1");
//		data.put("data", 100);
//		message.put("data", data);
//
//
//		try {
//			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
//			System.out.println("FCM 占쎌읈占쎈꽊:"+message.toString());
//			out.write(message.toString());
//			out.flush();
//			conn.getInputStream();
//			System.out.println("OK...............");
//
//		} catch (IOException e) {
//			System.out.println("Error while writing outputstream to firebase sending to ManageApp | IOException");
//			e.printStackTrace();
//		}	
//		
//		System.out.println("phone Send End...");
//	}
	
	@RequestMapping("/sendmtoiot.mc") 
	public ModelAndView sendMtoIoT(ModelAndView mv, String iot_id, String iot_contents) {
		System.out.println("Send Message to IoT Start...");
		System.out.println(iot_id+"로 "+iot_contents+"을 전송");
		client.sendTarget(iot_id, iot_contents);
		mv.setViewName("main");
		return mv;
	}
	
//	@RequestMapping("/car.mc")
//	   public void car(HttpServletRequest request) {
//	      String ip = request.getParameter("ip");
//	      String sensor = request.getParameter("sensor");
//	      String msg = ip+" "+sensor;
//	      
//	      System.out.println(msg+"test!!!!!");
//	      
//	      // 筌뤿굝議� �굜遺얜굡 占쎌넇占쎌뵥
//	      // 占쎄퉱嚥≪뮇�뒲 占쎈땾占쎈뻿 筌롫뗄�뻻筌욑옙占쎌뵬 野껋럩�뒭占쎈퓠筌랃옙 FCM 占쎌읈占쎈꽊
//	      String code = request.getParameter("code");
//	      //if(code.equals("U")) {
//			// FCM setting
//			URL url = null;
//			try {
//				url = new URL("https://fcm.googleapis.com/fcm/send");
//			} catch (MalformedURLException e) {
//				System.out.println("Error while creating Firebase URL | MalformedURLException");
//				e.printStackTrace();
//			}
//			HttpURLConnection conn = null;
//			try {
//				conn = (HttpURLConnection) url.openConnection();
//			} catch (IOException e) {
//				System.out.println("Error while createing connection with Firebase URL | IOException");
//				e.printStackTrace();
//			}
//			conn.setUseCaches(false);
//			conn.setDoInput(true);
//			conn.setDoOutput(true);
//			conn.setRequestProperty("Content-Type", "application/json");
//
//			// set my firebase server key
//			conn.setRequestProperty("Authorization", "key="
//					+ "AAAAK89FyMY:APA91bGxNwkQC6S_QQAKbn3COepWgndhyyjynT8ZvIEarTaGpEfMA1SPFo-ReN8b9uO21R1OfSOpNhfYbQaeohKP_sKzsgVTxu7K5tmzcjEfHzlgXRFrB1r0uqhfxLp4p836lbKw_iaN");
//			
//
//			// create notification message into JSON format
//			JSONObject message = new JSONObject();
//			message.put("to", "/topics/car");
//			message.put("priority", "high");
//			
//			JSONObject notification = new JSONObject();
//			notification.put("title", ip);
//			notification.put("body", sensor);
//			message.put("notification", notification);
//			
//			JSONObject data = new JSONObject();
//			data.put("control", "control1");
//			data.put("data", 100);
//			message.put("data", data);
//
//			try {
//				OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
//				out.write(message.toString());
//				out.flush();
//				conn.getInputStream();
//				System.out.println("OK...............");
//
//			} catch (IOException e) {
//				System.out.println("Error while writing outputstream to firebase sending to ManageApp | IOException");
//				e.printStackTrace();
//			}
//	      //}
//	   }
	
	
	@RequestMapping("/fcmPhone.mc") // 梨꾪똿�쓣 諛쏆븯�쓣 �븣
	public ModelAndView inputChat(ModelAndView mv, String fcmContents) throws IOException {

		System.out.println(fcmContents+"라는 내용으로 FCM 전송!!!!");

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
		message.put("to", "/topics/car");
		message.put("priority", "high");
		
		JSONObject notification = new JSONObject();
		notification.put("title", "공지알림");
		notification.put("body", "body1");
		message.put("notification", notification);
		
		JSONObject data = new JSONObject();
		data.put("control",fcmContents );
		data.put("data", "");
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
		
		
		mv.setViewName("main");
		return mv;
	}
	
}
