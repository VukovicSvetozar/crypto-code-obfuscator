package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import utility.Dijalog;
import utility.FileLogger;
import utility.KriptoloskeMetode;
import utility.Putanje;
import utility.RefreshableController;

import static controller.Prijava.aktivniKorisnik;
import static controller.Prijava.korisnici;
import static controller.Prijava.sertifikatCA;

public class EnkripcijaDatoteke implements RefreshableController, Putanje {

	@FXML
	private ComboBox<String> cbPrimaoc;

	@FXML
	private ComboBox<String> cbAlgoritamZaEnkripciju;

	@FXML
	private ComboBox<String> cbAlgoritamZaHes;

	@FXML
	private Button btnUnesiFajl;

	@FXML
	private Label lbUneseniFajl;

	@FXML
	private Button btnEnkripcija;

	public static String odabraniAlgoritam;

	private File odabraniFajl;

	@FXML
	void initialize() {
		postaviAlgoritme();
		unesiPrimaoce();
	}

	@FXML
	void unesiFajl(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Unesite fajl");
		fileChooser.setInitialDirectory(new File(PUTANJA_DO_IZVORNIH_KODOVA));
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Java source file", "*.java"));

		odabraniFajl = fileChooser.showOpenDialog(null);
		if (odabraniFajl != null)
			lbUneseniFajl.setText(odabraniFajl.getName());
		btnEnkripcija.setDisable(false);
	}

