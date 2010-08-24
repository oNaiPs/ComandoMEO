package org.onaips.comandomeo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.admob.android.ads.AdManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ScrollView;
import android.widget.Toast;

public class MEO_comando extends Activity {

	private static final int MENU_QUIT = 0;
	private static final int MENU_RECONNECT = 1;
	private static final int MENU_ONAIPS = 2;

	//	1,2,3,4,5,6,7,8,9,0,av,enter,v+,p+,v-,p-,ok,menu,back,screen,guia,video,i,switchscreen,gravacao,stop,play,pause,up,down,left,right,prev,rev,forw,next,red,green,yellow,blue,mute,song,riscas,tv
	int [] meo_key = {49,50,51,52,53,54,55,56,57,48,0,0,233,175,33,174,34,13,36,8,27,112,114,159,156,115,123,119,225,38,40,37,39,117,118,121,122,140,141,142,143,173,0,111,0};
	int [] meo_1_x = {24,135,242,24,139,234,22,135,242,135,26,240,242,24,242,22,242,118,112,22,99,178,260,22,133,240,22,133,240,105,103,35,230,22,101,180,225,22,99,180,257,24,99,176,257};
	int [] meo_1_y = {102,102,102,188,188,188,272,272,272,358,358,358,15,481,481,698,698,557,778,838,838,838,838,933,933,933,1016,1019,1019,491,686,561,560,1120,1124,1121,1127,1207,1204,1204,1207,1283,1285,1285,1289};
	int [] meo_2_x = {88,201,308,88,201,308,90,201,308,197,88,307,309,90,309,90,309,212,217,73,151,228,307,86,197,304,90,198,317,227,230,106,305,71,148,225,305,71,148,227,302,69,146,227,300};
	int [] meo_2_y = {170,170,170,254,254,254,343,343,343,426,426,426,84,549,549,768,771,673,820,889,889,889,889,999,999,999,1083,1083,1085,567,764,686,688,1175,1178,1179,1179,1256,1256,1258,1259,1340,1337,1337,1341};


	SharedPreferences preferences;
	private float lastX,lastY;
	private PrintWriter out;
	private BufferedReader in;

	private Vibrator mVibrator;

