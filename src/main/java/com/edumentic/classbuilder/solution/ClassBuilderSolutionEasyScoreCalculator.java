package com.edumentic.classbuilder.solution;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

public class ClassBuilderSolutionEasyScoreCalculator implements EasyScoreCalculator<ClassBuilderSolution, HardSoftScore> {
    @Override
    public HardSoftScore calculateScore(ClassBuilderSolution classBuilderSolution) {
        return null;
    }
}
