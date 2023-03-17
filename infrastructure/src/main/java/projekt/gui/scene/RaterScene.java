package projekt.gui.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import projekt.delivery.archetype.ProblemArchetype;
import projekt.delivery.rating.RatingCriteria;
import projekt.gui.controller.RaterSceneController;

public class RaterScene extends MenuScene<RaterSceneController> {
    private Map<RatingCriteria, Double> result;

    public RaterScene() {
        super(new RaterSceneController(), "Simulation Score", "projekt/gui/raterStyle.css");
    }

    public void init(List<ProblemArchetype> problems, Map<RatingCriteria, Double> result) {
        this.result = result;
        super.init(problems);
    }

    @Override
    public void initComponents() {
        System.out.println(result);
        root.setCenter(createResultsVBox());
    }

    private VBox createResultsVBox() {
        final VBox wrapper = new VBox();
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis(0, 1, 0.1);
        final BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);

        barChart.setTitle("Rater scores");
        xAxis.setLabel("Rater");
        yAxis.setLabel("Score");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (RatingCriteria criteria : result.keySet()) {
            series.getData().add(createData(criteria.name(), result.get(criteria)));
        }

        barChart.getData().add(series);
        barChart.setLegendVisible(false);

        wrapper.getChildren().addAll(barChart);
        return wrapper;
    }

    private XYChart.Data<String, Number> createData(String criteria, double value) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(criteria, value);

        String text = String.format("%.2f", value);

        StackPane node = new StackPane();
        Label label = new Label(text);
        label.setRotate(-90);
        Group group = new Group(label);
        StackPane.setAlignment(group, Pos.BOTTOM_CENTER);
        StackPane.setMargin(group, new Insets(0, 0, 5, 0));
        node.getChildren().add(group);
        data.setNode(node);

        return data;
    }

    @Override
    public void initReturnButton() {
        returnButton.setOnAction(e -> {
            MainMenuScene scene = (MainMenuScene) SceneSwitcher.loadScene(SceneSwitcher.SceneType.MAIN_MENU,
                    getController().getStage());
            scene.init(new ArrayList<>(problems));
        });
    }

    @Override
    public RaterSceneController getController() {
        return controller;
    }
}
