package com.data.db;

import android.provider.BaseColumns;

public interface FirstAccess extends  BaseColumns{
	
	String TABLE_NAME = "FIRST";
	String isFirst = "isFirst";
	String id_user = "id_user";
	
	
	String[] COLUMNS = new String[]	{ _ID, isFirst, id_user };

}
