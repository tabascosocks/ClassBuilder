package com.edumentic.classbuilder.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a group or class to which students can be allocated in the optimization solution.
 * <p>
 *     The {@code StudentClass} class is designed for extensibility—future requirements might introduce
 *     additional constraints or properties, such as limits on class size, subject specialization,
 *     or links to particular teachers. For now, the class serves primarily as a unique, numbered grouping.
 * </p>
 *
 * <h3>Domain Role:</h3>
 * <ul>
 *     <li>Acts as a problem fact in the OptaPlanner solution: each {@code StudentClass} instance represents
 *         a possible class/group to which students can be assigned.</li>
 *     <li>The {@link #classNumber} enables simple identification and distinction between classes.</li>
 *     <li>Can be extended with planning attributes as more complex allocation rules or constraints are introduced (e.g. teacher compatibility, time slots, room assignments).</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <ul>
 *     <li>Pre-populate a list of {@code StudentClass} objects before solving to act as the value range for student assignment.</li>
 *     <li>Refer to these objects in student-to-class assignments via the planning entities.</li>
 * </ul>
 *
 * <p>
 *     Lombok's {@code @Getter} and {@code @Setter} annotations are used to generate accessor methods for the {@link #classNumber} field.
 * </p>
 */
public class StudentClass {

    @Getter @Setter
    private int classNumber;

}
