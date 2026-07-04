# Crypto Code Obfuscator

*Aplikacija za kriptografsku zaštitu (enkripciju), digitalno potpisivanje i provjeru integriteta izvornog koda, zasnovana na infrastrukturi javnog ključa (PKI).*

Korisnik bira datoteku sa izvornim kodom i šalje je drugom registrovanom korisniku u zaštićenom obliku — samo primalac, koristeći svoj privatni ključ, može datoteku dekriptovati i pritom automatski provjeriti da sadržaj nije izmijenjen u prenosu niti da je poslat od strane nekog drugog. Nakon uspješne dekripcije, primalac po želji može odmah kompajlirati i pokrenuti dobijeni kod, bez napuštanja aplikacije.

[![Prijava na sistem](https://github.com/VukovicSvetozar/crypto-code-obfuscator/raw/main/assets/1.png)](/VukovicSvetozar/crypto-code-obfuscator/blob/main/assets/1.png) [![Glavna strana aplikacije](https://github.com/VukovicSvetozar/crypto-code-obfuscator/raw/main/assets/2.png)](/VukovicSvetozar/crypto-code-obfuscator/blob/main/assets/2.png)

---

## 🔐 O aplikaciji

Aplikacija radi u dva načina rada, organizovana kao dva taba na glavnoj strani:

- **Enkripcija** — pošiljalac bira primaoca, algoritam za simetričnu enkripciju i algoritam za digitalni potpis, zatim datoteku sa izvornim kodom (npr. `.java`). Aplikacija generiše zaštićenu poruku i smješta je direktno u "sandučе" primaoca.
- **Dekripcija** — primalac bira dolaznu poruku iz svog sandučeta. Aplikacija je dekriptuje, prikazuje metapodatke (pošiljalac, datum, korišćeni algoritmi), verifikuje digitalni potpis i upisuje originalni izvorni kod na disk. Ako je potpis validan, korisniku se odmah nudi opcija da kod kompajlira i izvrši.

Prijavi na sistem prethodi provjera tri stvari: korisničkog imena, lozinke i **X.509 sertifikata** korisnika koji se prijavljuje.

---

## 🪪 Infrastruktura javnog ključa (PKI)

Aplikacija pretpostavlja da već postoji uspostavljena PKI infrastruktura na fajl-sistemu, sa kojom samo radi (ne generiše sertifikate):

- **Root CA sertifikat** (`sertifikati/Root CA/rootCA.cer`) — korijen povjerenja.
- **Sertifikat svakog korisnika** (`sertifikati/Korisnicki sertifikati/*.cer`), potpisan od strane CA tijela.
- **CRL lista** (`crl/CA.crl`) sa povučenim sertifikatima.
- **Privatni ključ** trenutno prijavljenog korisnika, u PKCS8/DER formatu, na lokalnom fajl-sistemu.

Svaki sertifikat se validira **u trenutku upotrebe** (i pri prijavi, i pri enkripciji, i pri verifikaciji potpisa), kroz nekoliko provjera: potpis CA tijela, period važenja, poklapanje sa sertifikatom zapisanim za to korisničko ime, poklapanje izdavaoca i status opozivanja u CRL listi.

Sertifikatima je ograničena namjena preko **Key Usage** ekstenzije — aplikacija provjerava da li konkretan sertifikat dozvoljava digitalno potpisivanje (`digitalSignature`) prije potpisivanja poruke, odnosno enkripciju ključa (`keyEncipherment`) prije nego što se iskoristi za RSA zaštitu simetričnog ključa primaoca.

---

## 🔑 Hibridni kriptografski protokol

**Enkripcija (pošiljalac → primalac):**

1. Učitava se i validira sertifikat primaoca; iz njega se izdvaja javni RSA ključ.
2. Generiše se nasumičan **simetrični sesijski ključ**, algoritmom koji je pošiljalac odabrao.
3. Sesijski ključ se **enkriptuje RSA algoritmom**, javnim ključem primaoca.
4. Učitava se izvorni kod i formira poruka sa metapodacima (datum, pošiljalac, korišćeni algoritmi, naziv datoteke) i samim kodom.
5. Poruka se **digitalno potpisuje** privatnim RSA ključem pošiljaoca, odabranim hibridnim algoritmom potpisa.
6. Potpis i poruka se zajedno **enkriptuju simetričnim ključem**.
7. Enkriptovani ključ i šifrat se spajaju, kodiraju u Base64 i upisuju u `.xyz` datoteku (uz naziv korišćenog simetričnog algoritma u čistom tekstu, neophodan za dekripciju) direktno u folder primaoca.

**Dekripcija (primalac):**

1. Datoteka se učitava i Base64-dekodira; razdvaja se enkriptovani ključ (RSA blok) od šifrata.
2. Ključ se dekriptuje sopstvenim privatnim RSA ključem, čime se dobija simetrični ključ kojim se dekriptuje ostatak poruke.
3. Iz dekriptovanog sadržaja se izdvaja digitalni potpis od originalne poruke (metapodaci + izvorni kod).
4. Metapodaci se prikazuju korisniku, a izvorni kod se upisuje u lični folder primaoca (`Dekriptovane datoteke`).
5. Iskorišćena poruka se briše iz sandučeta (nad sandučetom radi i pozadinski `FileWatcher` koji upozorava ako je poruka nestala mimo same aplikacije).
6. Potpis se **verifikuje** javnim ključem pošiljaoca (izvučenim iz njegovog sertifikata) — korisnik dobija obavještenje da je sadržaj neizmijenjen ili upozorenje da validacija nije prošla.
7. Opcionalno: kod se **kompajlira** (`javax.tools.JavaCompiler`) i **pokreće** (`Runtime.exec`), a izlaz programa se prikazuje direktno u aplikaciji.

---

## 🧮 Podržani algoritmi

| Namjena | Podržane opcije |
|---|---|
| Simetrična enkripcija poruke | `DESede`, `AES`, `Blowfish`, `ARCFOUR` |
| Asimetrična zaštita sesijskog ključa | `RSA` |
| Digitalni potpis / heširanje poruke | `MD5withRSA`, `SHA256withRSA`, `SHA384withRSA`, `SHA512withRSA` |
| Heširanje korisničke lozinke | `PBKDF2WithHmacSHA512` (10 000 iteracija, so po korisniku) |

Simetrični algoritam i algoritam potpisa pošiljalac bira zasebno za svaku poruku, čime se ilustruje razlika u performansama i namjeni asimetrične (RSA — samo za mali sesijski ključ) i simetrične kriptografije (za sam sadržaj poruke, koji može biti znatno veći).

---

## 👤 Korisnici, lozinke i lični podaci

- Prijava zahtijeva korisničko ime, lozinku i putanju do `.cer` sertifikata.
- Lozinke se čuvaju u `lista korisnika/Lista korisnika.txt`, u formatu `korisničkoIme#so#heširanaLozinka` — nikad u čistom tekstu.
- Svaki korisnik je interno identifikovan heksadecimalnim **SHA-224 hešom svog korisničkog imena**; taj heš je i naziv njegovog ličnog foldera na fajl-sistemu, u kojem se čuvaju njegov privatni ključ, sanduče dolaznih poruka i dekriptovani fajlovi.
- Repozitorijum sadrži gotove demo naloge (Iva, Lana, Maja, Mia, Tea) sa odgovarajućim sertifikatima i CA infrastrukturom, kao i nekoliko kratkih `.java` fajlova (ispis šablona, unos imena i prezimena, demonstracija literala i kriptografskih klasa) spremnih za testiranje enkripcije.

---

## 🗂️ Perzistencija i struktura podataka

| Putanja | Sadržaj |
|---|---|
| `lista korisnika/Lista korisnika.txt` | Korisnička imena, so i heš lozinke |
| `sertifikati/Root CA/rootCA.cer` | Sertifikat CA tijela |
| `sertifikati/Korisnicki sertifikati/*.cer` | Sertifikati svih registrovanih korisnika |
| `crl/CA.crl` | Lista povučenih sertifikata |
| `korisnici/<heš korisničkog imena>/Privatni kljuc/privatniKljuc.der` | Privatni ključ korisnika (PKCS8) |
| `korisnici/<heš korisničkog imena>/Poruke korisnika/*.xyz` | Dolazne, još nepročitane poruke |
| `korisnici/<heš korisničkog imena>/Dekriptovane datoteke/` | Dekriptovan izvorni kod (i `.class` nakon kompajliranja) |
| `izvorni kodovi/` | Primjeri `.java` datoteka za testiranje enkripcije |
| `log.xml` | Log grešaka (`java.util.logging`) |

---

## 🖥️ Pregled grafičkog interfejsa

Aplikacija je izgrađena u **JavaFX**-u (FXML + CSS), sa prozorom za prijavu, glavnom stranom sa dva taba (enkripcija/dekripcija) i posebnim prozorom za kompajliranje i izvršavanje dekriptovanog koda.

> Snimci ekrana se nalaze u folderu [`assets/`](assets).

---

## 💻 Tehnologije i alati

- **Jezik:** Java
- **GUI biblioteka:** JavaFX (FXML + CSS stilizacija)
- **Kriptografija:** `javax.crypto`, `java.security` (X.509 sertifikati, CRL, digitalni potpisi, PKCS8 ključevi) uz **Bouncy Castle** (`bcprov-jdk15on`) kao dodatnog security provajdera
- **Kompajliranje i izvršavanje koda unutar aplikacije:** `javax.tools.JavaCompiler`, `Runtime.exec`
- **Praćenje fajl-sistema:** `java.nio.file.WatchService`
- **Logovanje:** `java.util.logging`
- **Razvojno okruženje:** Eclipse (bez Maven/Gradle build sistema)

> Napomena: folder `app/lib` sadrži i `mysql-connector-java` i `steganography` biblioteke, ali trenutni izvorni kod ih ne koristi — ostavljene su vjerovatno kao priprema za moguću nadogradnju (npr. čuvanje podataka u bazi ili skrivanje poruka u slikama).

---

## 🚀 Kako pokrenuti projekat lokalno

### Preduslovi

- Instaliran **Java JDK** (uključuje `javax.tools.JavaCompiler`, korišćen za kompajliranje dekriptovanog koda)
- Preuzet **JavaFX SDK** (nije uključen u sam JDK od verzije 11 naviše)

### Uvoz i pokretanje

1. Klonirajte repozitorijum i uvezite folder `app` kao postojeći projekat u Eclipse (ili drugo IDE po izboru), sa svim `.jar` fajlovima iz `app/lib` dodatim na build path.
2. U konfiguraciji pokretanja (Run Configuration / VM Options) dodajte JavaFX module:

```
--module-path /putanja/do/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
```

3. **Važno:** postavite radni direktorijum (Working Directory) konfiguracije pokretanja na folder `app/`. Aplikacija čita sertifikate, CRL listu, privatne ključeve i korisničke podatke preko relativnih putanja u odnosu na radni direktorijum.
4. Pokrenite klasu `application.PokretanjeAplikacije`.
5. Prijavite se jednim od demo naloga (npr. `Iva`) uz odgovarajući `.cer` iz `sertifikati/Korisnicki sertifikati/` — lozinke demo naloga nisu poznate/rekonstruišive iz heša, pa ih po potrebi treba prilagoditi ili registrovati novog korisnika ručnim dodavanjem u `Lista korisnika.txt` i pripremom para ključeva/sertifikata.

---

## 📁 Struktura projekta

```
app/
├── src/
│   ├── application/   → ulazna tačka aplikacije
│   ├── controller/    → JavaFX kontroleri (prijava, enkripcija, dekripcija, izvršavanje)
│   ├── model/         → domenski model (Korisnik)
│   ├── utility/       → kriptografske i pomoćne klase
│   └── view/          → FXML definicije ekrana
├── lib/                → eksterne biblioteke (Bouncy Castle i dr.)
├── lista korisnika/, sertifikati/, crl/, korisnici/, izvorni kodovi/
│                     → podaci aplikacije (vidi tabelu perzistencije)
docs/                 → tekst projektnog zadatka
assets/               → snimci ekrana za ovaj README
```

---

## 👤 Autor

Crypto Code Obfuscator je razvijen kao projektni zadatak iz predmeta **Kriptografija i računarska/kompjuterska zaštita**, na **Elektrotehničkom fakultetu Univerziteta u Banjoj Luci**, autora **Svetozara Vukovića**.
