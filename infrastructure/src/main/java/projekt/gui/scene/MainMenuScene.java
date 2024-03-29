package projekt.gui.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import projekt.delivery.archetype.ProblemArchetype;
import projekt.delivery.archetype.ProblemGroup;
import projekt.delivery.archetype.ProblemGroupImpl;
import projekt.delivery.generator.FridayOrderGenerator;
import projekt.delivery.rating.AmountDeliveredRater;
import projekt.delivery.rating.InTimeRater;
import projekt.delivery.rating.Rater;
import projekt.delivery.rating.RatingCriteria;
import projekt.delivery.rating.TravelDistanceRater;
import projekt.delivery.routing.Region.Edge;
import projekt.delivery.routing.Region.Node;
import projekt.delivery.routing.Vehicle;
import projekt.delivery.service.BasicDeliveryService;
import projekt.delivery.service.BogoDeliveryService;
import projekt.delivery.service.DeliveryService;
import projekt.delivery.service.OurDeliveryService;
import projekt.delivery.simulation.SimulationConfig;
import projekt.gui.controller.MainMenuSceneController;
import projekt.io.IOHelper;
import projekt.runner.RunnerImpl;

public class MainMenuScene extends MenuScene<MainMenuSceneController> {

    private int simulationRuns = 1;
    private DeliveryService.Factory deliveryServiceFactory;
    private final Insets preferredPadding = new Insets(20, 20, 20, 20);

    private ObjectProperty<ProblemArchetype> selectedProblemProperty = new SimpleObjectProperty<>();

    public MainMenuScene() {
        super(new MainMenuSceneController(), "Delivery Service Simulation");
    }

    @Override
    public void initComponents() {
        ScrollPane mainPane = new ScrollPane();
        mainPane.setContent(createOptionsVBox());
        mainPane.setFitToWidth(true);
        root.setCenter(mainPane);
    }

    /**
     * Initializes this {@link MainMenuScene} with the {@link ProblemArchetype}
     * presets in the resource dir.
     */
    public void init() {
        super.init(IOHelper.readProblems());
    }

    private VBox createOptionsVBox() {
        VBox optionsVbox = new VBox();
        optionsVbox.setAlignment(Pos.CENTER);
        optionsVbox.setSpacing(10);
        optionsVbox.setPadding(preferredPadding);

        optionsVbox.getChildren().addAll(
                createStartSimulationButton(),
                createSimulationRunsHBox(),
                createDeliveryServiceChoiceBox(),
                createProblemsVBox());

        optionsVbox.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .forEach(button -> {
                    button.setPrefSize(200, 50);
                    button.setMaxWidth(Double.MAX_VALUE);
                });

        return optionsVbox;
    }

    private Button createStartSimulationButton() {
        Button startSimulationButton = new Button("Start Simulation");
        startSimulationButton.setOnAction((e) -> {
            if (problems.size() == 0) {
                throw new IllegalArgumentException("No problems selected");
            }

            // store the SimulationScene
            AtomicReference<SimulationScene> simulationScene = new AtomicReference<>();
            // Execute the GUIRunner in a separate Thread to prevent it from blocking the
            // GUI
            new Thread(() -> {
                ProblemGroup problemGroup = new ProblemGroupImpl(problems,
                        problems.get(0).raterFactoryMap().keySet().stream().toList());
                new RunnerImpl().run(
                        problemGroup,
                        new SimulationConfig(20),
                        simulationRuns,
                        deliveryServiceFactory,
                        (simulation, problem, i) -> {
                            // CountDownLatch to check if the SimulationScene got created
                            CountDownLatch countDownLatch = new CountDownLatch(1);
                            // execute the scene switching on the javafx application thread
                            Platform.runLater(() -> {
                                // switch to the SimulationScene and set everything up
                                SimulationScene scene = (SimulationScene) SceneSwitcher
                                        .loadScene(SceneSwitcher.SceneType.SIMULATION, getController().getStage());
                                scene.init(simulation, problem, i, simulationRuns);
                                simulation.addListener(scene);
                                simulationScene.set(scene);
                                countDownLatch.countDown();
                            });

                            try {
                                // wait for the SimulationScene to be set
                                countDownLatch.await();
                            } catch (InterruptedException exc) {
                                throw new RuntimeException(exc);
                            }
                        },
                        (simulation, problem) -> {
                            // remove the scene from the list of listeners
                            simulation.removeListener(simulationScene.get());

                            // check if gui got stopped
                            return simulationScene.get().isClosed();
                        },
                        result -> {
                            // execute the scene switching on the javafx thread
                            Platform.runLater(() -> {
                                RaterScene raterScene = (RaterScene) SceneSwitcher
                                        .loadScene(SceneSwitcher.SceneType.RATING, getController().getStage());
                                raterScene.init(problemGroup.problems(), result);
                            });
                        });
            }).start();
        });

        return startSimulationButton;
    }

