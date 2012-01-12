package com.flat502.rox.log;

import java.util.HashMap;
import java.util.Map;

/**
 * Logging level constants.
 * <p>
 * @see com.flat502.rox.log.Log
 */
public class Level {
	/**
	 * The least verbose logging level: only errors are logged.
	 */
	public static final Level ERROR = new Level(0);
	/**
	 * Errors and warnings are logged.
	 */
	public static final Level WARNING = new Level(1);
	/**
	 * Errors, warnings and informational messages are logged.
	 */
	public static final Level INFO = new Level(2);
	/**
	 * Errors, warnings and informational and debug messages are logged.
	 */
	public static final Level DEBUG = new Level(3);
	/**
	 * The most verbose logging level: everything is logged.
	 */
	public static final Level TRACE = new Level(4);
	
	private static Map namesMap = new HashMap();

	private int value;
	private String name;

	private Level(int value) {
		this.value = value;
		switch (value) {
		case 0:
			name = "ERROR";
			break;
		case 1:
			name = "WARNING";
			break;
		case 2:
			name = "INFO";
			break;
		case 3:
			name = "DEBUG";
			break;
		case 4:
			name = "TRACE";
			break;
		}
	}

	/**
	 * Compares a given level to determine whether or not
	 * this instance meets the indicated verbosity requirement.
	 * <p>
	 * If the indicated <code>level</code> is less verbose than
	 * the level this instance indicates then this method will
	 * return <code>true</code>. The intention is to invoke
	 * this method on a configured level and pass it the 
	 * level constant representing the severity of the message
	 * being logged.  
	 * @param level
	 * 	The level to compare this instance's verbosity against.
	 * @return
	 * 	<code>true</code> if the verbosity of <code>level</code> 
	 * 	is less than that of this instance. 
	 */
	public boolean at(Level level) {
		return level.value <= this.value;
	}

	public String toString() {
		return "Level[" + this.name + ":" + this.value + "]";
	}
	
	/**
	 * A convenience method that converts a string name
	 * into a {@link Level} instance.
	 * <p>
	 * Names are case-insensitive and are named for the
	 * constant members of this class (so the string value 
	 * <code>trace</code> is mapped to {@link #TRACE}.
	 * @param name
	 * 	The name of the desired level.
	 * @param defaultLevel
	 * 	A level to return if the name is not matched.
	 * @return
	 * 	An appropriate {@link Level} instance, or
	 * 	the default value provided if the level is not supported.
	 */
	public static Level parseLevel(String name, Level defaultLevel) {
		Level level = (Level) namesMap.get(name.toLowerCase());
		if (level == null) {
			return defaultLevel;
		}
		return level;
	}
	
	static {
		namesMap.put(TRACE.name.toLowerCase(), TRACE);
		namesMap.put(DEBUG.name.toLowerCase(), DEBUG);
		namesMap.put(INFO.name.toLowerCase(), INFO);
		namesMap.put(WARNING.name.toLowerCase(), WARNING);
		namesMap.put(ERROR.name.toLowerCase(), ERROR);
	}
}
