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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

    private ObservableList<String> getBitrateOptionsForFormat(String format) {
        ObservableList<String> options = FXCollections.observableArrayList();
        switch (format) {
            case "MP3":
                options.addAll("96 kbps", "128 kbps", "192 kbps", "256 kbps", "320 kbps");
                break;
            case "WAV":
                options.addAll("16-bit", "24-bit", "32-bit");
                break;
            case "M4A":
                options.addAll("96 kbps", "128 kbps", "192 kbps", "256 kbps");
                break;
            case "FLAC":
                options.addAll("Lossless (Level 5)", "Lossless (Level 8)");
                break;
        }
        return options;
    }

    private ObservableList<String> getSampleRateOptionsForFormat(String format) {
        ObservableList<String> options = FXCollections.observableArrayList();
        switch (format) {
            case "MP3":
                options.addAll("32000 Hz", "44100 Hz", "48000 Hz");
                break;
            case "M4A":
                options.addAll("44100 Hz", "48000 Hz");
                break;
            case "WAV":
                options.addAll("44100 Hz", "48000 Hz", "88200 Hz", "96000 Hz");
                break;
            case "FLAC":
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
            // ลบไฟล์เดิมออกก่อน (ถ้ามี)
            if (fileSettingsMap.containsKey(file)) {
                fileListView.getItems().remove(file); // ลบจาก ListView
                fileSettingsMap.remove(file);         // ลบจาก Map
            }

            // สร้าง FileSettings ใหม่ด้วยค่า Default ล่าสุด
            FileSettings settings = new FileSettings();
            settings.format.set(defaultFormatComboBox.getValue());
            settings.bitrate.set(defaultBitrateComboBox.getValue());
            settings.sampleRate.set(defaultSampleRateComboBox.getValue());
            settings.channels.set(defaultChannelsComboBox.getValue());

            // เพิ่มกลับเข้าไป
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

    private int parseValue(String stringValue) {
        if (stringValue == null || stringValue.isEmpty()) {
            return 0;
        }
        String numericString = stringValue.replaceAll("[^\\d]", "");
        if (numericString.isEmpty()) {
            return 0;
        }
        int value = Integer.parseInt(numericString);
        if (stringValue.contains("kbps")) {
            value *= 1000;
        }
        return value;
    }

    @FXML
    protected void handleConvertButtonAction() {
        if (fileListView.getItems().isEmpty()) {
            System.out.println("Please drop files first!");
            return;
        }


        Stage loadingStage = new Stage();
        LoadingController loadingController = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("loading-view.fxml"));
            Scene scene = new Scene(loader.load());

            loadingStage.setScene(scene);
            loadingStage.initStyle(StageStyle.UTILITY);
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.setTitle("Processing...");
            loadingStage.setResizable(false);

            loadingController = loader.getController();
            loadingStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        AudioConverter converter = new AudioConverter();
        System.out.println("--- Starting Conversion Batch ---");

        final LoadingController finalLoadingController = loadingController;

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
                    // --------------------------------

                    FileSettings settings = fileSettingsMap.get(file);
                    String formatStr = settings.format.get();
                    int formatIndex = formatOptions.indexOf(formatStr);
                    int bitrateValue = parseValue(settings.bitrate.get());
                    int sampleRateValue = parseValue(settings.sampleRate.get());
                    int channelIndex = channelOptions.indexOf(settings.channels.get());

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
                finalLoadingController.updateStatus("Conversion complete!");


                PauseTransition delay = new PauseTransition(Duration.seconds(1));
                delay.setOnFinished(event -> {
                    finalLoadingController.closeWindow();


                    Platform.runLater(() -> {
                        System.out.println("--- All Conversions Finished ---");
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Conversion Complete");
                        alert.setHeaderText("All tasks have been processed.");
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

}