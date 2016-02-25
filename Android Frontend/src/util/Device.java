package util;


public class Device {
	private int id;
	private int status;
	private String name;
	private String IP;
	
	public Device(int id, int status, String name, String IP) {
		this.id = id;
		this.status = status;
		this.name = name;
		this.IP = IP;
	}
	

	public void setId(int id) {
		this.id = id;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	public int getId() {
		return id;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String toString(){
		System.out.println("ID: "+this.getId()+" STATUS: "+this.getStatus());
		return "ID:"+this.getId()+" STATUS:"+this.getStatus();
		
	}

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getIP() {
		return IP;
	}


	public void setIP(String iP) {
		IP = iP;
	}
}
