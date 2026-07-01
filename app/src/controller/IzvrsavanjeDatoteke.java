package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import utility.Dijalog;
import utility.FileLogger;
import utility.Putanje;

import static controller.DekripcijaDatoteke.izvorniKod;
import static controller.DekripcijaDatoteke.nazivDatoteke;
import static controller.Prijava.aktivniKorisnik;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class IzvrsavanjeDatoteke implements Putanje {

	@FXML
	private TextArea taIzvorniKod;

	@FXML
	private TextArea taIzvrsniKod;

	@FXML
	private Button btnIzvrsi;

	@FXML
	private Button btnIzadji;

	@FXML
	void initialize() {
		taIzvorniKod.setText(izvorniKod);
		taIzvorniKod.setEditable(false);
		taIzvorniKod.setWrapText(true);
		taIzvrsniKod.setWrapText(true);
	}

	@FXML
	void izvrsi(ActionEvent event) {
		taIzvrsniKod.setEditable(true);
		String lokacija = aktivniKorisnik.getPutanjaKorisnickogFoldera();
		lokacija += "Dekriptovane datoteke" + File.separator + nazivDatoteke;
		File fajl = new File(lokacija);

		Platform.runLater(new Runnable() {
			public void run() {

				if (fajl.isFile()) {
					new Thread(() -> {
						try {
							Runtime rt = Runtime.getRuntime();
							JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
							/*
							InputStream ulazniStream = new 		 
									ByteArrayInputStream(taIzvrsniKod.getText().getBytes(StandardCharsets.UTF_8));
							OutputStream izlazniStream = new OutputStream() {
								private StringBuilder string = new StringBuilder(taIzvrsniKod.getText());
								@Override
								public void write(int b) throws IOException {
									this.string.append((char) b);
								}
								public String toString() {
									return this.string.toString();
								}
							};
							System.setOut((PrintStream) izlazniStream);
							compiler.run(ulazniStream, izlazniStream, System.err, fajl.getPath());
							*/
							compiler.run(System.in, System.out, System.err, fajl.getPath());
							Thread.sleep(1000);
							Process process = rt.exec("java " + nazivDatoteke.split("\\.")[0], null,
									new File(aktivniKorisnik.getPutanjaKorisnickogFoldera() + File.separator
											+ "Dekriptovane datoteke"));
							StringBuilder output = new StringBuilder();
							BufferedReader citac = new BufferedReader(new InputStreamReader(process.getInputStream()));
							String linija;
							while ((linija = citac.readLine()) != null)
								output.append(linija + "\n");
							taIzvrsniKod.setText(output.toString());

						} catch (Exception e) {
							String porukaOGresci = "Doslo je do greske prilikom pokretanja koda!";
							Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
							FileLogger.log(Level.SEVERE, "Doslo je do greske prilikom pokretanja koda!", e);
						}
					}).start();
				} else {
					String porukaOGresci = "Navedena datoteka nije pronadjena na datoj putanji!";
					Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
				}

			}
		});
		btnIzvrsi.setDisable(true);
	}

	@FXML
	void izadji(ActionEvent event) {
		btnIzvrsi.setDisable(false);
		Stage stage = (Stage) btnIzadji.getScene().getWindow();
		stage.close();
	}

}
