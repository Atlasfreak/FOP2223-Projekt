package projekt.gui.scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import projekt.base.ChessboardDistanceCalculator;
import projekt.base.DistanceCalculator;
import projekt.base.EuclideanDistanceCalculator;
import projekt.base.ManhattanDistanceCalculator;
import projekt.delivery.archetype.ProblemArchetype;
import projekt.delivery.archetype.ProblemArchetypeImpl;
import projekt.delivery.generator.EmptyOrderGenerator;
import projekt.delivery.generator.FridayOrderGenerator;
import projekt.delivery.generator.OrderGenerator;
import projekt.delivery.rating.AmountDeliveredRater;
import projekt.delivery.rating.InTimeRater;
import projekt.delivery.rating.RatingCriteria;
import projekt.delivery.rating.TravelDistanceRater;
import projekt.delivery.routing.DijkstraPathCalculator;
import projekt.delivery.routing.Region;
import projekt.delivery.routing.VehicleManager;
import projekt.gui.controller.CreateProblemSceneController;
import projekt.gui.pane.MapPane;

public class CreateProblemScene extends MenuScene<CreateProblemSceneController> {
    private final VehicleManager.Builder vehicleManagerBuilder = VehicleManager.builder();
    private final UnaryOperator<Change> integerFilter = change -> {
        final String newText = change.getControlNewText();
        if (newText.matches("([0-9]*)?")) {
            return change;
        }
        return null;
    };

    private final UnaryOperator<Change> doubleFilter = change -> {
        final String newText = change.getControlNewText();
        if (newText.matches("[0-9]*\\.?[0-9]*")) {
            return change;
        }
        return null;
    };

    private OrderGenerator.FactoryBuilder orderGeneratorBuilder;
    private InTimeRater.FactoryBuilder inTimeRaterBuilder;
    private AmountDeliveredRater.FactoryBuilder amountDeliveredRaterBuilder;
    private TravelDistanceRater.FactoryBuilder travelDistanceRaterBuilder;
    private ProblemArchetype problem;
    private long simulationLength;
    private String name;

