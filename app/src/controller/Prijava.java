package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import model.Korisnik;
import utility.KriptoloskeMetode;
import utility.PogresanUnos;
import utility.Dijalog;
import utility.FileLogger;
import utility.FxmlLoader;
import utility.Putanje;

public class Prijava implements Putanje {

	@FXML
	private TextField txtKorisnickoIme;

	@FXML
	private PasswordField txtLozinka;

	@FXML
	private Button btnUnosSertifikata;

	@FXML
	private Label lblPutanjaSertifikata;

	@FXML
	private Button btnPrijava;

	@FXML
	private Button btnIzlaz;

	public static HashMap<String, Korisnik> korisnici;
	public static X509Certificate sertifikatCA;
	public static Korisnik aktivniKorisnik;

	private static HashMap<String, X509CRL> crls;
	private static String putanjaKorisnickogSertifikata;

	@FXML
	public void initialize() {
		ucitavanjeListeKorisnika();
		sertifikatCA = KriptoloskeMetode.ucitavanjeSertifikata(PUTANJA_DO_ROOT_CA + "RootCA.cer");
		crls = KriptoloskeMetode.ucitavanjeCRL(PUTANJA_DO_CRL + "CA.crl");
	}

	@FXML
	void unosSertifikata(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Unesite sertifikat");
		fileChooser.setInitialDirectory(new File(PUTANJA_DO_KORISNICKIH_SERTIFIKATA));
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("certificate", "*.cer", "*.crt"));

		File odabraniSertifikat = fileChooser.showOpenDialog(null);
		if (odabraniSertifikat != null) {
			putanjaKorisnickogSertifikata = odabraniSertifikat.getAbsolutePath();
			lblPutanjaSertifikata.setText(odabraniSertifikat.getName());
		}
	}

	@FXML
	void prijava(ActionEvent event) {
		String korisnickoIme = txtKorisnickoIme.getText();
		String lozinka = txtLozinka.getText();
		if (popunjenaPolja())
			if (provjeriLozinku(korisnickoIme, lozinka))
				if (provjeriKorisnickiSertifikat(korisnickoIme)) {
					aktivniKorisnik = korisnici.get(korisnickoIme);
					FxmlLoader.load(getClass(), "/view/GlavnaStrana.fxml", "Aplikacija");
				}
	}

	@FXML
	void izadji(ActionEvent event) {
		Platform.exit();
	}

	private static void ucitavanjeListeKorisnika() {
		korisnici = new HashMap<String, Korisnik>();
		try (BufferedReader in = new BufferedReader(new FileReader(new File(PUTANJA_DO_LISTE_KORISNIKA)))) {
			String linijaDatoteke;
			while ((linijaDatoteke = in.readLine()) != null) {
				String ucitanoKorisnickoIme = linijaDatoteke.split("#")[0];
				if (!("korisnickoIme".equals(ucitanoKorisnickoIme)))
					korisnici.put(ucitanoKorisnickoIme, new Korisnik(ucitanoKorisnickoIme));
			}
		} catch (FileNotFoundException e) {
			String porukaOGresci = "Navedena datoteka nije pronadjena na datoj putanji!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Navedena datoteka nije pronadjena na datoj putanji!", e);
		} catch (IOException e) {
			String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a.!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a!", e);
		}
	}

	private boolean popunjenaPolja() {
		String porukaOPogresnomUnosu = "";

		if (PogresanUnos.praznoPolje(txtKorisnickoIme.getText()))
			porukaOPogresnomUnosu += "Unesite korisnicko ime!\n";

		if (PogresanUnos.praznoPolje(txtLozinka.getText()))
			porukaOPogresnomUnosu += "Unesite lozinku!\n";

		if (putanjaKorisnickogSertifikata == null)
			porukaOPogresnomUnosu += "Unesite sertifikat!\n";

		if (porukaOPogresnomUnosu.length() != 0) {
			String upozorenje = "Niste unijeli sva polja!\n";
			Dijalog.showWarningDialog("Upozorenje", upozorenje, porukaOPogresnomUnosu);
			return false;
		} else
			return true;
	}

	private boolean provjeriLozinku(String korisnickoIme, String unesenaLozinka) {
		try (BufferedReader in = new BufferedReader(new FileReader(PUTANJA_DO_LISTE_KORISNIKA))) {
			String linijaDatoteke;
			String[] podaci;
			while ((linijaDatoteke = in.readLine()) != null) {
				podaci = linijaDatoteke.split("#");
				if (korisnickoIme.equals(podaci[0])) {
					String zasticenaLozinka = podaci[2];
					String salt = podaci[1];
					if (KriptoloskeMetode.verifikacijaKorisnickeLozinke(unesenaLozinka, zasticenaLozinka, salt))
						return true;
				}
			}
		} catch (IOException e) {
			String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a.!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a!", e);
			return false;
		}
		String upozorenje = "Uneseno je pogresno korisnicko ime ili lozinka!";
		Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", upozorenje);
		return false;
	}

	private boolean provjeriKorisnickiSertifikat(String korisnickoIme) {
		try {
			X509Certificate sertifikat = KriptoloskeMetode.ucitavanjeSertifikata(putanjaKorisnickogSertifikata);
			sertifikat.verify(sertifikatCA.getPublicKey());
			sertifikat.checkValidity(new Date());
			if (!sertifikat.equals(korisnici.get(korisnickoIme).getSertifikat())) {
				String upozorenje = "Odabrani sertifikat nije validan!";
				Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", upozorenje);
				return false;
			}
			if (!KriptoloskeMetode.provjeraIzdavaca(sertifikatCA, sertifikat)) {
				String upozorenje = "Izdavac nije odgovarajuci!";
				Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", upozorenje);
				return false;
			}
			for (X509CRL crl : crls.values())
				if (crl.isRevoked(sertifikat)) {
					String upozorenje = "Odabrani sertifikat je povucen iz upotrebe!";
					Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", upozorenje);
					return false;
				}
		} catch (InvalidKeyException e) {
			String porukaOGresci = "Kljuc nije ispravan!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Kljuc nije ispravan!", e);
			return false;
		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nije podrzan algoritam potpisa!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Nije podrzan algoritam potpisa!", e);
			return false;
		} catch (NoSuchProviderException e) {
			String porukaOGresci = "Ne postoji podrazumijevani provajder!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Ne postoji podrazumijevani provajder!", e);
			return false;
		} catch (SignatureException e) {
			String porukaOGresci = "Vas sertifikat nije potpisan od strane odgovarajuceg CA tijela!";
			Dijalog.showErrorDialog("Greska", "Aplikacija ce biti blokirana!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Vas sertifikat nije potpisan od strane odgovarajuceg CA tijela!", e);
			return false;
		} catch (CertificateExpiredException e) {
			String porukaOGresci = "Vas sertifikat je istekao.\nNe mozete slati poruke drugim korisnicima!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Vas sertifikat je istekao.\nNe mozete slati poruke drugim korisnicima!", e);
			return false;
		} catch (CertificateException e) {
			String porukaOGresci = "Greska je nastala tokom kodiranja!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Greska je nastala tokom kodiranja!", e);
			return false;
		}
		return true;
	}

}
