# QrPrint – Fejlesztési napló

## Elvégzett feladatok

### ✅ Task 1 – Címke elrendezés átstrukturálása

**Leírás:** A nyomtatott cimkén a PN (part number) és CC (customer code) a fejlécbe kerültek, alatta QR kód + info panel.

**Módosított fájlok:**

- `QrCodeFragment.kt` – `buildLabelBitmap()` új metódus, `PrintRequest` data class kibővítve

**Megrendelői teendő:** Nincs.

---

### ✅ Task 2 – Betűméret parametrizálás

**Leírás:** A fejléc (PN/CC) és az info panel (QTY, DESC, FP, RP) betűméretei konstansba kerültek, könnyen módosíthatók.

**Módosított fájlok:**

- `AppConstants.kt` – `LABEL_PART_NO_FONT_DP = 20f`, `LABEL_INFO_FONT_DP = 11f`

**Megrendelői teendő:** Ha a méretek nem megfelelők, megadja a kívánt értékeket (dp-ben).

---

### ✅ Task 3 – KKOD első karaktere kerül a QR kódba

**Leírás:** A QR kód szövegébe és a beolvasás-ellenőrzésnél is csak a kkod első karaktere (`firstOrNull()`) kerül.

**Módosított fájlok:**

- `QrCodeFragment.kt` – `createQrText()`, QR scan validáció

**Megrendelői teendő:** Nincs.

---

### ✅ Task 5 – Packout: 3 csomagolási egység + mennyiségeltérés figyelmeztetés

**Leírás:** A packout képernyőn 3 csomagolási sor adható meg (multiple/quantity párok). Ha az összesített mennyiség nem egyezik az eredeti sorral, figyelmeztető dialógus jelenik meg.

**Módosított fájlok:**

- `QrCodeFragment.kt` – `printPackoutImages()` újraírva, `executePrint()` belső függvény
- `fragment_qr_code.xml` – `multiple2Text`, `quantity2Text`, `multiple3Text`, `quantity3Text` mezők hozzáadva

**Megrendelői teendő:** Nincs.

> ⚠️ **Nyitott részfeladat (Task 5b):** A "bulk box" csomaghoz a **szállítói CC** (supplier customer code) kellene, a "kiszerelt" csomaghoz pedig a vevői CC. Jelenleg mindkét eset a vevői CC-t (`clientId`) használja, mert nem ismert, hogy a szállítói kód melyik táblából/mezőből jön. Kérdés a megrendelőnek: honnan kell a szállítói kódot lekérni?

---

### ✅ Task 6 – Szűrési időszak beállíthatósága típusonként

**Leírás:** A Számlák, Szállítólevelek, Bevételek listáknál külön-külön beállítható, hány napra visszamenőleg jelenjenek meg a rekordok. Alapértelmezések: számlák 4 nap, szállítólevelek és bevételek 10 nap.

**Módosított fájlok:**

- `PreferencesManager.kt` – `billsDaysBack`, `deliveryNotesDaysBack`, `revenuesDaysBack` property-k
- `BillsFragment.kt`, `DeliveryNotesFragment.kt`, `RevenuesFragment.kt` – SQL-ek az új paramétereket használják

**Megrendelői teendő:** A beállítások jelenleg csak kódból módosíthatók. Ha szükséges egy UI (Settings képernyő) a beállításukhoz, azt külön kell kérni.

---

### ✅ Task 7 – Tétel jellemzője megjelenik a leírás mellett (bevételek)

**Leírás:** A Bevételek részletező képernyőn a tétel „Megjegyzés" cellájában a szállítólevélen szereplő jellemző (`SZALLLEVRE='1'`) is megjelenik, `/` elválasztóval a megjegyzés után.

**Módosított fájlok:**

- `RevenueDetailsItem.kt` – `Cikk` adatosztályba `jellemzo: String?` mező
- `RevenueDetailsFragment.kt` – SQL bővítve subquery-vel, `updateViews()` megjelenítés frissítve

**Adatbázis alap:** `jellemzok` tábla, `SZALLLEVRE='1'` szűrő, eredmény formátuma: `"bruttó kg: 4.44"`.

**Megrendelői teendő:** Ellenőrizni, hogy a megjelenő jellemző a várt adat-e (más `SZALLLEVRE` értéket is figyelembe kell venni?).

---

### ✅ Task 8 – Naplózás (PRINT és STATUS_SET események)

**Leírás:** Az alkalmazás naplóz minden nyomtatást és státusz-beállítást a `QrPrint_log` táblában. A tábla automatikusan létrejön az első eseménynél.

**Log tábla struktúra** (`QrPrint_log`):
| Oszlop | Típus | Leírás |
|---|---|---|
| id | BIGINT IDENTITY | Automatikus azonosító |
| datum | DATETIME | Esemény időpontja (GETDATE()) |
| felhasznalo | VARCHAR(100) | Bejelentkezett DB felhasználó |
| esemeny | VARCHAR(50) | Esemény típusa |
| etk | VARCHAR(50) | Cikkszám |
| kbizszam | VARCHAR(100) | Bizonylat szám |
| mennyiseg | INT | Mennyiség |

**Eseménytípusok:**

