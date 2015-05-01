package org.onaips.comandomeo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private static final int REAL_COMANDO_WIDTH = 452;
	private static final int REAL_COMANDO_HEIGHT = 1987;

	private static final int REMOTE_PORT = 8082;
	private static final int ADDRESS_REACH_TIMEOUT = 3000;

	private static final int MENU_QUIT = 0;
	private static final int MENU_RECONNECT = 1;
	private static final int MENU_SELECTMEO = 2;

	private static final int VIBRATION_PERIOD = 50;

	private HandlerThread mConnectionThread;
	private Handler mConnectionHandler;

	private SharedPreferences mPreferences;
	private Socket mSocket;
	private PrintWriter mSocketOutput;
	private BufferedReader mSocketInput;

	private Display mDisplay;
	private int mDisplayWidth;
	private Vibrator mVibrator;

	private float mLastX,mLastY;
	private float mGlobalScale;
	private ImageView mRemoteView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		//check arrays
		Keys.checkArrays();

		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mDisplay = getWindowManager().getDefaultDisplay();
		mDisplayWidth = mDisplay.getWidth();

		// Initialize preferences
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		//Initialize connection handler thread
		mConnectionThread = new HandlerThread("socket");
		mConnectionThread.start();
		mConnectionHandler = new Handler(mConnectionThread.getLooper());

		//whether to set fullscreen or not
		if (mPreferences.getBoolean(getString(R.string.ui_fullscreen_key), false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		if (mPreferences.getBoolean(getString(R.string.ui_viewad_key), true)) {

			AdView adView = (AdView) findViewById(R.id.adView);
			AdRequest adRequest = new AdRequest.Builder()
			.build();
			adView.loadAd(adRequest);
		}

		mRemoteView = (ImageView) findViewById(R.id.ImageView01);
		mRemoteView.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mLastX = event.getX();
				mLastY = event.getY();
				return false;
			}
		}); 

		findViewById(R.id.ImageView01).setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				findAndCommitButton(mLastX, mLastY);
			} 
		});	

		adjustRemote();
		connect();		
	}

	public void adjustRemote() {
		Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.comando);

		mGlobalScale = mDisplayWidth / (float) img.getWidth();

		int userScale = Integer.valueOf(
				mPreferences.getString(getString(R.string.ui_scale_key), "100"));
		mGlobalScale *= userScale/100.0F;

		Log.v(TAG, "Display Width= " + mDisplayWidth + "\t" + "Comando " + img.getWidth() + "x" + img.getHeight() +  "\tscale=" + mGlobalScale);

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();

		// resize the bit map
		matrix.postScale(mGlobalScale, mGlobalScale);

		// recreate the new Bitmap and set it back
		Bitmap resizedBitmap = Bitmap.createBitmap(img, 0, 0,img.getWidth(), img.getHeight(), matrix, true);

		//remind the GC this image is ready to be deleted
		img.recycle();

		mRemoteView.setImageBitmap(resizedBitmap);
	}

	public void disconnect() {
		if (mSocket == null || mSocket.isConnected()) {
			return;
		}
		mConnectionHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					mSocketOutput.close();
					mSocketInput.close();
					mSocket.close();
					mSocket = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		String volBinding = mPreferences.getString(
				getString(R.string.vol_key), 
				getString(R.string.pgupdown));

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
				if (volBinding.equals(getString(R.string.pgupdown))) {
					sendButton(34);
				} else if (volBinding.equals(getString(R.string.volupdown))) {
					sendButton(174);
				}
			} else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
				if (volBinding.equals(getString(R.string.pgupdown))) {
					sendButton(33);
				} else if (volBinding.equals(getString(R.string.volupdown))) {
					sendButton(175);
				}
			}
			return true;
		} else if (keyCode==KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
		}
		return false;
	}

	public void findAndCommitButton(float x,float y) {
		float realScale = mDisplayWidth / (float)REAL_COMANDO_WIDTH;

		float userScale = mPreferences.getInt("scaleComando", 100);
		realScale *= userScale / 100.0F;

		x -= (mDisplayWidth - (userScale * mDisplayWidth) / 100.0F) / 2.0F;

		x /= realScale;
		y /= realScale;

		for (int i = 0; i < Keys.meo_key.length; i++){
			if (x > Keys.meo_1_x[i] &&
					x < Keys.meo_2_x[i] &&
					y > Keys.meo_1_y[i] &&
					y < Keys.meo_2_y[i]) {
				if (mPreferences.getBoolean(getString(R.string.ui_vibration_key), true)) {
					mVibrator.vibrate(VIBRATION_PERIOD);
				}
				sendButton(Keys.meo_key[i]);
				return;
			}
		}			
	}

	public void sendButton(final int button) {
		mConnectionHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mSocket != null && mSocket.isConnected()) {
					if (button == 0) {
						Toast.makeText(MainActivity.this, R.string.non_implemented_keyboard_warn,
								Toast.LENGTH_LONG).show();
					} else {
						mSocketOutput.println("key=" + String.valueOf(button));
						mSocketOutput.flush();
					}
				} else {
					Toast.makeText(MainActivity.this, R.string.inactive_connection_to_box,
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	public void connect() {
		mConnectionHandler.post(new Runnable() {

			@Override
			public void run() {
				String activeServer = mPreferences.getString(
						getString(R.string.active_server_key), getString(R.string.server_1_key));
				String server = mPreferences.getString(activeServer, getString(R.string.server_1_default));

				try {
					new URL("http://" + server);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this,
							getString(R.string.invalid_ip_address, server),
							Toast.LENGTH_LONG).show();
					return;
				}

				try {
					InetAddress addr = InetAddress.getByName(server);

					if (!addr.isReachable(ADDRESS_REACH_TIMEOUT)) {
						Toast.makeText(MainActivity.this, getString(R.string.timeout_finding_box, 
								activeServer, server), Toast.LENGTH_LONG).show();
						return;
					}

					mSocket = new Socket(addr, REMOTE_PORT);
					mSocketOutput = new PrintWriter(mSocket.getOutputStream(), true);
					mSocketInput = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

					mSocketInput.readLine();			

					Toast.makeText(MainActivity.this, 
							getString(R.string.connected_to_box, mSocket.getInetAddress().toString()),
							Toast.LENGTH_SHORT).show();
				} catch (UnknownHostException e) {
					mSocket = null;
					Toast.makeText(MainActivity.this, 
							getString(R.string.box_not_found, activeServer, server), 
							Toast.LENGTH_LONG).show();
					e.printStackTrace();
				} catch (IOException e) {
					Toast.makeText(MainActivity.this, 
							getString(R.string.box_not_found, activeServer, server), 
							Toast.LENGTH_LONG).show();
					mSocket = null;
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		menu.add(0, MENU_RECONNECT, 0, R.string.reconnect);
		menu.add(0, MENU_SELECTMEO, 0, R.string.selectbox);
		menu.add(0, MENU_QUIT,0 , R.string.quit);
		inflater.inflate(R.menu.menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * This method is called once the menu is selected
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.preferences:
			// Launch Preference activity
			startActivity(new Intent(MainActivity.this, Preferences.class));

			Toast.makeText(MainActivity.this, R.string.reconnect_after_changes_warn, 
					Toast.LENGTH_LONG).show();
			break;
		case MENU_QUIT:
			System.exit(1);
			break; 
		case MENU_RECONNECT:
			disconnect();
			connect();
			break;
		case MENU_SELECTMEO:
			final String[] meo_strings = getResources().getStringArray(R.array.meo_string);
			final String[] meo_servers = getResources().getStringArray(R.array.meo_servers);

			String activeServer = mPreferences.getString(
					getString(R.string.active_server_key), getString(R.string.server_1_key));

			int activeServerIndex;
			for (activeServerIndex = 0; activeServerIndex < meo_servers.length; activeServerIndex++) {
				if (meo_servers[activeServerIndex].equals(activeServer)) {
					break;
				}
			}

			new AlertDialog.Builder(this)
			.setTitle(R.string.select_active_box)
			.setSingleChoiceItems(meo_strings, activeServerIndex, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					mPreferences.edit()
					.putString(getString(R.string.active_server_key), meo_servers[item])
					.commit();
					dialog.dismiss();
					disconnect();
					connect();
				}
			})
			.create().show();
		}
		return true;
	}

	private void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		ImageView v = (ImageView)findViewById(R.id.ImageView01);
		unbindDrawables(v);
		System.gc();
	}
}	
