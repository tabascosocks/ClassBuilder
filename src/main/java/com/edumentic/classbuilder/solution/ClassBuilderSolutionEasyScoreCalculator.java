package com.edumentic.classbuilder.solution;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

/**
 * An {@link EasyScoreCalculator} implementation for quickly evaluating naive (easy) scores
 * for a {@link ClassBuilderSolution} in the class allocation optimization problem.
 * <p>
 *     This class serves as a simple way to calculate the {@link HardSoftScore} for a given solution state.
 *     Its {@link #calculateScore(ClassBuilderSolution)} method is called by OptaPlanner during solving,
 *     and should assign penalties or rewards based on adherence to hard constraints (must not be violated)
 *     and soft constraints (should be optimized if possible).
 * </p>
 * <h3>Usage and Role:</h3>
 * <ul>
 *     <li>Defines the rules for scoring a specific allocation of students to classes.</li>
 *     <li>This "easy" calculator computes the score directly from the current state, without advanced incremental tracking.
 *         It's simple to implement and debug, but may not be as fast as incremental score calculators on large datasets.</li>
 *     <li>The calculated score is used by the optimization engine to compare and select the best possible solutions found.</li>
 * </ul>
 * <h3>Customization:</h3>
 * <ul>
 *     <li>Implement your scoring logic in the {@code calculateScore} method—typically where constraint violations
 *         are penalized (lower hard score), and optimization goals are rewarded/penalized (soft score).</li>
 *     <li>The returned {@link HardSoftScore} contains the aggregated solution score,
 *         which OptaPlanner maximizes by default.</li>
 * </ul>
 *
 * <p>
 *     Team Note: Replace the current {@code return null;} statement with your constraint evaluation logic.
 * </p>
 */
public class ClassBuilderSolutionEasyScoreCalculator implements EasyScoreCalculator<ClassBuilderSolution, HardSoftScore> {
    @Override
    public HardSoftScore calculateScore(ClassBuilderSolution classBuilderSolution) {
        return null;
    }
}
