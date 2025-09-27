package com.edumentic.classbuilder;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import com.edumentic.classbuilder.io.StdInMonitor;
import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import com.edumentic.classbuilder.solution.ClassBuilderGlobalConstraints;
import com.edumentic.classbuilder.solution.ClassBuilderSolution;
import com.edumentic.classbuilder.solution.SolutionScoreCalculator;
import com.edumentic.classbuilder.solution.StudentClassAssignment;
import com.edumentic.classbuilder.view.ApplicationView;
import com.edumentic.classbuilder.viewmodel.ApplicationViewModel;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;


@Slf4j
public class ClassBuilder extends Application {

    private ApplicationView applicationView;


    public static void main(String[] args) {
        try {
            // Load logging configuration
            InputStream inputStream = ClassBuilder.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (IOException e) {
            System.err.println("Error loading logging configuration: " + e.getMessage());
        }

        Application.launch(ClassBuilder.class, args);
    }



    @Override
    public void init(){

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        applicationView = new ApplicationView(new ApplicationViewModel());
        Scene scene = new Scene(applicationView);

        // Get the primary screen's visual bounds
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Get the preferred width and height of your application window
        double prefWidth = applicationView.prefWidth(-1);
        double prefHeight = applicationView.prefHeight(-1);

        // Set the stage size depending on preferred size vs screen size
        double newWidth = Math.min(prefWidth > 0 ? prefWidth : 1200, screenBounds.getWidth());
        double newHeight = Math.min(prefHeight > 0 ? prefHeight : 800, screenBounds.getHeight());

        primaryStage.setWidth(newWidth);
        primaryStage.setHeight(newHeight);

        // Optionally center the stage
        primaryStage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - newWidth) / 2);
        primaryStage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - newHeight) / 2);

        primaryStage.setScene(scene);

        primaryStage.setTitle("Class Builder");
        primaryStage.getIcons().add(new Image(ClassBuilder.class.getResourceAsStream("/icons/appicon.png")));
        primaryStage.getIcons().add(new Image(ClassBuilder.class.getResourceAsStream("/icons/appicon@2x.png")));
        primaryStage.getIcons().add(new Image(ClassBuilder.class.getResourceAsStream("/icons/appicon@3x.png")));

        primaryStage.show();
    }


}