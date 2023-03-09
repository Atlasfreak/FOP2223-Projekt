package projekt.gui.scene;

import java.util.ArrayList;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import projekt.gui.controller.CreateProblemSceneController;

public class CreateProblemScene extends MenuScene<CreateProblemSceneController> {

    public CreateProblemScene() {
        super(new CreateProblemSceneController(), "Create Problem");
    }

    @Override
    public void initComponents() {
        VBox mainVBox = createMainVBox();
        root.setCenter(mainVBox);
    }

    private VBox createMainVBox() {
        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);

        container.getChildren().addAll();
        return container;
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
    public CreateProblemSceneController getController() {
        return controller;
    }

}
