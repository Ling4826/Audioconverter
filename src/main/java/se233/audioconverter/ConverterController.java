package se233.audioconverter;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import se233.audioconverter.Converter.AudioConverter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static se233.audioconverter.YoutubeToMp3RapidApiUtil.*;

public class ConverterController {
    private static class FileSettings {
        StringProperty format = new SimpleStringProperty();
        StringProperty bitrate = new SimpleStringProperty();
        StringProperty sampleRate = new SimpleStringProperty();
        StringProperty channels = new SimpleStringProperty();
    }

    private final Map<File, FileSettings> fileSettingsMap = new HashMap<>();

    private final ObservableList<String> formatOptions = FXCollections.observableArrayList("MP3", "WAV", "M4A", "FLAC");
    private final ObservableList<String> channelOptions = FXCollections.observableArrayList("Mono", "Stereo");

    @FXML private ListView<File> fileListView;
    @FXML private Button convertButton;
    @FXML private ComboBox<String> defaultFormatComboBox;
    @FXML private ComboBox<String> defaultBitrateComboBox;
    @FXML private ComboBox<String> defaultSampleRateComboBox;
    @FXML private ComboBox<String> defaultChannelsComboBox;

    @FXML private Button addFilesButton;
    @FXML private Button clearAllButton;
    @FXML private StackPane mainStackPane;
    @FXML private VBox converterPane;
    @FXML private VBox youtubePane;
    @FXML private ToggleGroup modeToggleGroup;
    @FXML private ToggleButton converterModeButton;
    @FXML private ToggleButton youtubeModeButton;
    @FXML private TextField youtubeUrlField;
    @FXML private Button youtubeDownloadButton;

    private ObservableList<String> getBitrateOptionsForFormat(String format) {
        ObservableList<String> options = FXCollections.observableArrayList();
        switch (mapFormatForConverter(format)) { // ใช้ mapping ตรงนี้
            case "mp3":
                options.addAll("96 kbps", "128 kbps", "192 kbps", "256 kbps", "320 kbps");
                break;
            case "wav":
                options.addAll("16-bit", "24-bit", "32-bit");
                break;
            case "ipod":
                options.addAll("96 kbps", "128 kbps", "192 kbps", "256 kbps");
                break;
            case "flac":
                options.addAll("Lossless (Level 5)", "Lossless (Level 8)");
                break;
        }
        return options;
    }

    private ObservableList<String> getSampleRateOptionsForFormat(String format) {
        ObservableList<String> options = FXCollections.observableArrayList();
        switch (mapFormatForConverter(format)) { // ใช้ mapping ตรงนี้
            case "mp3":
                options.addAll("32000 Hz", "44100 Hz", "48000 Hz");
                break;
            case "ipod":
                options.addAll("44100 Hz", "48000 Hz");
                break;
            case "wav":
                options.addAll("44100 Hz", "48000 Hz", "88200 Hz", "96000 Hz");
                break;
            case "flac":
                options.addAll("44100 Hz", "48000 Hz", "88200 Hz", "96000 Hz", "192000 Hz");
                break;
        }
        return options;
    }

