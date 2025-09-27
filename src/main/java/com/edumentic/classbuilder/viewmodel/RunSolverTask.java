package com.edumentic.classbuilder.viewmodel;

import com.edumentic.classbuilder.model.Student;
import com.edumentic.classbuilder.solution.ClassBuilderSolution;
import com.edumentic.classbuilder.solution.SolutionScoreCalculator;
import com.edumentic.classbuilder.solution.StudentClassAssignment;
import javafx.concurrent.Task;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

public class RunSolverTask extends Task<Void> {

    private Solver<ClassBuilderSolution> solver;
    private final BestSolutionConsumer consumer;
    private final ClassBuilderSolution solutionTemplate;

    public RunSolverTask(BestSolutionConsumer consumer, ClassBuilderSolution solutionTemplate){
        this.consumer = consumer;
        this.solutionTemplate = solutionTemplate;
    }
    @Override
    protected Void call() throws Exception {
        if(solver != null && solver.isSolving()){
            solver.terminateEarly();
        }

        SolverFactory<ClassBuilderSolution> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(ClassBuilderSolution.class)
                .withEntityClasses(StudentClassAssignment.class)
                .withEasyScoreCalculatorClass(SolutionScoreCalculator.class));


        // Build the solver
        solver = solverFactory.buildSolver();
        solver.addEventListener(consumer::onBestSolutionFound);

        solver.solve(solutionTemplate);
        return null;
    }
}
