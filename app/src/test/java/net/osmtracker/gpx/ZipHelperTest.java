package net.osmtracker.gpx;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import net.osmtracker.db.DataHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RunWith(RobolectricTestRunner.class)
public class ZipHelperTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Context context;
	private File mockGpxFile;
	private final String mockGpxFilename = "track2026.gpx";


	@Before
	public void setup() throws Exception {
		context = ApplicationProvider.getApplicationContext();
		// Create a fake GPX file in the temp folder
		mockGpxFile = tempFolder.newFile(mockGpxFilename);
		writeTextToFile(mockGpxFile, "<gpx></gpx>");
	}

	@Test
	public void testZipGPXFileCreatesValidZip() throws Exception {
		File zipResult = ZipHelper.zipGPXFile(context, mockGpxFile);
		// Basic checks
		Assert.assertNotNull(zipResult);
		Assert.assertTrue(zipResult.exists());
		// check for .zip extension
		Assert.assertTrue(zipResult.getName().endsWith(DataHelper.EXTENSION_ZIP));

		// Verify ZIP content
		try (ZipFile zipFile = new ZipFile(zipResult)) {
			ZipEntry entry = zipFile.getEntry(mockGpxFilename);
			Assert.assertNotNull("ZIP should contain the GPX file", entry);
		}
	}

	@Test
	public void testZipCacheFiles_IncludesMultimedia() throws Exception {
		// Create a track directory with one media files
		long trackId = 99L;
		File trackDir = DataHelper.getTrackDirectory(trackId, context);

		Assert.assertTrue("Failed to create track directory: " + trackDir.getAbsolutePath(),
				trackDir.mkdirs());

		// Create a photo file
		File photo = new File(trackDir, "image.jpg");
		writeTextToFile(photo, "fake_image_data");

		File zipFileResult = ZipHelper.zipCacheFiles(context, trackId, mockGpxFile);

		Assert.assertNotNull("Zip file should not be null", zipFileResult);
		Assert.assertTrue("Zip file should exist on disk", zipFileResult.exists());
		Assert.assertTrue("Zip file name should end with .zip",
				zipFileResult.getName().endsWith(DataHelper.EXTENSION_ZIP));

		// Verify the contents of the ZIP
		try (ZipFile zipFile = new ZipFile(zipFileResult)) {
			// Check for the GPX file
			Assert.assertNotNull("ZIP should contain the GPX file",
					zipFile.getEntry(mockGpxFilename));

			// Check for the photo
			ZipEntry photoEntry = zipFile.getEntry("image.jpg");
			Assert.assertNotNull("ZIP should contain the photo 'image.jpg'", photoEntry);
		}
	}

	private void writeTextToFile(File file, String content) throws Exception {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(content.getBytes(StandardCharsets.UTF_8));
		}
	}
}
