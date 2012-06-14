package org.onaips.comandomeo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {


	void warnRestartApp(Preference preference)
	{
		new AlertDialog.Builder(preference.getContext())
		.setTitle("Reiniciar Aplicação?")
		.setMessage("Para que as alterações sejam efetuadas deve reiniciar a aplicação. Poderá reiniciar mais tarde no menu de contexto.")
		.setNegativeButton("Não", null)
		.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				reloadApplication();
			}
		})
		.show();
	} 

	public void reloadApplication() {
		AlarmManager mgr = (AlarmManager)ActivityComandoMEO.ACTIVITY.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, ActivityComandoMEO.RESTART_INTENT);
		System.exit(2);
	}

	boolean verifyIpAddress(String server,Preference pref)
	{
		IpValidator validator=new IpValidator();

		if (validator.validate(server))
			return true;

		Toast.makeText(Preferences.this,"O IP introduzido \"" + server + "\" não é válido.", Toast.LENGTH_LONG).show();
		Log.v("COMANDOMEO","Wrong IP Adddress");
		return false;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Preferências");
		addPreferencesFromResource(R.xml.preferences);

		OnPreferenceChangeListener rebootPrefchange=new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				warnRestartApp(preference);
				return true;
			}
		};

		OnPreferenceChangeListener ipAddrValidatorChange=new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {

				return verifyIpAddress((String)newValue, preference);
			}
		};

		findPreference("fullscreen").setOnPreferenceChangeListener(rebootPrefchange);
		findPreference("viewad").setOnPreferenceChangeListener(rebootPrefchange);
		findPreference("scaleComando").setOnPreferenceChangeListener(rebootPrefchange);

		findPreference("server1").setOnPreferenceChangeListener(ipAddrValidatorChange);
		findPreference("server2").setOnPreferenceChangeListener(ipAddrValidatorChange);
		findPreference("server3").setOnPreferenceChangeListener(ipAddrValidatorChange);

		Preference about=findPreference("about");
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				String version = "";
				try {
					PackageInfo pi = getPackageManager().getPackageInfo("org.onaips.comandomeo", 0);
					version = pi.versionName;     
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};


				new AlertDialog.Builder(preference.getContext())
				.setTitle("ComandoMEO " + version)
				.setMessage(Html.fromHtml("Code: @oNaiPs<br><br>Graphics: Carlos Coelho | anti2.org"))
				.setPositiveButton("Fechar", null)
				.setNegativeButton("Open Website", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://onaips.com"));
						startActivity(myIntent);

					}
				})
				.show();
				return false;
			}
		});
	}
}
