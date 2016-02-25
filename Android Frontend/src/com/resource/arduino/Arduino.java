package com.resource.arduino;

import android.annotation.SuppressLint;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.json.JSONException;
import org.json.JSONObject;

import util.*;


@SuppressLint({ "ParserError", "ParserError" })
public class Arduino {
	private String arduinoIP = "";
	private String code="";
	public HashMap<String, Device> devices = new HashMap<String, Device>();
	
	private ActuatorBulb bulb1;
	private ActuatorBulb bulb2;
	private ActuatorPump pump1;
	private ActuatorPump pump2;
	private SensorTemp temp;
	private SensorHumidity hum;
	private SensorLDR ldr;
	
	private String randomToken;
	private String sessionToken;
	private String resultReq;
	
	//daytime (Secondi dalla mezzanotte)
	public enum codes {
		cmdtemp, bulb1, bulb2, cond1, cond2, temp, mois, pump1, pump2,
		daytime, allbulb, allcond, setbulb1,setbulb2,
		setbulb3, setbulb4, setcond1, setcond2, setcond3, 
		setcond4, setallbulb,setallcond, setpump1, setpump2
	} 
	
	public enum loginCode{
		new_user, login, rt, end			
	}

	public Arduino(String ip, String code){
		setArduinoIP(ip);
		setCode(code);
		
		bulb1 = new ActuatorBulb();
		bulb2 = new ActuatorBulb();
		pump1 = new ActuatorPump();
		pump2 = new ActuatorPump();
		temp = new SensorTemp();
		hum = new SensorHumidity();
		ldr = new SensorLDR();
		
		setRandomToken(new String());
		
	}
	
	
	public String getArduinoIp(){
		return this.arduinoIP;
	}
	
	public HashMap<String, Device> getDevices(){
		return this.devices;
	}
	
	public void setArduinoIP(String arduinoIP) {
		this.arduinoIP = arduinoIP;
	}
	
