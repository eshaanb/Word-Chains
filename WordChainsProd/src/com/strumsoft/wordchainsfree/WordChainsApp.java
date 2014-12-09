package com.strumsoft.wordchainsfree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

@ReportsCrashes(formKey = "", customReportContent = { org.acra.ReportField.APP_VERSION_NAME,
        org.acra.ReportField.APP_VERSION_CODE, 
        org.acra.ReportField.PACKAGE_NAME, 
        org.acra.ReportField.USER_CRASH_DATE, 
        org.acra.ReportField.BUILD, 
        org.acra.ReportField.DISPLAY,
        org.acra.ReportField.AVAILABLE_MEM_SIZE, 
        org.acra.ReportField.USER_APP_START_DATE, 
        org.acra.ReportField.BRAND, 
        org.acra.ReportField.TOTAL_MEM_SIZE,
        org.acra.ReportField.REPORT_ID, 
        org.acra.ReportField.PHONE_MODEL, 
        org.acra.ReportField.DEVICE_ID, 
        org.acra.ReportField.SHARED_PREFERENCES, 
        org.acra.ReportField.IS_SILENT,
        org.acra.ReportField.ANDROID_VERSION, 
        org.acra.ReportField.STACK_TRACE }, formUri="http://report.flockthere.com/report/")
public class WordChainsApp extends Application {

	@Override
	public void onCreate() {
		ACRA.init(this);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains("pref_key_acraInstall")) {
		String id = Acra.id(this);
		prefs.edit().putString("pref_key_acraInstall", id).commit();
	}
		super.onCreate();
	}
	
	private static class Acra {
	    private static String sID = null;
	    private static final String NAME = "ACRA";

	    public synchronized static String id(Context context) {
	        if (sID == null) {  
	            File installation = new File(context.getFilesDir(), NAME);
	            try {
	                if (!installation.exists()) {
	                    writeInstallationFile(installation);
	                }
	                sID = readInstallationFile(installation);
	            } catch (Exception e) {
	                throw new RuntimeException(e);
	            }
	        }
	        return sID;
	    }

	    private static String readInstallationFile(File installation) throws IOException {
	        RandomAccessFile f = new RandomAccessFile(installation, "r");
	        byte[] bytes = new byte[(int) f.length()];
	        f.readFully(bytes);
	        f.close();
	        return new String(bytes);
	    }

	    private static void writeInstallationFile(File installation) throws IOException {
	        FileOutputStream out = new FileOutputStream(installation);
	        String id = UUID.randomUUID().toString();
	        out.write(id.getBytes());
	        out.close();
	    }
	}
	
}
