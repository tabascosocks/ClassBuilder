package com.edumentic.classbuilder.solution;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

@Slf4j
public class SolutionScoreCalculator implements EasyScoreCalculator<ClassBuilderSolution, HardSoftScore> {

    private final ClassBuilderGlobalConstraints globalConstraints;

    public SolutionScoreCalculator(ClassBuilderGlobalConstraints globalConstraints){
        this.globalConstraints = globalConstraints;
    }
    @Override
    public HardSoftScore calculateScore(ClassBuilderSolution classBuilderSolution) {
        // Hard constraints - Class size checks
        for(StudentClass studentClass : classBuilderSolution.getStudentClasses()){
            long classSize = classBuilderSolution.getAssignments().stream()
                    .filter(a -> a.getStudentClass() == studentClass)
                    .count();
            log.debug("Checking class [{}]: size is {}, min={}, max={}",
                    studentClass.getClassCode(), classSize,
                    globalConstraints.getMinClassSize(), globalConstraints.getMaxClassSize());
            if(classSize < globalConstraints.getMinClassSize() || classSize > globalConstraints.getMaxClassSize()){
                log.warn("Class size constraint violated for [{}]: size={}", studentClass.getClassCode(), classSize);
                return HardSoftScore.ofHard(-1);
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
                    log.warn("'Cannot be with' violated: {} and {} together", student.getName(), cannotBeWith.getName());
                    return HardSoftScore.ofHard(-1);
                }
            }
            // Check "must include friends"
            for(Student mustBeWith : student.getMustIncludeFriends()){
                boolean together = classBuilderSolution.inSameClass(student, mustBeWith);
                log.debug("Checking 'must be with': {} and {} in same class? {}", student.getName(), mustBeWith.getName(), together);
                if(!together){
                    log.warn("'Must include friend' violated: {} and {} NOT together", student.getName(), mustBeWith.getName());
                    return HardSoftScore.ofHard(-1);
                }
            }
        }
        log.info("Solution passed all checks, returning zero score.");
        return HardSoftScore.ZERO;
    }
}
