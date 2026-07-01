package utility;

import org.bouncycastle.util.encoders.Hex;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class KriptoloskeMetode {

	private static final int BROJ_ITERACIJA = 10000;
	private static final int DUZINA_KLJUCA = 256;

	public static X509Certificate ucitavanjeSertifikata(String putanja) {
		X509Certificate sertifikat = null;
		try (FileInputStream fin = new FileInputStream(putanja)) {
			CertificateFactory fabrika = CertificateFactory.getInstance("X.509");
			sertifikat = (X509Certificate) fabrika.generateCertificate(fin);
		} catch (FileNotFoundException e) {
			String porukaOGresci = "Navedena datoteka nije pronadjena na datoj putanji!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Navedena datoteka nije pronadjena na datoj putanji!", e);
		} catch (CertificateException e) {
			String porukaOGresci = "Sertifikat se ne moze ucitati!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Sertifikat se ne moze ucitati!", e);
		} catch (IOException e) {
			String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a!", e);
		}
		return sertifikat;
	}

	public static HashMap<String, X509CRL> ucitavanjeCRL(String putanja) {
		HashMap<String, X509CRL> crls = new HashMap<String, X509CRL>();
		try (InputStream inStream = new FileInputStream(putanja)) {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509CRL crl = (X509CRL) cf.generateCRL(inStream);
			crls.put(KriptoloskeMetode.getX509CRLIssuerCN(crl), crl);
		} catch (FileNotFoundException e) {
			String porukaOGresci = "Navedena datoteka nije pronadjena na datoj putanji!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Navedena datoteka nije pronadjena na datoj putanji!", e);
		} catch (IOException e) {
			String porukaOGresci = "IO greska se javlja tokom citanja iz stream-a!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "IO greska se javlja tokom citanja iz stream-a", e);
		} catch (CertificateException e) {
			String porukaOGresci = "Sertifikat se ne moze ucitati!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Sertifikat se ne moze ucitati!", e);
		} catch (CRLException e) {
			String porukaOGresci = "Greska se javlja tokom parsiranja!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Greska se javlja tokom parsiranja!", e);
		}
		return crls;
	}

	// Returns the common name (CN) of this CRL's issuer.
	private static String getX509CRLIssuerCN(X509CRL crl) {
		if (crl == null) {
			String porukaOGresci = "Nedostaje crl!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
		}
		String idn = crl.getIssuerX500Principal().getName();
		String commonName = idn.split(", ")[0].split("=")[1];
		return commonName;
	}

	public static boolean verifikacijaKorisnickeLozinke(String unesenaLozinka, String zasticenaLozinka, String salt) {
		boolean povratnaVrijednost = false;
		byte[] hesVrijednost = vratiHes(unesenaLozinka.toCharArray(), salt.getBytes());
		String novaZasticenaLozinka = Base64.getEncoder().encodeToString(hesVrijednost);
		povratnaVrijednost = novaZasticenaLozinka.equalsIgnoreCase(zasticenaLozinka);
		return povratnaVrijednost;
	}

	private static byte[] vratiHes(char[] lozinka, byte[] salt) {
		PBEKeySpec specifikacija = new PBEKeySpec(lozinka, salt, BROJ_ITERACIJA, DUZINA_KLJUCA);
		Arrays.fill(lozinka, Character.MIN_VALUE);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return skf.generateSecret(specifikacija).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			String porukaOGresci = "Greska se javlja tokom hesovanja lozinke!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Greska se javlja tokom hesovanja lozinke!", e);
			return null;
		} finally {
			specifikacija.clearPassword();
		}
	}

	public static boolean provjeraIzdavaca(X509Certificate sertifikatCA, X509Certificate korisnickiSertifikat) {
		String subjectDN = getX509CertificateSubjectCN(sertifikatCA);
		if (subjectDN.equals(getX509CertificateIssuerCN(korisnickiSertifikat)))
			return true;
		else
			return false;
	}

	// Returns the common name (CN) of this certificate's subject.
	private static String getX509CertificateSubjectCN(X509Certificate cert) {
		if (cert == null) {
			String porukaOGresci = "Nedostaje sertifikat!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
		}
		String subjectDN = cert.getSubjectX500Principal().getName();
		String cn = subjectDN.split(", ")[0].split("=")[1];
		return cn;
	}

	// Returns the common name (CN) of this certificate's issuer.
	private static String getX509CertificateIssuerCN(X509Certificate cert) {
		if (cert == null) {
			String porukaOGresci = "Nedostaje sertifikat!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
		}
		String issuerDN = cert.getIssuerX500Principal().getName();
		String cn = issuerDN.split(", ")[0].split("=")[1];
		return cn;
	}

	public static SecretKey generisiKljuc(String odabraniAlgoritam) throws NoSuchAlgorithmException {
		KeyGenerator kgen = KeyGenerator.getInstance(odabraniAlgoritam);
		return kgen.generateKey();
	}

	public static String enkriptovanjePorukeSimetricnimKljucem(byte[] digitalniPotpis, Cipher cipher,
			byte[] sifrovanKljuc, String poruka) throws Exception {

		byte[] porukaBajtovi = poruka.getBytes("UTF-8");
		byte[] potpisIPorukaBajtovi = new byte[porukaBajtovi.length + digitalniPotpis.length];
		for (int i = 0; i < potpisIPorukaBajtovi.length; i++) {
			potpisIPorukaBajtovi[i] = (i < digitalniPotpis.length) ? digitalniPotpis[i]
					: porukaBajtovi[i - digitalniPotpis.length];
		}
		byte[] sifrovanPotpisIPorukaBajtovi = cipher.doFinal(potpisIPorukaBajtovi);
		byte[] kompletiraniBajtovi = new byte[sifrovanKljuc.length + sifrovanPotpisIPorukaBajtovi.length];
		for (int i = 0; i < kompletiraniBajtovi.length; i++) {
			kompletiraniBajtovi[i] = (i < sifrovanKljuc.length) ? sifrovanKljuc[i]
					: sifrovanPotpisIPorukaBajtovi[i - sifrovanKljuc.length];
		}
		byte[] encodedBytes = Base64.getEncoder().encode(kompletiraniBajtovi);
		return new String(encodedBytes, "UTF-8");
	}

	public static String vratiHesKorisnickogImena(String ime) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-224");
			byte[] hes = md.digest(ime.getBytes("UTF-8"));
			return Hex.toHexString(hes);
		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nijedan provajder ne podrzava implementaciju MessageDigestSpi za navedeni algoritam!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE,
					"Nijedan provajder ne podrzava implementaciju MessageDigestSpi za navedeni algoritam!", e);
		} catch (UnsupportedEncodingException e) {
			String porukaOGresci = "Nije podrzano kodiranje odgovarajuceg skupa karaktera!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Nije podrzano kodiranje odgovarajuceg skupa karaktera!", e);
		}
		return null;
	}

	public static String izracunajImeFajlaZaSlanje(String ime) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-224");
			String poruka = ime + new Date();
			byte[] hes = md.digest(poruka.getBytes("UTF-8"));
			return Hex.toHexString(hes);
		} catch (NoSuchAlgorithmException e) {
			String porukaOGresci = "Nije podrzan algoritam potpisa!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Nije podrzan algoritam potpisa", e);
		} catch (UnsupportedEncodingException e) {
			String porukaOGresci = "Nije podrzano kodiranje odgovarajuceg skupa karaktera!";
			Dijalog.showErrorDialog("Greska", "Doslo je do greske!", porukaOGresci);
			FileLogger.log(Level.SEVERE, "Nije podrzano kodiranje odgovarajuceg skupa karaktera!", e);
		}
		return null;
	}

}
