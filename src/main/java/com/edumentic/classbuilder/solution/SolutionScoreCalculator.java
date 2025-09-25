package com.edumentic.classbuilder.solution;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SolutionScoreCalculator implements EasyScoreCalculator<ClassBuilderSolution, HardSoftScore> {

    @Override
    public HardSoftScore calculateScore(ClassBuilderSolution classBuilderSolution) {
        // Hard constraints - Class size checks
        HardSoftScore score = HardSoftScore.ZERO;
        int hardScore = 0;
        for(StudentClass studentClass : classBuilderSolution.getStudentClasses()){
            int minClassSize = ClassBuilderGlobalConstraints.getInstance().getMinClassSize();
            int maxClassSize = ClassBuilderGlobalConstraints.getInstance().getMaxClassSize();
            int classSize = (int)classBuilderSolution.getAssignments().stream()
                    .filter(a -> a.getStudentClass() == studentClass)
                    .count();
            log.debug("Checking class [{}]: size is {}, min={}, max={}",
                    studentClass.getClassCode(), classSize,
                    ClassBuilderGlobalConstraints.getInstance().getMinClassSize(), ClassBuilderGlobalConstraints.getInstance().getMaxClassSize());
            if(classSize < minClassSize){
                log.debug("Min Class size constraint violated for [{}]: size={}", studentClass.getClassCode(), classSize);
                hardScore -= (minClassSize - classSize);
            }
            else if(classSize > maxClassSize){
                log.debug("Max Class size constraint violated for [{}]: size={}", studentClass.getClassCode(), classSize);
                hardScore -= (classSize - maxClassSize);
            }
        }

        // Student assignment constraints
        for(StudentClassAssignment assignment : classBuilderSolution.getAssignments()){
            Student student = assignment.getStudent();
            // Check "cannot be with"
            for(Student cannotBeWith : student.getCannotBeWith()){
                boolean together = classBuilderSolution.inSameClass(student, cannotBeWith);
                log.debug("Checking 'cannot be with': {} and {} in same class? {}", student.getName(), cannotBeWith.getName(), together);
                if(together){
                    log.debug("'Cannot be with' violated: {} and {} together", student.getName(), cannotBeWith.getName());
                    hardScore--;
                }
            }
            // Check "must include friends"
            for(Student mustBeWith : student.getMustIncludeFriends()){
                boolean together = classBuilderSolution.inSameClass(student, mustBeWith);
                log.debug("Checking 'must be with': {} and {} in same class? {}", student.getName(), mustBeWith.getName(), together);
                if(!together){
                    log.debug("'Must include friend' violated: {} and {} NOT together", student.getName(), mustBeWith.getName());
                    hardScore--;
                }
            }
        }

        if(hardScore < 0) return HardSoftScore.ofHard(hardScore);

        //Soft constraints
        int softScore = 0;
        for (StudentClassAssignment assignment : classBuilderSolution.getAssignments()) {
            Student student = assignment.getStudent();
            for (Student goodToHave : student.getShouldIncludeFriends()) {
                boolean together = classBuilderSolution.inSameClass(student, goodToHave);
                if (together) {
                    softScore += 1;
                    log.debug("'Good to have' satisfied: {} and {} together", student.getName(), goodToHave.getName());
                }
            }
            for (Student avoidBeingWith : student.getAvoidBeingWith()) {
                boolean together = classBuilderSolution.inSameClass(student, avoidBeingWith);
                if (together) {
                    softScore -= 1;
                    log.debug("'Avoid being With' not satisfied: {} and {} together", student.getName(), avoidBeingWith.getName());
                }
            }
        }


        softScore -= scoreClassVarianceOf(classBuilderSolution, Student::getNumeracy);
        softScore -= scoreClassVarianceOf(classBuilderSolution, Student::getLiteracy);
        softScore -= scoreClassVarianceOf(classBuilderSolution, Student::getSocialEmotional);

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
