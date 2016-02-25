package com.resource.arduino;

public class SensorTemp {

	private String actualValue;
	private String minValue;
	private String maxValue;
	
	public SensorTemp(){
		actualValue = "0";
		minValue = "0";
		maxValue = "0";
	}
	
	public String getActualValue() {
		return actualValue;
	}
	public void setActualValue(String actualValue) {
		this.actualValue = actualValue;
	}
	
	public String getMinValue() {
		return minValue;
	}
	public void setMinValue(String minValue) {
		this.minValue = minValue;
	}
	
	public String getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(String maxValue) {
		this.maxValue = maxValue;
	}

	

	
	
}
