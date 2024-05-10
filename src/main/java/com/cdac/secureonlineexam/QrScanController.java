package com.cdac.secureonlineexam;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii
 * @version 1.0
 * @since 5/9/24
 */

public class QrScanController {
    private static final Logger LOGGER = ApplicationLog.getLogger(QrScanController.class);
    private static final int COUNTDOWN_IN_SEC = 2;
    private static final AtomicInteger COUNTDOWN = new AtomicInteger(COUNTDOWN_IN_SEC);

    private static final int FIXED_DELAY_TIME_IN_MILLIS = 5; // in milliseconds
    private static final int EXECUTOR_SHUTDOWN_WAIT_TIME_IN_MILLIS = 20; // in milliseconds
    @FXML
    private Label messageLabel;


    @FXML
    private ImageView liveImageView;

    @FXML
    private Button startStopCameraBtn;
    private volatile boolean isCameraActive = false;
    private volatile boolean stopLive = false;
    private VideoCapture videoCapture;


    private ScheduledExecutorService scheduledExecutorService;

    // automatically called by JavaFx runtime.
    public void initialize() {
        startStopCameraBtn.setOnAction(this::startCamera);
    }

    // action for start/stop button
    private void startCamera(ActionEvent actionEvent) {
        // if active then stop it
        if (isCameraActive) {
            disableControls(startStopCameraBtn);
            stopLive = true;
            startStopCameraBtn.setText("Start Camera");
            // stop the camera and thread executor
            shutdownExecutorServiceAndReleaseResource();
            enableControls(startStopCameraBtn);
        } else {
            // active status to be used by worker thread
            videoCapture = new VideoCapture(App.getCameraId());
            if (!videoCapture.isOpened()) {
                LOGGER.log(Level.SEVERE, () -> "**Unable to start camera for index: " + App.getCameraId());
                messageLabel.setText("UNABLE TO START CAMERA. PLEASE CHECK CAMERA AND RESTART THE APP AGAIN.");
                return;
            }
            isCameraActive = true;
            stopLive = false;
            //disable controls during the countdown
            disableControls(startStopCameraBtn);
            startStopCameraBtn.setText("Stop Camera");
            // thread for showing live image
            App.getExecutorService().execute(this::liveImageThread);
            // thread for capturing photo
            App.getExecutorService().execute(this::capturePhotoThread);
        }
    }

    private void capturePhotoThread() {
        int countdown = COUNTDOWN.get();
        while (countdown >= 0) {
            try {
                Thread.sleep(Duration.ofSeconds(1).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            countdown--;
            updateUi(countdown + "");
        }
        // enable controls after countdown
        Platform.runLater(() -> {
            startStopCameraBtn.setText("Stop Camera");
            startStopCameraBtn.setDisable(false);
            messageLabel.setText("");
        });
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(this::grabFrame, 0, FIXED_DELAY_TIME_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void liveImageThread() {
        Mat matrix = new Mat();
        while (!stopLive) {
            boolean read = videoCapture.read(matrix);
            if (!read) {
                return;
            }
            updateImageView(liveImageView, mat2Image(matrix));
        }
    }

    public Image mat2Image(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }


    private void updateImageView(ImageView view, Image image) {
        Platform.runLater(() -> view.setImage(image));
    }


    private void grabFrame() {
        if (!isCameraActive) {
            shutdownExecutorServiceAndReleaseResource();
            return;
        }
        if (!videoCapture.isOpened()) {
            return;
        }
        Mat matrix = new Mat();
        // read the current matrix
        videoCapture.read(matrix);
        // if the matrix is empty, return
        if (matrix.empty()) {
            return;
        }
        Imgcodecs.imwrite("input_qr_code.png", matrix);
        String qrCodeText = QrCodeUtil.readQrCode("input_qr_code.png");

        if (!qrCodeText.isBlank()) {
            try {
                App.extractQrCodeAndPutToMap(qrCodeText);
                LOGGER.log(Level.INFO, "**Found a valid QR Code.\n***Shutting down the camera....");
                startStopCameraBtn.setDisable(true);
                // if reached here, then valid image is captured
                stopLive = true;
                // sleep for 2 seconds
                Thread.sleep(5);
                videoCapture.release();
                isCameraActive = false;
                shutdownExecutorServiceAndReleaseResource();
                // change view
                LOGGER.log(Level.INFO, "**Going to next screen...");
                App.setRoot("main");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (GenericException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            } catch (Exception ignored) {
                // ignore and continue until valid data is read.
                // "Unable to extract data from QR Code"
            }
        }
    }


    private void shutdownExecutorServiceAndReleaseResource() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            stopLive = true;
            try {
                // stop the timer
                scheduledExecutorService.shutdown();
                scheduledExecutorService.awaitTermination(EXECUTOR_SHUTDOWN_WAIT_TIME_IN_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Unable to close the camera."));
            }
            try {
                if (videoCapture.isOpened()) {
                    videoCapture.release();
                    isCameraActive = false;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "**Unable to close the camera.", ex);
                Platform.runLater(() -> messageLabel.setText("Unable to close the camera."));
            }
        }
    }

    private void disableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(true);
        }
    }

    private void enableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(false);
        }
    }

    private void updateUi(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }


    public String getAppVersion() {
        return "Secure Online Exam 1.0";
    }
}
