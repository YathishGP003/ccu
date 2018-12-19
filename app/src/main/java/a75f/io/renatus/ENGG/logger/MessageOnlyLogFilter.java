package a75f.io.renatus.ENGG.logger;

/**
 * Created by samjithsadasivan isOn 8/17/17.
 */

import a75f.io.logger.CcuLog;
import a75f.io.logger.LogNode;

/**
 * Simple {@link LogNode} filter, removes everything except the message.
 * Useful for situations like isOn-screen log output where you don't want a lot of metadata displayed,
 * just easy-to-read message updates as they're happening.
 */
public class MessageOnlyLogFilter implements LogNode {
	
	LogNode mNext;
	
	/**
	 * Takes the "next" LogNode as a parameter, to simplify chaining.
	 *
	 * @param next The next LogNode in the pipeline.
	 */
	public MessageOnlyLogFilter(LogNode next) {
		mNext = next;
	}
	
	public MessageOnlyLogFilter() {
	}
	
	@Override
	public void println(int priority, String tag, String msg, Throwable tr) {
		if (mNext != null) {
			getNext().println(CcuLog.NONE, null, msg, null);
		}
	}
	
	/**
	 * Returns the next LogNode in the chain.
	 */
	public LogNode getNext() {
		return mNext;
	}
	
	/**
	 * Sets the LogNode data will be sent to..
	 */
	public void setNext(LogNode node) {
		mNext = node;
	}
	
}