    private HBox createSimulationRunsHBox() {
        HBox simulationRunsHBox = new HBox();
        simulationRunsHBox.setMaxWidth(200);

        Label simulationRunsLabel = new Label("Simulation Runs:");
        TextField simulationRunsTextField = createPositiveIntegerTextField(value -> simulationRuns = value, 1);
        simulationRunsTextField.setMaxWidth(50);

        simulationRunsHBox.getChildren().addAll(simulationRunsLabel, createIntermediateRegion(0),
                simulationRunsTextField);

        return simulationRunsHBox;
    }

    private VBox createDeliveryServiceChoiceBox() {
        VBox deliveryServiceVBox = new VBox();
        deliveryServiceVBox.setMaxWidth(200);
        deliveryServiceVBox.setSpacing(10);

        HBox labelHBox = new HBox();
        Label label = new Label("Delivery Service:");
        labelHBox.getChildren().addAll(label);

        HBox choiceBoxHBox = new HBox();
        ChoiceBox<DeliveryService.Factory> choiceBox = new ChoiceBox<>();

        choiceBox.getItems().setAll(
                DeliveryService.BASIC,
                DeliveryService.OUR,
                DeliveryService.BOGO);
        choiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DeliveryService.Factory deliveryService) {
                if (deliveryService instanceof BasicDeliveryService.Factory) {
                    return "Basic Delivery Service";
                }
                if (deliveryService instanceof OurDeliveryService.Factory) {
                    return "Our Delivery Service";
                }
                if (deliveryService instanceof BogoDeliveryService.Factory) {
                    return "Bogo Delivery Service";
                }

                return "Delivery Service";
            }

