package net.osmtracker.gpx;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;

import net.osmtracker.OSMTracker;
import net.osmtracker.activity.TrackManager;
import net.osmtracker.db.DataHelper;
import net.osmtracker.util.MockData;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class ExportToStorageTaskTest {

	private long trackId;
	private File trackFile;

	@Rule
	public ActivityTestRule<TrackManager> mRule = new ActivityTestRule<>(TrackManager.class);

	@Rule // Storage permissions are required
	public GrantPermissionRule writePermission = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);


	@Before
	public void setUp() throws Exception {
		// Delete file entry in media library
		mRule.getActivity().getContentResolver().delete(
				MediaStore.Files.getContentUri("external"),
				MediaStore.Files.FileColumns.DATA + " LIKE ?",
				new String[] {"%/osmtracker/gpx-test"});

		Cursor cursor = mRule.getActivity().managedQuery(
				MediaStore.Files.getContentUri("external"),
				null,
				MediaStore.Files.FileColumns.DATA + " LIKE ?",
				new String[] {"%/osmtracker/gpx-test"},
				null);
		Assert.assertEquals(0, cursor.getCount());

		trackFile = new File(Environment.getExternalStorageDirectory(), "osmtracker/gpx-test.gpx");
		if (trackFile.exists()) {
			Assert.assertTrue(trackFile.delete());
		}
		
		trackId = MockData.mockTrack(mRule.getActivity());
				
		new DataHelper(mRule.getActivity()).stopTracking(trackId);
		
		// Ensure easy filename
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mRule.getActivity());
		Editor e = prefs.edit();
		e.clear();
		e.putString(OSMTracker.Preferences.KEY_OUTPUT_FILENAME, OSMTracker.Preferences.VAL_OUTPUT_FILENAME_NAME);
		e.putBoolean(OSMTracker.Preferences.KEY_OUTPUT_DIR_PER_TRACK, false);
		e.putBoolean(OSMTracker.Preferences.KEY_OUTPUT_GPX_HDOP_APPROXIMATION, true);
		Assert.assertTrue(e.commit());
	}

	@Test
	public void testExportTrackAsGpx() throws Exception {

		ExportToStorageTask exportToStorageTask = new ExportToStorageTask(mRule.getActivity(), trackId);

		exportToStorageTask.exportTrackAsGpx(trackId);

		// Ensure file contents are OK
		Assert.assertTrue(trackFile.exists());
		System.out.println(readFully(new FileInputStream(trackFile)));
		Assert.assertEquals(
				readFully(InstrumentationRegistry.getContext().getAssets().open("gpx/gpx-test.gpx")),
				readFully(new FileInputStream(trackFile)));

		Cursor c = null;
		c = mRule.getActivity().managedQuery(
				MediaStore.Files.getContentUri("external"),
				null,
				MediaStore.Files.FileColumns.DATA + " LIKE ?",
				new String[]{"%/osmtracker/gpx-test.gpx"},
				null);
		c.moveToFirst();

		Assert.assertEquals(1, c.getCount());
		Assert.assertEquals(0, c.getInt(c.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)));
		Assert.assertEquals("gpx-test", c.getString(c.getColumnIndex(MediaStore.Files.FileColumns.TITLE)));

	}

	private static String readFully(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(is));
		
		StringBuilder sb = new StringBuilder();
		String line;
		while( (line=reader.readLine()) != null ) {
			sb.append(line).append(System.getProperty("line.separator"));
		}
		reader.close();
		
		return sb.toString();
	}
	
}
