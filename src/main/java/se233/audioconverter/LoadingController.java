package se233.audioconverter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class LoadingController {

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar progressBar;


    public void updateProgress(double progress) {
        progressBar.setProgress(progress);
    }

    public void updateStatus(String message) {
        statusLabel.setText(message);
    }

    public void closeWindow() {
        Stage stage = (Stage) progressBar.getScene().getWindow();
        stage.close();
    }
}