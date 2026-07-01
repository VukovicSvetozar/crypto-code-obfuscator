package application;

import javafx.application.Application;
import javafx.stage.Stage;

import utility.FxmlLoader;

public class PokretanjeAplikacije extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		FxmlLoader.load(getClass(), "/view/Prijava.fxml", "Prijava");
	}

	public static void main(String[] args) {
		launch(args);
	}

}