	@FXML
	void enkripcija(ActionEvent event) {

		try {
			java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

			String imeOdabranogKorisnika = cbPrimaoc.getValue().toString();
			String hesPrimaoca = KriptoloskeMetode.vratiHesKorisnickogImena(imeOdabranogKorisnika);

			// Ucitavanje sertifikata primaoca poruke
			String putanjaSertifikataPrimaoca = PUTANJA_DO_KORISNICKIH_SERTIFIKATA + imeOdabranogKorisnika + ".cer";
			X509Certificate sertifikat = KriptoloskeMetode.ucitavanjeSertifikata(putanjaSertifikataPrimaoca);

			// Provjera sertifikata primaoca poruke koristenjem sertifikata CA tijela
			sertifikat.verify(sertifikatCA.getPublicKey());
			sertifikat.checkValidity(new Date());

			// Izdvajanje javnog kljuca primaoca poruke iz sertifikata
			PublicKey javniKljuc = sertifikat.getPublicKey();

			// Generisanje tajnog kljuca
			odabraniAlgoritam = cbAlgoritamZaEnkripciju.getValue().toString();
			SecretKey tajniKljuc = KriptoloskeMetode.generisiKljuc(odabraniAlgoritam);
			byte[] kodovaniKljuc = tajniKljuc.getEncoded();

			// Enkriptovanje tajnog kljuca javnim RSA kljucem primaoca poruke
			Cipher cipher;
			byte[] sifrovanKljuc = null;

			boolean keyUsagePosiljalac[] = aktivniKorisnik.getSertifikat().getKeyUsage();
			boolean keyUsagePrimalac[] = sertifikat.getKeyUsage();

			if (keyUsagePrimalac[2]) {
				cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.ENCRYPT_MODE, javniKljuc);
				sifrovanKljuc = cipher.doFinal(kodovaniKljuc);
			} else {
				String porukaOGresci = "Seritifikat odabranog korisnika se ne moze iskoristiti za sifrovanje kljuca!";
				Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			}

			// Ucitavanje poruke za slanje
			String izvorniKod = ucitavanjeIzvornogKoda();

			// Odabir algoritma za hesiranje
			String odabraniHes = cbAlgoritamZaHes.getValue().toString();

			// Kreiranje poruke za slanje
			LocalDateTime datum = LocalDateTime.now();
			DateTimeFormatter formatVremena = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

			String poruka = "Datum slanja poruke:         " + formatVremena.format(datum)
					+ "\nPosiljalac poruke:               " + aktivniKorisnik.getKorisnickoIme()
					+ "\nAlgoritam za enkripciju:     " + odabraniAlgoritam + "\nAlgoritam za hesiranje:      "
					+ odabraniHes + "\nNaziv datoteke:                  " + odabraniFajl.getName() + "\n\nPoruka:\n";
			poruka += izvorniKod;

			// Kreiranje digitalnog potpisa poruke
			byte[] digitalniPotpis = null;
			if (keyUsagePosiljalac[0]) {
				Signature potpis = Signature.getInstance(odabraniHes);
				potpis.initSign(aktivniKorisnik.getPrivatniKljuc());
				potpis.update(poruka.getBytes("UTF-8"));
				digitalniPotpis = potpis.sign();
			} else {
				String porukaOGresci = "Vas sertifikat se ne moze koristiti za digitalno potpisivanje!";
				Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			}

			cipher = Cipher.getInstance(odabraniAlgoritam);
			cipher.init(Cipher.ENCRYPT_MODE, tajniKljuc);

			// Enkriptovanje poruke simetricnim kljucem
			String enkriptovanaPoruka = KriptoloskeMetode.enkriptovanjePorukeSimetricnimKljucem(digitalniPotpis, cipher,
					sifrovanKljuc, poruka);

			// Upisivanje poruke u izlaznu datoteku
			String lokacija = PUTANJA_DO_KORISNICKOG_FOLDERA + hesPrimaoca + File.separator;
			lokacija += "Poruke korisnika" + File.separator
					+ KriptoloskeMetode.izracunajImeFajlaZaSlanje(aktivniKorisnik.getKorisnickoIme()) + ".xyz";

			String poslataPoruka = enkriptovanaPoruka + "\n" + "Algoritam:" + "\n" + odabraniAlgoritam;

			try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(lokacija))))) {
				out.println(poslataPoruka);
			}
			String obavjestenje = "Poruka je uspjesno enkriptovana, potpisana i poslata!";
			Dijalog.showInfoDialog("Obavjestenje", "Bravo!", obavjestenje);
			btnEnkripcija.setDisable(true);
			lbUneseniFajl.setText("Fajl nije unesen");
		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nije podrzan algoritam potpisa!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Nije podrzan algoritam potpisa!", e);
		} catch (NoSuchPaddingException e) {
			String porukaOGresci = "Mehanizam za dopunjavanje nije dostupan u okruzenju!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Mehanizam za dopunjavanje nije dostupan u okruzenju!", e);
		} catch (InvalidKeyException e) {
			String porukaOGresci = "Kljuc nije ispravan!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Kljuc nije ispravan!", e);
		} catch (IllegalBlockSizeException e) {
			String porukaOGresci = "Duzina podataka ne odgovara velicini bloka cipher-a!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Duzina podataka ne odgovara velicini bloka cipher-a!", e);
		} catch (BadPaddingException e) {
			String porukaOGresci = "Podaci nisu pravilno postavljeni za dati mehanizam dopunjavanja!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Podaci nisu pravilno postavljeni za dati mehanizam dopunjavanja!", e);
		} catch (CertificateExpiredException e) {
			String porukaOGresci = "Vas sertifikat je istekao.\nNe mozete slati poruke drugim korisnicima!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Vas sertifikat je istekao.\\nNe mozete slati poruke drugim korisnicima!", e);
		} catch (CertificateNotYetValidException e) {
			String porukaOGresci = "Trenutni ili naznaceni datum je prije datuma u odgovarajucem periodu vazenja sertifikata!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE,
					"Trenutni ili naznaceni datum je prije datuma u odgovarajucem periodu vazenja sertifikata!", e);
		} catch (CertificateException e) {
			String porukaOGresci = "Greska je nastala tokom kodiranja!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Greska je nastala tokom kodiranja!", e);
		} catch (NoSuchProviderException e) {
			String porukaOGresci = "Ne postoji podrazumijevani provajder!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Ne postoji podrazumijevani provajder!", e);
		} catch (SignatureException e) {
			String porukaOGresci = "Vas sertifikat nije potpisan od strane odgovarajuceg CA tijela!";
			Dijalog.showErrorDialog("Greska", "Aplikacija ce biti blokirana!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Vas sertifikat nije potpisan od strane odgovarajuceg CA tijela!", e);
		} catch (NullPointerException e) {
			e.printStackTrace();
			String upozorenje = "Pokusajte ponovo";
			Dijalog.showWarningDialog("Upozorenje", "Niste odabrali sve potrebne podatke!", upozorenje);
			FileLogger.log(Level.SEVERE, "Niste odabrali sve potrebne podatke!", e);
		} catch (Exception e) {
			String porukaOGresci = "Desila se greska prilikom upisivanja i slanja poruke. \nPoruka nije poslata!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Desila se greska prilikom upisivanja i slanja poruke. \nPoruka nije poslata!",
					e);
		}
	}

	@Override
	public void refresh() {
	}

	private void postaviAlgoritme() {
		cbAlgoritamZaEnkripciju.getItems().addAll("DESede", "AES", "Blowfish", "ARCFOUR");
		cbAlgoritamZaHes.getItems().addAll("MD5withRSA", "SHA256WithRSA", "SHA384withRSA", "SHA512WithRSA");
	}

	private void unesiPrimaoce() {
		for (String korisnickoIme : korisnici.keySet())
			if (!korisnickoIme.equals(aktivniKorisnik.getKorisnickoIme()))
				cbPrimaoc.getItems().add(korisnickoIme);
		cbPrimaoc.setValue("Odaberite primaoca:");
	}

	private String ucitavanjeIzvornogKoda() {
		String izvorniKod = "";
		String line = "";
		try (BufferedReader in = new BufferedReader(new FileReader(odabraniFajl))) {
			while ((line = in.readLine()) != null)
				izvorniKod += line;
		} catch (FileNotFoundException e) {
			String porukaOGresci = "Navedena datoteka nije pronadjena na datoj putanji!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Navedena datoteka nije pronadjena na datoj putanji!", e);
		} catch (IOException e) {
			String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a!", e);
		}
		return izvorniKod;
	}

}
