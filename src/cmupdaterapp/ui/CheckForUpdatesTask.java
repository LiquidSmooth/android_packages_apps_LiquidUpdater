package cmupdaterapp.ui;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import cmupdaterapp.service.FullUpdateInfo;
import cmupdaterapp.service.IUpdateServer;
import cmupdaterapp.utils.Preferences;

public class CheckForUpdatesTask extends UserTask<Void, Integer, FullUpdateInfo>
{	
	public static final String KEY_UPDATE_LIST = "cmupdaterapp.fullUpdateList";

	private static final String TAG = "<CM-Updater> CheckForUpdatesTask";

	private IUpdateServer mUpdateServer;
	private IUpdateProcessInfo mUpdateProcessInfo;	

	
	public CheckForUpdatesTask(IUpdateServer updateServer, IUpdateProcessInfo upi)
	{
		mUpdateServer = updateServer;
		mUpdateProcessInfo = upi;
	}

	@Override
	public FullUpdateInfo doInBackground(Void... params)
	{
		try
		{
			Log.i(TAG, "Checking for updates...");
			return mUpdateServer.getAvailableUpdates();
		}
		catch (IOException ex)
		{
			Log.e(TAG, "IOEx while checking for updates", ex);
			return null;
		}
		catch (RuntimeException ex)
		{
			Log.e(TAG, "RuntimeEx while checking for updates", ex);
			return null;
		}
	}
	
	@Override
	public void onPostExecute(FullUpdateInfo result)
	{
		IUpdateProcessInfo upi = mUpdateProcessInfo;
		
		Resources res = upi.getResources();
		if(result == null)
		{
			Toast.makeText(upi, R.string.exception_while_updating, Toast.LENGTH_SHORT).show();
			return;
		}
		
		Preferences prefs = Preferences.getPreferences(upi);
		prefs.setLastUpdateCheck(new Date());
		
		int updateCountRoms = result.getRomCount();
		int updateCountThemes = result.getThemeCount();
		int updateCount = result.getUpdateCount();
		if(updateCountRoms == 0 && updateCountThemes == 0)
		{
			Log.i(TAG, "No updates found");
			Toast.makeText(upi, R.string.no_updates_found, Toast.LENGTH_SHORT).show();
			upi.switchToUpdateChooserLayout(null);
		}
		else
		{
			Log.i(TAG, updateCountRoms + " ROM update(s) found; " + updateCountThemes + " Theme update(s) found");
			upi.switchToUpdateChooserLayout(result);
			
			Intent i = new Intent(upi, UpdateProcessInfo.class)
							.putExtra(UpdateProcessInfo.KEY_UPDATE_LIST, (Serializable)result)
							.putExtra(UpdateProcessInfo.KEY_REQUEST, UpdateProcessInfo.REQUEST_NEW_UPDATE_LIST);
			PendingIntent contentIntent = PendingIntent.getActivity(upi, 0, i, PendingIntent.FLAG_ONE_SHOT);

			Notification notification = new Notification(R.drawable.icon_notification,
								res.getString(R.string.not_new_updates_found_ticker),
								System.currentTimeMillis());
			
			String text = MessageFormat.format(res.getString(R.string.not_new_updates_found_body), updateCount);
			notification.setLatestEventInfo(upi, res.getString(R.string.not_new_updates_found_title), text, contentIntent);
			
			Uri notificationRingtone = prefs.getConfiguredRingtone();
			if(prefs.getVibrate())
				notification.defaults = Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
			else
				notification.defaults = Notification.DEFAULT_LIGHTS;
			if(notificationRingtone == null)
			{
				notification.sound = null;
			}
			else
			{
				notification.sound = notificationRingtone;
			}
			
			//Use a resourceId as an unique identifier
			((NotificationManager)upi.getSystemService(Context.NOTIFICATION_SERVICE)).
					notify(R.string.not_new_updates_found_title, notification);
		}
	}
}