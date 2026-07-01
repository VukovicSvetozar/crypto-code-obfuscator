package utility;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.util.encoders.Hex;

public class RacunanjeHesa {

	public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		System.out.println(izracunajHes("Ime"));
	}

	public static String izracunajHes(String ime) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-224");
		String poruka = ime;
		byte[] hes = md.digest(poruka.getBytes("UTF-8"));
		return Hex.toHexString(hes);
	}
}
