package se233.audioconverter;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import se233.audioconverter.Converter.AudioConverter;
import java.io.File;
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
    private final ObservableList<String> bitrateOptions = FXCollections.observableArrayList("64 kbps", "128 kbps", "192 kbps", "320 kbps");
    private final ObservableList<String> sampleRateOptions = FXCollections.observableArrayList("32000","44100 Hz", "48000 Hz", "96000 Hz");
    private final ObservableList<String> channelOptions = FXCollections.observableArrayList("Mono", "Stereo");

    @FXML private ListView<File> fileListView;
    @FXML private Button convertButton;
    @FXML private ComboBox<String> defaultFormatComboBox;
    @FXML private ComboBox<String> defaultBitrateComboBox;
    @FXML private ComboBox<String> defaultSampleRateComboBox;
    @FXML private ComboBox<String> defaultChannelsComboBox;

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
                options.addAll("64 kbps", "96 kbps", "128 kbps", "192 kbps", "256 kbps");
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
            case "M4A":
                options.addAll("32000 Hz", "44100 Hz", "48000 Hz");
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
            private final HBox hbox = new HBox(10);
            private final Label label = new Label();
            private final ComboBox<String> formatBox = new ComboBox<>(formatOptions);
            private final ComboBox<String> bitrateBox = new ComboBox<>();
            private final ComboBox<String> sampleRateBox = new ComboBox<>();
            private final ComboBox<String> channelsBox = new ComboBox<>(channelOptions);

            {
                HBox.setHgrow(label, Priority.ALWAYS);
                label.setMaxWidth(Double.MAX_VALUE);
                hbox.getChildren().addAll(label, formatBox, bitrateBox, sampleRateBox, channelsBox);

                formatBox.valueProperty().addListener((obs, oldFormat, newFormat) -> {
                    if (newFormat != null && getItem() != null) {
                        bitrateBox.setItems(getBitrateOptionsForFormat(newFormat));
                        sampleRateBox.setItems(getSampleRateOptionsForFormat(newFormat));


                        bitrateBox.getSelectionModel().selectFirst();
                        sampleRateBox.getSelectionModel().selectFirst();

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

                    formatBox.setValue(currentFormat);
                    bitrateBox.setValue(settings.bitrate.get());
                    sampleRateBox.setValue(settings.sampleRate.get());
                    channelsBox.setValue(settings.channels.get());

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
                fileListView.getItems().clear();
                fileSettingsMap.clear();

                List<File> files = db.getFiles();

                for (File file : files) {
                    FileSettings settings = new FileSettings();
                    settings.format.set(defaultFormatComboBox.getValue());
                    settings.bitrate.set(defaultBitrateComboBox.getValue());
                    settings.sampleRate.set(defaultSampleRateComboBox.getValue());
                    settings.channels.set(defaultChannelsComboBox.getValue());

                    fileSettingsMap.put(file, settings);
                    fileListView.getItems().add(file);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
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

        AudioConverter converter = new AudioConverter();
        System.out.println("--- Starting Conversion Batch ---");


        new Thread(() -> {
            for (File file : fileListView.getItems()) {
                try {

                    FileSettings settings = fileSettingsMap.get(file);
                    String formatStr = settings.format.get();
                    String bitrateStr = settings.bitrate.get();
                    String sampleRateStr = settings.sampleRate.get();
                    String channelsStr = settings.channels.get();

                    int formatIndex = formatOptions.indexOf(formatStr);
                    int bitrateIndex = bitrateOptions.indexOf(bitrateStr);
                    int sampleRateIndex = sampleRateOptions.indexOf(sampleRateStr);
                    int channelIndex = channelOptions.indexOf(channelsStr);

                    converter.convert(file, formatIndex, bitrateIndex, sampleRateIndex, channelIndex);

                } catch (IllegalArgumentException e) {
                    // This catches validation errors from the converter
                    Platform.runLater(() -> showErrorAlert(file.getName(), e.getMessage()));
                } catch (Exception e) {
                    Platform.runLater(() -> showErrorAlert(file.getName(), "An unexpected error occurred during conversion."));
                    e.printStackTrace();
                }
            }

            Platform.runLater(() -> {
                System.out.println("--- All Conversions Finished ---");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Conversion Complete");
                alert.setHeaderText("All tasks have been processed.");
                alert.showAndWait();
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