package org.onaips.comandomeo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
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
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

public class ActivityComandoMEO extends Activity {

	private static final int MENU_QUIT = 0;
	private static final int MENU_RECONNECT = 1;
	private static final int MENU_SELECTMEO = 2;

	private static final int REAL_COMANDO_WIDTH = 452;
	private static final int REAL_COMANDO_HEIGHT = 1987;

	Connection connection;
	SharedPreferences preferences;
	private float lastX,lastY;
	private PrintWriter out;
	private BufferedReader in;
	float globalScale;

	private Vibrator mVibrator;

	Socket sock=null;
	public static ActivityComandoMEO ACTIVITY;
	public static PendingIntent RESTART_INTENT;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//check arrays
		Keys.checkArrays();
		ACTIVITY = this;
		RESTART_INTENT = PendingIntent.getActivity(this.getBaseContext(), 0, new Intent(getIntent()), getIntent().getFlags());
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Initialize preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		//Initialize connection handler thread
		connection=new Connection();
		connection.start();

		//do i want fullscreen app?
		if (preferences.getBoolean("fullscreen", false))
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		//Set content UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);


		adjustComando();


		if (!preferences.getBoolean("viewad", true))
		{
			AdView adView=(AdView)findViewById(R.id.adView);
			adView.stopLoading();
		}

		connect();		

