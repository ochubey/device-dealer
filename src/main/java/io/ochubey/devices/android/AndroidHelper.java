package io.ochubey.devices.android;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ochubey on 11/27/17.
 */
public class AndroidHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidHelper.class);

    private static final String ADB_DEVICE_SELECTION_COMMAND = "adb -s %s";
    private final String deviceUdid;

    public AndroidHelper(String udid) {
        deviceUdid = udid;
    }

    public void resetKeyboard() {
        BufferedReader filterInput = getCommandOutput(getKeyboardFilterCli());
        try {
            String keyboard;
            if (filterInput != null) {
                while ((keyboard = filterInput.readLine()) != null) {
                    keyboard = keyboard.split(":")[0];
                    BufferedReader enableOut = getCommandErrors(getKeyboardEnableCli(keyboard));
                    if (enableOut == null || enableOut.lines().count() == 0) {
                        BufferedReader setOut = getCommandErrors(getKeyboardSetCli(keyboard));
                        if (setOut == null || setOut.lines().count() == 0) {
                            String keyboardSetMsg = keyboard + " was set successfully";
                            LOG.warn(keyboardSetMsg);
                            return;
                        }
                    } else {
                        String keyboardNotSetMsg = "Unable to set " + keyboard;
                        LOG.error(keyboardNotSetMsg);
                    }
                }
            }
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
    }

    @Nullable
    private BufferedReader getCommandOutput(String command) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
            return new BufferedReader(new InputStreamReader(process.getInputStream()));
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
        return null;
    }

    @Nullable
    private BufferedReader getCommandErrors(String command) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
            return new BufferedReader(new InputStreamReader(process.getErrorStream()));
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
        return null;
    }

    @NotNull
    private String getKeyboardEnableCli(String keyboardId) {
        List<String> cliCommand = new ArrayList<>();

        cliCommand.add(String.format(ADB_DEVICE_SELECTION_COMMAND, deviceUdid));
        cliCommand.add("shell ime enable");
        cliCommand.add(keyboardId);

        return String.join(" ", cliCommand);
    }

    @NotNull
    private String getKeyboardSetCli(String keyboardId) {
        List<String> cliCommand = new ArrayList<>();

        cliCommand.add(String.format(ADB_DEVICE_SELECTION_COMMAND, deviceUdid));
        cliCommand.add("shell ime set");
        cliCommand.add(keyboardId);

        return String.join(" ", cliCommand);
    }

    @NotNull
    private String getKeyboardFilterCli() {
        List<String> cliCommand = new ArrayList<>();

        cliCommand.add(String.format(ADB_DEVICE_SELECTION_COMMAND, deviceUdid));
        cliCommand.add("shell ime list");
        cliCommand.add("| grep :");
        cliCommand.add("| grep -v \"  \"");
        cliCommand.add("| grep -v appium");
        cliCommand.add("| grep -v voice");
        cliCommand.add("| grep -v Emotion");

        return String.join(" ", cliCommand);
    }
}
