package com.resource.arduino;

public class ActuatorBulb {

	private String state;
	
	public ActuatorBulb(){
		state = "0";
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	
}
