package com.flyzebra.utils;

public class ShellUtil {
    public static void exec(String command) {
        String[] cmdStrings = new String[]{"sh", "-c", command};
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmdStrings);
            int status = p.waitFor();
            if (status != 0) {
                FlyLog.e("runShellCommand: %s, status: %s", command, status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }
}
