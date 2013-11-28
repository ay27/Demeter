package org.bitman.demeter.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bitman.demeter.DemeterApplication;

import org.bitman.demeter.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * OptionActivity用于设置设置界面，以及preference文件的读写
 * @author ay27
 *
 */

@SuppressWarnings("deprecation")
public class OptionsActivity extends PreferenceActivity {

	private DemeterApplication mApplication = null;

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		mApplication = (DemeterApplication) getApplication();

		addPreferencesFromResource(R.xml.preferences);

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		final ListPreference videoEncoder = (ListPreference) findPreference("video_encoder");
		final ListPreference videoResolution = (ListPreference) findPreference("video_resolution");
		final ListPreference videoBitrate = (ListPreference) findPreference("video_bitrate");
		final ListPreference videoFramerate = (ListPreference) findPreference("video_framerate");

		videoEncoder.setEnabled(true);
		videoResolution.setEnabled(true);
		videoBitrate.setEnabled(true);
		videoFramerate.setEnabled(true);        

		videoEncoder.setValue(String.valueOf(mApplication.videoEncoder));
		videoFramerate.setValue(String.valueOf(mApplication.videoQuality.framerate));
		videoBitrate.setValue(String.valueOf(mApplication.videoQuality.bitrate/1000));
		videoResolution.setValue(mApplication.videoQuality.resX+"x"+mApplication.videoQuality.resY);

		videoResolution.setSummary(getString(R.string.settings0)+" "+videoResolution.getValue()+"px");
		videoFramerate.setSummary(getString(R.string.settings1)+" "+videoFramerate.getValue()+"fps");
		videoBitrate.setSummary(getString(R.string.settings2)+" "+videoBitrate.getValue()+"kbps");

		videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Editor editor = settings.edit();
				Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
				Matcher matcher = pattern.matcher((String)newValue);
				matcher.find();
				editor.putInt("video_resX", Integer.parseInt(matcher.group(1)));
				editor.putInt("video_resY", Integer.parseInt(matcher.group(2)));
				editor.commit();
				videoResolution.setSummary(getString(R.string.settings0)+" "+(String)newValue+"px");
				return true;
			}
		});

		videoFramerate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				videoFramerate.setSummary(getString(R.string.settings1)+" "+(String)newValue+"fps");
				return true;
			}
		});

		videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				videoBitrate.setSummary(getString(R.string.settings2)+" "+(String)newValue+"kbps");
				return true;
			}
		});

	}

}
