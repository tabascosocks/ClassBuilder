package com.edumentic.classbuilder.view;

import com.edumentic.classbuilder.viewmodel.ApplicationViewModel;
import com.edumentic.classbuilder.viewmodel.DatafileParseException;
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
        datafileSummaryLabel.textProperty().bind(viewModel.datafileSummaryProperty());

        loadDatafileInstructionsLabel.visibleProperty().bind(viewModel.dataIsLoadedProperty().not());
        datafileInfoHBox.visibleProperty().bind(viewModel.dataIsLoadedProperty());
        startSolverButton.disableProperty().bind(viewModel.dataIsLoadedProperty().not());

        viewModel.runningSolverProperty().addListener((prop, oldV, newV) -> {
            if(newV){
                startSolverButton.setText("Stop");
                startSolverButton.setDefaultButton(false);
            }else{
                startSolverButton.setText("Start");
                startSolverButton.setDefaultButton(true);
            }
        });

        solverContentSplitPane.disableProperty().bind(viewModel.runningSolverProperty().not());
        solutionHistoryList.itemsProperty().bind(viewModel.solutionsProperty());
        viewModel.currentSolutionProperty().bind(solutionHistoryList.getSelectionModel().selectedItemProperty());

        viewModel.currentSolutionProperty().addListener((prop, oldV, newV) -> {
            if(newV == null){
                currentSolutinHardScoreLabel.setText("-");
                currentsolutionSoftScoreLabel.setText("-");
                selectedSolutionReportWebView.getEngine().loadContent("");
            }else{
                currentSolutinHardScoreLabel.setText(String.valueOf(newV.getHardScore()));
                currentsolutionSoftScoreLabel.setText(String.valueOf(newV.getSoftScore()));
                // Read CSS contents from resources
                String css1 = "";
                String css2 = "";
                try {
                    css1 = new String(getClass().getResourceAsStream("/css/classbuilder-report.css").readAllBytes());
                    css2 = new String(getClass().getResourceAsStream("/css/scoring-report.css").readAllBytes());
                } catch (Exception e) {
                    // optionally handle error or log
                }
                // Wrap the solution HTML report with <html> and <style>
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

        viewModel.solutionsProperty().addListener((ListChangeListener<? super ApplicationViewModel.ClassSolutionData>) (evt) -> {
              if(! evt.getList().isEmpty()){
                  solutionHistoryList.getSelectionModel().select(0);
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

