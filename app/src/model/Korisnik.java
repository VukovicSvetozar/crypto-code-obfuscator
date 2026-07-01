package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;

import utility.Dijalog;
import utility.FileLogger;
import utility.KriptoloskeMetode;
import utility.Putanje;

public class Korisnik implements Putanje {

	private String korisnickoIme;
	private String putanjaSertifikata;
	private X509Certificate sertifikat;
	private String putanjaKorisnickogFoldera;
	private PrivateKey privatniKljuc;

	public Korisnik(String korisnickoIme) {
		ucitavanjePodatakaOKorisniku(korisnickoIme);
		postavljanjePutanjeDoLicnogFoldera();
		ucitavanjePrivatnogKljuca();
	}

	private void ucitavanjePodatakaOKorisniku(String korisnickoIme) {
		this.setKorisnickoIme(korisnickoIme);
		this.setPutanjaSertifikata(PUTANJA_DO_KORISNICKIH_SERTIFIKATA + this.getKorisnickoIme() + ".cer");
		this.setSertifikat(KriptoloskeMetode.ucitavanjeSertifikata(this.getPutanjaSertifikata()));
	}

	private void postavljanjePutanjeDoLicnogFoldera() {
		String putanja = KriptoloskeMetode.vratiHesKorisnickogImena(this.getKorisnickoIme());
		this.setPutanjaKorisnickogFoldera(PUTANJA_DO_KORISNICKOG_FOLDERA + putanja + File.separator);
	}

	private void ucitavanjePrivatnogKljuca() {
		File privatniKljuc = new File(
				this.getPutanjaKorisnickogFoldera() + PUTANJA_DO_PRIVATNOG_KLJUCA + "PrivatniKljuc.der");
		if (privatniKljuc.exists()) {
			java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			byte privatni[];
			try {
				privatni = ucitajKljuc(privatniKljuc.getPath());
				PKCS8EncodedKeySpec specifikacija = new PKCS8EncodedKeySpec(privatni);
				KeyFactory kf = KeyFactory.getInstance("RSA");
				this.setPrivatniKljuc(kf.generatePrivate(specifikacija));
			} catch (IOException e) {
				String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a!";
				Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
				FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a!", e);
			} catch (InvalidKeySpecException e) {
				String porukaOGresci = "Specifikacija kljuca nije odgovarajuca za KeyFactory pri kreiranju privatnog kljuca!";
				Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
				FileLogger.log(Level.SEVERE,
						"Specifikacija kljuca nije odgovarajuca za KeyFactory pri kreiranju privatnog kljuca!", e);
			} catch (NoSuchAlgorithmException e) {
				String porukaOGresci = "Nijedan provajder ne podrzava implementaciju KeyFactorySpi za navedeni algoritam!";
				Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
				FileLogger.log(Level.SEVERE, "Doslo je do greske prilikom pokretanja koda!", e);
			}
		}
	}

	private static byte[] ucitajKljuc(String nazivFajla) throws IOException {
		Path lokacija = Paths.get(nazivFajla);
		return Files.readAllBytes(lokacija);
	}

	public String getKorisnickoIme() {
		return korisnickoIme;
	}

	public void setKorisnickoIme(String korisnickoIme) {
		this.korisnickoIme = korisnickoIme;
	}

	public String getPutanjaSertifikata() {
		return putanjaSertifikata;
	}

	public void setPutanjaSertifikata(String putanjaSertifikata) {
		this.putanjaSertifikata = putanjaSertifikata;
	}

	public X509Certificate getSertifikat() {
		return sertifikat;
	}

	public void setSertifikat(X509Certificate sertifikat) {
		this.sertifikat = sertifikat;
	}

	public String getPutanjaKorisnickogFoldera() {
		return putanjaKorisnickogFoldera;
	}

	public void setPutanjaKorisnickogFoldera(String putanjaKorisnickogFoldera) {
		this.putanjaKorisnickogFoldera = putanjaKorisnickogFoldera;
	}

	public PrivateKey getPrivatniKljuc() {
		return privatniKljuc;
	}

	public void setPrivatniKljuc(PrivateKey privatniKljuc) {
		this.privatniKljuc = privatniKljuc;
	}

}
