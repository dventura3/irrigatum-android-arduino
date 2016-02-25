package com.data.db;

import android.provider.BaseColumns;

public interface ArduinoTable  extends  BaseColumns {

	String TABLE_NAME = "ardTab";
	
	String dev_name = "dev_name";
	
	String dev_ip = "dev_ip";
	
	String[] COLUMNS = new String[]
	{ _ID, dev_name, dev_ip };
	
}
