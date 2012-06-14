package org.onaips.comandomeo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;


public class Connection extends Thread {
	Handler handler;
	@Override
	public void run() {
		try {
			// preparing a looper on current thread
			// the current thread is being detected implicitly
			Looper.prepare();

			// now, the handler will automatically bind to the
			// Looper that is attached to the current thread
			// You don't need to specify the Looper explicitly
			handler = new Handler();

			// After the following line the thread will start
			// running the message loop and will not normally
			// exit the loop unless a problem happens or you
			// quit() the looper (see below)
			Looper.loop();
		} catch (Throwable t) {
			Log.e("COMANDOMEO", "Connection class halted due to an error", t);
		}
	}
	 
	public Handler getHandler(){
		return handler;
	}
    
}