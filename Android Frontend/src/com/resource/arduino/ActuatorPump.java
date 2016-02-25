package com.resource.arduino;

public class ActuatorPump {
	
	private String state;

	public ActuatorPump(){
		state = "0";
	}
	
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	
}
