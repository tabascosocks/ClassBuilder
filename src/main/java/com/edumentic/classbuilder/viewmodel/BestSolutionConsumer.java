package com.edumentic.classbuilder.viewmodel;

import com.edumentic.classbuilder.solution.ClassBuilderSolution;
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;

public interface BestSolutionConsumer {
    void onBestSolutionFound(BestSolutionChangedEvent<ClassBuilderSolution> bestSolutionChangedEvent);
}
