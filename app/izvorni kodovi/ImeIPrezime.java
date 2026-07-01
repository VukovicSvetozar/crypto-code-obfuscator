import java.util.Scanner;

  public class ImeIPrezime {

	public static void main(String[] args) {
		String ime = "";
		
		Scanner scan = new Scanner(System.in);
		System.out.print("Unesite ime: ");
		ime = scan.nextLine();
		
		System.out.println(ime);
	}
}
