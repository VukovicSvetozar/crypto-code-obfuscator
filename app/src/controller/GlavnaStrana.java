package controller;

import static controller.Prijava.aktivniKorisnik;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import utility.Dijalog;
import utility.FileLogger;
import utility.FileWatcher;
import utility.Putanje;
import utility.RefreshableController;

public class GlavnaStrana implements Putanje, Initializable {

	@FXML
	private Label lbIme;

	@FXML
	private Tab tab1;

	@FXML
	private Tab tab2;

	@FXML
	private Button izlazDugme;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		lbIme.setText(Prijava.aktivniKorisnik.getKorisnickoIme());
		try {
			ucitajTab(tab1, "/view/EnkripcijaDatoteke.fxml");
			ucitajTab(tab2, "/view/DekripcijaDatoteke.fxml");
		} catch (IOException e) {
			String porukaOGresci = "Aplikacija nije u mogucnosti da prikaze tabove!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Aplikacija nije u mogucnosti da prikaze tabove!", e);
		}
		posmatrajSvojstva();
	}

	@FXML
	public void izadji(ActionEvent event) {
		Stage stage = (Stage) izlazDugme.getScene().getWindow();
		stage.close();
	}

	private void ucitajTab(Tab tab, String fxml) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
		Parent root = loader.load();
		RefreshableController controller = loader.getController();

		AnchorPane pane = (AnchorPane) tab.getContent();
		pane.getChildren().add(root);
		AnchorPane.setBottomAnchor(root, 0.0);
		AnchorPane.setLeftAnchor(root, 0.0);
		AnchorPane.setRightAnchor(root, 0.0);
		AnchorPane.setTopAnchor(root, 0.0);

		tab.setOnSelectionChanged(e -> {
			if (tab.selectedProperty().get())
				controller.refresh();
		});
	}

	private void posmatrajSvojstva() {
		FileWatcher watchDir;
		try {
			watchDir = new FileWatcher(Paths.get(aktivniKorisnik.getPutanjaKorisnickogFoldera() + "Poruke korisnika"));
			watchDir.setNotificationExecutor(Platform::runLater);
			Thread watchThread = new Thread(watchDir::processEvents);
			watchThread.setDaemon(true);
			watchThread.start();

		} catch (IOException e) {
			String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a!", e);
		}
	}

}
