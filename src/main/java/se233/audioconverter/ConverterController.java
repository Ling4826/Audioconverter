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
import java.util.*;

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
    private final ObservableList<String> sampleRateOptions = FXCollections.observableArrayList("32000 Hz","44100 Hz", "48000 Hz", "96000 Hz");
    private final ObservableList<String> channelOptions = FXCollections.observableArrayList("Mono", "Stereo");

    // Define valid bitrates per format
    private final Map<String, List<String>> validBitrates = Map.of(
            "MP3", Arrays.asList("64 kbps", "128 kbps", "192 kbps", "320 kbps"),
            "WAV", Collections.emptyList(),
            "M4A", Arrays.asList("96 kbps", "128 kbps", "192 kbps", "256 kbps"),
            "FLAC", Collections.emptyList()
    );

    // Define valid sample rates per format
    private final Map<String, List<String>> validSampleRates = Map.of(
            "MP3", Arrays.asList("32000", "44100 Hz", "48000 Hz"),
            "WAV", Arrays.asList("44100 Hz", "48000 Hz", "96000 Hz"),
            "M4A", Arrays.asList("44100 Hz", "48000 Hz"),
            "FLAC", Arrays.asList("44100 Hz", "48000 Hz", "96000 Hz", "192000 Hz")
    );

    @FXML private ListView<File> fileListView;
    @FXML private Button convertButton;
    @FXML private ComboBox<String> defaultFormatComboBox;
    @FXML private ComboBox<String> defaultBitrateComboBox;
    @FXML private ComboBox<String> defaultSampleRateComboBox;
    @FXML private ComboBox<String> defaultChannelsComboBox;

    @FXML
    public void initialize() {
        defaultFormatComboBox.setItems(formatOptions);
        defaultChannelsComboBox.setItems(channelOptions);
        // Instead of setting fixed bitrate/sampleRate, update based on format
        updateDependentOptions("MP3", defaultBitrateComboBox, defaultSampleRateComboBox);

        defaultFormatComboBox.getSelectionModel().selectFirst();
        defaultChannelsComboBox.getSelectionModel().select(1);
        // Listen to format changes for defaults
        defaultFormatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateDependentOptions(newVal, defaultBitrateComboBox, defaultSampleRateComboBox);
        });

        fileListView.setCellFactory(listView -> new ListCell<File>() {
            private final HBox hbox = new HBox(10);
            private final Label label = new Label();
            private final ComboBox<String> formatBox = new ComboBox<>(formatOptions);
            private final ComboBox<String> bitrateBox = new ComboBox<>(bitrateOptions);
            private final ComboBox<String> sampleRateBox = new ComboBox<>(sampleRateOptions);
            private final ComboBox<String> channelsBox = new ComboBox<>(channelOptions);

            {
                HBox.setHgrow(label, Priority.ALWAYS);
                label.setMaxWidth(Double.MAX_VALUE);
                hbox.getChildren().addAll(label, formatBox, bitrateBox, sampleRateBox, channelsBox);

                // formatBox updates dependent options
                formatBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    updateDependentOptions(newVal, bitrateBox, sampleRateBox);
                    updateSettings(fs -> fs.format.set(newVal));
                });
                bitrateBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSettings(fs -> fs.bitrate.set(newVal)));
                sampleRateBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSettings(fs -> fs.sampleRate.set(newVal)));
                channelsBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSettings(fs -> fs.channels.set(newVal)));
            }

            // Helper method to update the settings for the current item
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

                    // make sure per-row bitrate/sampleRate match format
                    updateDependentOptions(settings.format.get(), bitrateBox, sampleRateBox);

                    // --- KEY CHANGE: Manually set the value, don't bind ---
                    formatBox.setValue(settings.format.get());
                    bitrateBox.setValue(settings.bitrate.get());
                    sampleRateBox.setValue(settings.sampleRate.get());
                    channelsBox.setValue(settings.channels.get());

                    setGraphic(hbox);
                }
            }
        });

        setupDragAndDrop();
    }

    // helper to update options based on selected format
    private void updateDependentOptions(String format, ComboBox<String> bitrateBox, ComboBox<String> sampleRateBox) {
        // Bitrates
        List<String> bitrates = validBitrates.getOrDefault(format, Collections.emptyList());
        bitrateBox.setItems(FXCollections.observableArrayList(bitrates));
        if (!bitrates.isEmpty()) {
            bitrateBox.getSelectionModel().selectFirst();
        } else {
            bitrateBox.getSelectionModel().clearSelection();
        }

        // Sample rates
        List<String> rates = validSampleRates.getOrDefault(format, Collections.emptyList());
        sampleRateBox.setItems(FXCollections.observableArrayList(rates));
        if (!rates.isEmpty()) {
            sampleRateBox.getSelectionModel().selectFirst();
        } else {
            sampleRateBox.getSelectionModel().clearSelection();
        }
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

    @FXML
    protected void handleConvertButtonAction() {
        if (fileListView.getItems().isEmpty()) {
            System.out.println("Please drop files first!");
            return;
        }

        // 1. Create the AudioConverter instance before the loop
        AudioConverter converter = new AudioConverter();
        System.out.println("--- Starting Conversion Batch ---");

        // Run on a separate thread to keep the UI responsive
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
                    // This catches any other unexpected errors during conversion
                    Platform.runLater(() -> showErrorAlert(file.getName(), "An unexpected error occurred during conversion."));
                    e.printStackTrace(); // It's good to print the full error to the console
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