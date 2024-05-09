package com.cdac.secureonlineexam;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author athisii
 * @version 1.0
 * @since 5/8/24
 */

public class App extends Application {
    private static final int NUMBER_OF_FIELD_REQUIRED = 5;
    private static final int NUMBER_OF_CHAR_FOR_HT = 19;
    public static final int COUNTDOWN_IN_MIN = 90;
    private static int cameraId = 0;
    // file index
    public static final int HALL_TICKET_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int NO_OF_QUESTION_INDEX = 2;
    public static final int NO_OF_ANSWER_CHOICE_INDEX = 3;
    public static final int PAPER_NAME_INDEX = 4;


    private static final Map<Integer, String> dataErrorIndexMap = new HashMap<>();
    private static final Map<Integer, String> dataMap = new HashMap<>();
    public static final String[] ANSWER_OPTION = {"A", "B", "C", "D", "E", "F", "G", "H"};

    static {
        dataErrorIndexMap.put(HALL_TICKET_INDEX, "Hall Ticket");
        dataErrorIndexMap.put(NAME_INDEX, "Name");
        dataErrorIndexMap.put(NO_OF_QUESTION_INDEX, "No. Of Question");
        dataErrorIndexMap.put(NO_OF_ANSWER_CHOICE_INDEX, "No. Of Answer Choice");
        dataErrorIndexMap.put(PAPER_NAME_INDEX, "Paper Name");

        extractQrCodeAndPutToMap("AV2312CHE402GDBA209|Grace Johnson|100|5|AA");
    }

    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("starter"), 1366, 768);
        stage.setTitle("Secure Online Exam");
        stage.setScene(scene);
        stage.setResizable(false);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(getCssFileName())).toExternalForm());
//        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnCloseRequest(event -> {
            event.consume();
            Platform.exit();  //Comment this line in production/deployment (Alt+f4 and close button)
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) throws IOException {
        // load native library from running  jar itself.
        URL jarLocation = App.class.getProtectionDomain().getCodeSource().getLocation();
        String jarFileName = jarLocation.getFile();
        int lastSlashIndex = jarFileName.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            jarFileName = jarFileName.substring(lastSlashIndex + 1);
        }
        if (jarFileName.isBlank()) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } else {
            loadNativeLibraryFromJar(jarFileName, "libopencv_java3413.so");
        }
        try {
            cameraId = Integer.parseInt(args[0].trim());
        } catch (Exception ignored) {
        }

        launch();
    }

    public static String getCssFileName() {
        // landscape 0r portrait
        return "/style/base.css";
    }

    public static void extractQrCodeAndPutToMap(String qrCodeText) {
        dataMap.clear();
        String[] splitData = qrCodeText.split("\\|");
        if (splitData.length != NUMBER_OF_FIELD_REQUIRED) {
            throw new GenericException("Invalid QR code. Expected " + NUMBER_OF_FIELD_REQUIRED + " fields in the QR code");
        }
        if (splitData[0].length() != NUMBER_OF_CHAR_FOR_HT) {
            throw new GenericException("Invalid Hall Ticket character count. Must be " + NUMBER_OF_CHAR_FOR_HT);
        }
        for (int i = 0; i < splitData.length; i++) {
            if (splitData[i].isBlank()) {
                throw new GenericException("'" + dataErrorIndexMap.get(i) + "' field is blank.");
            }

            if (i == NO_OF_QUESTION_INDEX && Integer.parseInt(splitData[i].trim()) <= 0) {
                throw new GenericException("'" + dataErrorIndexMap.get(i) + "' value is less than or equal to 0.");
            }
            if (i == NO_OF_ANSWER_CHOICE_INDEX && Integer.parseInt(splitData[i].trim()) <= 0) {
                throw new GenericException("'" + dataErrorIndexMap.get(i) + "' value is less than or equal to 0.");
            }
            dataMap.put(i, splitData[i]);
        }
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static int getCameraId() {
        return cameraId;
    }

    public static String getFromDataMap(int key) {
        return dataMap.get(key);
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    private static void loadNativeLibraryFromJar(String jarFileName, String libName) throws IOException {
        // Load the .so file from the JAR into a temporary directory
        try (InputStream inputStream = App.class.getResourceAsStream("/" + libName)) {
            if (inputStream == null) {
                throw new IOException("Native library not found in JAR: " + libName);
            }
            Path tempFile = Files.createTempFile(libName, "");
            tempFile.toFile().deleteOnExit();

            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            System.load(tempFile.toAbsolutePath().toString());
        }
    }
}