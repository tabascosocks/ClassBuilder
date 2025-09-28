package com.edumentic.classbuilder.solution;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The {@code ClassBuilderSolution} describes the overall planning solution for the student-class allocation problem
 * using OptaPlanner. It defines the key domain objects and how OptaPlanner should treat them during optimization.
 *
 * <p>
 *    This solution consists of:
 *    <ul>
 *       <li>A {@code List<StudentClassAssignment>} which represents the planning entities—each item is an individual student assignment to a class.
 *           These must be pre-populated at the start, with typically one assignment per student.</li>
 *       <li>A {@code List<StudentClass>} which holds all the available classes that students could be assigned to.
 *           This collection acts as a value range for class assignment, and should include enough classes to cover the minimum required per optimization rules.</li>
 *       <li>A {@code HardSoftScore} which is computed as the solution score, reflecting adherence to hard constraints (must haves)
 *           and soft constraints (nice-to-haves/custom preferences).</li>
 *    </ul>
 * </p>
 *
 * <h3>OptaPlanner Annotations:</h3>
 * <ul>
 *   <li>{@link PlanningSolution}: Marks this class as the solution model recognized by OptaPlanner.</li>
 *   <li>{@link PlanningEntityCollectionProperty}: Denotes the collection of planning entities for OptaPlanner to change during solving.</li>
 *   <li>{@link ValueRangeProvider}: Makes the available classes a selectable range for entity assignment.</li>
 *   <li>{@link ProblemFactCollectionProperty}: Marks available classes as problem facts, which stay constant during solving.</li>
 *   <li>{@link PlanningScore}: Indicates the final solution score.</li>
 * </ul>
 *
 * <p>To use this class, populate {@code solutionAssignments} (one per student) and {@code availableStudentClasses} prior to solving.
 * OptaPlanner will assign each student to a class, attempting to optimize according to domain constraints/rules.</p>
 */
@PlanningSolution
@Getter
@Setter
public class ClassBuilderSolution {
    /**
     * List of assignments representing the link between each student and the class they are (to be) placed in.
     * <p>
     * Each assignment is a {@link StudentClassAssignment} (planning entity) and must be initialized with
     * at least one per student to begin planning.
     * </p>
     */
    @PlanningEntityCollectionProperty
    private List<StudentClassAssignment> assignments = new ArrayList<>();

    /**
     * Collection of all classes available for student assignment.
     * <p>
     * This list must be populated up front, with enough classes to fit all students subject to allocation rules,
     * e.g., minimum/maximum class size. It acts as the value range for the assignments.
     * </p>
     */
    @ValueRangeProvider(id = "studentClasses")
    @ProblemFactCollectionProperty
    private List<StudentClass> studentClasses = new ArrayList<>();

    /**
     * The solution score calculated by OptaPlanner. Encodes how well the current assignments satisfy hard
     * and soft constraints.
     */
    @PlanningScore
    private HardSoftScore score;

    private String scoringReportHtml;

    public boolean inSameClass(Student studentA, Student studentB){
        StudentClass studentAClass = assignments.stream()
                .filter(a -> a.getStudent() == studentA)
                .map(StudentClassAssignment::getStudentClass)
                .findAny()
                .orElseThrow();
        StudentClass studentBClass = assignments.stream()
                .filter(a -> a.getStudent() == studentB)
                .map(StudentClassAssignment::getStudentClass)
                .findAny()
                .orElseThrow();
        return studentAClass == studentBClass;
    }

    public List<Student> getStudentsInClass(StudentClass studentClass){
        return assignments.stream()
                .filter(a -> a.getStudentClass() == studentClass)
                .map(StudentClassAssignment::getStudent)
                .toList();
    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder("ClassBuilderSolution {\n");
        sb.append("  assignments=[\n");
        if (assignments != null) {
            for (StudentClassAssignment a : assignments) {
                sb.append("    ")
                        .append(a == null ? "null" : a.toPrettyString().replace("\n", "\n    "))
                        .append(",\n");
            }
        }
        sb.append("  ],\n");

        sb.append("  studentClasses=[\n");
        if (studentClasses != null) {
            for (StudentClass c : studentClasses) {
                sb.append("    ")
                        .append(c == null ? "null" : c.toPrettyString().replace("\n", "\n    "))
                        .append(",\n");
            }
        }
        sb.append("  ],\n");

        sb.append("  score=").append(score).append("\n");
        sb.append("}");
        return sb.toString();
    }

    public String toBriefString() {
        return String.format(
                "ClassBuilderSolution: score=%s",
                score
        );
    }

    public String toHtmlReport() {
        StringBuilder html = new StringBuilder();
        html.append("<div class='classbuilder-report'>");

        html.append("<h2 class='cb-section-title'>Class Allocations</h2>");
        html.append("<div class='cb-class-list'>");

        for (StudentClass studentClass : studentClasses.stream().sorted(Comparator.comparing(StudentClass::getClassCode)).toList()) {
            html.append("<div class='cb-class-block'>");
            html.append(String.format("<h3 class='cb-class-title'>%s</h3>", studentClass.getClassCode()));

            // Students in this class
            List<Student> studentsIn = getStudentsInClass(studentClass).stream().sorted(Comparator.comparing(Student::getName)).toList();
            if (studentsIn.isEmpty()) {
                html.append("<div class='cb-class-empty'>No students assigned.</div>");
            } else {
                html.append("<table class='cb-student-table'><thead><tr><th>Name</th><th>Numeracy</th><th>Literacy</th><th>Social-Emotional</th></tr></thead><tbody>");
                for (Student s : studentsIn) {
                    html.append(String.format(
                            "<tr><td class='cb-student-name'>%s</td>" +
                                    "<td class='cb-metric'>%d</td>" +
                                    "<td class='cb-metric'>%d</td>" +
                                    "<td class='cb-metric'>%d</td>" +
                                    "<td class='cb-metric'>%s</td>" +
                                    "</tr>",
                            s.getName(),
                            s.getNumeracy(),
                            s.getLiteracy(),
                            s.getSocialEmotional(),
                            s.getGender().toString()
                    ));
                }
                html.append("</tbody></table>");
            }
            html.append("</div>");
        }
        html.append("</div>"); // cb-class-list

        // Scoring report
        if (scoringReportHtml != null && !scoringReportHtml.isEmpty()) {
            html.append("<h2 class='cb-section-title'>Scoring Breakdown</h2>");
            html.append(scoringReportHtml);
        }

        html.append("</div>");
        return html.toString();
    }

}
