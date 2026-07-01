package controller;

import static controller.Prijava.aktivniKorisnik;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import utility.Dijalog;
import utility.FileLogger;
import utility.FxmlLoader;
import utility.KriptoloskeMetode;
import utility.Putanje;
import utility.RefreshableController;

public class DekripcijaDatoteke implements RefreshableController, Putanje {

	@FXML
	private TextArea taPodaci;

	@FXML
	private Button btnUnesiFajl;

	@FXML
	private Label lbUneseniFajl;

	@FXML
	private Button btnDekripcija;

	@FXML
	private Button btnKompajliranje;

	private File putanjaPorukeZaCitanje;
	private String porukaZaCitanje;
	private PrivateKey privatniKljuc;
	private byte[] sifrovaniKljuc;
	private byte[] sifrat;
	private byte[] potpis;
	private byte[] desifrovanaPoruka;
	private byte[] dekriptovaniPotpisIPoruka;
	private String porukaZaPrikaz;
	private String posiljalac;
	private String odabraniAlgoritam;
	private String odabraniHes;

	public static String nazivDatoteke;
	public static String izvorniKod;
	public static boolean mojeBrisanjePoruke;

	@FXML
	void initialize() throws IOException {
		odabraniAlgoritam = new String();
		btnDekripcija.setDisable(true);
		btnKompajliranje.setDisable(true);
	}

