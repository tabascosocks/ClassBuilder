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

import java.io.IOException;

public class ApplicationView extends VBox {

    private final ApplicationViewModel viewModel;

    @FXML
    private HBox datafileSectionHBox;
    @FXML private Label loadDatafileInstructionsLabel;
    @FXML private HBox datafileInfoHBox;
    @FXML private Label datafileNameLabel;
    @FXML private Button refreshFromDatafileButton;
    @FXML private Button clearDatafileButton;
    @FXML private Label datafileSummaryLabel;
    @FXML private Label datafileErrorsLabel;
    @FXML private HBox solutionScoreDisplayHBox;

    @FXML private Button startSolverButton;

    @FXML private ListView<ApplicationViewModel.ClassSolutionData> solutionHistoryList;
    @FXML private Button clearSolutionHistoryButton;
    @FXML private Button exportSelectedSolutionButton;

    @FXML private SplitPane solverContentSplitPane;
    @FXML private Label currentSolutinHardScoreLabel;
    @FXML private Label currentsolutionSoftScoreLabel;

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
        // Bind the datafile summary label's text property to the ViewModel's datafile summary property.
        datafileSummaryLabel.textProperty().bind(viewModel.datafileSummaryProperty());

        // Show load instructions only when no data is loaded.
        loadDatafileInstructionsLabel.visibleProperty().bind(viewModel.dataIsLoadedProperty().not());
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
            }else{
                startSolverButton.setText("Start");
                startSolverButton.setDefaultButton(true);
                startSolverButton.setCancelButton(false);
            }
        });

        // Bind the solution history list to the solutions property in the ViewModel.
        solutionHistoryList.itemsProperty().bind(viewModel.solutionsProperty());
        // Bind the ViewModel's currentSolution to the selected item in the solution history list.
        viewModel.currentSolutionProperty().bind(solutionHistoryList.getSelectionModel().selectedItemProperty());

        // Listen for changes in the current solution selection to update score labels and HTML preview.
        viewModel.currentSolutionProperty().addListener((prop, oldV, newV) -> {
            if(newV == null){
                // When nothing is selected, show dashes and clear the webview.
                currentSolutinHardScoreLabel.setText("-");
                currentsolutionSoftScoreLabel.setText("-");
                selectedSolutionReportWebView.getEngine().loadContent("");
            }else{
                // Show the scores of the selected solution.
                currentSolutinHardScoreLabel.setText(String.valueOf(newV.getHardScore()));
                currentsolutionSoftScoreLabel.setText(String.valueOf(newV.getSoftScore()));
                //adjust background color
                if(newV.getHardScore() < 0) {
                    solutionScoreDisplayHBox.setStyle("-fx-background-color: #f29197");
                }else{
                    solutionScoreDisplayHBox.setStyle("-fx-background-color: #333333");
                }

                // Read required CSS files from resources for the report.
                String css1 = "";
                String css2 = "";
                try {
                    css1 = new String(getClass().getResourceAsStream("/css/classbuilder-report.css").readAllBytes());
                    css2 = new String(getClass().getResourceAsStream("/css/scoring-report.css").readAllBytes());
                } catch (Exception e) {
                    // Swallow errors or log if necessary.
                }
                // Compose the HTML with embedded styles and load into webview.
                String html = """
                    <html>
                    <head>
                    <style>%s</style>
                    <style>%s</style>
                    </head>
                    <body>
                    %s
                    </body>
                    </html>
                    """.formatted(css1, css2, newV.getSolutionReportHtml());
                selectedSolutionReportWebView.getEngine().loadContent(html);
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
        // TODO: Implement export logic
    }


}

