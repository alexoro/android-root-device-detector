package com.alexoro.androidrootdevicedetector;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by uas.sorokin@gmail.com
 * @author uas.sorokin@gmail.com
 */
public class RootDeviceDetector {

    private Context mContext;

    public RootDeviceDetector(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null");
        }
        mContext = context.getApplicationContext();
    }

    public boolean isRooted() {
        return isSpecificApplicationsIsInstalled()
                || isCanExecuteRootCommands()
                || isRootUserId()
                || isDeviceRunningWithRomNonReleaseKeys();
    }

    protected boolean isSpecificApplicationsIsInstalled() {
        // some root-specific applications
        Process process = null;
        BufferedReader br = null;
        try {
            // we cannot use grep, because it may be not installed on application
            process = Runtime.getRuntime().exec("ls -l /system/app/");
            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.contains("superuser") || line.contains("busybox")) {
                    return true;
                }
            }
        } catch (Throwable ex) {
            // ignore
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (process != null) {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    // ignore
                }
                process.destroy();
            }
        }

        // some known root applications
        String[] rootApps = new String[] {
                "eu.chainfire.supersu",
                "eu.chainfire.supersu.pro",
                "eu.chainfire.mobileodin",
                "eu.chainfire.mobileodin.pro",
                "eu.chainfire.pryfi"
        };
        for (ApplicationInfo app: mContext.getPackageManager().getInstalledApplications(0)) {
            for (String rootApp: rootApps) {
                if (app.packageName.equals(rootApp)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean isCanExecuteRootCommands() {
        return canExecuteCommand("/system/xbin/which su")
                || canExecuteCommand("/system/bin/which su") || canExecuteCommand("which su");
    }

    protected boolean isRootUserId() {
        Process process = null;
        BufferedReader br = null;
        try {
            // we cannot use grep, because it may be not installed on application
            process = Runtime.getRuntime().exec("su id");
            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = br.readLine();
            if (line == null) {
                return false;
            } else {
                Matcher matcher = Pattern.compile("uid=(\\d+)").matcher(line);
                boolean isFound = matcher.find();
                if (isFound) {
                    try {
                        String uidString = matcher.group(1);
                        return Integer.parseInt(uidString) == 0;
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            }
        } catch (Throwable ex) {
            // ignore
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (process != null) {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    // ignore
                }
                process.destroy();
            }
        }

        return false;
    }

    protected boolean isDeviceRunningWithRomNonReleaseKeys() {
        String buildTags = Build.TAGS;
        return  (buildTags != null && buildTags.toLowerCase().contains("test-keys"));
    }

    protected boolean canExecuteCommand(String command) {
        boolean executedSuccessfully;
        try {
            Runtime.getRuntime().exec(command);
            executedSuccessfully = true;
        } catch (Exception e) {
            executedSuccessfully = false;
        }
        return executedSuccessfully;
    }

}