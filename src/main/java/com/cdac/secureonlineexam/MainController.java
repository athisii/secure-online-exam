package com.cdac.secureonlineexam;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii
 * @version 1.0
 * @since 5/8/24
 */

public class MainController {
    private static final Logger LOGGER = ApplicationLog.getLogger(MainController.class);
    private final Map<Integer, ToggleGroup> questionAnswerMapToggleGroupMap = new HashMap<>(this.getNoOfQuestion());
    String fileName;
    String directoryName = "response";

    private String jsonData;
    @FXML
    private VBox vBoxContent;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Button submitBtn;
    @FXML
    private Button generateQrCodeBtn;

    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private HBox hiddenQrCodeHBox;
    @FXML
    private HBox mainContentHBox;
    @FXML
    private Label remainingTimeLabel;

    private int countdownTime;
    private volatile boolean stopTimer = false;

    public void initialize() {
        fileName = directoryName + "/" + getName().replaceAll("[^a-zA-Z0-9.]", "_").toLowerCase() + "-" + getHallTicketNo();
        remainingTimeLabel.setText(App.COUNTDOWN_IN_MIN + "m 0s");
        countdownTime = App.COUNTDOWN_IN_MIN;
        submitBtn.setOnAction(actionEvent -> submitBtnAction());
        generateQrCodeBtn.setOnAction(actionEvent -> generateQrCodeAndDisplayOnUi());


        for (int i = 1; i <= getNoOfQuestion(); i++) {
            HBox hBox = new HBox();
            hBox.setSpacing(20);
            hBox.setAlignment(Pos.CENTER);
            hBox.getStyleClass().add("padding-small");
            Label questionLabel = new Label(i + ".");
            questionLabel.setMinWidth(3);
            hBox.getChildren().add(questionLabel);

            ToggleGroup toggleGroup = new ToggleGroup();
            questionAnswerMapToggleGroupMap.put(i, toggleGroup);

            for (int j = 0; j < getNoOfAnswerChoice(); j++) {
                RadioButton radioButton = new RadioButton(App.ANSWER_OPTION[j]);
                radioButton.setToggleGroup(toggleGroup);
                hBox.getChildren().add(radioButton);
            }
            vBoxContent.getChildren().add(hBox);
        }
        App.getExecutorService().execute(this::startTimer);
    }

    private void startTimer() {
        int second = 60;
        while (countdownTime >= 0) {
            if (stopTimer) {
                return;
            }
            try {
                Thread.sleep(Duration.ofSeconds(1).toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            second--;
            if (second == 59) {
                countdownTime--;
            }
            updateTimeCountDownLabel(second);
            if (second == 0) {
                second = 60;
            }
        }
        // after timeout, automate the process.
        submitBtn.setDisable(true);
        generateQrCodeBtn.setDisable(false);
        scrollPane.setDisable(true);
        saveResponseToFile();
        generateQrCodeAndDisplayOnUi();
    }

    private void submitBtnAction() {
        stopTimer = true;
        submitBtn.setDisable(true);
        scrollPane.setDisable(true);
        saveResponseToFile();
        generateQrCodeBtn.setDisable(false);
    }

    private void saveResponseToFile() {
        StringBuilder sb = new StringBuilder("{\"name\":\"");
        sb.append(getName()).append("\",\"hallTicketNo\":\"");
        sb.append(getHallTicketNo()).append("\",\"paperName\":\"");
        sb.append(getPaperName()).append("\",\"responses\":[");

        boolean flag = false;

        for (int i = 1; i <= getNoOfQuestion(); i++) {
            Toggle selectedToggle = questionAnswerMapToggleGroupMap.get(i).getSelectedToggle();
            if (selectedToggle != null) {
                flag = true;
                RadioButton radioButton = (RadioButton) selectedToggle;
                sb.append("{\"question\": ").append(i).append(",\"choice\": \"").append(radioButton.getText()).append("\"},");
            }
        }
        jsonData = sb.toString();
        // remove last comma
        if (flag) {
            jsonData = jsonData.substring(0, jsonData.length() - 1);
        }
        jsonData = jsonData + "]}";

        LOGGER.log(Level.INFO, () -> "Saving response to file: " + fileName + ".json");


        // try to create directory
        // if it exists then good to go.
        try {
            Files.createDirectory(Path.of(directoryName));
        } catch (FileAlreadyExistsException ignored) {
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error creating directory.", ex);
            throw new GenericException(ex.getMessage());
        }
        try {
            Files.writeString(Paths.get(fileName + ".json"), jsonData);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error writing to file.", ex);
            throw new GenericException(ex.getMessage());
        }
    }

    private void generateQrCodeAndDisplayOnUi() {
        LOGGER.log(Level.INFO, () -> "Saving QR code to file: " + fileName + ".jpeg");
        byte[] bytes;
        try {
            bytes = QrCodeUtil.createQR(jsonData, fileName + ".jpeg", 400, 400);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return;
        }
        qrCodeImageView.setImage(new Image(new ByteArrayInputStream(bytes)));
        mainContentHBox.getChildren().add(hiddenQrCodeHBox);
        generateQrCodeBtn.setDisable(true);
    }

    private void updateTimeCountDownLabel(int second) {
        Platform.runLater(() -> {
            if (countdownTime <= 5) {
                remainingTimeLabel.setTextFill(Color.RED);
            }
            remainingTimeLabel.setText(countdownTime + "m " + second + "s");
        });
    }

    public String getPaperName() {
        return App.getFromDataMap(App.PAPER_NAME_INDEX);
    }

    public String getName() {
        return App.getFromDataMap(App.NAME_INDEX);
    }

    public String getHallTicketNo() {
        return App.getFromDataMap(App.HALL_TICKET_INDEX);
    }

    public int getNoOfQuestion() {
        return Integer.parseInt(App.getFromDataMap(App.NO_OF_QUESTION_INDEX));
    }

    public int getNoOfAnswerChoice() {
        return Integer.parseInt(App.getFromDataMap(App.NO_OF_ANSWER_CHOICE_INDEX));
    }
}