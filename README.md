# QrPrint – Android alkalmazás dokumentáció (AI generált)

## Összefoglaló
A QrPrint egy Android (Kotlin) alapú alkalmazás, amely gyártási / logisztikai folyamatokban használt dobozcímkék (QR kód + leíró matrica) generálását, QR kódok beolvasását és Brother hálózati nyomtatókra történő nyomtatását végzi. Az alkalmazás több adatforrásból (SQL Server jellegű DB – jTDS driver), valamint felhasználói bevitelből állítja elő a címke tartalmát. A címke két részből áll: QR kód bitmap + szöveges metaadat bitmap, amelyet egymás mellé illesztve küld a nyomtatóra.

## Fő funkciók
- Cikkszám / tétel kiválasztás és adatainak megjelenítése.
- QR kód generálás az aktuális metaadatokból (cikkszám, megnevezés, mennyiség, dokumentum szám, LOT, dátum, kód).
- Több példány nyomtatása (szétcsomagolás / packout logika).
- Brother hálózati nyomtatók keresése és kiválasztása.
- Már meglévő QR kód beolvasása (kamera) és tartalmának feldolgozása.
- Dátumkezelés: első nyomtatás + aktuális nyomtatás idősor.
- Dinamikus leíró matrica generálása szöveg tördeléssel.
- Jogosultságkezelés (CAMERA permission) PermissionDispatcher keretrendszerrel.
- Navigáció több funkcionális modul (Revenues, Delivery Notes, QR Print) között (Android Navigation komponens).

## Fő komponensek és fájlok