		findViewById(R.id.ImageView01).setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				lastX=event.getX();
				lastY=event.getY();
				return false;
			}
		}); 

		findViewById(R.id.ImageView01).setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				findAndCommitButton(lastX, lastY);
			} 
		});	

		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	}

	public void adjustComando()
	{
		try {
			ImageView v=(ImageView)findViewById(R.id.ImageView01);

			AssetManager assets = getResources().getAssets();
			InputStream buffer = null;

			buffer = new BufferedInputStream((assets.open("comando.png")));

			BitmapFactory.Options opts=new BitmapFactory.Options();
			opts.inDither=false;                     //Disable Dithering mode
			opts.inPurgeable=true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
			opts.inInputShareable=true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
			opts.inTempStorage=new byte[32 * 1024]; 

			Rect r=new Rect();
			Bitmap img = BitmapFactory.decodeStream(buffer,r,opts);

			Display display = getWindowManager().getDefaultDisplay(); 
			int width = display.getWidth();

			globalScale=width/(float)img.getWidth();

			int userScale=preferences.getInt("scaleComando", 100);
			globalScale*=userScale/100.0F;

			Log.v("COMANDOMEO", "Display Width= " + width + "\t" + "Comando " + img.getWidth() + "x" + img.getHeight() +  "\tscale=" + globalScale);

			// create a matrix for the manipulation
			Matrix matrix = new Matrix();

			// resize the bit map
			matrix.postScale(globalScale, globalScale);

			// recreate the new Bitmap and set it back
			Bitmap resizedBitmap = Bitmap.createBitmap(img, 0, 0,img.getWidth(), img.getHeight(), matrix, true);
			img.recycle();


			v.setImageBitmap(resizedBitmap);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void disconnect()
	{
		connection.getHandler().post(new Runnable() {

			@Override
			public void run() {

				if (sock!=null && sock.isConnected())
					try {
						out.close();
						in.close();
						sock.close();
						sock=null;
					} catch (IOException e) {
						Toast.makeText(ActivityComandoMEO.this,"Erro a fechar socket",Toast.LENGTH_LONG).show();
						e.printStackTrace();
					}
			}
		});
	}


	public boolean onKeyDown(int keyCode, KeyEvent event) {
		String vol_binding=preferences.getString("volkeys", "pgupdown");

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
				if (vol_binding.equals("pgupdown"))
					out.println("key=34");
				else if (vol_binding.equals("volupdown"))
					out.println("key=174");
				out.flush();
				return true;
			}
			else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
				if (vol_binding.equals("pgupdown"))
					out.println("key=33");
				else if (vol_binding.equals("volupdown"))
					out.println("key=175");
				out.flush();
				return true;
			}
		}else if (keyCode==KeyEvent.KEYCODE_BACK)
		{
			moveTaskToBack(true);
		}
		return false;
	}

	public void findAndCommitButton(float x,float y)
	{

		Log.v("AKIII" , "OLD\t\t" + x + " x " + y);

		Display display = getWindowManager().getDefaultDisplay(); 
		float width = display.getWidth();

		float realScale=width/(float)REAL_COMANDO_WIDTH;

		float userScale=preferences.getInt("scaleComando", 100);
		realScale*=userScale/100.0F;

		x-=(width-(userScale*width)/100.0F)/2.0F;

		x/=realScale;
		y/=realScale;

		Log.v("AKIII" , "NEW\t\t" + x + " x " + y);

		for (int i=0;i<Keys.meo_key.length;i++){
			if (x>Keys.meo_1_x[i] && x<Keys.meo_2_x[i] && y > Keys.meo_1_y[i] && y<Keys.meo_2_y[i])
			{
				mVibrator.vibrate(50);
				sendButton(Keys.meo_key[i]);
				return;
			}

		}			
	}


	public void sendButton(final int button)
	{
		connection.getHandler().post(new Runnable() {

			@Override
			public void run() {
				if (sock!=null && sock.isConnected())
				{
					if (button==0)
						Toast.makeText(ActivityComandoMEO.this,"Botão nao implementado\n\n(não sei o keycode)",Toast.LENGTH_LONG).show();
					else
					{
						out.println("key=" + String.valueOf(button));
						out.flush();
					}
				}
				else
				{
					Toast.makeText(ActivityComandoMEO.this,"Erro: A ligação com o MEO não está ativa.",Toast.LENGTH_LONG).show();
				}
			}
		});

	}


	public void connect()
	{
		connection.getHandler().post(new Runnable() {

			@Override
			public void run() {
				String active_server=preferences.getString("active", "server1");
				String server=preferences.getString(active_server, "192.168.1.64");

				IpValidator validator=new IpValidator();

				if (!validator.validate(server))
				{
					Toast.makeText(ActivityComandoMEO.this,"O IP introduzido \"" + server + "\" não é válido.", Toast.LENGTH_LONG);
					return;
				}

				try {
					InetAddress addr=InetAddress.getByName(server);

					if (!addr.isReachable(3000))
					{
						Toast.makeText(ActivityComandoMEO.this,"Timeout a encontrar MEO\n\nTente reconetar ou verifique as preferências\n\n- Servidor actual: " + active_server + " (" + server + ")",Toast.LENGTH_LONG).show();
						return;
					}

					sock = new Socket(addr,8082);
					out = new PrintWriter(sock.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

					in.readLine();			

					Toast.makeText(ActivityComandoMEO.this,"Ligado a meo: " + sock.getInetAddress().toString(),Toast.LENGTH_SHORT).show();
				} catch (UnknownHostException e) {
					sock=null;
					Toast.makeText(ActivityComandoMEO.this,"O servidor não foi encontrado, verifique as prefêrencias\n\n- Servidor actual: " + active_server + " (" + server + ")",Toast.LENGTH_LONG).show();
					e.printStackTrace();
				} catch (IOException e) {
					Toast.makeText(ActivityComandoMEO.this,"O servidor não foi encontrado, verifique as prefêrencias\n\n- Servidor actual: " + active_server + (server.equals("")? "":" (" + server + ")"),Toast.LENGTH_LONG).show();
					sock=null;
					e.printStackTrace();
				}
			}
		});
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		menu.add(0, MENU_RECONNECT, 0, "Reconectar");
		menu.add(0, MENU_SELECTMEO, 0, "Selecionar Box");
		inflater.inflate(R.menu.menu, menu);
		menu.add(0, MENU_QUIT,0 , "Fechar");

		return super.onCreateOptionsMenu(menu);
	}

	// This method is called once the menu is selected
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.preferences:
			// Launch Preference activity
			Intent i = new Intent(ActivityComandoMEO.this, Preferences.class);
			startActivity(i);
			Toast.makeText(ActivityComandoMEO.this,"Não se esqueça de reconetar depois de efetuar as alterações.",Toast.LENGTH_LONG).show();
			break;
		case MENU_QUIT:
			System.exit(1);
			break; 
		case MENU_RECONNECT:
			disconnect();
			connect();
			break;
		case MENU_SELECTMEO:
			Resources res=getResources();
			final String[] meo_strings=res.getStringArray(R.array.meo_string);
			final String[] meo_servers=res.getStringArray(R.array.meo_servers);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String activeServer=preferences.getString("active", "server1");

			int aServer=0;
			for (;aServer<meo_servers.length;aServer++)
				if (meo_servers[aServer].equals(activeServer))
					break;

			builder.setTitle("Selecione Box ativa");
			builder.setSingleChoiceItems(meo_strings, aServer, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					Editor e=preferences.edit();
					e.putString("active", meo_servers[item]);
					e.commit();
					dialog.dismiss();
					disconnect();
					connect();
				}
			});
			builder.create().show();

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

		ImageView v=(ImageView)findViewById(R.id.ImageView01);
		unbindDrawables(v);
		System.gc();
	}

}	
