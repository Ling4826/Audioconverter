module se233.audioconverter {
    requires javafx.controls;
    requires javafx.fxml;
    requires jave.core;


    opens se233.audioconverter to javafx.fxml;
    exports se233.audioconverter;
}