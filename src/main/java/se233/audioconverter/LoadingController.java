package se233.audioconverter;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

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

    @FXML private ImageView leftFace;
    @FXML private ImageView rightFace;

    private Timeline spinAnimation;
    private Timeline shakeAnimation;

    public void initialize() {
        spinAnimation = new Timeline(
                new KeyFrame(Duration.millis(50), e -> {
                    leftFace.setRotate(leftFace.getRotate() + 1);
                    rightFace.setRotate(rightFace.getRotate() - 1);
                })
        );
        spinAnimation.setCycleCount(Timeline.INDEFINITE);

        shakeAnimation = new Timeline(
                new KeyFrame(Duration.millis(200), e -> {
                    double translateY = Math.random() > 0.5 ? -1 : 1;
                    leftFace.setTranslateY(translateY);
                    rightFace.setTranslateY(-translateY);
                }),
                new KeyFrame(Duration.millis(400), e -> {
                    leftFace.setTranslateY(0);
                    rightFace.setTranslateY(0);
                })
        );
        shakeAnimation.setCycleCount(Timeline.INDEFINITE);
    }

    public void startSpin() {
        if (spinAnimation != null) {
            spinAnimation.play();
        }
    }

    public void stopSpin() {
        if (spinAnimation != null) {
            spinAnimation.stop();
            leftFace.setRotate(0);
            rightFace.setRotate(0);
        }
    }

    public void startShake() {
        if (shakeAnimation != null) {
            shakeAnimation.play();
        }
    }

    public void stopShake() {
        if (shakeAnimation != null) {
            shakeAnimation.stop();
            leftFace.setTranslateY(0);
            rightFace.setTranslateY(0);
        }
    }
}