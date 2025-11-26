# QrPrint Low-Level Design (LLD)

Ez a dokumentum a QrPrint Android alkalmazás alacsony szintű tervezését írja le. A QrPrint célja, hogy különféle tartalmakból (URL, szöveg, WiFi, vCard, stb.) QR kódokat generáljon, megjelenítse, elrendezze és nyomtatásra/exportálásra előkészítse. REST API nem szükséges; a fókusz az Android kliensen van.

## Platform és technológiák
- Platform: Android (minSdk: javasolt 24+, targetSdk: aktuális)
- Nyelv: Kotlin
- UI: Jetpack Compose (ajánlott), alternatíva: XML + ViewBinding
- QR kódolás: ZXing (`com.google.zxing:core`)
- Képgenerálás: Bitmap (Android `android.graphics.Bitmap`), Canvas
- PDF export (opcionális): Android `PdfDocument` vagy 3rd party PDF lib (ha szükséges)
- Nyomtatás: Android Printing Framework (`android.print.PrintManager`, `PrintDocumentAdapter`)
- Fájlkezelés: MediaStore + scoped storage, SAF (Storage Access Framework)
- Beállítások: DataStore (Preferences/Proto)
- Naplózás: Logcat + opcionális fájl-alapú naplózás

## Modulok és rétegek
- Presentation (UI)
  - Compose képernyők: HomeScreen, GenerateScreen, LayoutScreen, PrintPreviewScreen, SettingsScreen
  - ViewModel-ek: állapotkezelés (UI state, events)
- Application (Use case-k / Interactorok)
  - GenerateQrUseCase
  - GenerateLayoutUseCase
  - ExportQrUseCase (PNG/SVG/PDF*)
  - PrintQrUseCase
  - ImportBatchUseCase (CSV/JSON – opcionális)
- Domain (Modellek és szerződések)
  - QrContent, QrSettings, RenderSettings, PrintSettings, Layout
  - Szolgáltatás interfészek: QrEncoder, QrRenderer, LayoutComposer, PrinterService, TemplateEngine, Validator
- Infrastructure (Implementációk)
  - ZxingQrEncoder
  - AndroidQrRenderer (Bitmap/SVG/PDF*)
  - AndroidPrinterService (PrintDocumentAdapter)
  - FileRepository (MediaStore/SAF)
  - SettingsRepository (DataStore)
  - TemplateEngineImpl (WiFi/vCard)
  - ValidatorImpl

(*) Megjegyzés: SVG export Androidon nem natív; alternatíva a vektoros Path-ek és saját SVG XML generálás.

## Adatmodell (Kotlin)
- QrType: `TEXT, URL, WIFI, VCARD, MECARD, EMAIL, SMS, GEO, JSON`
- QrContent
  - type: QrType
  - payload: String vagy típus-specifikus adat (WiFiContent, VCardContent)
- QrSettings
  - errorCorrectionLevel: `L, M, Q, H`
  - version: `Int?` (1–40)
  - mask: `Int?` (0–7)
  - marginModules: `Int = 4`
- RenderSettings
  - format: `PNG, SVG, PDF`
  - moduleSizeDp: `Float` (Compose/Android skálázáshoz)
  - foregroundColor: `Color`
  - backgroundColor: `Color?`
  - embedLogo: `LogoSpec?` (uri, sizeRatio, position)
- PrintSettings
  - paperSize: `A4, A5, LETTER, CUSTOM(widthMm, heightMm)`
  - orientation: `PORTRAIT, LANDSCAPE`
  - dpi: `Int?` (Android a printer drivertől függ)
  - copies: `Int`
  - marginsMm: `InsetsMm`
- Layout
  - grid: `rows, cols`
  - cellSizeMm: `width, height`
  - gutterMm: `Double`
  - frame: `show, strokeWidth, color`
  - textBelow: `enabled, fontFamily, fontSizeSp, color, contentExpression`

## Fő folyamatok (Use case részletek)
- GenerateQrUseCase
  1) Input validálás (Validator)
  2) QrEncoder → BitMatrix (ZXing)
  3) QrRenderer → Bitmap (PNG) vagy SVG/PDF string/bytearray
  4) Opcionális mentés (FileRepository)
- GenerateLayoutUseCase
  1) Tömbös encode minden QR tartalomra
  2) LayoutComposer rács elrendezés: cellák, gutter, frame
  3) Render: egyetlen PDF oldal vagy több oldal (PdfDocument); vagy nagy Bitmap kompozit
- ExportQrUseCase
  - PNG: `Bitmap.compress` → MediaStore mentés
  - SVG: XML generálás és `.svg` mentés SAF-fal
  - PDF: `PdfDocument` drawRect/drawBitmap modulonként
- PrintQrUseCase
  - `PrintManager` → saját `PrintDocumentAdapter` (render PDF/bitmap on-demand)
  - Attribútumok: Copies, Duplex (ha támogatott), Orientation
  - Státusz visszajelzés (onStart/onFinish/onWriteFailed)

