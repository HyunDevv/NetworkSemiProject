<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script>

$(document).ready(function(){
	$('#iot').click(function(){
		$.ajax({
			url:'iot.mc',
			success:function(data){
				alert('Send IoT Complete...');
			}
		});
	});
	$('#phone').click(function(){
		$.ajax({
			url:'phone.mc',
			success:function(data){
				alert('Send Phone Complete...');
			}
		});
	});
});
</script>
</head>
<body>
<h1>Main page</h1>
<h2><a id="iot" href="#">Send IoT(TCP/IP)</a></h2>
<h2><a id="phone" href="#">Send Phone(FCM)</a></h2>
<h3>Send Control Message to IoT</h3>
<form action="sendmtoiot.mc" method="post">
	<input type="text" name="iot_id" id="iot_id">
	<input type="text" name="iot_contents" id="iot_contents">
	<button onclick="alert('메시지를 보냈습니다.')" type="submit">보내기</button>
</form>
</body>
</html>