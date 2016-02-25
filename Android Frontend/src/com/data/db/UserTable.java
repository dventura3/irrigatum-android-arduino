package com.data.db;

import android.provider.BaseColumns;

public interface UserTable extends BaseColumns {
	
	String TABLE_NAME = "USERS";
	String user = "user";
	String pwd = "pwd";
	String sessionToken = "sessionToken";
	String timestamp = "timestamp";
	
	
	String[] COLUMNS = new String[]	{ _ID, user, pwd, sessionToken, timestamp};
}