## QR kódolás szabályok
- ECC alapértelmezés: M (állítható)
- Verzió/maszk: auto (ZXing hints), manuális override támogatás
- Hints: CHARACTER_SET=UTF-8, MARGIN=quiet zone, ERROR_CORRECTION, QR_VERSION?, QR_MASK_PATTERN?
- Strukturált payloadok:
  - WiFi: `WIFI:S:<ssid>;T:<WEP|WPA|nopass>;P:<password>;H:<hidden>;`
  - vCard: RFC 6350 mezők: FN, TEL, EMAIL, ORG; sorvég `\\n`
- Logo beágyazás
  - ECC H ajánlott, max ~20% felület, középre igazítva
  - Overlay a Bitmapen: Canvas drawBitmap, alpha
- Quiet zone: legalább 4 modul

## Renderelés (Android)
- Bitmap render
  - Méret: `modules * moduleSizePx`
  - Antialias: ki; `Paint.isAntiAlias=false`, `FilterBitmap=false`
  - Színek: `Paint.color`
- SVG render
  - `StringBuilder` XML: `<svg>` + `<rect>` modulokra; `shape-rendering="crispEdges"`
- PDF render
  - `PdfDocument.Page` Canvas: modulonként `drawRect`
  - Layout és marginok mm→px konverzió DPI/ppi alapján (printer/Canvas felbontás)

## Nyomtatás (Android Printing Framework)
- PrintDocumentAdapter
  - `onLayout`: oldalak mérete, `PrintAttributes`
  - `onWrite`: PDF/Bitmap előállítása és átadása a rendszernek (`ParcelFileDescriptor`)
- PrintAttributes
  - `MediaSize`, `Resolution`, `ColorMode`, `DuplexMode`
- Printer kiválasztás: rendszer dialógus; app csak javaslatokat adhat

## Fájlkezelés és jogosultságok
- Scoped storage: MediaStore (Pictures/Documents) mentés
- SAF: `Intent.ACTION_CREATE_DOCUMENT` és `ACTION_OPEN_DOCUMENT` export/importhoz
- Jogosultságok: MANAGE_EXTERNAL_STORAGE nélkül, lehetőleg SAF; CAMERA csak ha QR beolvasás is lesz

## Beállítások és perzisztencia
- DataStore (Preferences)
  - alapértelmezett ECC, margin, színek, moduleSize, export formátum
- Opcionális cache: payload+settings hash → utolsó generált fájl URI

## Validálás és hibakezelés
- Validator
  - URL séma: `http(s)://`, host validáció
  - WiFi mezők: SSID nem üres; encryption érték készlet
  - vCard mezők: kötelező FN
- Error típusok
  - DomainError: InvalidPayload, UnsupportedType, CapacityExceeded
  - RenderError: ImageRenderFailure, PdfRenderFailure
  - PrintError: PrintJobFailed
  - InfraError: FileIOError, PermissionDenied
- Lokalizáció: `strings.xml` (HU/EN)

## Teljesítmény
- Coroutines (Dispatchers.Default/IO) párhuzamosítás
- Nagy layoutok: oldalankénti render (PDF), streaming
- Bitmap memória: `Bitmap.Config.ALPHA_8` vagy `RGB_565` ha elég, preferált `ARGB_8888` a színekhez

## Tesztelés
- Unit
  - QrEncoder (hints mapping), TemplateEngine, Validator
- Instrumented
  - Renderer Bitmap/PDF; PrintDocumentAdapter mockolt környezet
- UI teszt (Compose)
  - állapotváltások, preview-k
- Golden image/snapshot
  - Pixel összehasonlítás toleranciával

## UI Flow (Compose példaváz)
- HomeScreen: gyors generálás, utolsó beállítások
- GenerateScreen: tartalom bevitel, form (URL/WiFi/vCard), preview
- LayoutScreen: rács beállítások, élő előnézet
- PrintPreviewScreen: nyomtatás indítása
- SettingsScreen: alapbeállítások

## Példa interfészek
```kotlin
interface QrEncoder {
    fun encode(content: QrContent, settings: QrSettings): BitMatrix
}

interface QrRenderer {
    fun renderBitmap(matrix: BitMatrix, settings: RenderSettings): Bitmap
    fun renderSvg(matrix: BitMatrix, settings: RenderSettings): String
    fun renderPdf(matrices: List<BitMatrix>, settings: RenderSettings, layout: Layout): ByteArray
}

interface PrinterService {
    suspend fun printPdf(pdf: ByteArray, printSettings: PrintSettings): PrintJobStatus
}
```

## Projekt szerkezet (Android)
- `app/`
  - `ui/` (Compose képernyők és komponensek)
  - `viewmodel/`
  - `domain/` (modellek, interfészek)
  - `data/` (repo-k: FileRepository, SettingsRepository)
  - `infra/` (ZxingQrEncoder, AndroidQrRenderer, AndroidPrinterService)
  - `usecase/` (GenerateQrUseCase, stb.)
  - `di/` (Hilt/Koin)
- `build.gradle` (app modul): ZXing, DataStore, Compose, Coroutines, Hilt/Koin, Optional Pdf
- `src/main/AndroidManifest.xml`
- `src/main/res/values/strings.xml`

## Következő lépések
- Add hozzá ezt a fájlt a repóhoz: `docs/lld.md`
- Ha szeretnéd, készítek hozzá PR-t; ehhez kérlek erősítsd meg, melyik branchre menjen és adj hozzáférést a push-hoz vagy jelezd, ha draft PR-t kérsz.