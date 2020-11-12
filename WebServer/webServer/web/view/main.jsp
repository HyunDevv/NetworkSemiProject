<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>

<style>

div{ 
	width: 70%;
	height: 500px;
	overflow: auto;
	background: green;
	border: 2px solid black;
}
	
</style>

<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script>

Client client  = new Client();

$(document).ready(function(){
	$('#iot').click(function(){
		$.ajax({
			url:'iot.mc',
			success:function(data){
				alert('Send IoT Complete...');
			}
		});
	});
	
});
</script>
</head>
<body>
<h1>Main page</h1>
<h2><a id="iot" href="#">Send IoT(TCP/IP)</a></h2>
<h2>Send Phone(FCM)</h2>
<form action="fcmPhone.mc" method="post">
	<input type="text" name="fcmContents" id="fcmContents" placeholder="FCM보낼 내용을 입력하세요">
	<button onclick="alert('FCM을 보냈습니다.')" type="submit">보내기</button>
</form>
<h3>Send Control Message to IoT</h3>
<form action="sendmtoiot.mc" method="post">
	<input type="text" name="iot_id" id="iot_id" placeholder="타겟IP를 입력하세요">
	<input type="text" name="iot_contents" id="iot_contents"placeholder="보낼내용을 입력하세요">
	<button onclick="alert('메시지를 보냈습니다.')" type="submit">보내기</button>
</form>
</body>
</html>