- `PRINT` – QrCodeFragment, minden nyomtatáskor
- `STATUS_SET` – BillDetailsFragment, LiveOrderDetailsFragment, RevenueDetailsFragment – checkbox bejelölésekor
- `SCAN_MISMATCH` – QrCodeFragment – ha a beolvasott QR nem egyezik a kiválasztott tétellel

**Módosított fájlok:**

- `AppConstants.kt` – `loggedInUser`, `LOG_TABLE_NAME`
- `LoginFragment.kt` – bejelentkezéskor elmenti a felhasználónevet
- `BaseFragment.kt` – `logEvent()` metódus (tábla-létrehozás + INSERT)
- `QrCodeFragment.kt` – PRINT és SCAN_MISMATCH logolás
- `BillDetailsFragment.kt` – STATUS_SET logolás
- `LiveOrderDetailsFragment.kt` – STATUS_SET logolás
- `RevenueDetailsFragment.kt` – STATUS_SET logolás

**Megrendelői teendő:** Ellenőrizni, hogy a naplóban várnak-e más eseménytípusokat is (pl. QR scan sikeres, bejelentkezés, stb.).

---

### ✅ Task 9 – Cikk keresésből közvetlen navigáció QR képernyőre

**Leírás:** A cikkszám keresési képernyőn a cikkszámra kattintva közvetlen navigáció a QR kód képernyőre.

**Módosított fájlok:**

- `SearchFragment.kt`

**Megrendelői teendő:** Nincs.

---

### ✅ Task 10 – Checkbox (jelölés) a Számlák tételeinél

**Leírás:** A számla részletező képernyőn a tételek jelölhetők checkboxszal, az állapot a `SZAMLA_marking` táblában tárolódik (ugyanolyan módon mint a LiveOrders esetén a `RENDELT_marking` táblában).

**Módosított fájlok:**

- `BillDetailsFragment.kt`
- `BillDetailsItem.kt`
- `template_bill_details_table_row.xml`

**Megrendelői teendő:** Nincs.

---

### ✅ Task 11 & 12 – Numerikus értékek 3 tizedesre kerekítve

**Leírás:** Minden SQL lekérdezésben a `round(x,2)` helyett `round(x,3)` szerepel.

**Módosított fájlok:**

- `BillsFragment.kt`, `BillDetailsFragment.kt`
- `DeliveryNotesFragment.kt`
- `RevenuesFragment.kt`, `RevenueDetailsFragment.kt`
- `LiveOrderDetailsFragment.kt`
- `SearchFragment.kt`

**Megrendelői teendő:** Nincs.

---

## ❌ Nyitott feladatok

### Task 4 – QR scan validáció szállítólevél KKOD alapján + LOG

**Státusz:** Részben megvalósítva.

**Ami megvan:**

- Scan eltérésnél `SCAN_MISMATCH` esemény kerül a naplóba

**Ami hiányzik:**

- Ha a delivery note-ból érkezik a QR képernyőre (szállítólevél), a kkod ellenőrzéséhez a **szállítólevél tételeinek KKOD-ját kellene lekérni** az adatbázisból, és összehasonlítani a beolvasottal.
- Nem ismert, hogy milyen szállítólevélszám formátumban (pl. `24/00908-B`) érkezik az adat, és pontosan melyik táblából kell a kkod-ot kikeresni.

**Nyitott kérdés a megrendelőnek:** A szállítólevél-alapú QR scan validáció pontosan hogyan működjön? A `KESZLETF.KBIZSZAM` bizonylat alapján kell a tételek KKOD-ját lekérni, és ha a beolvasott kkod nincs a tételek között, az eltérés?

---

### Task 5b – Szállítói CC a bulk csomaghoz

**Státusz:** Nyitott.

**Leírás:** A packout során a "bulk" (nagy) dobozhoz a szállítói CC kellene, a kiszerelt dobozhoz a vevői CC.

**Nyitott kérdés a megrendelőnek:** A szállítói CC melyik táblából/mezőből jön? Az `ajanlat` táblában az `UGYFELETK` mező tartalmazza a vevői kódot. Van-e külön szállítói kód tábla, vagy az `UGYFELKOD` egy másik `ajanlat` sorából kellene lekérni?

---

## Összefoglalás

| #     | Feladat                           | Státusz                                      |
| ----- | --------------------------------- | -------------------------------------------- |
| 1     | Címke elrendezés                  | ✅ Kész                                      |
| 2     | Betűméret parametrizálás          | ✅ Kész                                      |
| 3     | KKOD első karaktere a QR-ben      | ✅ Kész                                      |
| 4     | QR scan validáció + LOG           | ⚠️ Részleges (log megvan, validáció nyitott) |
| 5     | Packout 3 egység + figyelmeztetés | ✅ Kész                                      |
| 5b    | Bulk csomag szállítói CC          | ❌ Nyitott kérdés                            |
| 6     | Szűrési időszak beállítás         | ✅ Kész (UI nélkül)                          |
| 7     | Jellemző a bevétel leíráshoz      | ✅ Kész                                      |
| 8     | Naplózás                          | ✅ Kész                                      |
| 9     | Cikk keresés → QR navigáció       | ✅ Kész                                      |
| 10    | Checkbox számlák tételeinél       | ✅ Kész                                      |
| 11–12 | 3 tizedes kerekítés               | ✅ Kész                                      |
