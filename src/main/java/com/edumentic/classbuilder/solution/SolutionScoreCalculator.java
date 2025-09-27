package com.edumentic.classbuilder.solution;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

import java.util.ArrayList;
import java.util.List;
@Slf4j
public class SolutionScoreCalculator implements EasyScoreCalculator<ClassBuilderSolution, HardSoftScore> {

    @Override
    public HardSoftScore calculateScore(ClassBuilderSolution classBuilderSolution) {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("<div class='scoring-report'>");

        // Hard constraints - Class size checks
        int hardScore = 0;
        for(StudentClass studentClass : classBuilderSolution.getStudentClasses()){
            int minClassSize = ClassBuilderGlobalConstraints.getInstance().getMinClassSize();
            int maxClassSize = ClassBuilderGlobalConstraints.getInstance().getMaxClassSize();
            int classSize = (int)classBuilderSolution.getAssignments().stream()
                    .filter(a -> a.getStudentClass() == studentClass)
                    .count();
            if(classSize < minClassSize){
                hardScore -= (minClassSize - classSize);
                reportBuilder.append(String.format(
                    "<div class='constraint-violation class-size'><span class='class-code'>%s</span>: <span class='violation'>Min class size violated</span> (%d &lt; %d)</div>",
                    studentClass.getClassCode(), classSize, minClassSize));
            }
            else if(classSize > maxClassSize){
                hardScore -= (classSize - maxClassSize);
                reportBuilder.append(String.format(
                    "<div class='constraint-violation class-size'><span class='class-code'>%s</span>: <span class='violation'>Max class size violated</span> (%d &gt; %d)</div>",
                    studentClass.getClassCode(), classSize, maxClassSize));
            } else {
                reportBuilder.append(String.format(
                    "<div class='constraint-ok class-size'><span class='class-code'>%s</span>: Class size OK (%d)</div>",
                    studentClass.getClassCode(), classSize));
            }
        }

        // Student assignment constraints
        for(StudentClassAssignment assignment : classBuilderSolution.getAssignments()){
            Student student = assignment.getStudent();
            for(Student cannotBeWith : student.getCannotBeWith()){
                boolean together = classBuilderSolution.inSameClass(student, cannotBeWith);
                if(together){
                    hardScore--;
                    reportBuilder.append(String.format(
                        "<div class='constraint-violation cannot-be-with'><span class='student'>%s</span> and <span class='student'>%s</span>: <span class='violation'>'Cannot be with' violated</span></div>",
                        student.getName(), cannotBeWith.getName()));
                }
            }
            for(Student mustBeWith : student.getMustIncludeFriends()){
                boolean together = classBuilderSolution.inSameClass(student, mustBeWith);
                if(!together){
                    hardScore--;
                    reportBuilder.append(String.format(
                        "<div class='constraint-violation must-be-with'><span class='student'>%s</span> and <span class='student'>%s</span>: <span class='violation'>'Must include friend' NOT together</span></div>",
                        student.getName(), mustBeWith.getName()));
                } else {
                    reportBuilder.append(String.format(
                        "<div class='constraint-ok must-be-with'><span class='student'>%s</span> and <span class='student'>%s</span>: <span class='ok'>'Must include' satisfied</span></div>",
                        student.getName(), mustBeWith.getName()));
                }
            }
        }

        // Early exit if hard violated
        if(hardScore < 0) {
            reportBuilder.append(String.format("<div class='hardscore-summary'>Hard score: %d</div>", hardScore));
            reportBuilder.append("</div>");
            classBuilderSolution.setScoringReportHtml(reportBuilder.toString());
            return HardSoftScore.ofHard(hardScore);
        }

        // Soft constraints
        int softScore = 0;
        for (StudentClassAssignment assignment : classBuilderSolution.getAssignments()) {
            Student student = assignment.getStudent();
            for (Student goodToHave : student.getShouldIncludeFriends()) {
                boolean together = classBuilderSolution.inSameClass(student, goodToHave);
                if (together) {
                    softScore += 1;
                    reportBuilder.append(String.format(
                        "<div class='soft-constraint-ok good-to-have'><span class='student'>%s</span> and <span class='student'>%s</span>: <span class='ok'>'Good to have' satisfied</span></div>",
                        student.getName(), goodToHave.getName()));
                }
            }
            for (Student avoidBeingWith : student.getAvoidBeingWith()) {
                boolean together = classBuilderSolution.inSameClass(student, avoidBeingWith);
                if (together) {
                    softScore -= 1;
                    reportBuilder.append(String.format(
                        "<div class='soft-constraint-violation avoid-being-with'><span class='student'>%s</span> and <span class='student'>%s</span>: <span class='violation'>'Avoid being with' NOT satisfied</span></div>",
                        student.getName(), avoidBeingWith.getName()));
                }
            }
        }

        int numeracyVariance = scoreClassVarianceOf(classBuilderSolution, Student::getNumeracy);
        softScore -= numeracyVariance;
        reportBuilder.append(String.format(
            "<div class='variance numeracy-variance'><span class='metric'>Numeracy variance penalty</span>: %d</div>", numeracyVariance));
        int literacyVariance = scoreClassVarianceOf(classBuilderSolution, Student::getLiteracy);
        softScore -= literacyVariance;
        reportBuilder.append(String.format(
            "<div class='variance literacy-variance'><span class='metric'>Literacy variance penalty</span>: %d</div>", literacyVariance));
        int socialVariance = scoreClassVarianceOf(classBuilderSolution, Student::getSocialEmotional);
        softScore -= socialVariance;
        reportBuilder.append(String.format(
            "<div class='variance socialemotional-variance'><span class='metric'>SocialEmotional variance penalty</span>: %d</div>", socialVariance));

        reportBuilder.append(String.format("<div class='score-summary'>Hard score: %d, Soft score: %d</div>", hardScore, softScore));
        reportBuilder.append("</div>");
        classBuilderSolution.setScoringReportHtml(reportBuilder.toString());

        return HardSoftScore.of(hardScore, softScore);
    }

    private int scoreClassVarianceOf(ClassBuilderSolution classBuilderSolution, StudentMetricProvider studentMetricProvider){
        List<StudentClass> classes = classBuilderSolution.getStudentClasses();
        List<Double> classAverages = new ArrayList<>();
        for (StudentClass studentClass : classes) {
            List<Student> studentsInClass = classBuilderSolution.getStudentsInClass(studentClass);
            double averageNumeracy = studentsInClass.stream()
                    .mapToInt(studentMetricProvider::getMetric)
                    .average()
                    .orElse(0.0);
            classAverages.add(averageNumeracy);
        }
        double overallMean = classAverages.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double numeracyVariance = classAverages.stream()
                .mapToDouble(avg -> (avg - overallMean) * (avg - overallMean))
                .average()
                .orElse(0.0);

        return (int)Math.round(numeracyVariance); // Or scale as appropriate
    }

    @FunctionalInterface
    private interface StudentMetricProvider{
        int getMetric(Student student);
    }
}