    private Map<Control, Boolean> validFields = new HashMap<>();
    private Border errorBorder = new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, new CornerRadii(2),
            new BorderWidths(2), new Insets(-2)));

    public CreateProblemScene() {
        super(new CreateProblemSceneController(), "Create Problem");
        vehicleManagerBuilder.pathCalculator(new DijkstraPathCalculator());
    }

    @Override
    public void initComponents() {
        root.setCenter(createMainContainer());
    }

    private ScrollPane createMainContainer() {
        final ScrollPane container = new ScrollPane();
        final VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);

        vBox.getChildren().addAll(createForm());
        vBox.setPadding(new Insets(20, 20, 20, 20));
        container.setContent(vBox);
        container.setFitToWidth(true);
        return container;
    }

    private GridPane createForm() {
        final GridPane formGridPane = new GridPane();
        formGridPane.setHgap(10);
        formGridPane.setVgap(10);
        formGridPane.setAlignment(Pos.CENTER);

        final Label nameLabel = new Label("Problem Name:");
        formGridPane.add(nameLabel, 0, 0);

        final TextField nameField = new TextField();
        formGridPane.add(nameField, 1, 0);
        nameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == "" || problems.stream().map((problem) -> problem.name())
                    .anyMatch((problem) -> problem.equals(newValue))) {
                nameField.setBorder(errorBorder);
                validFields.put(nameField, false);
                return;
            }
            nameField.setBorder(null);
            validFields.put(nameField, true);
            name = newValue;
        });

        final Label simulationLengthLabel = new Label("Simulation Length:");
        formGridPane.add(simulationLengthLabel, 0, 1);

        final TextField simulationLengthField = new TextField();
        formGridPane.add(simulationLengthField, 1, 1);

        simulationLengthField
                .setTextFormatter(new TextFormatter<>(new LongStringConverter(), Long.valueOf(0), integerFilter));
        simulationLengthField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                simulationLength = Long.parseLong(newValue);
                simulationLengthField.setBorder(null);
                validFields.put(simulationLengthField, true);
            } catch (NumberFormatException e) {
                validFields.put(simulationLengthField, false);
                simulationLengthField.setBorder(errorBorder);
            }
        });

        int offset = 2;
        offset += createRaterSelection(formGridPane, offset);
        offset += createOrderGeneratorSelection(formGridPane, offset);

        createSubmitButton(formGridPane, offset);

        return formGridPane;
    }

    private int createOrderGeneratorSelection(final GridPane formGridPane, int rowOffset) {
        int rows = 8;
        final BooleanProperty fridaySelectedProperty = new SimpleBooleanProperty(false);

        Label label = new Label("Select used Order Generator:");
        formGridPane.add(label, 0, rowOffset + 0);

        ChoiceBox<OrderGenerator.FactoryBuilder> choiceBox = new ChoiceBox<>();

        choiceBox.getItems().addAll(FridayOrderGenerator.Factory.builder(), EmptyOrderGenerator.Factory::new);
        choiceBox.setConverter(new StringConverter<OrderGenerator.FactoryBuilder>() {
            @Override
            public String toString(OrderGenerator.FactoryBuilder orderGenerator) {
                if (orderGenerator instanceof FridayOrderGenerator.FactoryBuilder) {
                    return "Friday Order Generator";
                }
                if (orderGenerator instanceof EmptyOrderGenerator.FactoryBuilder) {
                    return "Empty Order Generator";
                }

                return "Order Generator";
            }

            @Override
            public OrderGenerator.FactoryBuilder fromString(String string) {
                throw new UnsupportedOperationException("Unimplemented method 'fromString'");
            }
        });

        choiceBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
            orderGeneratorBuilder = choiceBox.getItems().get((Integer) obs.getValue());
            fridaySelectedProperty.set(orderGeneratorBuilder instanceof FridayOrderGenerator.FactoryBuilder);
        });

        formGridPane.add(choiceBox, 0, rowOffset + 1);

        Label orderCountLabel = new Label("Order Count:");
        formGridPane.add(orderCountLabel, 0, rowOffset + 2);

        TextField orderCountField = new TextField();
        orderCountField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        orderCountField.disableProperty().bind(fridaySelectedProperty.not());
        orderCountField.textProperty().addListener((obs, oldValue, newValue) -> {
            FridayOrderGenerator.FactoryBuilder castedBuilder = (FridayOrderGenerator.FactoryBuilder) orderGeneratorBuilder;
            try {
                castedBuilder.setOrderCount(Integer.parseInt(newValue));
                orderCountField.setBorder(null);
                validFields.put(orderCountField, true);
            } catch (NumberFormatException e) {
                validFields.put(orderCountField, false);
                orderCountField.setBorder(errorBorder);
            }
        });

        formGridPane.add(orderCountField, 1, rowOffset + 2);

        Label deliveryIntervalLabel = new Label("Delivery Intervall:");
        formGridPane.add(deliveryIntervalLabel, 0, rowOffset + 3);

        TextField deliveryIntervalField = new TextField();
        deliveryIntervalField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        deliveryIntervalField.disableProperty().bind(fridaySelectedProperty.not());
        deliveryIntervalField.textProperty().addListener((obs, oldValue, newValue) -> {
            FridayOrderGenerator.FactoryBuilder castedBuilder = (FridayOrderGenerator.FactoryBuilder) orderGeneratorBuilder;
            try {
                castedBuilder.setDeliveryInterval(Integer.parseInt(newValue));
                deliveryIntervalField.setBorder(null);
                validFields.put(deliveryIntervalField, true);
            } catch (NumberFormatException e) {
                validFields.put(deliveryIntervalField, false);
                deliveryIntervalField.setBorder(errorBorder);
            }
        });

        formGridPane.add(deliveryIntervalField, 1, rowOffset + 3);

        Label maxWeightLabel = new Label("Max Weight:");
        formGridPane.add(maxWeightLabel, 0, rowOffset + 4);

        TextField maxWeightField = new TextField();
        maxWeightField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        maxWeightField.disableProperty().bind(fridaySelectedProperty.not());
        maxWeightField.textProperty().addListener((obs, oldValue, newValue) -> {
            FridayOrderGenerator.FactoryBuilder castedBuilder = (FridayOrderGenerator.FactoryBuilder) orderGeneratorBuilder;
            try {
                castedBuilder.setMaxWeight(Double.parseDouble(newValue));
                maxWeightField.setBorder(null);
                validFields.put(maxWeightField, true);
            } catch (NumberFormatException e) {
                validFields.put(maxWeightField, false);
                maxWeightField.setBorder(errorBorder);
            }
        });

        formGridPane.add(maxWeightField, 1, rowOffset + 4);

        Label standardDeviationLabel = new Label("Standard Deviation:");
        formGridPane.add(standardDeviationLabel, 0, rowOffset + 5);

        TextField standardDeviationField = new TextField();
        standardDeviationField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        standardDeviationField.disableProperty().bind(fridaySelectedProperty.not());
        standardDeviationField.textProperty().addListener((obs, oldValue, newValue) -> {
            FridayOrderGenerator.FactoryBuilder castedBuilder = (FridayOrderGenerator.FactoryBuilder) orderGeneratorBuilder;
            try {
                castedBuilder.setStandardDeviation(Double.parseDouble(newValue));
                standardDeviationField.setBorder(null);
                validFields.put(standardDeviationField, true);
            } catch (NumberFormatException e) {
                standardDeviationField.setBorder(errorBorder);
                validFields.put(standardDeviationField, false);
            }
        });

        formGridPane.add(standardDeviationField, 1, rowOffset + 5);

        Label lastTickLabel = new Label("Last Tick:");
        formGridPane.add(lastTickLabel, 0, rowOffset + 6);

        TextField lastTickField = new TextField();
        lastTickField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        lastTickField.disableProperty().bind(fridaySelectedProperty.not());
        lastTickField.textProperty().addListener((obs, oldValue, newValue) -> {
            FridayOrderGenerator.FactoryBuilder castedBuilder = (FridayOrderGenerator.FactoryBuilder) orderGeneratorBuilder;
            try {
                castedBuilder.setLastTick(Integer.parseInt(newValue));
                lastTickField.setBorder(null);
                validFields.put(lastTickField, true);
            } catch (NumberFormatException e) {
                validFields.put(lastTickField, false);
                lastTickField.setBorder(errorBorder);
            }
        });

        formGridPane.add(lastTickField, 1, rowOffset + 6);

        Label seedLabel = new Label("Seed:");
        formGridPane.add(seedLabel, 0, rowOffset + 7);

        TextField seedField = new TextField();
        seedField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        seedField.disableProperty().bind(fridaySelectedProperty.not());
        seedField.textProperty().addListener((obs, oldValue, newValue) -> {
            FridayOrderGenerator.FactoryBuilder castedBuilder = (FridayOrderGenerator.FactoryBuilder) orderGeneratorBuilder;
            try {
                castedBuilder.setSeed(Integer.parseInt(newValue));
                seedField.setBorder(null);
                validFields.put(seedField, true);
            } catch (NumberFormatException e) {
                validFields.put(seedField, false);
                seedField.setBorder(errorBorder);
            }
        });

        formGridPane.add(seedField, 1, rowOffset + 7);

        return rows;
    }

    private int createRaterSelection(final GridPane formGridPane, int rowOffset) {
        int rows = 8;

        final Label ratersLabel = new Label("Select used Raters:");
        formGridPane.add(ratersLabel, 0, rowOffset + 0, 2, 1);

        final CheckBox inTimeRaterCheckBox = new CheckBox("In Time Rater");
        formGridPane.add(inTimeRaterCheckBox, 0, rowOffset + 1, 2, 1);
        inTimeRaterCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                inTimeRaterBuilder = InTimeRater.Factory.builder();
                return;
            }
            inTimeRaterBuilder = null;
        });

        final Label inTimeRaterIgnoredTicksOffLabel = new Label("Ignored Ticks Off:");
        formGridPane.add(inTimeRaterIgnoredTicksOffLabel, 0, rowOffset + 2);
        final TextField inTimeRaterIgnoredTicksOffField = new TextField();
        formGridPane.add(inTimeRaterIgnoredTicksOffField, 1, rowOffset + 2);

        inTimeRaterIgnoredTicksOffField
                .setTextFormatter(new TextFormatter<>(new LongStringConverter(), Long.valueOf(0), integerFilter));
        inTimeRaterIgnoredTicksOffField.disableProperty().bind(inTimeRaterCheckBox.selectedProperty().not());
        inTimeRaterIgnoredTicksOffField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                inTimeRaterBuilder.setIgnoredTicksOff(Long.parseLong(newValue));
                inTimeRaterIgnoredTicksOffField.setBorder(null);
                validFields.put(inTimeRaterIgnoredTicksOffField, true);
            } catch (NumberFormatException e) {
                validFields.put(inTimeRaterIgnoredTicksOffField, false);
                inTimeRaterIgnoredTicksOffField.setBorder(errorBorder);
            }
        });

        final Label inTimeRatermaxTicksOffLabel = new Label("Ignored Ticks Off:");
        formGridPane.add(inTimeRatermaxTicksOffLabel, 0, rowOffset + 3);
        final TextField inTimeRatermaxTicksOffField = new TextField();
        formGridPane.add(inTimeRatermaxTicksOffField, 1, rowOffset + 3);

        inTimeRatermaxTicksOffField
                .setTextFormatter(new TextFormatter<>(new LongStringConverter(), Long.valueOf(0), integerFilter));
        inTimeRatermaxTicksOffField.disableProperty().bind(inTimeRaterCheckBox.selectedProperty().not());
        inTimeRatermaxTicksOffField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                inTimeRaterBuilder.setMaxTicksOff(Long.parseLong(newValue));
                inTimeRatermaxTicksOffField.setBorder(null);
                validFields.put(inTimeRatermaxTicksOffField, true);
            } catch (NumberFormatException e) {
                validFields.put(inTimeRatermaxTicksOffField, false);
                inTimeRatermaxTicksOffField.setBorder(errorBorder);
            }
        });

        final CheckBox amountDeliveredRaterCheckBox = new CheckBox("Amount Delivered Rater");
        formGridPane.add(amountDeliveredRaterCheckBox, 0, rowOffset + 4, 2, 1);
        amountDeliveredRaterCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                amountDeliveredRaterBuilder = AmountDeliveredRater.Factory.builder();
                return;
            }
            amountDeliveredRaterBuilder = null;
        });

        final Label amountDeliveredRaterFactorLabel = new Label("Factor:");
        formGridPane.add(amountDeliveredRaterFactorLabel, 0, rowOffset + 5);
        final TextField amountDeliveredRaterFactorField = new TextField();
        formGridPane.add(amountDeliveredRaterFactorField, 1, rowOffset + 5);

        amountDeliveredRaterFactorField
                .setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        amountDeliveredRaterFactorField.disableProperty().bind(amountDeliveredRaterCheckBox.selectedProperty().not());
        amountDeliveredRaterFactorField.textProperty().addListener((obs, oldValue, newValue) -> {
            amountDeliveredRaterFactorField.setBorder(null);
            try {
                amountDeliveredRaterBuilder.setFactor(Double.parseDouble(newValue));
                validFields.put(amountDeliveredRaterFactorField, true);
            } catch (IllegalArgumentException e) {
                validFields.put(amountDeliveredRaterFactorField, false);
                amountDeliveredRaterFactorField.setBorder(errorBorder);
            }
        });

        final CheckBox travelDistanceRaterCheckBox = new CheckBox("Travel Distance Rater");
        formGridPane.add(travelDistanceRaterCheckBox, 0, rowOffset + 6, 2, 1);
        travelDistanceRaterCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                travelDistanceRaterBuilder = TravelDistanceRater.Factory.builder();
                return;
            }
            travelDistanceRaterBuilder = null;
        });

        final Label travelDistanceRaterFactorLabel = new Label("Factor:");
        formGridPane.add(travelDistanceRaterFactorLabel, 0, rowOffset + 7);
        final TextField travelDistanceRaterFactorField = new TextField();
        formGridPane.add(travelDistanceRaterFactorField, 1, rowOffset + 7);

        travelDistanceRaterFactorField
                .setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        travelDistanceRaterFactorField.disableProperty().bind(travelDistanceRaterCheckBox.selectedProperty().not());
        travelDistanceRaterFactorField.textProperty().addListener((obs, oldValue, newValue) -> {
            travelDistanceRaterFactorField.setBorder(null);
            try {
                travelDistanceRaterBuilder.setFactor(Double.parseDouble(newValue));
                validFields.put(travelDistanceRaterFactorField, true);
            } catch (IllegalArgumentException e) {
                validFields.put(travelDistanceRaterFactorField, false);
                travelDistanceRaterFactorField.setBorder(errorBorder);
            }
        });

        return rows;
    }

    private int createVehiclesManagerSection(final GridPane formGridPane, int offset) {
        int rows = 20;
        final Label label = new Label("Vehicle Manager");
        formGridPane.add(label, 0, offset, formGridPane.getColumnCount(), 1);

        Region.Builder regionBuilder = Region.builder();

        final ChoiceBox<DistanceCalculator> distanceCalculatorChoiceBox = new ChoiceBox<>();
        distanceCalculatorChoiceBox.getItems().addAll(new EuclideanDistanceCalculator(),
                new ChessboardDistanceCalculator(), new ManhattanDistanceCalculator());
        distanceCalculatorChoiceBox.setConverter(new StringConverter<DistanceCalculator>() {
            @Override
            public String toString(DistanceCalculator distanceCalculator) {
                if (distanceCalculator instanceof EuclideanDistanceCalculator) {
                    return "Euclidean Distance Calculator";
                }
                if (distanceCalculator instanceof ChessboardDistanceCalculator) {
                    return "Chessboard Distance Calculator";
                }
                if (distanceCalculator instanceof ManhattanDistanceCalculator) {
                    return "Manhatten Distance Calculator";
                }

                return "Distance Calculator";
            }

            @Override
            public DistanceCalculator fromString(String string) {
                throw new UnsupportedOperationException("Unimplemented method 'fromString'");
            }
        });
        distanceCalculatorChoiceBox.getSelectionModel().selectedIndexProperty()
                .addListener((obs, oldValue, newValue) -> {
                    regionBuilder
                            .distanceCalculator(distanceCalculatorChoiceBox.getItems().get((Integer) obs.getValue()));
                });

        formGridPane.add(distanceCalculatorChoiceBox, 0, offset + 1);

        final Accordion accordion = new Accordion();

        accordion.getPanes().addAll();
        accordion.setExpandedPane(accordion.getPanes().get(0));
        formGridPane.add(accordion, 0, offset + 2, formGridPane.getColumnCount(), 5);

        MapPane map = new MapPane();
        formGridPane.add(map, 0, offset + 12, formGridPane.getColumnCount(), 10);

        return rows;
    }

    private void createSubmitButton(final GridPane formGridPane, int offset) {
        final Label errorLabel = new Label("You haven't selected all necessary properties");
        formGridPane.add(errorLabel, 0, offset, formGridPane.getColumnCount(), 1);
        errorLabel.setVisible(false);

        final Button submitButton = new Button("Create");
        formGridPane.add(submitButton, 0, offset + 1, formGridPane.getColumnCount(), 1);

        submitButton.setOnAction(event -> {
            if (orderGeneratorBuilder == null
                    || vehicleManagerBuilder == null || (travelDistanceRaterBuilder == null
                            && amountDeliveredRaterBuilder == null && inTimeRaterBuilder == null)
                    || name == null || name == "" || validFields.values().size() == 0
                    || validFields.values().stream().reduce(true, (left, right) -> left & right)) {
                errorLabel.setVisible(true);
                return;
            }
            errorLabel.setVisible(false);
            problem = new ProblemArchetypeImpl(orderGeneratorBuilder.build(), vehicleManagerBuilder.build(),
                    Map.of(RatingCriteria.TRAVEL_DISTANCE, travelDistanceRaterBuilder.build(),
                            RatingCriteria.AMOUNT_DELIVERED, amountDeliveredRaterBuilder.build(),
                            RatingCriteria.IN_TIME, inTimeRaterBuilder.build()),
                    simulationLength, name);
            problems.add(problem);
        });
    }

    @Override
    public void initReturnButton() {
        returnButton.setOnAction(e -> {
            final MainMenuScene scene = (MainMenuScene) SceneSwitcher.loadScene(SceneSwitcher.SceneType.MAIN_MENU,
                    getController().getStage());
            scene.init(new ArrayList<>(problems));
        });
    }

    @Override
    public CreateProblemSceneController getController() {
        return controller;
    }
}
