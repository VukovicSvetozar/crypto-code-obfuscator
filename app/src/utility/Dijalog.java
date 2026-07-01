package utility;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Dijalog {

	private static void showDialog(AlertType tip, String naslov, String zaglavlje, String sadrzaj) {
		Alert alert = new Alert(tip);
		alert.setTitle(naslov);
		alert.setHeaderText(zaglavlje);
		alert.setContentText(sadrzaj);
		alert.showAndWait();
	}

	public static void showErrorDialog(String naslov, String zaglavlje, String sadrzaj) {
		showDialog(AlertType.ERROR, naslov, zaglavlje, sadrzaj);
		Platform.exit();
	}

	public static void showInfoDialog(String naslov, String zaglavlje, String sadrzaj) {
		showDialog(AlertType.INFORMATION, naslov, zaglavlje, sadrzaj);
	}

	public static void showWarningDialog(String naslov, String zaglavlje, String sadrzaj) {
		showDialog(AlertType.WARNING, naslov, zaglavlje, sadrzaj);
	}

}
