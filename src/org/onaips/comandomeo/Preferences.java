package org.onaips.comandomeo;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
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
import android.widget.Toast;

public class Preferences extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.preferences);
		addPreferencesFromResource(R.xml.preferences);

		OnPreferenceChangeListener rebootPrefchange = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				new AlertDialog.Builder(preference.getContext())
				.setTitle(R.string.restart_app_title)
				.setMessage(R.string.restart_app_prompt)
				.setNegativeButton(R.string.no, null)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonIndex) {
						restartApplication();
					}
				})
				.show();

				return true;
			}
		};

		OnPreferenceChangeListener ipAddrValidatorChange=new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String server = (String)newValue;
				//verify validity of the address
				try {
					new URL("http://" + server);
				} catch (MalformedURLException e) {
					Toast.makeText(Preferences.this,
							"O IP introduzido \"" + server + "\" não é válido.", Toast.LENGTH_LONG).show();
					return false;
				}
				return true;
			}
		};

		findPreference(getString(R.string.ui_fullscreen_key)).setOnPreferenceChangeListener(rebootPrefchange);
		findPreference(getString(R.string.ui_viewad_key)).setOnPreferenceChangeListener(rebootPrefchange);
		findPreference(getString(R.string.ui_scale_key)).setOnPreferenceChangeListener(rebootPrefchange);

		findPreference(getString(R.string.server_1_key)).setOnPreferenceChangeListener(ipAddrValidatorChange);
		findPreference(getString(R.string.server_2_key)).setOnPreferenceChangeListener(ipAddrValidatorChange);
		findPreference(getString(R.string.server_3_key)).setOnPreferenceChangeListener(ipAddrValidatorChange);

		Preference about = findPreference(getString(R.string.ui_about_key));
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				String version = "unknown";
				try {
					PackageInfo packageInfo = getPackageManager().getPackageInfo(
							Preferences.this.getPackageName(), 0);
					version = packageInfo.versionName;     
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				};

				new AlertDialog.Builder(Preferences.this)
				.setTitle(getString(R.string.app_name) + " " + version)
				.setMessage(Html.fromHtml("Code: @oNaiPs<br><br>Graphics: Carlos Coelho | anti2.org"))
				.setNegativeButton(R.string.close, null)
				.setPositiveButton("Open Website", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int index) {
						Intent myIntent = new Intent(Intent.ACTION_VIEW, 
								Uri.parse(getString(R.string.project_website)));
						startActivity(myIntent);
					}
				})
				.show();
				return false;
			}
		});
	}

	/**
	 * Restarts the application.
	 */
	public void restartApplication() {
		AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		Intent mainActivityIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
				mainActivityIntent, 0);

		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent);
		System.exit(2);
	}
}
