package com.edumentic.classbuilder.viewmodel;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import com.edumentic.classbuilder.solution.ClassBuilderGlobalConstraints;
import com.edumentic.classbuilder.solution.ClassBuilderSolution;
import com.edumentic.classbuilder.solution.SolutionScoreCalculator;
import com.edumentic.classbuilder.solution.StudentClassAssignment;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;
import org.optaplanner.core.config.solver.SolverConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ApplicationViewModel implements BestSolutionConsumer{

    private Service<Void> solverService;

    private List<Student> students;
    private List<StudentClass> classes;
    private File datafile;



    private final ListProperty<ClassSolutionData> solutions = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<ClassSolutionData> currentSolution = new SimpleObjectProperty<>(null);

    private final StringProperty datafileSummary = new SimpleStringProperty();

    private final BooleanProperty runningSolver = new SimpleBooleanProperty(false);
    private final BooleanProperty dataIsLoaded = new SimpleBooleanProperty(false);

    public ApplicationViewModel(){
        solverService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                //create template solution
                ClassBuilderSolution solutionTemplate = new ClassBuilderSolution();
                solutionTemplate.setStudentClasses(classes);
                //generate a StudentClassAssignment for every student
                for(Student student : students){
                    StudentClassAssignment assignment = new StudentClassAssignment();
                    assignment.setStudent(student);
                    assignment.setStudentClass(classes.getFirst());
                    assignment.setId((long) solutionTemplate.getAssignments().size());
                    solutionTemplate.getAssignments().add(assignment);
                }
                return new RunSolverTask(ApplicationViewModel.this, solutionTemplate);
            }
        };
        solverService.setOnRunning(evt -> {
            runningSolver.set(true);
        });
        solverService.setOnCancelled(evt -> {
            runningSolver.set(false);
        });
    }

    public void runSolver() {
        solverService.restart();
    }

    public void stopSolver(){
        solverService.cancel();
    }

    public void loadDatafile() throws DatafileParseException{
        loadDatafile(datafile);
    }

    public void loadDatafile(File file) throws DatafileParseException{
        dataIsLoaded.set(false);
        if(file == null || ! file.exists() || ! file.canRead()){
            throw new DatafileParseException("Cannot read file:  " + file);
        }
        datafile = file;
        try (InputStream fileStream = new FileInputStream(datafile);
             Workbook workbook = WorkbookFactory.create(fileStream)) {

            Sheet generalSheet = workbook.getSheet("General");
            Sheet studentSheet = workbook.getSheet("Students");
            Sheet classSheet = workbook.getSheet("Classes");

            if (generalSheet == null || studentSheet == null || classSheet == null) {
                throw new DatafileParseException("Missing required sheets. Ensure the spreadsheet has 'General', 'Students', and 'Classes' sheets.");
            }

            fromGeneralSheet(generalSheet);
            students = fromStudentSheet(studentSheet);
            classes = fromClassesSheet(classSheet);

            datafileSummary.set("Loaded " + students.size() + " students for " + classes.size() + "classes");

            dataIsLoaded.set(true);

        } catch (Exception e) {
            throw new DatafileParseException("Failed to process the Excel file: " + e.getMessage());
        }
    }

    public void clearDataFile(){
        dataIsLoaded.set(false);
    }

    public void onBestSolutionFound(BestSolutionChangedEvent<ClassBuilderSolution> bestSolutionChangedEvent) {
        ClassBuilderSolution solution = bestSolutionChangedEvent.getNewBestSolution();
        Platform.runLater(() -> {
            solutions.addFirst(new ClassSolutionData(solution, bestSolutionChangedEvent.getTimeMillisSpent()));
        });
        log.info("Found next best solution {}", solution.toBriefString());
    }

    public void fromGeneralSheet(Sheet generalSheet) {

        // Assumes the first row has "Min Class Size", "Max Class Size"
        // And the second row has their respective values
        Row valueRow = generalSheet.getRow(1);
        if (valueRow != null) {
            ClassBuilderGlobalConstraints.getInstance().setMinClassSize((int) valueRow.getCell(0).getNumericCellValue());
            ClassBuilderGlobalConstraints.getInstance().setMaxClassSize((int) valueRow.getCell(1).getNumericCellValue());
        }
    }


    public List<Student> fromStudentSheet(Sheet studentSheet) throws DatafileParseException {
        List<Student> students = new ArrayList<>();
        // First pass: create students with names & add to list
        for (int i = 1; i <= studentSheet.getLastRowNum(); i++) {
            Row row = studentSheet.getRow(i);
            if (row == null) continue;

            // Skip completely blank rows
            boolean blank = true;
            for (int c = 0; c <= 7; c++) {
                if (row.getCell(c) != null && !row.getCell(c).toString().trim().isEmpty()) { blank = false; break; }
            }
            if (blank) continue;

            // Required: name and numeracy/literacy/social-emotional scores (cells 0, 5, 6, 7)
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().trim().isEmpty()) {
                throw new DatafileParseException(
                        String.format("Missing required student name in row %d", i + 1)
                );
            }
            for (int c : new int[]{5, 6, 7}) {
                if (row.getCell(c) == null || row.getCell(c).getCellType() != org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    throw new DatafileParseException(
                            String.format("Student row %d: Missing or non-numeric required score at column %d", i + 1, c + 1)
                    );
                }
            }

            Student s = new Student();
            s.setName(row.getCell(0).getStringCellValue().trim());
            students.add(s);
        }
        // Second pass: fill in fields, perform lookup by iterating list
        for (int i = 1, j = 0; i <= studentSheet.getLastRowNum() && j < students.size(); i++) {
            Row row = studentSheet.getRow(i);
            if (row == null) continue;
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().trim().isEmpty()) continue;

            for (int c : new int[]{5, 6, 7}) {
                if (row.getCell(c) == null || row.getCell(c).getCellType() != org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    throw new DatafileParseException(
                            String.format("Student row %d: Missing or non-numeric required score at column %d", i + 1, c + 1)
                    );
                }
            }
            Student s = students.get(j++);
            s.setMustIncludeFriends(resolveByNameList(row.getCell(1), students, s.getName(), "mustIncludeFriends"));
            s.setShouldIncludeFriends(resolveByNameList(row.getCell(2), students, s.getName(), "shouldIncludeFriends"));
            s.setCannotBeWith(resolveByNameList(row.getCell(3), students, s.getName(), "cannotBeWith"));
            s.setAvoidBeingWith(resolveByNameList(row.getCell(4), students, s.getName(), "avoidBeingWith"));
            s.setNumeracy((int) row.getCell(5).getNumericCellValue());
            s.setLiteracy((int) row.getCell(6).getNumericCellValue());
            s.setSocialEmotional((int) row.getCell(7).getNumericCellValue());
        }
        return students;
    }

    // Helper: Find students matching names by iterating
    private List<Student> resolveByNameList(org.apache.poi.ss.usermodel.Cell cell, List<Student> students,
                                                   String parentName, String fieldName) throws DatafileParseException{
        List<String> names = toList(cell);
        List<Student> resolved = new ArrayList<>();
        for (String name : names) {
            Student found = findStudentByName(students, name.trim());
            if (found != null) {
                resolved.add(found);
            } else {
                throw new DatafileParseException(
                        String.format("In %s: %s references unknown student '%s' in %s", fieldName, parentName, name, fieldName)
                );
            }
        }
        return resolved;
    }

    private Student findStudentByName(List<Student> students, String name) {
        for (Student s : students) {
            if (s.getName() != null && s.getName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    private List<String> toList(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null || cell.getStringCellValue().trim().isEmpty()) return List.of();
        return Arrays.asList(cell.getStringCellValue().split("\\s*,\\s*"));
    }

    public List<StudentClass> fromClassesSheet(Sheet classSheet) throws DatafileParseException{
        List<StudentClass> classes = new ArrayList<>();
        // Skip the header (assume first row, index 0)
        for (int i = 1; i <= classSheet.getLastRowNum(); i++) {
            Row row = classSheet.getRow(i);
            if (row == null) continue;
            // Required: class code and teacher
            String code = (row.getCell(0) != null) ? row.getCell(0).getStringCellValue().trim() : "";
            String teacher = (row.getCell(1) != null) ? row.getCell(1).getStringCellValue().trim() : "";
            if (code.isEmpty() && teacher.isEmpty()) continue;

            if (code.isEmpty() || teacher.isEmpty()) {
                log.warn("Skipping class row {}: Missing required class code or teacher name", i + 1);
                continue;
            }
            StudentClass sc = new StudentClass();
            sc.setClassCode(code);
            sc.setTeacher(teacher);
            classes.add(sc);
        }
        return classes;
    }

    public StringProperty datafileSummaryProperty() {
        return datafileSummary;
    }

    public ListProperty<ClassSolutionData> solutionsProperty() {
        return solutions;
    }

    public ObjectProperty<ClassSolutionData> currentSolutionProperty() {
        return currentSolution;
    }

    public BooleanProperty runningSolverProperty() {
        return runningSolver;
    }

    public BooleanProperty dataIsLoadedProperty() {
        return dataIsLoaded;
    }

    public void clearAllSolutions() {
        solutions.clear();
    }

    @Getter
    @Setter
    public static class ClassSolutionData{

        private int hardScore;
        private int softScore;
        private String solutionReportHtml;
        private long generationDurationMillis;
        private Instant generatedAt;

        public ClassSolutionData(ClassBuilderSolution classBuilderSolution, long generationDurationMillis){
            this.hardScore = classBuilderSolution.getScore().hardScore();
            this.softScore = classBuilderSolution.getScore().softScore();
            this.solutionReportHtml = classBuilderSolution.toHtmlReport();
            this.generationDurationMillis = generationDurationMillis;
            this.generatedAt = Instant.now();

        }

        @Override
        public String toString(){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            String formattedTime = generatedAt != null ? formatter.format(generatedAt) : "N/A";
            return String.format("%s: [Hard Score: %d, Soft Score: %d] %d ms",
                    formattedTime, hardScore, softScore, generationDurationMillis);
        }
    }
}
