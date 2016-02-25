package com.resource.arduino;

public class SensorLDR {

	private String thr;
	private String time;
	
	public SensorLDR(){
		thr = "0";
		time = "0";
	}
	
	public String getThr() {
		return thr;
	}
	public void setThr(String thr) {
		this.thr = thr;
	}
	
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	
}
