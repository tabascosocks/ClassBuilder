package com.edumentic.classbuilder.solution;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class StudentClassAssignment {

    @PlanningId
    //This is required for optaplanner
    private Long id;

    /**
     * The assigned class for the student
     * optaplanner will be modifying this variable to generate a solution
     */
    @PlanningVariable(valueRangeProviderRefs = {"studentClasses"}, nullable = false)
    private StudentClass studentClass;

    /**
     * The student to which this assignmetn will associate a student class
     * this variable must be set prior to running optaplanner, and
     * there can only be one assignment per studets, and every student
     * needs an assignment/
     */
    private Student student;

}
