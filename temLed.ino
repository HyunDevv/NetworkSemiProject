const int ledPin = A2;
const int tempPin = A0;

void setup() {
  Serial.begin(9600);
  //randomSeed(analogRead(0)); // 랜덤하게 숫자 발생
  pinMode(ledPin, OUTPUT);

}

void loop() {
  int data = analogRead(tempPin);
  float temp=((5.0*data)/1240.0) * 100;
  Serial.println(temp);
  //Serial.println("Ready");
  String cmd ="";
  if(Serial.available()>0){
    cmd = Serial.readString();
    //Serial.println("aduino  "+cmd);
      if(cmd == "s"){
        digitalWrite(ledPin, HIGH);
      }else if(cmd=="t"){
        digitalWrite(ledPin, LOW);
     }
  }
 delay(2000);
}
