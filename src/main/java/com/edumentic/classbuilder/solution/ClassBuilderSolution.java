package com.edumentic.classbuilder.solution;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.ArrayList;
import java.util.List;

@PlanningSolution
public class ClassBuilderSolution {

    /**
     * This need to be pre-populated, one assingment per student
     */
    @PlanningEntityCollectionProperty
    private List<StudentClassAssignment> solutionAssignments = new ArrayList<>();

    /**
     * this needs to be pre-populated, enough classes to fit the minimum number of students per class
     * the @ValueRangeProvider annotation means that this list will be used to randomly assign StudentClass instances
     * in teh StudentClassAssignment
     */
    @ValueRangeProvider(id = "studentClasses")
    @ProblemFactCollectionProperty
    private List<StudentClass> availableStudentClasses = new ArrayList<>();

    @PlanningScore
    private HardSoftScore score;
}
