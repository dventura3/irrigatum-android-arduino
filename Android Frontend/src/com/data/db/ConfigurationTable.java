package com.data.db;

import android.provider.BaseColumns;

public interface ConfigurationTable extends  BaseColumns {

	String TABLE_NAME = "configPiante";
	 
	String NAME = "name";
 
	String HUMIDITY_MIN = "h_min";
	
	String HUMIDITY_MAX = "h_max";
	
	String TEMPERATURE_MIN = "t_min";

	String TEMPERATURE_MAX = "t_max";
	
	String THR_IRRID = "thr_irrid";
 
	String TIME_IRRID = "time_irrid";
	
	String[] COLUMNS = new String[]
	{ _ID, NAME, HUMIDITY_MIN, HUMIDITY_MAX, TEMPERATURE_MIN, TEMPERATURE_MAX, THR_IRRID, TIME_IRRID };
}