	Socket sock=null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//check arrays
		if (meo_key.length!=meo_1_x.length || meo_key.length!=meo_1_y.length ||meo_key.length!=meo_2_x.length ||meo_key.length!=meo_2_y.length)
		{
			Toast.makeText(MEO_comando.this,"Erro aki",Toast.LENGTH_LONG).show();
			System.exit(-1);
		}
		//fullscreen
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);  


		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// Initialize preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		connect();		

		findViewById(R.id.ImageView01).setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent arg1) {
				lastX=arg1.getX();
				lastY=arg1.getY();
				return false;
			}
		}); 



		findViewById(R.id.ImageView01).setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {

				if (sock!=null && sock.isConnected())
					sendButton(lastX, lastY);
				else
					Toast.makeText(MEO_comando.this,"Erro: A ligação com o MEO não está activa.",Toast.LENGTH_LONG).show();

			}
		});

		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

	}

	public void disconnect()
	{

		if (sock!=null && sock.isConnected())
			try {
				out.close();
				in.close();
				sock.close();
				sock=null;
			} catch (IOException e) {
				Toast.makeText(MEO_comando.this,"Erro a fechar socket",Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
	}


	public boolean onKeyDown(int keyCode, KeyEvent event) {
		String vol_binding=preferences.getString("volkeys", "pgupdown");


		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			{
				if (sock!=null && sock.isConnected())
				{
					if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
						if (vol_binding.equals("pgupdown"))
							out.println("key=34");
						else
							out.println("key=174");
						out.flush();
						return true;
					}
					else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
						if (vol_binding.equals("pgupdown"))
							out.println("key=33");
						else
							out.println("key=175");
						out.flush();
						return true;
					}
				}
				else
					Toast.makeText(MEO_comando.this,"Erro: A ligação com o MEO não está activa.",Toast.LENGTH_LONG).show();
			}
Log.v("AKI","AKI");
			return false;
		}

		public void sendButton(float x,float y)
		{
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);

			x=(int)(x*320/dm.widthPixels);
			y=(int)(y*320/dm.widthPixels);


			for (int i=0;i<meo_key.length;i++){
				if (x>meo_1_x[i] && x<meo_2_x[i] && y > meo_1_y[i] && y<meo_2_y[i])
				{
					mVibrator.vibrate(50);
					if (meo_key[i]==0)
						Toast.makeText(MEO_comando.this,"Botao nao implementado\n\n(não sei o keycode)",Toast.LENGTH_LONG).show();
					else
					{
						//Toast.makeText(MEO_comando.this,String.valueOf(meo_key[i]),Toast.LENGTH_LONG).show();
						out.println("key=" + String.valueOf(meo_key[i]));
						out.flush();
					}
					return;
				}
			}			
		}



		public void connect()
		{
			String active_server=preferences.getString("active", "server1");
			String server=preferences.getString(active_server, "192.168.1.64");


			try {
				InetAddress addr=InetAddress.getByName(server);

				if (!addr.isReachable(10000))
				{
					Toast.makeText(MEO_comando.this,"Timeout a encontrar MEO\n\nTente reconectar ou verifique as prefêrencias\n\n- Servidor actual: " + active_server + " (" + server + ")",Toast.LENGTH_LONG).show();
					return;
				}

				sock = new Socket(addr,8082);
				out = new PrintWriter(sock.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

				in.readLine();			

				Toast.makeText(MEO_comando.this,"Ligado a meo: " + sock.getInetAddress().toString(),Toast.LENGTH_SHORT).show();
			} catch (UnknownHostException e) {
				sock=null;
				Toast.makeText(MEO_comando.this,"UnknownHostException\n\nO servidor não foi encontrado, verifique as prefêrencias\n\n- Servidor actual: " + active_server + " (" + server + ")",Toast.LENGTH_LONG).show();
				e.printStackTrace();
			} catch (IOException e) {
				Toast.makeText(MEO_comando.this,"IOException\n\nO servidor não foi encontrado, verifique as prefêrencias\n\n- Servidor actual: " + active_server + (server.equals("")? "":" (" + server + ")"),Toast.LENGTH_LONG).show();
				sock=null;
				e.printStackTrace();
			}

		}

		public boolean onCreateOptionsMenu(Menu menu) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu, menu);

			menu.add(0,MENU_RECONNECT,0,"Reconectar");
			menu.add(0,MENU_ONAIPS,0,"Sobre");
			menu.add(0, MENU_QUIT, 0, "Fechar");

			return true;
		}

		// This method is called once the menu is selected
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			// We have only one menu option
			case R.id.preferences:
				// Launch Preference activity
				Intent i = new Intent(MEO_comando.this, preferences.class);
				startActivity(i);
				// A toast is a view containing a quick little message for the user.
				Toast.makeText(MEO_comando.this,
						"Não se esqueça de reconectar depois de fazer alterações.",
						Toast.LENGTH_LONG).show();
				break;
			case MENU_QUIT:
				System.exit(1);
				break;
			case MENU_RECONNECT:
				disconnect();
				connect();
				break;
			case MENU_ONAIPS:
				//			Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://onaips.blogspot.com"));
				//		startActivity(myIntent);

				String version = "";
				try {
					PackageInfo pi = getPackageManager().getPackageInfo("org.onaips.comandomeo", 0);
					version = pi.versionName;     
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};


				new AlertDialog.Builder(this)
				.setTitle("Sobre")
				.setMessage(Html.fromHtml("Comando MEO versão " + version + "<br><br>Mais informações visite<br> <font color=\"blue\">onaips.blogspot.com</font>"))
				.setPositiveButton("Fechar", null)
				.show();


			}
			return true;
		}


	}