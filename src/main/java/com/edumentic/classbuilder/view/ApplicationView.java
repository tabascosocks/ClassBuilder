package com.edumentic.classbuilder.view;

import com.edumentic.classbuilder.viewmodel.ApplicationViewModel;
import com.edumentic.classbuilder.viewmodel.DatafileParseException;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

public class ApplicationView extends VBox {

    private final ApplicationViewModel viewModel;

    @FXML
    private HBox datafileSectionHBox;
    @FXML private Label loadDatafileInstructionsLabel;
    @FXML private HBox loadDatafileInstructionsHBox;
    @FXML private HBox datafileInfoHBox;
    @FXML private Label datafileNameLabel;
    @FXML private Button refreshFromDatafileButton;
    @FXML private Button clearDatafileButton;
    @FXML private Label datafileSummaryLabel;
    @FXML private Label datafileErrorsLabel;
    @FXML private HBox solutionScoreDisplayHBox;
    @FXML private FontIcon startButtonFontIcon;

    @FXML private Button startSolverButton;

    @FXML private CheckBox enableMustIncludeOthersCheckbox;
    @FXML private CheckBox enableShouldIncludeOthersCheckbox;
    @FXML private CheckBox enableMustAvoidOthersCheckbox;
    @FXML private CheckBox enableShouldAvoidOthersCheckbox;
    @FXML private CheckBox enableBalanceNumeracyCheckbox;
    @FXML private CheckBox enableBalanceLiteracyCheckbox;
    @FXML private CheckBox enableBalanceSocialEmotionalCheckbox;
    @FXML private CheckBox enableBalanceGenderCheckbox;
    @FXML private Spinner<Integer> minClassSizeSpinner;
    @FXML private Spinner<Integer> maxClassSizeSpinner;
    @FXML private Slider classBalanceSensitivitySlider;

    @FXML private ListView<ApplicationViewModel.ClassSolutionData> solutionHistoryList;
    @FXML private Button clearSolutionHistoryButton;
    @FXML private Button exportSelectedSolutionButton;

    @FXML private SplitPane solverContentSplitPane;
    @FXML private Label scoreLabel;

    @FXML private WebView selectedSolutionReportWebView;

    public ApplicationView(ApplicationViewModel viewModel) {
        this.viewModel = viewModel;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ApplicationView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    private void initialize() {
        minClassSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100));
        minClassSizeSpinner.getValueFactory().setValue(viewModel.minClassSizeProperty().getValue());
        viewModel.minClassSizeProperty().bind(minClassSizeSpinner.valueProperty());

        maxClassSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100));
        maxClassSizeSpinner.getValueFactory().setValue(viewModel.maxClassSizeProperty().getValue());
        viewModel.maxClassSizeProperty().bind(maxClassSizeSpinner.valueProperty());

        // Bind the datafile summary label's text property to the ViewModel's datafile summary property.
        datafileSummaryLabel.textProperty().bind(viewModel.datafileSummaryProperty());

        // Show load instructions only when no data is loaded.
        loadDatafileInstructionsHBox.visibleProperty().bind(viewModel.dataIsLoadedProperty().not());
        // Show the datafile info section only when data is loaded.
        datafileInfoHBox.visibleProperty().bind(viewModel.dataIsLoadedProperty());
        // Enable the start solver button only when data is loaded.
        startSolverButton.disableProperty().bind(viewModel.dataIsLoadedProperty().not());

        // Update Start/Stop button text and default status depending on solver running state.
        viewModel.runningSolverProperty().addListener((prop, oldV, newV) -> {
            if(newV){
                startSolverButton.setText("Stop");
                startSolverButton.setDefaultButton(false);
                startSolverButton.setCancelButton(true);
                startButtonFontIcon.setIconLiteral("mdoal-cancel");

            }else{
                startSolverButton.setText("Start");
                startSolverButton.setDefaultButton(true);
                startSolverButton.setCancelButton(false);
                startButtonFontIcon.setIconLiteral("mdrmz-play_circle_outline");
            }
        });

        enableMustIncludeOthersCheckbox.selectedProperty().bindBidirectional(viewModel.mustIncludeOthersProperty());
        enableShouldIncludeOthersCheckbox.selectedProperty().bindBidirectional(viewModel.shouldIncludeOthersProperty());
        enableMustAvoidOthersCheckbox.selectedProperty().bindBidirectional(viewModel.mustAvoidOthersProperty());
        enableShouldAvoidOthersCheckbox.selectedProperty().bindBidirectional(viewModel.shouldAvoidOthersProperty());
        enableBalanceNumeracyCheckbox.selectedProperty().bindBidirectional(viewModel.balanceNumeracyProperty());
        enableBalanceLiteracyCheckbox.selectedProperty().bindBidirectional(viewModel.balanceLiteracyProperty());
        enableBalanceSocialEmotionalCheckbox.selectedProperty().bindBidirectional(viewModel.balanceSocialEmotionalProperty());
        enableBalanceGenderCheckbox.selectedProperty().bindBidirectional(viewModel.balanceGenderProperty());

        classBalanceSensitivitySlider.valueProperty().bindBidirectional(viewModel.classMetricVarianceSensitivityProperty());


        // Bind the solution history list to the solutions property in the ViewModel.
        solutionHistoryList.itemsProperty().bind(viewModel.solutionsProperty());
        // Bind the ViewModel's currentSolution to the selected item in the solution history list.
        viewModel.currentSolutionProperty().bind(solutionHistoryList.getSelectionModel().selectedItemProperty());

        // Listen for changes in the current solution selection to update score labels and HTML preview.
        viewModel.currentSolutionProperty().addListener((prop, oldV, newV) -> {
            if(newV == null){
                // When nothing is selected, show dashes and clear the webview.
                scoreLabel.setText("-");
                selectedSolutionReportWebView.getEngine().loadContent("");
            }else{
                // Show the score of the seelcted solution as either a penalty of the ahrd scontraint or a score of the soft
                scoreLabel.setText(String.valueOf(newV.getHardScore() < 0 ? newV.getHardScore() : newV.getSoftScore()));
                //adjust background color
                if(newV.getHardScore() < 0) {
                    solutionScoreDisplayHBox.setStyle("-fx-background-color: #f29197");
                }else{
                    solutionScoreDisplayHBox.setStyle("-fx-background-color: #a5f7ad");
                }

                selectedSolutionReportWebView.getEngine().loadContent(viewModel.getCurrentSolutionReportHtml());
            }
        });

        // Ensure the latest element in solution history list is auto-selected when added.
        solutionHistoryList.getItems().addListener((ListChangeListener<? super ApplicationViewModel.ClassSolutionData>) evt -> {
            while(evt.next()) {
                if (evt.wasAdded() && !solutionHistoryList.getItems().isEmpty()) {
                    javafx.application.Platform.runLater(() ->
                        solutionHistoryList.getSelectionModel().selectFirst()
                    );
                }
            }
        });

    }

    @FXML
    private void browseUploadSpreadsheetButtonClicked(){
        if(viewModel.runningSolverProperty().get()) {
            // Prevent file selection while solver is running
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel Spreadsheet");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (selectedFile != null && selectedFile.canRead()) {
            try {
                datafileNameLabel.setText(selectedFile.getName());
                viewModel.loadDatafile(selectedFile);
            } catch (DatafileParseException e) {
                datafileErrorsLabel.setText(e.getMessage());
            }
        }
    }

    @FXML
    private void onDatafileSectionHBoxDragDropped(javafx.scene.input.DragEvent event) {
        if(viewModel.runningSolverProperty().get()) {
            event.setDropCompleted(false);
            event.consume();
            return;
        }

        var dragboard = event.getDragboard();
        boolean success = false;
        if (dragboard.hasFiles() && !dragboard.getFiles().isEmpty()) {
            var file = dragboard.getFiles().getFirst();
            if(file.getName().toLowerCase().endsWith(".xlsx") && file.canRead()) {
                try {
                    datafileNameLabel.setText(file.getName());
                    viewModel.loadDatafile(file);
                } catch (DatafileParseException e) {
                    datafileErrorsLabel.setText(e.getMessage());
                }
                success = true;
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void onDatafileSectionHBoxDragOver(javafx.scene.input.DragEvent event) {
        if(viewModel.runningSolverProperty().get()) {
            event.consume();
            return;
        }

        var dragboard = event.getDragboard();
        if (dragboard.hasFiles() && dragboard.getFiles().stream()
                .anyMatch(f -> f.getName().toLowerCase().endsWith(".xlsx"))) {
            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
        } else {
            event.consume();
        }
    }
    @FXML
    private void refreshFromDatafileButtonClicked() {
        try {
            viewModel.loadDatafile();
        } catch (DatafileParseException e) {
            datafileErrorsLabel.setText(e.getMessage());
        }
    }
    @FXML
    private void onClearDatafileButtonClicked() {
        viewModel.clearDataFile();
    }
    @FXML
    private void onStartSolverButtonClicked() {
        if(viewModel.runningSolverProperty().get()){
            viewModel.stopSolver();
        }else{
            viewModel.runSolver();
        }

    }
    @FXML
    private void onClearSolutionHistoryButton() {
        viewModel.clearAllSolutions();
    }
    @FXML
    private void onExportSelectedSolutionButton() {
        ApplicationViewModel.ClassSolutionData selected = viewModel.currentSolutionProperty().get();
        if (selected == null) {
            // No solution is selected, ignore or inform user as desired.
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Solution Report as HTML");

        // Create sensible default filename using title and timestamp.
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(java.time.LocalDateTime.now());
        String defaultTitle = "classbuilder_solution_" + timestamp + ".html";
        fileChooser.setInitialFileName(defaultTitle);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files (*.html)", "*.html"));

        java.io.File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file != null) {
            String html = viewModel.getCurrentSolutionReportHtml();
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write(html);
            } catch (IOException e) {
                // Optionally show an error dialog if saving fails
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save file: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }


}

