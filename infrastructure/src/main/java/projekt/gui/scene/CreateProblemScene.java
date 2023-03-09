package projekt.gui.scene;

import java.util.ArrayList;

import projekt.gui.controller.CreateProblemSceneController;

public class CreateProblemScene extends MenuScene<CreateProblemSceneController> {

    public CreateProblemScene() {
        super(new CreateProblemSceneController(), "Create Problem");
    }

    @Override
    public void initComponents() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'initComponents'");
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