    @FXML
    public void initialize() {
        defaultFormatComboBox.setItems(formatOptions);
        defaultChannelsComboBox.setItems(channelOptions);

        defaultFormatComboBox.valueProperty().addListener((obs, oldFormat, newFormat) -> {
            if (newFormat != null) {
                defaultBitrateComboBox.setItems(getBitrateOptionsForFormat(newFormat));
                defaultSampleRateComboBox.setItems(getSampleRateOptionsForFormat(newFormat));

                defaultBitrateComboBox.getSelectionModel().selectFirst();
                defaultSampleRateComboBox.getSelectionModel().selectFirst();
            }
        });

        defaultFormatComboBox.getSelectionModel().selectFirst();
        defaultChannelsComboBox.getSelectionModel().select("Stereo");
        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // ถ้าไม่มีปุ่มไหนถูกเลือกเลย ให้บังคับเลือกปุ่มเดิมกลับมา
                oldToggle.setSelected(true);
            } else if (newToggle == converterModeButton) {
                // แสดงหน้า Converter ซ่อนหน้า YouTube
                converterPane.setVisible(true);
                youtubePane.setVisible(false);
            } else if (newToggle == youtubeModeButton) {
                // แสดงหน้า YouTube ซ่อนหน้า Converter
                converterPane.setVisible(false);
                youtubePane.setVisible(true);
            }
        });
        converterPane.setVisible(true);
        youtubePane.setVisible(false);
        fileListView.setCellFactory(listView -> new ListCell<File>() {
            private final HBox hbox = new HBox(15);
            private final Label label = new Label();
            private final ComboBox<String> formatBox = new ComboBox<>(formatOptions);
            private final ComboBox<String> bitrateBox = new ComboBox<>();
            private final ComboBox<String> sampleRateBox = new ComboBox<>();
            private final ComboBox<String> channelsBox = new ComboBox<>(channelOptions);
            private final Button deleteButton = new Button("✕");
            private final Region spacer = new Region();

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                label.setMaxWidth(Double.MAX_VALUE);
                deleteButton.getStyleClass().add("delete-button");
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().addAll(label, spacer, formatBox, bitrateBox, sampleRateBox, channelsBox, deleteButton);

                deleteButton.setOnAction(event -> {
                    File item = getItem();
                    if (item != null) {
                        getListView().getItems().remove(item);
                        fileSettingsMap.remove(item);
                    }
                });

                formatBox.valueProperty().addListener((obs, oldFormat, newFormat) -> {
                    if (newFormat != null && getItem() != null) {
                        String currentBitrate = bitrateBox.getValue();
                        ObservableList<String> newBitrateOptions = getBitrateOptionsForFormat(newFormat);
                        bitrateBox.setItems(newBitrateOptions);
                        if (currentBitrate != null && newBitrateOptions.contains(currentBitrate)) {
                            bitrateBox.setValue(currentBitrate);
                        } else {
                            bitrateBox.getSelectionModel().selectFirst();
                        }

                        String currentSampleRate = sampleRateBox.getValue();
                        ObservableList<String> newSampleRateOptions = getSampleRateOptionsForFormat(newFormat);
                        sampleRateBox.setItems(newSampleRateOptions);
                        if (currentSampleRate != null && newSampleRateOptions.contains(currentSampleRate)) {
                            sampleRateBox.setValue(currentSampleRate);
                        } else {
                            sampleRateBox.getSelectionModel().selectFirst();
                        }
                        updateSettings(fs -> fs.format.set(newFormat));
                    }
                });
                bitrateBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSettings(fs -> fs.bitrate.set(newVal)));
                sampleRateBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSettings(fs -> fs.sampleRate.set(newVal)));
                channelsBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSettings(fs -> fs.channels.set(newVal)));
            }

            private void updateSettings(java.util.function.Consumer<FileSettings> updater) {
                if (getItem() != null) {
                    FileSettings settings = fileSettingsMap.get(getItem());
                    if (settings != null) {
                        updater.accept(settings);
                    }
                }
            }

            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setGraphic(null);
                } else {
                    label.setText(file.getName());
                    FileSettings settings = fileSettingsMap.get(file);
                    String currentFormat = settings.format.get();

                    bitrateBox.setItems(getBitrateOptionsForFormat(currentFormat));
                    sampleRateBox.setItems(getSampleRateOptionsForFormat(currentFormat));

                    bitrateBox.setValue(settings.bitrate.get());
                    sampleRateBox.setValue(settings.sampleRate.get());
                    channelsBox.setValue(settings.channels.get());

                    formatBox.setValue(currentFormat);

                    setGraphic(hbox);
                }
            }
        });

        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        fileListView.setOnDragOver(event -> {
            if (event.getGestureSource() != fileListView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        fileListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                addFilesToList(db.getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void addFilesToList(List<File> files) {
        for (File file : files) {
            if (fileSettingsMap.containsKey(file)) {
                fileListView.getItems().remove(file);
                fileSettingsMap.remove(file);
            }

            FileSettings settings = new FileSettings();
            settings.format.set(defaultFormatComboBox.getValue());
            settings.bitrate.set(defaultBitrateComboBox.getValue());
            settings.sampleRate.set(defaultSampleRateComboBox.getValue());
            settings.channels.set(defaultChannelsComboBox.getValue());

            fileSettingsMap.put(file, settings);
            fileListView.getItems().add(file);
        }
    }

    @FXML
    protected void handleAddFilesAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Audio Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.flac", "*.m4a"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(addFilesButton.getScene().getWindow());
        if (selectedFiles != null) {
            addFilesToList(selectedFiles);
        }
    }

    @FXML
    protected void handleClearAllAction() {
        fileListView.getItems().clear();
        fileSettingsMap.clear();
    }

    private int parseBitrateValue(String bitrateString) {
        if (bitrateString == null || bitrateString.isEmpty()) return 0;

        if (bitrateString.contains("Lossless")) {
            return 0;
        }

        String numeric = bitrateString.replaceAll("[^\\d]", "");
        if (numeric.isEmpty()) return 0;

        int value = Integer.parseInt(numeric);
        if (bitrateString.contains("kbps")) {
            value *= 1000;
        }
        return value;
    }

    private int parseSampleRateValue(String sampleRateString) {
        if (sampleRateString == null || sampleRateString.isEmpty()) return 0;

        String numeric = sampleRateString.replaceAll("[^\\d]", "");
        if (numeric.isEmpty()) return 0;

        return Integer.parseInt(numeric);
    }

    private boolean isValidFormatSettings(String formatName, int sampleRate, int bitrate) {
        switch (formatName) {
            case "mp3":
                boolean isValidMp3SampleRate = (sampleRate == 32000 || sampleRate == 44100 || sampleRate == 48000);
                boolean isValidMp3Bitrate = (bitrate >= 64000 && bitrate <= 320000);
                System.out.println("MP3 Validation: " + isValidMp3SampleRate + " " + isValidMp3Bitrate);
                return isValidMp3SampleRate && isValidMp3Bitrate;

            case "wav":
                boolean isValidWavSampleRate = (sampleRate == 44100 || sampleRate == 48000 || sampleRate == 96000);
                return isValidWavSampleRate;

            case "ipod":
                boolean isValidAacSampleRate = (sampleRate == 44100 || sampleRate == 48000);
                boolean isValidAacBitrate = (bitrate >= 96000 && bitrate <= 256000);
                return isValidAacSampleRate && isValidAacBitrate;

            case "flac":
                boolean isValidFlacSampleRate = (sampleRate == 44100 || sampleRate == 48000 || sampleRate == 96000 || sampleRate == 192000);
                return isValidFlacSampleRate;

            default:
                return false;
        }
    }

    private String mapFormatForConverter(String uiFormat) {
        switch (uiFormat.toUpperCase()) {
            case "M4A": return "ipod";
            case "MP3": return "mp3";
            case "WAV": return "wav";
            case "FLAC": return "flac";
            default: return uiFormat.toLowerCase();
        }
    }

    @FXML
    protected void handleConvertButtonAction() {
        if (fileListView.getItems().isEmpty()) {
            showErrorAlert("No Files", "Please add files to convert.");
            return;
        }

        Stage loadingStage = new Stage();
        LoadingController loadingController;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("loading-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("loading-view.css").toExternalForm());

            loadingStage.setScene(scene);
            loadingStage.initStyle(StageStyle.UTILITY);
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.setTitle("Converting...");
            loadingStage.setResizable(false);

            loadingController = loader.getController();
            loadingController.setTitle("⏳ Converting files... Please wait.");
            loadingController.startSpin();
            loadingController.startShake();
            loadingStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final LoadingController finalLoadingController = loadingController;
        AudioConverter converter = new AudioConverter();

        new Thread(() -> {
            try {
                int totalFiles = fileListView.getItems().size();
                for (int i = 0; i < totalFiles; i++) {
                    File file = fileListView.getItems().get(i);
                    final int currentFileIndex = i;

                    Platform.runLater(() -> {
                        finalLoadingController.updateStatus("Converting: " + file.getName() + " (" + (currentFileIndex + 1) + "/" + totalFiles + ")");
                        finalLoadingController.updateProgress((double) currentFileIndex / totalFiles);
                    });

                    FileSettings settings = fileSettingsMap.get(file);
                    String formatStr = settings.format.get();
                    int formatIndex = formatOptions.indexOf(formatStr);
                    int bitrateValue = parseBitrateValue(settings.bitrate.get());
                    int sampleRateValue = parseSampleRateValue(settings.sampleRate.get());
                    int channelIndex = channelOptions.indexOf(settings.channels.get());
                    String formatForValidation = mapFormatForConverter(formatStr);

                    if (!isValidFormatSettings(formatForValidation, sampleRateValue, bitrateValue)) {
                        Platform.runLater(() -> {
                            finalLoadingController.closeWindow();
                            showErrorAlert("Invalid Settings", "Invalid settings for file: " + file.getName());
                        });
                        return;
                    }
                    converter.convert(file, formatIndex, bitrateValue, sampleRateValue, channelIndex);
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    finalLoadingController.closeWindow();
                    showErrorAlert("Conversion Error", "An unexpected error occurred during the batch conversion.");
                });
                e.printStackTrace();
                return;
            }

            Platform.runLater(() -> {
                finalLoadingController.updateProgress(1.0);
                finalLoadingController.updateStatus("✅ Conversion completed!");
                finalLoadingController.stopSpin();
                finalLoadingController.stopShake();

                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(event -> {
                    finalLoadingController.closeWindow();

                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("🎉 Success!");
                        alert.setHeaderText("All files have been successfully converted.");
                        alert.setContentText("Thank you for using our Audio Converter!");
                        alert.showAndWait();
                    });
                });
                delay.play();

            });
        }).start();
    }

    private void showErrorAlert(String fileName, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Conversion Error");
        alert.setHeaderText("Failed to convert: " + fileName);
        alert.setContentText("Reason: " + message);
        alert.showAndWait();
    }

    @FXML
    protected void handleYoutubeDownloadAction() {
        String userUrl = youtubeUrlField.getText();
        if (userUrl == null || userUrl.trim().isEmpty()) {
            showErrorAlert("Invalid URL", "Please enter a YouTube URL.");
            return;
        }

        youtubeDownloadButton.setDisable(true);
        youtubeDownloadButton.setText("Processing...");

        new Thread(() -> {
            try {
                String videoId = extractVideoId(userUrl);
                if (videoId == null) {
                    Platform.runLater(() -> showErrorAlert("Error", "Could not find a valid video ID in the URL."));
                    return;
                }

                String mp3Url = fetchMp3LinkFromApi(videoId);
                if (mp3Url == null) {
                    Platform.runLater(() -> showErrorAlert("API Error", "Failed to get the MP3 link from the API."));
                    return;
                }

                final Stage[] loadingStage = new Stage[1];

                Platform.runLater(() -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save MP3 File");
                    fileChooser.setInitialFileName(videoId + ".mp3");
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Audio", "*.mp3"));
                    File file = fileChooser.showSaveDialog(youtubePane.getScene().getWindow());

                    if (file != null) {
                        loadingStage[0] = new Stage();
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("loading-view.fxml"));
                        try {
                            Scene scene = new Scene(loader.load());
                            scene.getStylesheets().add(getClass().getResource("loading-view.css").toExternalForm());
                            loadingStage[0].setScene(scene);
                            loadingStage[0].initStyle(StageStyle.UTILITY);
                            loadingStage[0].initModality(Modality.APPLICATION_MODAL);
                            loadingStage[0].setTitle("Loading...");
                            loadingStage[0].setResizable(false);
                            loadingStage[0].show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        new Thread(() -> {
                            boolean success = downloadMp3(mp3Url, file.getAbsolutePath());
                            Platform.runLater(() -> {
                                if (loadingStage[0] != null) loadingStage[0].close();
                                if (success) {
                                    showSuccessAlert("Download Complete", "Successfully downloaded MP3 to:\n" + file.getAbsolutePath());
                                } else {
                                    showErrorAlert("Download Failed", "An error occurred while downloading the file.");
                                }
                            });
                        }).start();
                    }
                });
            } finally {
                Platform.runLater(() -> {
                    youtubeDownloadButton.setDisable(false);
                    youtubeDownloadButton.setText("Download MP3");
                });
            }
        }).start();
    }
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}