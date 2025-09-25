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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;
import org.optaplanner.core.config.solver.SolverConfig;

@Slf4j
public class ClassBuilder {

    static List<Student> students;
    static List<StudentClass> classes;

    private static ClassBuilderSolution currentBestSolution;
    private static Solver<ClassBuilderSolution> solver;
    private static StdInMonitor monitor;

    public static void main(String[] args) {
        try {
            // Load logging configuration
            InputStream inputStream = ClassBuilder.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (IOException e) {
            System.err.println("Error loading logging configuration: " + e.getMessage());
        }

        if (args.length < 1) {
            log.error("Please provide the input filename as a command-line argument.");
            log.error("Usage: java -jar classbuilder.jar <input_file.xlsx>");
            System.exit(1);
        }

        //
        log.info("Welcome to the ClassBuilder!");
        log.info("This application helps you generate balanced and rules-based class assignments.");
        log.info("To get started, please provide the path to your Excel input file as a command-line argument.");
        log.info("During the program execution, you can type CTRL+C to exit at any time.");
        log.info("Press ENTER to continue...");

        try {
            System.in.read();
        } catch (IOException e) {
            log.error("Error reading input: {}", e.getMessage());
        }

        //start the system in listen thread
        monitor = new StdInMonitor((data) -> {
            //if the use has written a quit command
            if (data.equalsIgnoreCase("Q")
                    || data.equalsIgnoreCase("X")
                    || data.equalsIgnoreCase("QUIT")) {
                //stop the solver
                if(solver != null){
                    solver.terminateEarly();
                }
                //print the current solution and exit
                if (currentBestSolution != null) {
                    System.out.println(currentBestSolution.toPrettyString());
                }
                System.exit(0);
            }
        });
        monitor.start();

        String inputFilename = args[0];
        try (InputStream file = new java.io.FileInputStream(inputFilename);
             Workbook workbook = WorkbookFactory.create(file)) {

            Sheet generalSheet = workbook.getSheet("General");
            Sheet studentSheet = workbook.getSheet("Students");
            Sheet classSheet = workbook.getSheet("Classes");

            if (generalSheet == null || studentSheet == null || classSheet == null) {
                log.error("Missing required sheets. Ensure the spreadsheet has 'General', 'Students', and 'Classes' sheets.");
                System.exit(2);
            }

            fromGeneralSheet(generalSheet);
            students = fromStudentSheet(studentSheet);
            classes = fromClassesSheet(classSheet);

            log.info("Loaded constraints: {}", ClassBuilderGlobalConstraints.getInstance());
            log.info("Loaded {} students.", students.size());
            log.info("Loaded {} classes.", classes.size());

            //build the solution class

        } catch (Exception e) {
            log.error("Failed to process the Excel file: {}", e.getMessage(), e);
            System.exit(3);
        }

        runSolver();
    }

    private static void runSolver() {
        SolverFactory<ClassBuilderSolution> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(ClassBuilderSolution.class)
                .withEntityClasses(StudentClassAssignment.class)
                .withEasyScoreCalculatorClass(SolutionScoreCalculator.class));


        // Build the solver
        solver = solverFactory.buildSolver();
        solver.addEventListener(ClassBuilder::onBetterSolutionFound);

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

        ClassBuilderSolution solution = solver.solve(solutionTemplate);

    }

    private static void onBetterSolutionFound(BestSolutionChangedEvent<ClassBuilderSolution> bestSolutionChangedEvent) {
        ClassBuilderSolution solution = bestSolutionChangedEvent.getNewBestSolution();
        currentBestSolution = solution;
        log.info("Found next best solution {}", solution.toBriefString());
    }

    public static void fromGeneralSheet(Sheet generalSheet) {

        // Assumes the first row has "Min Class Size", "Max Class Size"
        // And the second row has their respective values
        Row valueRow = generalSheet.getRow(1);
        if (valueRow != null) {
            ClassBuilderGlobalConstraints.getInstance().setMinClassSize((int) valueRow.getCell(0).getNumericCellValue());
            ClassBuilderGlobalConstraints.getInstance().setMaxClassSize((int) valueRow.getCell(1).getNumericCellValue());
        }
    }


    public static List<Student> fromStudentSheet(Sheet studentSheet) {
        List<Student> students = new ArrayList<>();
        // First pass: create students with names & add to list
        for (int i = 1; i <= studentSheet.getLastRowNum(); i++) {
            Row row = studentSheet.getRow(i);
            if (row == null) continue;
            // Required: name and numeracy/literacy/social-emotional scores (cells 0, 5, 6, 7)
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().trim().isEmpty()) {
                log.warn("Skipping student row {}: Missing required student name", i + 1);
                continue;
            }
            boolean missingScore = false;
            for (int c : new int[]{5, 6, 7}) {
                if (row.getCell(c) == null || row.getCell(c).getCellType() != org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    log.warn("Skipping student row {}: Missing or non-numeric required score at column {}", i + 1, c + 1);
                    missingScore = true;
                    break;
                }
            }
            if (missingScore) continue;
            Student s = new Student();
            s.setName(row.getCell(0).getStringCellValue().trim());
            students.add(s);
        }
        // Second pass: fill in fields, perform lookup by iterating list
        for (int i = 1, j = 0; i <= studentSheet.getLastRowNum() && j < students.size(); i++) {
            Row row = studentSheet.getRow(i);
            if (row == null) continue;
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().trim().isEmpty()) continue;
            boolean missingScore = false;
            for (int c : new int[]{5, 6, 7}) {
                if (row.getCell(c) == null || row.getCell(c).getCellType() != org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    missingScore = true;
                    break;
                }
            }
            if (missingScore) continue;
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
    private static List<Student> resolveByNameList(org.apache.poi.ss.usermodel.Cell cell, List<Student> students,
                                                   String parentName, String fieldName) {
        List<String> names = toList(cell);
        List<Student> resolved = new ArrayList<>();
        for (String name : names) {
            Student found = findStudentByName(students, name.trim());
            if (found != null) {
                resolved.add(found);
            } else {
                log.warn("In {}: {} references unknown student '{}' in {}", fieldName, parentName, name, fieldName);
            }
        }
        return resolved;
    }

    private static Student findStudentByName(List<Student> students, String name) {
        for (Student s : students) {
            if (s.getName() != null && s.getName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    private static List<String> toList(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null || cell.getStringCellValue().trim().isEmpty()) return List.of();
        return Arrays.asList(cell.getStringCellValue().split("\\s*,\\s*"));
    }

    public static List<StudentClass> fromClassesSheet(Sheet classSheet) {
        List<StudentClass> classes = new ArrayList<>();
        // Skip the header (assume first row, index 0)
        for (int i = 1; i <= classSheet.getLastRowNum(); i++) {
            Row row = classSheet.getRow(i);
            if (row == null) continue;
            // Required: class code and teacher
            String code = (row.getCell(0) != null) ? row.getCell(0).getStringCellValue().trim() : "";
            String teacher = (row.getCell(1) != null) ? row.getCell(1).getStringCellValue().trim() : "";
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

}