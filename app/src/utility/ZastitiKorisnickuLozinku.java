package utility;

public class ZastitiKorisnickuLozinku {

	public static void main(String[] args) {

		String myPassword = "ime unatrag";

		String salt = MetodeZaLozinke.getSalt(30);

		String mySecurePassword = MetodeZaLozinke.generateSecurePassword(myPassword, salt);

		System.out.println("Sigurna lozinka = " + mySecurePassword);
		System.out.println("Salt = " + salt);
	}
}
