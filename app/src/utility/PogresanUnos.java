package utility;

public class PogresanUnos {

	public static boolean praznoPolje(String unos) {
		if (unos == null)
			return true;
		return unos.trim().length() == 0;
	}
}
