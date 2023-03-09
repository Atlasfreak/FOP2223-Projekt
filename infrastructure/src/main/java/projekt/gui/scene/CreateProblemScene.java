package projekt.gui.scene;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import projekt.delivery.generator.EmptyOrderGenerator;
import projekt.delivery.generator.FridayOrderGenerator;
import projekt.delivery.generator.OrderGenerator;
import projekt.gui.controller.CreateProblemSceneController;

public class CreateProblemScene extends MenuScene<CreateProblemSceneController> {
    private OrderGenerator.FactoryBuilder orderGeneratorFactoryBuilder;

    final UnaryOperator<Change> integerFilter = change -> {
        final String newText = change.getControlNewText();
        if (newText.matches("([0-9]*)?")) {
            return change;
        }
        return null;
    };

    final UnaryOperator<Change> doubleFilter = change -> {
        final String newText = change.getControlNewText();
        try {
            Double.parseDouble(newText);
            return change;
        } catch (final NumberFormatException e) {
            return null;
        }
    };

    public CreateProblemScene() {
        super(new CreateProblemSceneController(), "Create Problem");
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

        final Label simulationLengthLabel = new Label("Simulation Length:");
        formGridPane.add(simulationLengthLabel, 0, 1);

        final TextField simulationLengthField = new TextField();
        formGridPane.add(simulationLengthField, 1, 1);

        simulationLengthField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        int offset = 2;
        offset += createRaterSelection(formGridPane, offset);
        offset += createOrderGeneratorSelection(formGridPane, offset);

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
            orderGeneratorFactoryBuilder = choiceBox.getItems().get((Integer) obs.getValue());
            fridaySelectedProperty.set(orderGeneratorFactoryBuilder instanceof FridayOrderGenerator.FactoryBuilder);
        });

        formGridPane.add(choiceBox, 0, rowOffset + 1);

        Label orderCountLabel = new Label("Order Count:");
        formGridPane.add(orderCountLabel, 0, rowOffset + 2);

        TextField orderCountField = new TextField();
        orderCountField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        orderCountField.disableProperty().bind(fridaySelectedProperty.not());

        formGridPane.add(orderCountField, 1, rowOffset + 2);

        Label deliveryIntervalLabel = new Label("Delivery Intervall:");
        formGridPane.add(deliveryIntervalLabel, 0, rowOffset + 3);

        TextField deliveryIntervalField = new TextField();
        deliveryIntervalField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        deliveryIntervalField.disableProperty().bind(fridaySelectedProperty.not());

        formGridPane.add(deliveryIntervalField, 1, rowOffset + 3);

        Label maxWeightLabel = new Label("Max Weight:");
        formGridPane.add(maxWeightLabel, 0, rowOffset + 4);

        TextField maxWeightField = new TextField();
        maxWeightField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        maxWeightField.disableProperty().bind(fridaySelectedProperty.not());

        formGridPane.add(maxWeightField, 1, rowOffset + 4);

        Label standardDeviationLabel = new Label("Standard Deviation:");
        formGridPane.add(standardDeviationLabel, 0, rowOffset + 5);

        TextField standardDeviationField = new TextField();
        standardDeviationField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        standardDeviationField.disableProperty().bind(fridaySelectedProperty.not());

        formGridPane.add(standardDeviationField, 1, rowOffset + 5);

        Label lastTickLabel = new Label("Last Tick:");
        formGridPane.add(lastTickLabel, 0, rowOffset + 6);

        TextField lastTickField = new TextField();
        lastTickField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        lastTickField.disableProperty().bind(fridaySelectedProperty.not());

        formGridPane.add(lastTickField, 1, rowOffset + 6);

        Label seedLabel = new Label("Seed:");
        formGridPane.add(seedLabel, 0, rowOffset + 7);

        TextField seedField = new TextField();
        seedField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        seedField.disableProperty().bind(fridaySelectedProperty.not());

        formGridPane.add(seedField, 1, rowOffset + 7);

        return rows;
    }

    private int createRaterSelection(final GridPane formGridPane, int rowOffset) {
        int rows = 8;

        final Label ratersLabel = new Label("Select used Raters:");
        formGridPane.add(ratersLabel, 0, rowOffset + 0, 2, 1);

        final CheckBox inTimeRaterCheckBox = new CheckBox("In Time Rater");
        formGridPane.add(inTimeRaterCheckBox, 0, rowOffset + 1, 2, 1);

        final Label inTimeRaterIgnoredTicksOffLabel = new Label("Ignored Ticks Off:");
        formGridPane.add(inTimeRaterIgnoredTicksOffLabel, 0, rowOffset + 2);
        final TextField inTimeRaterIgnoredTicksOffField = new TextField();
        formGridPane.add(inTimeRaterIgnoredTicksOffField, 1, rowOffset + 2);

        inTimeRaterIgnoredTicksOffField
                .setTextFormatter(new TextFormatter<>(new LongStringConverter(), Long.valueOf(0), integerFilter));
        inTimeRaterIgnoredTicksOffField.disableProperty().bind(inTimeRaterCheckBox.selectedProperty().not());

        final Label inTimeRatermaxTicksOffLabel = new Label("Ignored Ticks Off:");
        formGridPane.add(inTimeRatermaxTicksOffLabel, 0, rowOffset + 3);
        final TextField inTimeRatermaxTicksOffField = new TextField();
        formGridPane.add(inTimeRatermaxTicksOffField, 1, rowOffset + 3);

        inTimeRatermaxTicksOffField
                .setTextFormatter(new TextFormatter<>(new LongStringConverter(), Long.valueOf(0), integerFilter));
        inTimeRatermaxTicksOffField.disableProperty().bind(inTimeRaterCheckBox.selectedProperty().not());

        final CheckBox amountDeliverdRaterCheckBox = new CheckBox("Amount Delivered Rater");
        formGridPane.add(amountDeliverdRaterCheckBox, 0, rowOffset + 4, 2, 1);

        final Label amountDeliverdRaterFactorLabel = new Label("Factor:");
        formGridPane.add(amountDeliverdRaterFactorLabel, 0, rowOffset + 5);
        final TextField amountDeliverdRaterFactorField = new TextField();
        formGridPane.add(amountDeliverdRaterFactorField, 1, rowOffset + 5);

        amountDeliverdRaterFactorField
                .setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        amountDeliverdRaterFactorField.disableProperty().bind(amountDeliverdRaterCheckBox.selectedProperty().not());

        final CheckBox travelDistanceRaterCheckBox = new CheckBox("Travel Distance Rater");
        formGridPane.add(travelDistanceRaterCheckBox, 0, rowOffset + 6, 2, 1);

        final Label travelDistanceRaterFactorLabel = new Label("Factor:");
        formGridPane.add(travelDistanceRaterFactorLabel, 0, rowOffset + 7);
        final TextField travelDistanceRaterFactorField = new TextField();
        formGridPane.add(travelDistanceRaterFactorField, 1, rowOffset + 7);

        travelDistanceRaterFactorField
                .setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        travelDistanceRaterFactorField.disableProperty().bind(travelDistanceRaterCheckBox.selectedProperty().not());

        return rows;
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
