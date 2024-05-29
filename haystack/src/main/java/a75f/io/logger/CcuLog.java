package a75f.io.logger;

/**
 * Created by samjithsadasivan isOn 8/17/17.
 */

import android.util.Log;

import org.projecthaystack.UnknownRecException;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

/**
 * Helper class for a list (or tree) of LoggerNodes.
 *
 * <p>When this is set as the head of the list,
 * an instance of it can function as a drop-in replacement for {@link android.util.Log}.
 * Most of the methods in this class server only to map a method call in CcuLog to its equivalent
 * in LogNode.</p>
 */
public class CcuLog
{
	// Grabbing the native values from Android's native logging facilities,
	// to make for easy migration and interop.
	public static final int NONE = -1;
	public static final int VERBOSE = 0;
	public static final int DEBUG = 1;
	public static final int INFO = 2;
	public static final int WARN = 3;
	public static final int ERROR = 4;

	// Stores the beginning of the LogNode topology.
	private static LogNode mLogNode;

	/**
	 * Sets the LogNode data will be sent to.
	 */
	public static void setLogNode(LogNode node) {
		mLogNode = node;
	}
	
	/**
	 * Instructs the LogNode to print the log data provided. Other LogNodes can
	 * be chained to the end of the LogNode as desired.
	 *
	 * @param priority CcuLog level of the data being logged. Verbose, Error, etc.
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 * @param tr If an exception was thrown, this can be sent along for the logging facilities
	 *           to extract and print useful information.
	 */
	public static void println(int priority, String tag, String msg, Throwable tr) {
		if (mLogNode != null) {
			mLogNode.println(priority, tag, msg, tr);
		}
		Log.i(tag, msg);
		//System.out.println(msg);
		if (tr != null) {
			tr.printStackTrace();
		}
		
	}
	
	/**
	 * Instructs the LogNode to print the log data provided. Other LogNodes can
	 * be chained to the end of the LogNode as desired.
	 *
	 * @param priority CcuLog level of the data being logged. Verbose, Error, etc.
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged. The actual message to be logged.
	 */
	public static void println(int priority, String tag, String msg) {
		println(priority, tag, msg, null);
	}
	
	/**
	 * Prints a message at VERBOSE priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 * @param tr If an exception was thrown, this can be sent along for the logging facilities
	 *           to extract and print useful information.
	 */
	public static void v(String tag, String msg, Throwable tr) {
		if(getLogLevel() <= VERBOSE)
			println(VERBOSE, tag, msg, tr);
	}
	
	/**
	 * Prints a message at VERBOSE priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 */
	public static void v(String tag, String msg) {
		v(tag, msg, null);
	}
	
	
	/**
	 * Prints a message at DEBUG priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 * @param tr If an exception was thrown, this can be sent along for the logging facilities
	 *           to extract and print useful information.
	 */
	public static void d(String tag, String msg, Throwable tr) {
		if(getLogLevel() <= DEBUG)
			println(DEBUG, tag, msg, tr);
	}
	
	/**
	 * Prints a message at DEBUG priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 */
	public static void d(String tag, String msg) {
		d(tag, msg, null);
	}
	
	/**
	 * Prints a message at INFO priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 * @param tr If an exception was thrown, this can be sent along for the logging facilities
	 *           to extract and print useful information.
	 */
	public static void i(String tag, String msg, Throwable tr) {
		if(getLogLevel() <= INFO)
			println(INFO, tag, msg, tr);
	}
	
	/**
	 * Prints a message at INFO priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 */
	public static void i(String tag, String msg) {
		i(tag, msg, null);
	}
	
	/**
	 * Prints a message at WARN priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 * @param tr If an exception was thrown, this can be sent along for the logging facilities
	 *           to extract and print useful information.
	 */
	public static void w(String tag, String msg, Throwable tr) {
		if(getLogLevel() <= WARN)
			println(WARN, tag, msg, tr);
	}
	
	/**
	 * Prints a message at WARN priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 */
	public static void w(String tag, String msg) {
		w(tag, msg, null);
	}
	
	/**
	 * Prints a message at WARN priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param tr If an exception was thrown, this can be sent along for the logging facilities
	 *           to extract and print useful information.
	 */
	public static void w(String tag, Throwable tr) {
		w(tag, null, tr);
	}
	
	/**
	 * Prints a message at ERROR priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 * @param tr If an exception was thrown, this can be sent along for the logging facilities
	 *           to extract and print useful information.
	 */
	public static void e(String tag, String msg, Throwable tr) {
		if(getLogLevel() <= ERROR)
			println(ERROR, tag, msg, tr);
	}
	
	/**
	 * Prints a message at ERROR priority.
	 *
	 * @param tag Tag for for the log data. Can be used to organize log statements.
	 * @param msg The actual message to be logged.
	 */
	public static void e(String tag, String msg) {
		e(tag, msg, null);
	}

	public static void printLongMessage(String tag, String str) {
		if (str.length() > 4000) {
			Log.i(tag, str);
			printLongMessage(tag, str.substring(4000));
		} else
			Log.i(tag, str);
	}


	private static int getLogLevel(){
		double level = 4.0;
		try {
			return CCUHsApi.getInstance().getCcuLogLevel();
		} catch (IllegalStateException e) {
			Log.e("CcuLog", "hayStack is not initialized");
			return 0;
		}
	}
}