| Fájl | Szerep |
|------|--------|
| [AndroidManifest.xml](https://github.com/najdanovszki/QrPrint/blob/abd1a92f1f88e2179658f747841997d35cbd070c/app/src/main/AndroidManifest.xml) | Alkalmazás konfiguráció, engedélyek (INTERNET, CAMERA), `QrPrintApplication`, `MainActivity`. |
| [QrPrintApplication.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/QrPrintApplication.kt) | Hilt inicializálás az @HiltAndroidApp annotációval. |
| [MainActivity.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/MainActivity.kt) | Alap activity, menü kamera választással (front/back). |
| [MenuFragment.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/ui/menu/MenuFragment.kt) | Belépési pont funkcionális modulokhoz, navigáció gombokkal. |
| [QrCodeFragment.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/ui/qrcode/QrCodeFragment.kt) | A QR generálás, nyomtatás és beolvasás központi logikája. |
| [NavigationItem.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/ui/qrcode/NavigationItem.kt) | Adatátadás fragmentek között (Serializable). |
| [DeliveryNoteDetailsFragment.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/ui/deliverynotes/DeliveryNoteDetailsFragment.kt) | Szállítólevelek részletei, sor kiválasztás → átadás a QR generáláshoz. |
| [RevenueDetailsFragment.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/ui/revenues/RevenueDetailsFragment.kt) | Bevételezés részletei, sorrendezés, kiválasztás. |
| [AppConstants.kt](https://github.com/najdanovszki/QrPrint/blob/c95ec535b5839c2a99c17e54f286607244deb228/app/src/main/java/com/webtic/qrprint/util/AppConstants.kt) | Adatbázis kapcsolat paraméterek + konstansok. |
| BrotherPrintLibrary modul | Külső AAR integráció a Brother nyomtatók SDK-jához. |

Megjegyzés: A keresési eredmények nem biztos, hogy a teljes kódot lefedik.

## Technológiai stack
- Nyelv: Kotlin
- UI: Fragments + ConstraintLayout
- Függőségek:
    - ZXing (QR kód generálás + beolvasás) `com.journeyapps:zxing-android-embedded`, `com.google.zxing:core`
    - Hilt (DI)
    - Android Navigation Component (SafeArgs)
    - PermissionDispatcher (kamera jogosultság)
    - Brother SDK (AAR)
    - jTDS JDBC driver (SQL Server / Sybase jellegű DB elérés)
    - Multidex támogatás
- Minimum SDK: 21
- Target SDK: 30

## Architektúra áttekintés
Az alkalmazás moduláris fragment-alapú felépítést használ. A fragmentek között a Navigation komponens és SafeArgs biztosítja az adatok típusbiztos átadását (pl. `NavigationItem`). A Hilt gondoskodik az erőforrások (pl. `PreferencesManager`) injektálásáról. A `QrCodeFragment` a workflow középpontja: adatbetöltés (DB lekérdezés eredményei → autocomplete), QR generálás, bitmap előállítás, nyomtatás.

### Fő folyamat: új címke nyomtatás
1. Felhasználó kitölti a mezőket (cikkszám, mennyiség, LOT, dokumentum).
2. Validáció (`TextView.invalid()` segédfüggvénnyel).
3. QR szöveg összeállítása `createQrText(...)`.
4. QR bitmap generálás `encodeAsBitmap(text)`.
5. Leíró bitmap generálás `generateBitmapForDescription(...)`.
6. Bitmap-ek összeépítése és átadás a nyomtató rétegnek `preferencesManager.printImage(...)`.

### Packout (szétcsomagolás)
Logika: fő doboz maradék mennyisége + új doboz(ok) címkéi. Ha a fő dobozban még marad tartalom (`mainQuantity > 0`), új "maradék" címke is készül.

### Beolvasás
- Kamera nyitása: `scanQrCodeWithPermissionCheck()` → ZXing integrátor konfigurálás.
- Eredmény feldolgozás: `onActivityResult` → QR szöveg sorokra bontása → mezők kitöltése.
- Hibakezelés: Snackbar üzenetek (pl. „Olvasás megszakítva”).

## QR kód formátum
A `createQrText` függvény soronként fűzi össze:
```
PART_NUMBER
DESCRIPTION (sortörések tisztítva)
QUANTITY
DOCUMENT
LOT
[Első nyomtatás dátuma + kód]? (ha van)
Mai dátum + kód
```
Ez biztosítja, hogy egy már nyomtatott címke QR kódja újranyomtatható és kiegészíthető az idősorral.

## Bitmap generálás részletei
- Méret: 500x500 (`QR_SIZE`)
- QR: MultiFormatWriter + ErrorCorrectionLevel.L
- Leíró matrica: Canvas + TextPaint, tördelés `StaticLayout`.
- Speciális szóköz csere `Typography.nbsp` használatával a tördelés javításához.
- Dátum formátum: `yyyy-MM-dd`.

## Nyomtatás
A `PrintRequest` adatszerkezet (belső data class) tárolja:
- `qrCode: Bitmap?`
- `desc: Bitmap?`
- `quantity: Int`
  Az összes képpár összeállítása után kétszeres szélességű bitmap (QR + leírás egymás mellett) készül minden példányhoz.

## Jogosultságkezelés
PermissionDispatcher annotációk:
- `@NeedsPermission(Manifest.permission.CAMERA)` → kamera használat.
- `@OnPermissionDenied` → felhasználói tájékoztatás.
- `@OnShowRationale` → magyarázó dialog.

## Navigáció és adatátadás
`NavigationItem`:
- Tartalmazza a cikkszámot, K-kódot, leírást, dokumentum számozást, mennyiséget, ügyfél azonosítót, állapotflaget.
- Egyes modulok (pl. DeliveryNoteDetailsFragment) a sor kiválasztásával átirányítanak a `QrCodeFragment`-be előre kitöltött mezőkkel.

## Adatbázis integráció
- Kapcsolati konstansok: `AppConstants.kt` (DB_SERVER, DB_NAME, stb.).
- jTDS driver lokálisan (`libs/jtds-1.2.7.jar`).
- Lekérdezések eredményeit `ResultSet` futja be (`collectAdapterData`), autocomplete Map feltöltése.
- FIGYELEM: A felhasználónév/jelszó mezők üresek – konfigurációt környezeti szinten célszerű kezelni (lásd Biztonság).

## Hibakezelés és visszajelzések
- SnackBar üzenetek nyomtatási állapothoz (siker / error code).
- Input ellenőrzéskor mező hibaüzenet: "Kötelező kitölteni!"
- Érvénytelen cikkszám, hiányzó QR olvasás → azonnali felhasználói feedback.

## Teljesítmény és optimalizálás
Lehetséges fejlesztési pontok:
- Nagy méretű bitmap-ek memóriakezelése (Bitmap pooling / kisebb méret testelése).
- QR kód generálás aszinkron (Coroutines + Dispatchers.IO).
- Adatbázis hívások cache-elése (Room wrapper / repository réteg).
- Brother nyomtató API hívások retry mechanizmus.

## Biztonság
- Adatbázis hozzáférési adatok ne legyenek a forrásban (jelszó/username üres – jó, de biztosítani kell külső konfigurációt).
- Hálózati elérés (INTERNET permission) → potenciálisan TLS réteg javasolt, ha távoli szerver lesz.
- A QR kódba írt adatok (pl. ügyfélkód) szükség esetén anonimizálható.

## Nemzetköziesítés
- Hardcode-olt magyar sztringek (pl. „Érvénytelen cikkszám”) → javasolt `strings.xml`-be kiszervezni. Így későbbi többnyelvű támogatás megvalósítható.

## Build és futtatás
1. Klónozás: `git clone https://github.com/najdanovszki/QrPrint.git`
2. Android Studio (Arctic Fox vagy újabb) megnyitás.
3. Gradle sync (függőségek letöltése).
4. A Brother AAR már be van húzva (`BrotherPrintLibrary.aar`).
5. Beállítás:
    - DB paraméterek módosítása (ha szükséges) `AppConstants.DB_NAME`.
    - Ha szükséges Hilt modulok: (Nincs bemutatva itt, de `PreferencesManager` injektálva).
6. Futtatás: Run → választott eszköz (kamera funkcióhoz fizikai eszköz ajánlott).

## Példa kód részletek

QR szöveg építése:
```kotlin
private fun createQrText(partno: String, description: String, quantity: String, document: String, lot: String, firstPrint: String, kkod: String): String =
    StringBuilder().apply {
        append("$partno\n")
        append("${description.replace('\u000A', Typography.nbsp)}\n")
        append("$quantity\n")
        append("$document\n")
        append("$lot\n")
        val today = DATE_FORMAT.format(Date())
        if (firstPrint.isNotBlank()) {
            append("$firstPrint $kkod\n")
            append("$today $kkod")
        } else {
            append("$today $kkod")
        }
    }.toString()
```

QR Bitmap előállítás:
```kotlin
MultiFormatWriter().encode(
    text,
    BarcodeFormat.QR_CODE,
    QR_SIZE,
    QR_SIZE,
    mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L)
)
```

Leíró matrica generálás (részlet):
```kotlin
val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
val canvas = Canvas(bitmap)
canvas.drawColor(Color.WHITE)
// fő és másodlagos TextPaint objektumok → StaticLayout tördeléssel
```

Nyomtatás többször:
```kotlin
val output = Bitmap.createBitmap(2 * QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
val combined = Canvas(output)
combined.drawBitmap(qrCode, 0f, 0f, null)
combined.drawBitmap(desc, QR_SIZE.toFloat(), 0f, null)
// preferencesManager.printImage(...)
```

## Validáció
Példa mező ellenőrzés:
```kotlin
private fun TextView.invalid(): Boolean = if (text.isBlank()) {
    error = "Kötelező kitölteni!"
    true
} else false
```

## Jövőbeni fejlesztési javaslatok
- Unit tesztek: QR formátum, bitmap generálás, mennyiségi kalkuláció (`decodeQuantity`).
- ViewBinding / Jetpack Compose bevezetése a `kotlin-android-extensions` helyett (deprecated).
- Konfigurációk: DB szerver + nyomtató preferenciák externalizálása (pl. `.properties` / Remote config).
- Dark mode támogatás (jelenleg kényszerített `MODE_NIGHT_NO`).
- Logging egységesítése (Timber).

## Licenc és hozzájárulás
A jelenlegi repóban nincs részletezett licenc információ. Javasolt:
- MIT / Apache 2.0 fájl elhelyezése.
- Contributing irányelvek (Branch naming, PR template).

## Összegzés
A QrPrint megoldás fókuszáltan kezeli a gyártási címkenyomtatás és QR adatkörforgás igényeit. A kód szerkezetileg áttekinthető, de hosszú fragment logika további rétegezéssel (ViewModel, UseCase) még tisztítható. A meglévő komponensek jó alapot adnak a skálázható továbbfejlesztéshez.

---

Megjegyzés: A fenti dokumentáció a rendelkezésre álló (részleges) keresési találatok alapján készült; előfordulhatnak a repóban további releváns osztályok vagy erőforrások, melyek nincsenek itt felsorolva.