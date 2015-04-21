package com.example.CosyDVR;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

//import android.content.Context;

public class SDList {
	public ArrayList<String> sVold = new ArrayList<String>();

	public void determineStorageOptions(/*Context context*/) {
		readVoldFile();
		testAndCleanList();

	}

	private void readVoldFile() {
		/*
		 * Scan the /system/etc/vold.fstab file and look for lines like this:
		 * dev_mount sdcard /mnt/sdcard 1
		 * /devices/platform/s3c-sdhci.0/mmc_host/mmc0
		 * 
		 * When one is found, split it into its elements and then pull out the
		 * path to the that mount point and add it to the arraylist
		 * 
		 * some devices are missing the vold file entirely so we add a path here
		 * to make sure the list always includes the path to the first sdcard,
		 * whether real or emulated.
		 */

		File voldfile = new File("/system/etc/vold.fstab");
		if(voldfile.exists()) {      
			try {
				Scanner scanner = new Scanner(voldfile);
				while (scanner.hasNext()) {
					String line = scanner.nextLine();
					if (line.startsWith("dev_mount")) {
						String[] lineElements = line.split(" ");
						String element = lineElements[2];
	
						if (element.contains(":"))
							element = element.substring(0, element.indexOf(":"));
	
						//if (element.contains("usb"))
						//	continue;
	
						// don't add the default vold path
						// it's already in the list.
						if (!sVold.contains(element))
							sVold.add(element);
					}
				}
                scanner.close();
			} catch (Exception e) {
				// swallow - don't care
				e.printStackTrace();
			}
		} else {
			sVold.add("/mnt/sdcard");	//default when no vold.fstab file at all
		}
	}

	private void testAndCleanList() {
		/*
		 * Now that we have a cleaned list of mount paths, test each one to make
		 * sure it's a valid and available path. If it is not, remove it from
		 * the list.
		 */

		for (int i = sVold.size()-1; i >=0 ; i--) {
			String voldPath = sVold.get(i);
			File path = new File(voldPath);
			if (!path.exists() || !path.isDirectory() || !path.canWrite())
				sVold.remove(i);
		}
	}

}