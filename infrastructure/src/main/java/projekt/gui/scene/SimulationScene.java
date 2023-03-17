package projekt.gui.scene;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import projekt.delivery.archetype.ProblemArchetype;
import projekt.delivery.event.ArrivedAtEdgeEvent;
import projekt.delivery.event.ArrivedAtNodeEvent;
import projekt.delivery.event.Event;
import projekt.delivery.event.SpawnEvent;
import projekt.delivery.routing.ConfirmedOrder;
import projekt.delivery.routing.Region;
import projekt.delivery.routing.Vehicle;
import projekt.delivery.routing.VehicleManager;
import projekt.delivery.simulation.Simulation;
import projekt.delivery.simulation.SimulationListener;
import projekt.gui.controller.ControlledScene;
import projekt.gui.controller.SimulationSceneController;
import projekt.gui.pane.ControlsPane;
import projekt.gui.pane.MapPane;

import java.util.List;

public class SimulationScene extends Scene implements SimulationListener, ControlledScene<SimulationSceneController> {

    private final BorderPane root;
    private final SimulationSceneController controller;

    private MapPane mapPane;
    private ControlsPane controlsPane;
    private TableView<Vehicle> vehicelsTableView;
    private VehicleManager vehicleManager;

    private boolean closed;

    public SimulationScene() {
        super(new BorderPane());
        controller = new SimulationSceneController();
        root = (BorderPane) getRoot();

        root.setPrefSize(700, 700);
        root.getStylesheets().addAll("projekt/gui/darkMode.css", "projekt/gui/simulationStyle.css");
    }

    public void init(Simulation simulation, ProblemArchetype problem, int run, int simulationRuns) {
        vehicleManager = simulation.getDeliveryService().getVehicleManager();
        Region region = vehicleManager.getRegion();

        mapPane = new MapPane(region.getNodes(), region.getEdges(), vehicleManager.getVehicles());

        controlsPane = new ControlsPane(simulation, problem, run, simulationRuns, problem.simulationLength(), mapPane);
        TitledPane titledControlsPane = new TitledPane("Controls", controlsPane);
        titledControlsPane.setCollapsible(false);

        final TitledPane titledVehiclesPane = createTitledPane();

        final VBox bottomContainer = new VBox(titledControlsPane);

        root.setCenter(mapPane);
        root.setBottom(bottomContainer);
        root.setRight(titledVehiclesPane);

        // stop the simulation when closing the window
        controller.getStage().setOnCloseRequest(e -> {
            simulation.endSimulation();
            closed = true;
        });
    }

    private TitledPane createTitledPane() {
        vehicelsTableView = new TableView<>();
        final TableColumn<Vehicle, String> idTableColumn = new TableColumn<>("Id");
        final TableColumn<Vehicle, String> locationTableColumn = new TableColumn<>("Location");
        final TableColumn<Vehicle, List<ConfirmedOrder>> ordersTableColumn = new TableColumn<>("Orders");

        idTableColumn.setCellValueFactory((cellData) -> new SimpleStringProperty(Integer.toString(
                cellData.getValue().getId())));

        locationTableColumn.setCellValueFactory((cellData) -> {
            VehicleManager.Occupied<?> occupied = cellData.getValue().getOccupied();
            if (occupied.getComponent() instanceof Region.Node) {
                Region.Node castedNode = (Region.Node) occupied.getComponent();
                return new SimpleStringProperty(String.format("%s %s", castedNode.getName(), castedNode.getLocation()));
            }
            if (occupied.getComponent() instanceof Region.Edge) {
                Region.Edge castedEdge = (Region.Edge) occupied.getComponent();
                return new SimpleStringProperty(String.format("%s %s,%s", castedEdge.getName(),
                        castedEdge.getNodeA().getLocation(), castedEdge.getNodeB().getLocation()));
            }
            return new SimpleStringProperty("invalid");
        });

        ordersTableColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(List<ConfirmedOrder> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.size() == 0) {
                    setText(null);
                    return;
                }

                final Accordion accordion = new Accordion();
                for (ConfirmedOrder confirmedOrder : item) {
                    final ListView<String> foodListView = new ListView<String>(
                            FXCollections.observableList(confirmedOrder.getFoodList()));
                    foodListView.setPrefHeight(75);
                    final TitledPane pane = new TitledPane(String.format("Order %s", confirmedOrder.getOrderID()),
                            foodListView);
                    accordion.getPanes().add(pane);
                }
                setGraphic(accordion);
            }
        });
        ordersTableColumn.setCellValueFactory((cellData) -> {
            Vehicle vehicle = cellData.getValue();
            return new SimpleObjectProperty<>(List.copyOf(vehicle.getOrders()));
        });
        ordersTableColumn.prefWidthProperty().bind(vehicelsTableView.widthProperty()
                .subtract(idTableColumn.widthProperty()).subtract(locationTableColumn.widthProperty()));

        vehicelsTableView.getColumns().addAll(idTableColumn, locationTableColumn, ordersTableColumn);

        vehicelsTableView.prefHeightProperty().bind(mapPane.heightProperty());

        TitledPane titledPane = new TitledPane("Vehicles", vehicelsTableView);
        titledPane.prefWidthProperty().bind(root.widthProperty().multiply(new SimpleDoubleProperty(0.4)));

        return titledPane;
    }

    @Override
    public void onTick(List<Event> events, long tick) {
        // Execute GUI updates on the javafx application thread
        Platform.runLater(() -> {
            events.stream()
                    .filter(SpawnEvent.class::isInstance)
                    .map(SpawnEvent.class::cast)
                    .forEach(spawnEvent -> mapPane.addVehicle(spawnEvent.getVehicle()));

            events.stream()
                    .filter(ArrivedAtNodeEvent.class::isInstance)
                    .map(ArrivedAtNodeEvent.class::cast)
                    .forEach(arrivedAtNodeEvent -> mapPane.redrawVehicle(arrivedAtNodeEvent.getVehicle()));

            events.stream()
                    .filter(ArrivedAtEdgeEvent.class::isInstance)
                    .map(ArrivedAtEdgeEvent.class::cast)
                    .forEach(arrivedAtEdgeEvent -> mapPane.redrawVehicle(arrivedAtEdgeEvent.getVehicle()));

            final List<Vehicle> vehiclesTableData = List.copyOf(vehicleManager.getVehicles());
            vehicelsTableView.setItems(FXCollections.observableList(vehiclesTableData));
            vehicelsTableView.refresh();

            controlsPane.updateTickLabel(tick);
        });

    }

    @Override
    public SimulationSceneController getController() {
        return controller;
    }

    public boolean isClosed() {
        return closed;
    }
}