	@FXML
	void unesiFajl(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(aktivniKorisnik.getPutanjaKorisnickogFoldera() + "Poruke korisnika"));
		fileChooser.setTitle("Unesite fajl");
		putanjaPorukeZaCitanje = fileChooser.showOpenDialog(null);
		if (putanjaPorukeZaCitanje != null)
			lbUneseniFajl.setText("Fajl je uspjesno unesen!");
		btnDekripcija.setDisable(false);
	}

	@FXML
	void dekriptovanje() {
		try {
			if (!putanjaPorukeZaCitanje.exists()) {
				String upozorenje = "Odabrana poruka ne postoji na fajl sistemu!";
				Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", upozorenje);
			} else {
				ucitavanjePoruke();
				izdvajanjeSifrovanogKljucaISifrata();
				ucitavanjePrivatnogKljuca();
				dekripcijaKljuca(odabraniAlgoritam);
				izdvajanjePotpisaIzPoruke();
				prikazPoruke();
				izdvajanjePodatakaIzPoruke();
				upisivanjeIzvornogKoda();
				obrisiPoruku();
				validacijaPotpisaPoruke();
				postaviPodrazumijevaneVrijednosti();
			}
		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nije podrzan algoritam potpisa!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Nije podrzan algoritam potpisa!", e);
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			String porukaOGresci = "Mehanizam za dopunjavanje nije dostupan u okruzenju!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Mehanizam za dopunjavanje nije dostupan u okruzenju", e);
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
			FileLogger.log(Level.SEVERE, "Vas sertifikat je istekao.\nNe mozete slati poruke drugim korisnicima!", e);
		} catch (CertificateNotYetValidException e) {
			String porukaOGresci = "Trenutni ili naznaceni datum je prije datuma u odgovarajucem periodu vazenja sertifikata!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE,
					"Trenutni ili naznaceni datum je prije datuma u odgovarajucem periodu vazenja sertifikata!", e);
		} catch (CertificateException e) {
			String porukaOGresci = "Greska je nastala tokom kodiranja!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Greska je nastala tokom kodiranja!", e);
		} catch (SignatureException e) {
			String porukaOGresci = "Vas sertifikat nije potpisan od strane odgovarajuceg CA tijela!";
			Dijalog.showErrorDialog("Greska", "Aplikacija ce biti blokirana!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Vas sertifikat nije potpisan od strane odgovarajuceg CA tijela!", e);
		} catch (NullPointerException e) {
			String upozorenje = "Pokusajte ponovo";
			Dijalog.showWarningDialog("Upozorenje", "Odredjeni podaci nisu inicijalizovani!", upozorenje);
			FileLogger.log(Level.SEVERE, "Odredjeni podaci nisu inicijalizovani!", e);
			e.printStackTrace();
		} catch (Exception e) {
			String porukaOGresci = "Doslo je do greske prilikom citanja poruke!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Doslo je do greske prilikom citanja poruke!", e);
		}
	}

	@FXML
	void kompajliranje() {
		lbUneseniFajl.setText("Unesite fajl:");
		taPodaci.clear();
		btnKompajliranje.setDisable(true);
		mojeBrisanjePoruke = false;
		FxmlLoader.load(getClass(), "/view/IzvrsavanjeDatoteke.fxml", "Izvrsavanje Datoteke");
	}

	@Override
	public void refresh() {
	}

	private void ucitavanjePoruke() throws FileNotFoundException, IOException {
		String linija = "";
		try (BufferedReader in = new BufferedReader(new FileReader(putanjaPorukeZaCitanje))) {
			porukaZaCitanje = "";
			while (!"Algoritam:".equals(linija = in.readLine()))
				porukaZaCitanje += linija;
			while ((linija = in.readLine()) != null)
				odabraniAlgoritam += linija;
		}
	}

	private void izdvajanjeSifrovanogKljucaISifrata() throws UnsupportedEncodingException {
		byte[] dekodiranaPoruka = Base64.getDecoder().decode(porukaZaCitanje.getBytes("UTF-8"));
		sifrovaniKljuc = new byte[256];
		sifrat = new byte[dekodiranaPoruka.length - sifrovaniKljuc.length];
		for (int i = 0; i < dekodiranaPoruka.length; i++) {
			if (i < sifrovaniKljuc.length)
				sifrovaniKljuc[i] = dekodiranaPoruka[i];
			else
				sifrat[i - sifrovaniKljuc.length] = dekodiranaPoruka[i];
		}
	}

	private void ucitavanjePrivatnogKljuca() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		File putanjaPrivatnogKljuca = new File(
				aktivniKorisnik.getPutanjaKorisnickogFoldera() + PUTANJA_DO_PRIVATNOG_KLJUCA + "PrivatniKljuc.der");
		byte[] privBytes = Files.readAllBytes(Paths.get(putanjaPrivatnogKljuca.getPath()));
		// Generisanje privatnog kljuca
		PKCS8EncodedKeySpec specifikacija = new PKCS8EncodedKeySpec(privBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		privatniKljuc = kf.generatePrivate(specifikacija);
	}

	private void dekripcijaKljuca(String algoritam) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, privatniKljuc);
		byte[] keyb = cipher.doFinal(sifrovaniKljuc);
		SecretKey skey = new SecretKeySpec(keyb, algoritam);
		cipher = Cipher.getInstance(algoritam);
		cipher.init(Cipher.DECRYPT_MODE, skey);
		dekriptovaniPotpisIPoruka = cipher.doFinal(sifrat);
	}

	private void izdvajanjePotpisaIzPoruke() {
		potpis = new byte[256];
		desifrovanaPoruka = new byte[dekriptovaniPotpisIPoruka.length - potpis.length];
		for (int i = 0; i < dekriptovaniPotpisIPoruka.length; i++) {
			if (i < potpis.length)
				potpis[i] = dekriptovaniPotpisIPoruka[i];
			else
				desifrovanaPoruka[i - potpis.length] = dekriptovaniPotpisIPoruka[i];
		}
	}

	private void prikazPoruke() throws UnsupportedEncodingException {
		porukaZaPrikaz = new String(desifrovanaPoruka, "UTF-8");
		taPodaci.setText(porukaZaPrikaz.split("\\n\\n")[0]);
		taPodaci.setEditable(false);
	}

	private void izdvajanjePodatakaIzPoruke() {
		String podaci = porukaZaPrikaz.split("\\n\\n")[0];
		izvorniKod = porukaZaPrikaz.split("\\n\\n")[1].substring(7);
		posiljalac = podaci.split("\\n")[1].substring(33);
		odabraniHes = podaci.split("\\n")[3].substring(29);
		nazivDatoteke = podaci.split("\\n")[4].substring(33);
	}

	private void upisivanjeIzvornogKoda() {
		String lokacija = aktivniKorisnik.getPutanjaKorisnickogFoldera();
		lokacija += "Dekriptovane datoteke" + File.separator + nazivDatoteke;
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(lokacija))))) {
			out.println(izvorniKod);
		} catch (IOException e) {
			String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a!", e);
		}
	}

	private void obrisiPoruku() {
		mojeBrisanjePoruke = true;
		putanjaPorukeZaCitanje.delete();
	}

	private void validacijaPotpisaPoruke() throws FileNotFoundException, CertificateException, NoSuchAlgorithmException,
			SignatureException, InvalidKeyException {

		String putanjaPosiljaocaSertifikata = PUTANJA_DO_KORISNICKIH_SERTIFIKATA + posiljalac + ".cer";
		X509Certificate posiljalacSertifikata = KriptoloskeMetode.ucitavanjeSertifikata(putanjaPosiljaocaSertifikata);

		// Izdvajanje javnog kljuca posiljaoca poruke iz sertifikata
		PublicKey javniKljucPosiljaoca = posiljalacSertifikata.getPublicKey();

		Signature signature = Signature.getInstance(odabraniHes);
		signature.initVerify(javniKljucPosiljaoca);
		signature.update(desifrovanaPoruka);
		if (signature.verify(potpis)) {
			String porukaOObavjestenju = "Potpis je uspjesno verifikovan!";
			Dijalog.showInfoDialog("Obavjestenje", "Ovo je obavjestenje!", porukaOObavjestenju);
		} else {
			String porukaOUpozorenju = "Potpis nije uspjesno verifikovan!";
			Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", porukaOUpozorenju);
		}
	}
	
	private void postaviPodrazumijevaneVrijednosti() {
		btnKompajliranje.setDisable(false);
		btnDekripcija.setDisable(true);
		odabraniAlgoritam = "";
	}

}
