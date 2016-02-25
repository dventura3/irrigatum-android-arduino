package util;

public class Session {

	private int IDuser;
	private String randomToken;
	private String sessionToken;
	private long startTime;

	public int getIDuser() {
		return IDuser;
	}

	public void setIDuser(int iDuser) {
		IDuser = iDuser;
	}

	public String getRandomToken() {
		return randomToken;
	}

	public void setRandomToken(String randomToken) {
		this.randomToken = randomToken;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public void setSessionToken(String sexionToken) {
		sessionToken = sexionToken;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
		System.out.println("startTime: " + startTime);
	}

	//false = sessione scaduta
	//true = sessione ancora viva
	public boolean compareTime(long actualTime){
		long diffTime = actualTime - startTime;
		System.out.println("diffTime: " + diffTime);
		long compareValue = 3600 * 1000 * 2;
		//long compareValue = 60 * 1000 * 1;
		System.out.println("compareValue: " + compareValue);
		if(diffTime > compareValue)
			return false;
		return true;
	}
	
	
}
