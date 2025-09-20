package com.edumentic.classbuilder.solution;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.model.StudentClass;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
/**
 * Represents a planning entity for assigning a single {@link Student} to a {@link StudentClass}
 * in the context of the OptaPlanner-based class allocation problem.
 *
 * <p>
 *     Each {@code StudentClassAssignment} is a mutable object managed by OptaPlanner during solving.
 *     The {@link #student} property is fixed (immutable)—it identifies the student being assigned.
 *     The {@link #studentClass} property is a planning variable, and OptaPlanner is allowed to change
 *     its value to find the optimal allocation according to all constraints and optimization goals.
 * </p>
 *
 * <h3>OptaPlanner Annotations:</h3>
 * <ul>
 *     <li>{@link PlanningEntity}: Marks this class as an entity whose state (the assigned class) will be optimized.</li>
 *     <li>{@link PlanningVariable}: Specifies {@code studentClass} as the variable allowed to change during planning.
 *         The {@code valueRangeProviderRefs} attribute declares which value range(s) this variable can take—referencing
 *         the "studentClasses" range provided in {@code ClassBuilderSolution}.</li>
 * </ul>
 *
 * <p>
 *     This class is typically created one per student, pre-populated with a reference to that student.
 *     When solving, OptaPlanner will assign (and reassign) the {@code studentClass} field to optimize for all hard and soft constraints.
 * </p>
 */
@PlanningEntity
public class StudentClassAssignment {
    /**
     * The student to be assigned to a class.
     * <p>
     *     This field is considered a problem fact and does not change during planning.
     * </p>
     */
    @Getter @Setter
    private Student student;

    /**
     * The class to which the student is assigned.
     * <p>
     *     This is the planning variable—OptaPlanner will change this value to find an optimal solution.
     *     The possible values come from the {@code "studentClasses"} value range, as provided by the planning solution.
     * </p>
     */
    @PlanningVariable(valueRangeProviderRefs = "studentClasses")
    @Getter @Setter
    private StudentClass studentClass;

    // Getters and setters omitted for brevity.
}
