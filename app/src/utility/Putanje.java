package utility;

import java.io.File;

public interface Putanje {

	public final static String PUTANJA_DO_LISTE_KORISNIKA = "lista korisnika" + File.separator + "Lista korisnika.txt";

	public final static String PUTANJA_DO_SERTIFIKATA = "." + File.separator + "sertifikati";

	public final static String PUTANJA_DO_KORISNICKIH_SERTIFIKATA = PUTANJA_DO_SERTIFIKATA + File.separator
			+ "Korisnicki sertifikati" + File.separator;

	public final static String PUTANJA_DO_ROOT_CA = PUTANJA_DO_SERTIFIKATA + File.separator + "Root CA"
			+ File.separator;

	public final static String PUTANJA_DO_CRL = "." + File.separator + "crl" + File.separator;

	public final static String PUTANJA_DO_IZVORNIH_KODOVA = "." + File.separator + "izvorni kodovi";

	public final static String PUTANJA_DO_KORISNICKOG_FOLDERA = "." + File.separator + "korisnici" + File.separator;

	public final static String PUTANJA_DO_PRIVATNOG_KLJUCA = File.separator + "Privatni kljuc" + File.separator;
}
