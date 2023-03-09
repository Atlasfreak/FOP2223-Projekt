package projekt.gui.scene;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import projekt.gui.controller.CreateProblemSceneController;

public class CreateProblemScene extends MenuScene<CreateProblemSceneController> {

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
        final VBox mainVBox = createMainVBox();
        root.setCenter(mainVBox);
    }

    private VBox createMainVBox() {
        final VBox container = new VBox();
        container.setAlignment(Pos.CENTER);

        final GridPane formGridPane = createForm();

        container.getChildren().addAll(formGridPane);
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

        createRaterSelection(formGridPane);

        return formGridPane;
    }

    private void createRaterSelection(final GridPane formGridPane) {
        final Label ratersLabel = new Label("Select used Raters:");
        formGridPane.add(ratersLabel, 0, 2, 2, 1);

        final CheckBox inTimeRaterCheckBox = new CheckBox("In Time Rater");
        formGridPane.add(inTimeRaterCheckBox, 0, 3, 2, 1);

        final Label inTimeRaterIgnoredTicksOffLabel = new Label("Ignored Ticks Off:");
        formGridPane.add(inTimeRaterIgnoredTicksOffLabel, 0, 4);
        final TextField inTimeRaterIgnoredTicksOffField = new TextField();
        formGridPane.add(inTimeRaterIgnoredTicksOffField, 1, 4);

        inTimeRaterIgnoredTicksOffField
                .setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        inTimeRaterIgnoredTicksOffField.disableProperty().bind(inTimeRaterCheckBox.selectedProperty().not());

        final Label inTimeRatermaxTicksOffLabel = new Label("Ignored Ticks Off:");
        formGridPane.add(inTimeRatermaxTicksOffLabel, 0, 5);
        final TextField inTimeRatermaxTicksOffField = new TextField();
        formGridPane.add(inTimeRatermaxTicksOffField, 1, 5);

        inTimeRatermaxTicksOffField
                .setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        inTimeRatermaxTicksOffField.disableProperty().bind(inTimeRaterCheckBox.selectedProperty().not());

        final CheckBox amountDeliverdRaterCheckBox = new CheckBox("Amount Delivered Rater");
        formGridPane.add(amountDeliverdRaterCheckBox, 0, 6, 2, 1);

        final Label amountDeliverdRaterFactorLabel = new Label("Factor:");
        formGridPane.add(amountDeliverdRaterFactorLabel, 0, 7);
        final TextField amountDeliverdRaterFactorField = new TextField();
        formGridPane.add(amountDeliverdRaterFactorField, 1, 7);

        amountDeliverdRaterFactorField
                .setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        amountDeliverdRaterFactorField.disableProperty().bind(amountDeliverdRaterCheckBox.selectedProperty().not());

        final CheckBox travelDistanceRaterCheckBox = new CheckBox("Travel Distance Rater");
        formGridPane.add(travelDistanceRaterCheckBox, 0, 8, 2, 1);

        final Label travelDistanceRaterFactorLabel = new Label("Factor:");
        formGridPane.add(travelDistanceRaterFactorLabel, 0, 9);
        final TextField travelDistanceRaterFactorField = new TextField();
        formGridPane.add(travelDistanceRaterFactorField, 1, 9);

        travelDistanceRaterFactorField
                .setTextFormatter(new TextFormatter<>(new DoubleStringConverter(), 0.0, doubleFilter));
        travelDistanceRaterFactorField.disableProperty().bind(travelDistanceRaterCheckBox.selectedProperty().not());
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
