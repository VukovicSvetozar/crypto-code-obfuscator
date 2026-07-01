package utility;

import java.io.IOException;
import java.util.logging.Level;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class FxmlLoader {

	public static void load(Class<?> c, String fxml, String naslov) {
		try {
			Stage s = new Stage();
			Pane root = (Pane) FXMLLoader.load(c.getResource(fxml));
			Scene scene = new Scene(root);
			s.setScene(scene);
			s.setTitle(naslov);
			s.setResizable(false);
			s.show();
		} catch (IOException ex) {
			FileLogger.log(Level.SEVERE, null, ex);
		}
	}

}
