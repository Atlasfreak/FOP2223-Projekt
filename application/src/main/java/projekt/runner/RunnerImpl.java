package projekt.runner;

import projekt.delivery.archetype.ProblemArchetype;
import projekt.delivery.archetype.ProblemGroup;
import projekt.delivery.rating.RatingCriteria;
import projekt.delivery.service.DeliveryService;
import projekt.delivery.simulation.BasicDeliverySimulation;
import projekt.delivery.simulation.Simulation;
import projekt.delivery.simulation.SimulationConfig;
import projekt.runner.handler.ResultHandler;
import projekt.runner.handler.SimulationFinishedHandler;
import projekt.runner.handler.SimulationSetupHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunnerImpl implements Runner {

    @Override
    public void run(ProblemGroup problemGroup,
            SimulationConfig simulationConfig,
            int simulationRuns,
            DeliveryService.Factory deliveryServiceFactory,
            SimulationSetupHandler simulationSetupHandler,
            SimulationFinishedHandler simulationFinishedHandler,
            ResultHandler resultHandler) {
        Map<ProblemArchetype, Simulation> problemToSimulation = createSimulations(problemGroup, simulationConfig,
                deliveryServiceFactory);
        Map<RatingCriteria, List<Double>> ratingCriteriaToListOfRatings = new HashMap<>();

        for (ProblemArchetype problem : problemToSimulation.keySet()) {
            Simulation simulation = problemToSimulation.get(problem);

            for (int i = 0; i < simulationRuns; i++) {
                simulationSetupHandler.accept(simulation, problem, i);
                simulation.runSimulation(problem.simulationLength());

                if (simulationFinishedHandler.accept(simulation, problem)) {
                    return;
                }

                for (RatingCriteria ratingCriteria : problem.raterFactoryMap().keySet()) {
                    if (!ratingCriteriaToListOfRatings.containsKey(ratingCriteria)) {
                        ratingCriteriaToListOfRatings.put(ratingCriteria, new ArrayList<>());
                    }

                    ratingCriteriaToListOfRatings.get(ratingCriteria)
                            .add(simulation.getRatingForCriterion(ratingCriteria));
                }
            }
        }

        Map<RatingCriteria, Double> ratingCriteriaToAverageRatings = new HashMap<>();
        ratingCriteriaToListOfRatings.forEach((ratingCriteria, ratings) -> {
            ratingCriteriaToAverageRatings.put(ratingCriteria,
                    ratings.stream().mapToDouble(Double::doubleValue).sum() / (double) ratings.size());
        });

        resultHandler.accept(ratingCriteriaToAverageRatings);
    }

    @Override
    public Map<ProblemArchetype, Simulation> createSimulations(ProblemGroup problemGroup,
            SimulationConfig simulationConfig,
            DeliveryService.Factory deliveryServiceFactory) {
        List<ProblemArchetype> problems = problemGroup.problems();
        Map<ProblemArchetype, Simulation> problemToSimulation = new HashMap<>();
        for (ProblemArchetype problem : problems) {
            problemToSimulation.put(problem, new BasicDeliverySimulation(simulationConfig, problem.raterFactoryMap(),
                    deliveryServiceFactory.create(problem.vehicleManager()), problem.orderGeneratorFactory()));
        }
        return problemToSimulation;
    }

}
