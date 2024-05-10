package com.cdac.secureonlineexam;

import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.logging.*;

/**
 * @author athisii
 * @version 1.0
 * @since 5/9/24
 */

public class ApplicationLog {
    //Suppress default constructor for noninstantiability
    private ApplicationLog() {
        throw new AssertionError("The ApplicationLog methods should be accessed statically");
    }

    private static Handler handler;
    private static final Logger LOGGER = Logger.getLogger(ApplicationLog.class.getName());
    private static String logFileName;

    static {
        try {
            String logDirectory = "log";
            try {
                Files.createDirectory(Paths.get(logDirectory));
            } catch (Exception ignored) {
                // ignored if log directory exists
            }

            LocalDateTime now = LocalDateTime.now();
            String dateTime = now.getDayOfMonth() + "-" + now.getMonth().getValue() + "-" + now.getYear() + "-" + now.getHour() + ":" + now.getMinute();
            logFileName = logDirectory + "/soe" + "-" + dateTime + ".log";
            handler = new FileHandler(logFileName, 10240000, 2); //1024000 is 1Mb - It will roll over after a file becomes 1Mb
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.ALL);
            LOGGER.addHandler(handler);
            LOGGER.setLevel(Level.INFO);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            LOGGER.log(Level.SEVERE, () -> "Error creating log " + logFileName + " Shutting down the app.......");
            Platform.exit();
            System.exit(-1);
        }
    }

    public static Logger getLogger(Class<?> klass) {
        var logger = Logger.getLogger(klass.getName());
        logger.addHandler(handler);
        return logger;
    }
}
