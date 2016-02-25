package util;

public class Utils {

	//Session, if is setted
	public static Session mySessionObject = null;
	
	//Device, if is setted
	public static Device myActualDevice = null;
	
	//Configuration, if is setted on "DatabaseConfig" Activity
	public static Configuration myActualConfig = null;
	
	public static boolean checkMail(String mail){
		  String EMAIL_REGEX = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
	      //String email1 = "user@domain.com";
	      return mail.matches(EMAIL_REGEX);
	}
}