	public void setDev(HashMap<String, Device> list){
		this.devices=list;
				
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	public String toString(){
		System.out.println("Arduino Ip Address: "+this.arduinoIP);
		Iterator<String> it = devices.keySet().iterator();
		while(it.hasNext()){
			String key = (String)(it.next());
			Device value = devices.get(key);
			System.out.println("Key: "+key+" Value: "+value.toString());
		}		
		return this.arduinoIP;
	}
	/** Arduino API list */
	
	
	public boolean reqStatus() throws JSONException{
		JSONObject response = null;
		String URL = "http://"+arduinoIP+"/";
		System.out.println("URL: "+ URL);
		
		String result = HttpRequest.requestGET(URL);
		if(result!=null){
			
			System.out.println("RESULT: "+ result);
			
			response = new JSONObject(result);
			
			return true;
		}
		
		return false;
	}
	
	
	
	/** Richiesta http 
	 * @throws JSONException **/
	public boolean reqHttp(String arduinoIP) throws JSONException{
		JSONObject response = null;
		//"http://"+this.getArduinoIp()+"/?t="+sessionToken;
		String URL = "http://"+arduinoIP+"/?t="+sessionToken;
		System.out.println("URL: "+ URL);
		
		String result = HttpRequest.requestGET(URL);
		if(result!=null){
			
			System.out.println("RESULT: "+ result);
			
			response = new JSONObject(result);
			
			bulb1.setState(response.get("bulb1").toString());
			bulb2.setState(response.get("bulb2").toString());
			
			pump1.setState(response.get("pump1").toString());
			pump2.setState(response.get("pump2").toString());
			
			temp.setActualValue(response.get("temp").toString());
			
			hum.setActualValue(response.get("mois").toString());
			
			ldr.setThr(response.get("rad").toString());
			
			//-99 se non settato
			int min = response.getJSONArray("cmdtemp").getInt(0);
			temp.setMinValue(String.valueOf(min));
			int max = response.getJSONArray("cmdtemp").getInt(1);
			temp.setMaxValue(String.valueOf(max));
			
			//-1 se non settato
			min = response.getJSONArray("cmdmois").getInt(0);
			hum.setMinValue(String.valueOf(min));
			max = response.getJSONArray("cmdmois").getInt(1);
			hum.setMaxValue(String.valueOf(max));
			
			//-1 se non settato
			int slider = response.getJSONArray("cmdrad").getInt(0);
			ldr.setThr(String.valueOf(slider));
			int time = response.getJSONArray("cmdrad").getInt(1);
			ldr.setTime(String.valueOf(time));
			
			return true;
		}
		
		return false;
	}
	
	
	
	@SuppressLint("ParserError")
	public boolean mySetHttp()  throws JSONException{
		JSONObject response = null;
		String URL="";
		URL = "http://"+this.getArduinoIp()+"/get?t="+sessionToken+"&";
		
		int tmp = 0;
		
		if(!temp.getMaxValue().equals("-1") || !temp.getMinValue().equals("-1")){
			int min = Integer.parseInt(temp.getMinValue());
			int max = Integer.parseInt(temp.getMaxValue());
			URL = URL + "cmdtemp="+min+","+max+"$&";
		}else{
			URL = URL + "cmdtemp&";
		}
		
		if(!hum.getMaxValue().equals("-1") || !hum.getMinValue().equals("-1")){
			int min = Integer.parseInt(hum.getMinValue());
			int max = Integer.parseInt(hum.getMaxValue());
			URL = URL + "cmdmois="+min+","+max+"$&";
		}else{
			URL = URL + "cmdmois&";
		}
		
		if(!ldr.getThr().equals("-1") || !ldr.getTime().equals("-1")){
			int thr = Integer.parseInt(ldr.getThr());
			int time = Integer.parseInt(ldr.getTime());
			URL = URL + "cmdrad="+thr+","+time+"$";
		}else{
			URL = URL + "cmdrad";
		}
		
		URL = URL + "&save";
		
		//send request
		System.out.println("URL: " + URL);
		String result = HttpRequest.requestGET(URL);
		if(result!=null){
			response = new JSONObject(result);
			System.out.println("RESULT: " + result);
			return true;
		}
		
		return false;
	}
	
	
	public boolean forceSetHttp(int b1, int b2, int p1, int p2) throws JSONException{
		JSONObject response = null;
		String URL="";
			
		URL = "http://"+this.getArduinoIp()+"/get?t="+sessionToken+"&bulb1="+b1+"&bulb2="+b2+"&pump1="+p1+"&pump2="+p2;
	
		//send request
		System.out.println("URL: " + URL);
		String result = HttpRequest.requestGET(URL);
		System.out.println("RESULT: " + result);
		if(result!=null){
			response = new JSONObject(result);
			return true;
		}
		
		return false;
	}
	
	/*
	public boolean forceSetHttp(String tag, int value) throws JSONException{
		JSONObject response = null;
		String URL="";
		
		switch(codes.valueOf(tag)){
			
			case setbulb1:
						URL = "http://"+this.getArduinoIp()+"/get?t="+sessionToken+"&bulb1="+value;
						break;
						
			case setbulb2:
						URL = "http://"+this.getArduinoIp()+"/get?t="+sessionToken+"&bulb2="+value;
						break;
						
			case setpump1:
						URL = "http://"+this.getArduinoIp()+"/get?t="+sessionToken+"&pump1="+value;
						break;
				
			case setpump2:
						URL = "http://"+this.getArduinoIp()+"/get?t="+sessionToken+"&pump2="+value;
						break;
		}
	
		//send request
		System.out.println("URL: " + URL);
		String result = HttpRequest.requestGET(URL);
		if(result!=null){
			response = new JSONObject(result);
			System.out.println("RESULT: " + result);
			return true;
		}
		
		return false;
	}
	*/
	
	public boolean loginHandler(String value) throws JSONException{
		JSONObject response = null;
		String URL="";

		URL = "http://"+arduinoIP+"/login?user="+value;
	
		System.out.println("nome utente:" +value);
		
		//send request
		System.out.println("URL: " + URL);
		String result = HttpRequest.requestGET(URL);
		
		System.out.println("result json: " + result);
		
		if(result!=null){
			response = new JSONObject(result);
			System.out.println("RESULT: " + result);
			
			resultReq = response.get("result").toString();
			System.out.println("RESULT: " +resultReq);
			if(resultReq.equals("OK")){
				randomToken = response.get("t").toString();
				System.out.println("randomToken: " +randomToken);
			}
			
			return true;
		}
		
		return false;
	}
	
	
	public boolean randomTokenHandler(String value) throws JSONException{
		JSONObject response = null;
		String URL="";

		URL = "http://"+arduinoIP+"/login?rt="+value;
	
		//send request
		System.out.println("URL: " + URL);
		String result = HttpRequest.requestGET(URL);
		
		System.out.println("result json: " + result);
		
		if(result!=null){
			response = new JSONObject(result);
			
			resultReq = response.get("result").toString();

			if(resultReq.equals("OK")){
				sessionToken = response.get("t").toString();
				System.out.println("sessionToken: " +sessionToken);
			}
			return true;
		}		
		return false;
	}
	
	
	public boolean endLogin() throws JSONException{
		JSONObject response = null;
		String URL="";

		URL = "http://"+arduinoIP+"/login?end&t="+sessionToken;
	
		//send request
		System.out.println("URL: " + URL);
		String result = HttpRequest.requestGET(URL);
		
		System.out.println("result json: " + result);
		
		if(result!=null){
			response = new JSONObject(result);		
			resultReq = response.get("result").toString();
			return true;
		}		
		return false;
	}
	
	

	
	/* Get all devices connected to Arduino */
	
	public boolean Discovery() {
		boolean value = false;
		JSONObject response = null;
		String URL = "http://"+arduinoIP+"/discovery";

		try {
			response = new JSONObject(HttpRequest.requestGET(URL));
			System.out.println(response.toString());
			value = true;
		} catch (Exception e) {
			value = false;
		}

		return value;
	}


	public ActuatorBulb getBulb1() {
		return bulb1;
	}


	public void setBulb1(ActuatorBulb bulb1) {
		this.bulb1 = bulb1;
	}


	public ActuatorBulb getBulb2() {
		return bulb2;
	}


	public void setBulb2(ActuatorBulb bulb2) {
		this.bulb2 = bulb2;
	}


	public ActuatorPump getPump1() {
		return pump1;
	}


	public void setPump1(ActuatorPump pump1) {
		this.pump1 = pump1;
	}


	public ActuatorPump getPump2() {
		return pump2;
	}


	public void setPump2(ActuatorPump pump2) {
		this.pump2 = pump2;
	}


	public SensorTemp getTemp() {
		return temp;
	}


	public void setTemp(SensorTemp temp) {
		this.temp = temp;
	}


	public SensorHumidity getHum() {
		return hum;
	}


	public void setHum(SensorHumidity hum) {
		this.hum = hum;
	}


	public SensorLDR getLdr() {
		return ldr;
	}


	public void setLdr(SensorLDR ldr) {
		this.ldr = ldr;
	}


	public String getRandomToken() {
		return randomToken;
	}


	public void setRandomToken(String randomToken) {
		this.randomToken = randomToken;
	}


	public String getResultReq() {
		return resultReq;
	}


	public void setResultReq(String resultReq) {
		this.resultReq = resultReq;
	}


	public String getSessionToken() {
		return sessionToken;
	}


	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}
	
}
