package com.edumentic.classbuilder.viewmodel;

import com.edumentic.classbuilder.model.Gender;
import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import com.edumentic.classbuilder.solution.ClassBuilderConstraints;
import com.edumentic.classbuilder.solution.ClassBuilderSolution;
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
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;

import java.io.File;
import java.io.FileInputStream;
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

    private final BooleanProperty mustIncludeOthers = new SimpleBooleanProperty();
    private final BooleanProperty shouldIncludeOthers = new SimpleBooleanProperty();
    private final BooleanProperty mustAvoidOthers = new SimpleBooleanProperty();
    private final BooleanProperty shouldAvoidOthers = new SimpleBooleanProperty();
    private final BooleanProperty balanceNumeracy = new SimpleBooleanProperty();
    private final BooleanProperty balanceLiteracy = new SimpleBooleanProperty();
    private final BooleanProperty balanceSocialEmotional = new SimpleBooleanProperty();
    private final BooleanProperty balanceGender = new SimpleBooleanProperty();

    private final IntegerProperty minClassSize = new SimpleIntegerProperty();
    private final IntegerProperty maxClassSize = new SimpleIntegerProperty();

    private final IntegerProperty classMetricVarianceSensitivity = new SimpleIntegerProperty();

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

        ClassBuilderConstraints constraints = ClassBuilderConstraints.getInstance();

        mustIncludeOthers.set(constraints.isMustIncludeOthers());
        mustIncludeOthers.addListener((obs, oldV, newV) -> constraints.setMustIncludeOthers(newV));

        shouldIncludeOthers.set(constraints.isShouldIncludeOthers());
        shouldIncludeOthers.addListener((obs, oldV, newV) -> constraints.setShouldIncludeOthers(newV));

        mustAvoidOthers.set(constraints.isMustAvoidOthers());
        mustAvoidOthers.addListener((obs, oldV, newV) -> constraints.setMustAvoidOthers(newV));

        shouldAvoidOthers.set(constraints.isShouldAvoidOthers());
        shouldAvoidOthers.addListener((obs, oldV, newV) -> constraints.setShouldAvoidOthers(newV));

        balanceNumeracy.set(constraints.isBalanceNumeracy());
        balanceNumeracy.addListener((obs, oldV, newV) -> constraints.setBalanceNumeracy(newV));

        balanceLiteracy.set(constraints.isBalanceLiteracy());
        balanceLiteracy.addListener((obs, oldV, newV) -> constraints.setBalanceLiteracy(newV));

        balanceSocialEmotional.set(constraints.isBalanceSocialEmotional());
        balanceSocialEmotional.addListener((obs, oldV, newV) -> constraints.setBalanceSocialEmotional(newV));

        balanceGender.set(constraints.isBalanceGender());
        balanceGender.addListener((obs, oldV, newV) -> constraints.setBalanceGender(newV));

        minClassSize.set(constraints.getMinClassSize());
        minClassSize.addListener((obs, oldV, newV) -> constraints.setMinClassSize(newV.intValue()));

        maxClassSize.set(constraints.getMaxClassSize());
        maxClassSize.addListener((obs, oldV, newV) -> {
            constraints.setMaxClassSize(newV.intValue());
        });

        classMetricVarianceSensitivity.set(constraints.getClassMetricVarianceSensitivity());
        classMetricVarianceSensitivity.addListener((obs, oldV, newV) -> constraints.setClassMetricVarianceSensitivity(newV.intValue()));

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

            Sheet studentSheet = workbook.getSheet("Students");
            Sheet classSheet = workbook.getSheet("Classes");

            if (studentSheet == null || classSheet == null) {
                throw new DatafileParseException("Missing required sheets. Ensure the spreadsheet has 'General', 'Students', and 'Classes' sheets.");
            }

            students = fromStudentSheet(studentSheet);
            classes = fromClassesSheet(classSheet);

            datafileSummary.set("Loaded " + students.size() + " students for " + classes.size() + " classes");

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

    public String getCurrentSolutionReportHtml(){
        if(currentSolution.get() == null) return "";
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
                    """.formatted(css1, css2, currentSolution.get().getSolutionReportHtml());
        return html;
    }

    /*
    public void fromGeneralSheet(Sheet generalSheet) {

        // Assumes the first row has "Min Class Size", "Max Class Size"
        // And the second row has their respective values
        Row valueRow = generalSheet.getRow(1);
        if (valueRow != null) {
            ClassBuilderConstraints.getInstance().setMinClassSize((int) valueRow.getCell(0).getNumericCellValue());
            ClassBuilderConstraints.getInstance().setMaxClassSize((int) valueRow.getCell(1).getNumericCellValue());
        }
    }

     */


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

            // Required: name
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().trim().isEmpty()) {
                throw new DatafileParseException(
                        String.format("Missing required student name in row %d", i + 1)
                );
            }
            //required: numeracy/literacy/social-emotional scores (cells 0, 5, 6, 7)
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

            Student s = students.get(j++);
            s.setMustIncludeFriends(resolveByNameList(row.getCell(1), students, s.getName(), "mustIncludeFriends"));
            s.setShouldIncludeFriends(resolveByNameList(row.getCell(2), students, s.getName(), "shouldIncludeFriends"));
            s.setCannotBeWith(resolveByNameList(row.getCell(3), students, s.getName(), "cannotBeWith"));
            s.setAvoidBeingWith(resolveByNameList(row.getCell(4), students, s.getName(), "avoidBeingWith"));
            s.setNumeracy((int) row.getCell(5).getNumericCellValue());
            s.setLiteracy((int) row.getCell(6).getNumericCellValue());
            s.setSocialEmotional((int) row.getCell(7).getNumericCellValue());
            //gender
            String genderStr = row.getCell(8, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            switch(genderStr){
                case "M": s.setGender(Gender.MALE); break;
                case "F": s.setGender(Gender.FEMALE); break;
                default: s.setGender(Gender.NA); break;
            }
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

    public BooleanProperty mustIncludeOthersProperty() {
        return mustIncludeOthers;
    }

    public BooleanProperty shouldIncludeOthersProperty() {
        return shouldIncludeOthers;
    }

    public BooleanProperty mustAvoidOthersProperty() {
        return mustAvoidOthers;
    }

    public BooleanProperty shouldAvoidOthersProperty() {
        return shouldAvoidOthers;
    }

    public BooleanProperty balanceNumeracyProperty() {
        return balanceNumeracy;
    }

    public BooleanProperty balanceLiteracyProperty() {
        return balanceLiteracy;
    }

    public BooleanProperty balanceSocialEmotionalProperty() {
        return balanceSocialEmotional;
    }

    public BooleanProperty balanceGenderProperty() {
        return balanceGender;
    }

    public IntegerProperty minClassSizeProperty() {
        return minClassSize;
    }

    public IntegerProperty maxClassSizeProperty() {
        return maxClassSize;
    }

    public int getClassMetricVarianceSensitivity() {
        return classMetricVarianceSensitivity.get();
    }

    public IntegerProperty classMetricVarianceSensitivityProperty() {
        return classMetricVarianceSensitivity;
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            String formattedTime = generatedAt != null ? formatter.format(generatedAt) : "N/A";
            return String.format("%s: [Score: %d/%d] %d ms",
                    formattedTime, hardScore, softScore, generationDurationMillis);
        }
    }
}
