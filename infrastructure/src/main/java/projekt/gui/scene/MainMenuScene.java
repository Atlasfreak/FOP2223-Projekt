package projekt.gui.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
        root.setCenter(createOptionsVBox());
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
        optionsVbox.setPrefSize(200, 100);
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
            // store the SimulationScene
            AtomicReference<SimulationScene> simulationScene = new AtomicReference<>();
            // Execute the GUIRunner in a separate Thread to prevent it from blocking the
            // GUI
            new Thread(() -> {
                ProblemGroup problemGroup = new ProblemGroupImpl(problems,
                        Arrays.stream(RatingCriteria.values()).toList());
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
        Label problemsLabel = new Label("Problems:");

        ListView<ProblemArchetype> problemsListView = createProblemsListView();
        problemsListView.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldValue, newValue) -> selectedProblemProperty
                        .set(problemsListView.getItems().get((Integer) newValue)));

        Label problemDetailsLabel = new Label();
        problemDetailsLabel.textProperty()
                .bind(selectedProblemProperty.asString().concat(" details:"));

        TabPane problemDetailsPane = createProblemDetailsPane();

        VBox wrapperVBox = new VBox(problemsLabel, problemsListView, problemDetailsLabel, problemDetailsPane);
        wrapperVBox.setAlignment(Pos.CENTER);
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

        final Tab otherTab = new Tab("Other"); // Note: simulationLength & orderGenerator & distanceCalculator

        pane.getTabs().addAll(ratersTab, nodesTab, edgesTab, vehiclesTab, otherTab);

        final List<Map<RatingCriteria, Rater.Factory>> raterTableData = new ArrayList<>();
        final List<Vehicle> vehiclesTableData = new ArrayList<>();

        selectedProblemProperty.addListener((obs, oldValue, newValue) -> {
            System.out.println("aaaaaaaaah Whyyyyyy");
            raterTableData.clear();
            vehiclesTableData.clear();
            vehiclesTableData.addAll(obs.getValue().vehicleManager().getAllVehicles());
            for (RatingCriteria criteria : RatingCriteria.values()) {
                Map<RatingCriteria, Rater.Factory> data = new HashMap<>();
                data.put(criteria, obs.getValue().raterFactoryMap().get(criteria));
                raterTableData.add(data);
            }
            ratersTableView.setItems(FXCollections.observableList(raterTableData));
            vehiclesTableView.setItems(FXCollections.observableList(vehiclesTableData));
        });

        return pane;
    }

    private TableView<Edge> createEdgesTableView() {
        final TableView<Edge> tableView = new TableView<>();
        final TableColumn<Edge, String> nameTableColumn = new TableColumn<>("Name");
        final TableColumn<Edge, String> locationATableColumn = new TableColumn<>("LocationB");
        final TableColumn<Edge, String> locationBTableColumn = new TableColumn<>("LocationA");
        final TableColumn<Edge, String> lengthTableColumn = new TableColumn<>("Length");

        return tableView;
    }

    private TableView<Node> createNodesTableView() {
        final TableView<Node> tableView = new TableView<>();
        final TableColumn<Node, String> nameTableColumn = new TableColumn<>("Name");
        final TableColumn<Node, String> locationTableColumn = new TableColumn<>("Location");
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

        tableView.getColumns().addAll(idTableColumn, locationTableColumn,
                capacityTableColumn);
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

        criteriaNameTableColumn.setCellValueFactory((cellData) -> new SimpleStringProperty(
                cellData.getValue().keySet().toArray()[0].toString()));
        raterNameTableColumn.setCellValueFactory((cellData) -> {
            return new SimpleStringProperty(cellData.getValue().values().stream().findFirst().get().getClass()
                    .getDeclaringClass().getSimpleName());
        });
        raterParametersTableColumn.setCellValueFactory((cellData) -> {
            Rater.Factory raterFactory = cellData.getValue().values().stream().findFirst().get();
            if (raterFactory == null) {
                return new SimpleStringProperty("unused");
            }
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

    private ListView<ProblemArchetype> createProblemsListView() {
        ListView<ProblemArchetype> listView = new ListView<>(FXCollections.observableList(problems));
        listView.setMaxHeight(300);
        listView.setMaxWidth(150);
        listView.minHeight(100);
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