            @Override
            public DeliveryService.Factory fromString(String distanceCalculator) {
                throw new UnsupportedOperationException();
            }
        });

        choiceBox.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldValue, newValue) -> deliveryServiceFactory = choiceBox.getItems().get((Integer) newValue));

        choiceBox.getSelectionModel().select(0);

        choiceBoxHBox.getChildren().addAll(choiceBox);

        deliveryServiceVBox.getChildren().addAll(label, choiceBox);

        return deliveryServiceVBox;
    }

    private VBox createProblemsVBox() {
        List<ProblemArchetype> availableProblems = IOHelper.readProblems();
        for (ProblemArchetype problem : List.copyOf(availableProblems)) {
            for (ProblemArchetype alreadyAddedProblem : problems) {
                if (problem.name().equals(alreadyAddedProblem.name())) {
                    availableProblems.remove(problem);
                    break;
                }
            }
        }

        Label problemsLabel = new Label("Problems:");

        ListView<ProblemArchetype> problemsListView = createProblemsListView(problems);
        problemsListView.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldValue, newValue) -> {
                    if (newValue == null || newValue.intValue() == -1) {
                        selectedProblemProperty.set(null);
                        return;
                    }
                    selectedProblemProperty.set(problemsListView.getItems().get((Integer) newValue));
                });

        Label availableProblemsLabel = new Label("Available Problems:");

        ObjectProperty<ProblemArchetype> selectedAvailableProperty = new SimpleObjectProperty<>();
        ListView<ProblemArchetype> availableProblemsListView = createProblemsListView(availableProblems);
        availableProblemsListView.getSelectionModel().selectedIndexProperty()
                .addListener((obs, oldValue, newValue) -> {
                    if (newValue == null || newValue.intValue() == -1) {
                        selectedAvailableProperty.set(null);
                        return;
                    }
                    selectedAvailableProperty.set(availableProblemsListView.getItems().get((Integer) newValue));
                });

        Label problemDetailsLabel = new Label();
        problemDetailsLabel.textProperty()
                .bind(selectedProblemProperty.asString().concat(" details:"));

        HBox buttonsWrapperBox = new HBox();
        buttonsWrapperBox.setAlignment(Pos.CENTER);
        buttonsWrapperBox.setSpacing(10);

        Button deleteProblemButton = new Button("Delete selected Problem");
        deleteProblemButton.setOnAction((event) -> {
            availableProblems.add(problemsListView.getSelectionModel().getSelectedItem());
            problems.remove(problemsListView.getSelectionModel().getSelectedItem());
            availableProblemsListView.setItems(FXCollections.observableList(availableProblems));
            problemsListView.getSelectionModel().clearSelection();
            problemsListView.refresh();
            availableProblemsListView.refresh();
        });

        Button addProblemButton = new Button("Add selected available Problem");
        addProblemButton.setOnAction((event) -> {
            availableProblems.remove(availableProblemsListView.getSelectionModel().getSelectedItem());
            problems.add(availableProblemsListView.getSelectionModel().getSelectedItem());
            availableProblemsListView.setItems(FXCollections.observableList(availableProblems));
            availableProblemsListView.getSelectionModel().clearSelection();
            problemsListView.refresh();
            availableProblemsListView.refresh();
        });

        Button createProblemButton = new Button("Create new Problem");
        createProblemButton.setOnAction((event) -> {
            CreateProblemScene scene = (CreateProblemScene) SceneSwitcher
                    .loadScene(SceneSwitcher.SceneType.CREATE_PROBLEM, getController().getStage());
            scene.init(problems);
        });

        buttonsWrapperBox.getChildren().addAll(deleteProblemButton, addProblemButton, createProblemButton);

        TabPane problemDetailsPane = createProblemDetailsPane();

        VBox wrapperVBox = new VBox(problemsLabel, problemsListView, buttonsWrapperBox, availableProblemsLabel,
                availableProblemsListView, problemDetailsLabel, problemDetailsPane);
        wrapperVBox.setAlignment(Pos.CENTER);
        wrapperVBox.setSpacing(10);

        problemsListView.getSelectionModel().select(0);
        return wrapperVBox;
    }

    private TabPane createProblemDetailsPane() {
        final TabPane pane = new TabPane();
        pane.maxHeight(300);

        final Tab ratersTab = createRatersTab();

        final TableView<Map<RatingCriteria, Rater.Factory>> ratersTableView = createRatersTable();

        ratersTab.setContent(ratersTableView);

        final Tab nodesTab = new Tab("Nodes");
        final TableView<Node> nodesTableView = createNodesTableView();

        nodesTab.setContent(nodesTableView);

        final Tab edgesTab = new Tab("Edges");
        final TableView<Edge> edgesTableView = createEdgesTableView();

        edgesTab.setContent(edgesTableView);

        final Tab vehiclesTab = new Tab("Vehicles");
        final TableView<Vehicle> vehiclesTableView = createVehiclesTable();

        vehiclesTab.setContent(vehiclesTableView);

        final Tab othersTab = new Tab("Other"); // Note: simulationLength & orderGenerator & distanceCalculator
        final VBox othersVBox = createOthersVBox();

        othersTab.setContent(othersVBox);

        pane.getTabs().addAll(ratersTab, nodesTab, edgesTab, vehiclesTab, othersTab);

        selectedProblemProperty.addListener((obs, oldValue, newValue) -> {
            final List<Map<RatingCriteria, Rater.Factory>> raterTableData = new ArrayList<>();
            final List<Vehicle> vehiclesTableData = new ArrayList<>();
            final List<Node> nodesTableData = new ArrayList<>();
            final List<Edge> edgesTableData = new ArrayList<>();

            if (obs.getValue() != null) {
                nodesTableData.addAll(obs.getValue().vehicleManager().getRegion().getNodes());
                edgesTableData.addAll(obs.getValue().vehicleManager().getRegion().getEdges());
                vehiclesTableData.addAll(obs.getValue().vehicleManager().getAllVehicles());

                for (RatingCriteria criteria : RatingCriteria.values()) {
                    Map<RatingCriteria, Rater.Factory> data = new HashMap<>();
                    data.put(criteria, obs.getValue().raterFactoryMap().get(criteria));
                    raterTableData.add(data);
                }
            }

            ratersTableView.setItems(FXCollections.observableList(raterTableData));
            nodesTableView.setItems(FXCollections.observableList(nodesTableData));
            edgesTableView.setItems(FXCollections.observableList(edgesTableData));
            vehiclesTableView.setItems(FXCollections.observableList(vehiclesTableData));
        });

        return pane;
    }

    private VBox createOthersVBox() {
        final LongProperty simulationLengthProperty = new SimpleLongProperty();
        final StringProperty orderGeneratorClassNameProperty = new SimpleStringProperty();
        final StringProperty orderGeneratorParametersProperty = new SimpleStringProperty();
        final StringProperty distanceCalculatorClassNameProperty = new SimpleStringProperty();

        selectedProblemProperty.addListener((obs, oldValue, newValue) -> {
            if (obs.getValue() == null) {
                simulationLengthProperty.set(-1);
                orderGeneratorClassNameProperty.set("invalid");
                orderGeneratorParametersProperty.set("invalid");
                distanceCalculatorClassNameProperty.set("invalid");
                return;
            }

            simulationLengthProperty.set(obs.getValue().simulationLength());
            orderGeneratorClassNameProperty
                    .set(obs.getValue().orderGeneratorFactory().getClass().getDeclaringClass().getSimpleName());
            String parametersText = "unknown";
            if (obs.getValue().orderGeneratorFactory() instanceof FridayOrderGenerator.Factory) {
                FridayOrderGenerator.Factory castedFactory = (FridayOrderGenerator.Factory) obs.getValue()
                        .orderGeneratorFactory();
                parametersText = String.format(
                        "orderCount: %s\ndeliveryInterval: %s\nmaxWeight: %s\nlastTick: %s\nstandardDeviation: %s\nseed: %s",
                        castedFactory.orderCount, castedFactory.deliveryInterval, castedFactory.maxWeight,
                        castedFactory.lastTick, castedFactory.standardDeviation, castedFactory.seed);
            }
            orderGeneratorParametersProperty.set(parametersText);
            distanceCalculatorClassNameProperty.set(
                    obs.getValue().vehicleManager().getRegion().getDistanceCalculator().getClass().getSimpleName());
        });

        final VBox othersVBox = new VBox();
        othersVBox.setAlignment(Pos.CENTER);
        othersVBox.setSpacing(5);
        othersVBox.minHeight(300);

        Label simulationLengthLabel = new Label();
        simulationLengthLabel.textProperty().bind(
                new SimpleStringProperty("Simulation length: ").concat(simulationLengthProperty).concat(" ticks"));

        Label distanceCalculatorLabel = new Label();
        distanceCalculatorLabel.textProperty()
                .bind(new SimpleStringProperty("Distance calculator: ").concat(distanceCalculatorClassNameProperty));

        Label orderGeneratorLabel = new Label();
        orderGeneratorLabel.textProperty().bind(new SimpleStringProperty("Order generator: ")
                .concat(orderGeneratorClassNameProperty));
        Label orderGeneratorParameterLabel = new Label();
        orderGeneratorParameterLabel.textProperty()
                .bind(new SimpleStringProperty("Order generator parameters:\n")
                        .concat(orderGeneratorParametersProperty));

        othersVBox.getChildren().addAll(simulationLengthLabel, distanceCalculatorLabel, orderGeneratorLabel,
                orderGeneratorParameterLabel);
        return othersVBox;
    }

    private TableView<Edge> createEdgesTableView() {
        final TableView<Edge> tableView = new TableView<>();
        final TableColumn<Edge, String> nameTableColumn = new TableColumn<>("Name");
        final TableColumn<Edge, String> locationATableColumn = new TableColumn<>("LocationA");
        final TableColumn<Edge, String> locationBTableColumn = new TableColumn<>("LocationB");
        final TableColumn<Edge, String> lengthTableColumn = new TableColumn<>("Length");

        nameTableColumn
                .setCellValueFactory((cellData) -> new SimpleStringProperty(cellData.getValue().getName().trim()));
        locationATableColumn.setCellValueFactory(
                (cellData) -> new SimpleStringProperty(cellData.getValue().getNodeA().getLocation().toString()));
        locationBTableColumn.setCellValueFactory(
                (cellData) -> new SimpleStringProperty(cellData.getValue().getNodeB().getLocation().toString()));
        lengthTableColumn.setCellValueFactory(
                (cellData) -> new SimpleStringProperty(Long.toString(cellData.getValue().getDuration())));

        tableView.getColumns().addAll(nameTableColumn, locationATableColumn, locationBTableColumn, lengthTableColumn);

        return tableView;
    }

    private TableView<Node> createNodesTableView() {
        final TableView<Node> tableView = new TableView<>();
        final TableColumn<Node, String> nameTableColumn = new TableColumn<>("Name");
        final TableColumn<Node, String> locationTableColumn = new TableColumn<>("Location");

        nameTableColumn.setCellValueFactory((cellData) -> new SimpleStringProperty(cellData.getValue().getName()
                .trim()));
        locationTableColumn.setCellValueFactory(
                (cellData) -> new SimpleStringProperty(cellData.getValue().getLocation().toString()));

        tableView.getColumns().addAll(nameTableColumn, locationTableColumn);

        return tableView;
    }

    private TableView<Vehicle> createVehiclesTable() {
        final TableView<Vehicle> tableView = new TableView<>();
        final TableColumn<Vehicle, String> idTableColumn = new TableColumn<>("Id");
        final TableColumn<Vehicle, String> locationTableColumn = new TableColumn<>("Location");
        final TableColumn<Vehicle, String> capacityTableColumn = new TableColumn<>("Capacity");

        idTableColumn.setCellValueFactory((cellData) -> new SimpleStringProperty(Integer.toString(
                cellData.getValue().getId())));
        locationTableColumn.setCellValueFactory((cellData) -> new SimpleStringProperty(
                cellData.getValue().getStartingNode().getComponent().getLocation().toString()));
        capacityTableColumn.setCellValueFactory((cellData) -> new SimpleStringProperty(Double.toString(
                cellData.getValue().getCapacity())));

        tableView.getColumns().addAll(idTableColumn, locationTableColumn, capacityTableColumn);
        return tableView;
    }

    private TableView<Map<RatingCriteria, Rater.Factory>> createRatersTable() {
        final TableView<Map<RatingCriteria, Rater.Factory>> tableView = new TableView<>();
        final TableColumn<Map<RatingCriteria, Rater.Factory>, String> criteriaNameTableColumn = new TableColumn<>(
                "Criteria");
        final TableColumn<Map<RatingCriteria, Rater.Factory>, String> raterNameTableColumn = new TableColumn<>(
                "Rater");
        final TableColumn<Map<RatingCriteria, Rater.Factory>, String> raterParametersTableColumn = new TableColumn<>(
                "Parameters");

        criteriaNameTableColumn.setCellValueFactory(
                (cellData) -> new SimpleStringProperty(cellData.getValue().keySet().toArray()[0].toString()));
        raterNameTableColumn.setCellValueFactory((cellData) -> {
            if (cellData.getValue().values().contains(null)) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().values()
                    .stream().findFirst().get().getClass().getDeclaringClass().getSimpleName());
        });
        raterParametersTableColumn.setCellValueFactory((cellData) -> {
            if (cellData.getValue().values().contains(null)) {
                return new SimpleStringProperty("unused");
            }

            Rater.Factory raterFactory = cellData.getValue().values().stream().findFirst().get();

            if (raterFactory instanceof AmountDeliveredRater.Factory) {
                AmountDeliveredRater.Factory castedRaterFactory = (AmountDeliveredRater.Factory) raterFactory;
                return new SimpleStringProperty(String.format("factor: %s", castedRaterFactory.factor));
            }
            if (raterFactory instanceof InTimeRater.Factory) {
                InTimeRater.Factory castedFactory = (InTimeRater.Factory) raterFactory;
                return new SimpleStringProperty(String.format("ignoredTicksOff: %s, maxTicksOff: %s",
                        castedFactory.ignoredTicksOff, castedFactory.maxTicksOff));
            }
            if (raterFactory instanceof TravelDistanceRater.Factory) {
                TravelDistanceRater.Factory castedFactory = (TravelDistanceRater.Factory) raterFactory;
                return new SimpleStringProperty(String.format("factor: %s", castedFactory.factor));
            }
            return new SimpleStringProperty("unknown");
        });

        tableView.getColumns().addAll(criteriaNameTableColumn, raterNameTableColumn, raterParametersTableColumn);
        return tableView;
    }

    private Tab createRatersTab() {
        return new Tab("Raters");
    }

    private ListView<ProblemArchetype> createProblemsListView(List<ProblemArchetype> data) {
        ListView<ProblemArchetype> listView = new ListView<>(FXCollections.observableList(data));
        listView.setMaxHeight(300);
        listView.setPrefHeight(100);
        listView.setMaxWidth(150);
        return listView;
    }

    @Override
    public void initReturnButton() {
        ((HBox) root.getBottom()).getChildren().remove(returnButton);
    }

    @Override
    public MainMenuSceneController getController() {
        return controller;
    }
}
