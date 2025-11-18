package com.webtic.qrprint.ui.qrcode

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Camera
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.brother.ptouch.sdk.NetPrinter
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.webtic.qrprint.R
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.util.*
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.PreferencesManager.PrinterResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_qr_code.*
import kotlinx.android.synthetic.main.fragment_qr_code.view.*
import permissions.dispatcher.*
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@RuntimePermissions
@AndroidEntryPoint
class QrCodeFragment : BaseFragment(), PrinterResultListener {


    @Inject
    lateinit var preferencesManager: PreferencesManager

    private lateinit var partNumberText: AutoCompleteTextView
    private lateinit var totalQuantityText: TextView
    private lateinit var kkodText: TextView
    private lateinit var documentText: TextView
    private lateinit var lotText: TextView
    private lateinit var multipleText: TextView
    private lateinit var quantityText: TextView
    private lateinit var resetBtn: Button
    private lateinit var packoutBtn: Button
    private lateinit var generateBtn: Button
    private lateinit var readBtn: Button
    private lateinit var settingsBtn: Button
    private lateinit var progress: ProgressBar
    private var scannedItem: QrMetadataItem? = null
    private var partNumbers: MutableMap<String, Pair<String, String>> = mutableMapOf()
    private var clientId: String? = null
    private var fromRevenues: Boolean? = null

    private val args: QrCodeFragmentArgs by navArgs()

    companion object {
        private const val QR_SIZE = 500
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
    }

    private fun TextView.invalid(): Boolean = if (text.isBlank()) {
        this.error = "Kötelező kitölteni!"
        true
    } else {
        false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_qr_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initButtons()

        args.selection?.let {
            partNumberText.setText(it.partNumber.toSearchable())
            kkodText.text = it.kkod
            if (it.document != null) documentText.text = it.document
            quantityText.text = it.quantity.toString()
            multipleText.text = "1"
            fromRevenues = it.notFromRevenues.not()
            initPartNumberText(it.partNumber)
            if (it.clientId != null)
                requestClientKey(it.clientId, it.partNumber)
            if (it.notFromRevenues)
                scanQrCode()
        } ?: initPartNumberText(null)

        view.backToMenuButton.setOnClickListener {
            findNavController().popBackStack(R.id.menuFragment, false)
        }
    }

// region Initializer

    private fun initViews(view: View) {
        partNumberText = view.findViewById(R.id.partnumberText)
        totalQuantityText = view.findViewById(R.id.totalQuantityText)
        kkodText = view.findViewById(R.id.kkodText)
        documentText = view.findViewById(R.id.documentText)
        lotText = view.findViewById(R.id.lotText)
        multipleText = view.findViewById(R.id.multipleText)
        quantityText = view.findViewById(R.id.quantityText)
        resetBtn = view.findViewById(R.id.resetBtn)
        packoutBtn = view.findViewById(R.id.packoutBtn)
        generateBtn = view.findViewById(R.id.generateBtn)
        readBtn = view.findViewById(R.id.readBtn)
        settingsBtn = view.findViewById(R.id.settingsBtn)
        progress = view.findViewById(R.id.progress)
    }

    private fun requestClientKey(clientIdentifier: Int, partNumber: String) {
        val requestSQL =
            "select a.ETK, UGYFELETK, LEFT(c.CIKKNEV1,32)+'...', CONVERT (date, SYSDATETIME()) datum\n" +
                    "from ${DB_NAME}..ajanlat a,\n" +
                    " ${DB_NAME}..cikk c\n" +
                    "where \n" +
                    " a.ugyfelkod=$clientIdentifier\n" +
                    " and a.ETK='$partNumber'\n" +
                    " and c.etk=a.etk\n" +
                    " and isnull(MIKORTOL,'1979-10-19')=(select isnull(MAX(MIKORTOL),'1979-10-19')\n" +
                    "  from ${DB_NAME}..ajanlat\n" +
                    "  where ugyfelkod=$clientIdentifier\n" +
                    "   and ETK='$partNumber')"

        when (val result = connectionManager.executeQuery(requestSQL)) {
            is QuerySuccess -> if (result.resultSet.next())
                clientId = result.resultSet.getString("UGYFELETK")
            is QueryError -> Log.e(
                "QueryError",
                "ClientID " + result.exception.message.toString()
            )
        }
    }

    /**
     * Initializes the AutoCompleteTextView
     * @param partNumber Value used to filter the results. If null there is no filtering.
     */
    private fun initPartNumberText(partNumber: String?) {
        var partNumberSQL = "select ETK, CIKKNEV1\n" +
                "from ${DB_NAME}.dbo.cikk"
        if (partNumber != null)
            partNumberSQL += "where ETK='$partNumber'"
        when (val result = connectionManager.executeQuery(partNumberSQL)) {
            is QuerySuccess -> partNumberText.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    collectAdapterData(result.resultSet)
                )
            )
            is QueryError -> Log.e(
                "QueryError",
                "autoComplete " + result.exception.message.toString()
            )
        }
    }

    /**
     * Strips the input list of strings from '-' and '/'
     * The new list is added to the adapter of the AutoCompleteTextView
     * @param result The SQL query ResultSet that contains the data
     */
    private fun collectAdapterData(result: ResultSet): List<String> {
        while (result.next()) {
            val key = result.getString("ETK")
            val value = result.getString("CIKKNEV1")
            partNumbers[key.toSearchable()] = Pair(key, value)
        }
        return partNumbers.keys.toList()
    }

    private fun initButtons() {
        readBtn.setOnClickListener {
            scanQrCodeWithPermissionCheck()
        }

        settingsBtn.setOnClickListener {
            progress.visibility = View.VISIBLE
            preferencesManager.availablePrinters(this)
        }

        resetBtn.setOnClickListener {
            partNumberText.text.clear()
            totalQuantityText.text = ""
            kkodText.text = ""
            documentText.text = ""
            lotText.text = ""
            multipleText.text = ""
            quantityText.text = ""
        }

        val alertDialog = AlertDialog.Builder(requireContext()).setTitle("Kitöltetlen mező!")
            .setMessage("\"Lot no\" mező üres. Biztosan folytatni akarod a nyomtatást?")
            .setNegativeButton("Mégse") { dialog, _ -> dialog.cancel() }


        generateBtn.setOnClickListener {
            if (partNumberText.invalid() || totalQuantityText.invalid() || documentText.invalid())
                return@setOnClickListener

//            if (fromRevenues!!)
//                alertDialog.setPositiveButton("Tovább") { _, _ -> printNewImage() }.show()
//            else
                printNewImage()
        }

        packoutBtn.setOnClickListener {
            if (partNumberText.invalid() || kkodText.invalid() || documentText.invalid() || multipleText.invalid() || quantityText.invalid())
                return@setOnClickListener

            if (fromRevenues ?: true)
                alertDialog.setPositiveButton("Tovább") { _, _ -> printPackoutImages() }.show()
            else
                printPackoutImages()
        }
    }

// endregion

// region Bitmap generation and printing

    private fun printNewImage() {
        val pair = partNumbers[partNumberText.text.toString()]
        if (args.selection == null && pair == null) {
            Toast.makeText(requireContext(), "Érvénytelen cikkszám", Toast.LENGTH_LONG).show()
            return
        }
        // Both of them cannot be null here so the !! operator will never throw an exception
        val qrCodeImage = encodeAsBitmap(
            createQrText(
                partno = pair?.first ?: args.selection!!.partNumber,
                description = pair?.second ?: args.selection!!.description,
                quantity = totalQuantityText.text.toString(),
                document = documentText.text.toString(),
                lot = lotText.text.toString(),
                firstPrint = "",
                kkod = kkodText.text.toString()
            )
        )
        val descriptionImage = generateBitmapForDescription(
            partNo = pair?.first ?: args.selection!!.partNumber,
            description = pair?.second ?: args.selection!!.description,
            quantity = totalQuantityText.text.toString(),
            firstPrint = "",
            kod = kkodText.text.toString().first(),
            clientCode = clientId
        )
        val numberOfPieces =
            if (multipleText.text.isNotBlank()) multipleText.text.toString().toInt() else 1
        try {
            requestPrinting(PrintRequest(qrCodeImage, descriptionImage, numberOfPieces))
        } catch (e: IllegalStateException) {
            Snackbar.make(
                requireView(),
                "Nincs kiválasztva nyomtató!",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun printPackoutImages() {
        val pair = partNumbers[partNumberText.text.toString()]
        if (args.selection == null && pair == null) {
            Snackbar.make(requireView(), "Érvénytelen cikkszám", Snackbar.LENGTH_LONG).show()
            return
        }
        if (args.selection != null) {
            if (scannedItem == null) {
                Snackbar.make(requireView(), "Nincs beolvasott QR kód", Snackbar.LENGTH_LONG).show()
                return
            }
        }
        val packed = multipleText.text.toString().toInt() * quantityText.text.toString().toInt()
        val mainQuantity = when {
            scannedItem != null -> scannedItem!!.quantity - packed
            totalQuantityText.text.isNotBlank() -> totalQuantityText.text.toString()
                .toInt() - packed
            else -> 0
        }

        // A new sticker should be printed only when there are contents left in the main box
        var qrCodeImage: Bitmap? = null
        var descriptionImage: Bitmap? = null
        if (mainQuantity > 0) {
            qrCodeImage = encodeAsBitmap(
                createQrText(
                    partno = pair?.first ?: args.selection!!.partNumber,
                    description = pair?.second ?: scannedItem!!.description,
                    quantity = mainQuantity.toString(),
                    document = documentText.text.toString(),
                    lot = lotText.text.toString(),
                    firstPrint = scannedItem?.firstPrinted?.let { DATE_FORMAT.format(it) } ?: "",
                    kkod = kkodText.text.toString()
                )
            )
            descriptionImage = generateBitmapForDescription(
                partNo = pair?.first ?: args.selection!!.partNumber,
                description = pair?.second ?: scannedItem!!.description,
                quantity = mainQuantity.toString(),
                firstPrint = scannedItem?.firstPrinted?.let { DATE_FORMAT.format(it) } ?: "",
                kod = kkodText.text.toString().first(),
                clientCode = null
            )
        }

        // Printing the stickers for the new boxes
        val newQrCodeImage = encodeAsBitmap(
            createQrText(
                partno = pair?.first ?: args.selection!!.partNumber,
                description = pair?.second ?: args.selection!!.description,
                quantity = quantityText.text.toString(),
                document = documentText.text.toString(),
                lot = lotText.text.toString(),
                firstPrint = "",
                kkod = kkodText.text.toString()
            )
        )
        val newDescriptionImage = generateBitmapForDescription(
            partNo = pair?.first ?: args.selection!!.partNumber,
            description = pair?.second ?: args.selection!!.description,
            quantity = quantityText.text.toString(),
            firstPrint = "",
            kod = kkodText.text.toString().first(),
            clientCode = clientId
        )
        try {
            val args: MutableList<PrintRequest> = mutableListOf()
            if (mainQuantity > 0)
                args.add(PrintRequest(qrCodeImage, descriptionImage, 1))

            args.add(
                PrintRequest(
                    newQrCodeImage,
                    newDescriptionImage,
                    multipleText.text.toString().toInt()
                )
            )
            requestPrinting(*args.toTypedArray())
        } catch (e: IllegalStateException) {
            Snackbar.make(
                requireView(),
                "Nincs kiválasztva nyomtató!",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun createQrText(
        partno: String,
        description: String,
        quantity: String,
        document: String,
        lot: String,
        firstPrint: String, // if this is null then today's date is used
        kkod: String
    ): String {
        return StringBuilder().apply {
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
    }

    /**
     * Generates a QR code as a bitmap from the input String
     */
    private fun encodeAsBitmap(text: String): Bitmap? {
        val result: BitMatrix = try {
            MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                QR_SIZE,
                QR_SIZE,
                mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L)
            )
        } catch (e: IllegalArgumentException) {
            // Unsupported format
            return null
        }
        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * Creates a bitmap that contains the information embedded in the qr code
     * This method was copied from the first iteration of the application
     */
    private fun generateBitmapForDescription(
        partNo: String,
        description: String,
        quantity: String,
        firstPrint: String,
        kod: Char,
        clientCode: String?
    ): Bitmap? {
        fun staticLayout(text: CharSequence, paint: TextPaint) = StaticLayout(
            // Replacing spaces with NO-BREAK SPACE so they wrap at the middle of the word as well
            text.toString().replace(" ", Typography.nbsp.toString()),
            paint,
            500,
            Layout.Alignment.ALIGN_NORMAL,
            1f,
            0f,
            false
        )

        val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val dp = 3f
        val mainPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 16 * dp
            color = Color.BLACK
        }
        val secondaryPaint = TextPaint().apply {
            set(mainPaint)
            textSize = 11 * dp
        }
        val currentDate = DATE_FORMAT.format(Date())

        val mainText = StringBuilder().apply {
            append("PN: $partNo")
            if (clientCode != null) {
                appendLine()
                append("CC: $clientCode")
            }
            appendLine()
            append("QTY: $quantity")
        }

        val mainLayout = staticLayout(
            mainText,
            mainPaint
        )
        val infix = "DESC: $description"
        val infixFirst = secondaryPaint.breakText(
            infix.toCharArray(),
            0,
            infix.length,
            500F,
            null
        )
        val infixSecond = if (infixFirst != infix.length)
            secondaryPaint.breakText(
                infix.toCharArray(),
                infixFirst,
                infix.length - infixFirst,
                500F,
                null
            )
        else null

        val secondaryText = StringBuilder().apply {
            append(infix.subSequence(0, infixFirst))
            if (infixSecond != null) {
                appendLine()
                if ((infixFirst + infixSecond) < infix.length) {
                    append(infix.subSequence(infixFirst, infixFirst + infixSecond - 3))
                    append("...")
                } else {
                    append(infix.subSequence(infixFirst, infixFirst + infixSecond))
                }
            }
            if (firstPrint == "") {
                appendLine()
                append("FP: $currentDate $kod")
                appendLine()
                append("RP: ")
            } else {
                appendLine()
                append("FP: $firstPrint $kod")
                appendLine()
                append("RP: $currentDate $kod")
            }
        }

        val secondaryLayout = staticLayout(
            secondaryText,
            secondaryPaint
        )
        val startY = (500 - mainLayout.height - secondaryLayout.height).toFloat() / 2
        canvas.save()
        canvas.translate(0f, startY)
        mainLayout.draw(canvas)
        canvas.translate(0f, mainLayout.height.toFloat())
        secondaryLayout.draw(canvas)
        canvas.restore()

        return bitmap
    }

    /**
     * Combines the input bitmaps and delegates the print request to the PrinterManager object
     */
    private fun requestPrinting(vararg images: PrintRequest) {
        val requestList = images.map {
            if (it.qrCode == null || it.desc == null) return
            val output = Bitmap.createBitmap(2 * QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
            val combined = Canvas(output)
            combined.drawBitmap(it.qrCode, 0f, 0f, null)
            combined.drawBitmap(it.desc, QR_SIZE.toFloat(), 0f, null)
            output to it.quantity
        }
        preferencesManager.printImage(this, *requestList.toTypedArray())
    }

// endregion

// region QR Scan with permission handling

    @NeedsPermission(Manifest.permission.CAMERA)
    fun scanQrCode() {
        val integrator = IntentIntegrator.forSupportFragment(this)

        integrator.setCameraId(
            if (preferencesManager.cameraBack)
                Camera.CameraInfo.CAMERA_FACING_BACK
            else
                Camera.CameraInfo.CAMERA_FACING_FRONT
        )
        integrator.setOrientationLocked(true)
        integrator.setPrompt("QR kód beolvasása")
        integrator.setBeepEnabled(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)

        integrator.initiateScan()
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onScanDenied() {
        Snackbar.make(requireView(), "Nincs engedély a kamera használatához", Snackbar.LENGTH_LONG)
            .show()
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForScan(request: PermissionRequest) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Figyelem!")
            .setMessage("A QR kód beolvasásához szükség van a kamera használatához.")
            .setCancelable(false)
            .setPositiveButton("Folytatás") { _, _ -> request.proceed() }
            .setNegativeButton("Vissza") { _, _ -> request.cancel() }
            .create()
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    /**
     * Called when a QR code is scanned with the camera
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Snackbar.make(requireView(), "Olvasás megszakítva", Snackbar.LENGTH_LONG).show()
            } else {
                val rows = result.contents.split("\n")
                val firstPrint: List<String>
                val kkod: String
                val date: String
                if (rows.size > 5) {
                    firstPrint = rows.last().split(" ")
                    kkod = firstPrint.subList(1, firstPrint.size).joinToString(" ")
                    date = firstPrint.first()
                } else {
                    Snackbar.make(
                        requireView(),
                        "A QR kód tartalma nem megfelelő",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return
                }
                if (args.selection != null && (rows.first() != args.selection!!.partNumber || kkod != args.selection!!.kkod)) {
                    Log.d("ErrorRead", firstPrint.toString())
                    Log.d("ErrorRead", rows.first() + " " + args.selection!!.partNumber)
                    Log.d("ErrorRead", kkod + " " + args.selection!!.kkod)
                    Snackbar.make(
                        requireView(),
                        "A beolvasott adat nem egyezik meg a kiválasztottal",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return
                }
                scannedItem = QrMetadataItem(
                    partNumber = rows[0],
                    description = rows[1],
                    kkod = kkod,
                    quantity = rows[2].toInt(),
                    document = rows[3],
                    lot = rows[4],
                    firstPrinted = DATE_FORMAT.parse(date),
                    reprinted = null,
                ).also { scannedData ->
                    partNumberText.setText(scannedData.partNumber.toSearchable())
                    kkodText.text = kkod
                    totalQuantityText.text = scannedData.quantity.toString()
                    documentText.text = scannedData.document
                    lotText.text = scannedData.lot
                    if (args.selection != null && scannedData.quantity < args.selection!!.quantity)
                        Snackbar.make(
                            requireView(),
                            "Figyelem: A beolvasott csomagban nincs megfelelő mennyiségű tétel!",
                            Snackbar.LENGTH_LONG
                        ).show()
                }
                Log.d("ScannedItem", scannedItem.toString())

            }
        }
    }

// endregion

// region PrinterResultListener functions

    /**
     * Called when the list of available printers on the network is collected
     */
    override fun onPrinterListReady(list: List<NetPrinter>) {
        progress.visibility = View.INVISIBLE
        val printers: Map<String, NetPrinter> = list.map { it.nodeName to it }.toMap()
        val keys = printers.keys.toList()
        val view = layoutInflater.inflate(R.layout.dialog_printer_selector, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Elérhető nyomtatók")
            .setMessage(if (printers.isEmpty()) "Nem található nyomtató" else "")
            .setView(view)
            .setNegativeButton("Mégse") { dialog, _ -> dialog?.cancel() }
            .create()

        view.findViewById<ListView>(R.id.printerList).let {
            it.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                keys
            )
            it.setOnItemClickListener { _, _, position, _ ->
                dialog.cancel()
                preferencesManager.macAddr = printers.getValue(keys[position]).macAddress
            }
        }
        dialog.show()
    }

    /**
     * Called when the QR printing has a result
     */
    override fun onPrintResult(result: PrinterResult) {
        Snackbar.make(
            requireView(),
            when (result) {
                Success -> "Nyomtatás sikeres"
                is Error -> "${result.errorCode.ordinal}: ${result.errorCode}"
            },
            Snackbar.LENGTH_LONG
        ).show()
    }

    // endregion

    private data class PrintRequest(
        val qrCode: Bitmap?,
        val desc: Bitmap?,
        val quantity: Int,
    )
}

private fun String.toSearchable(): String = filter { "[-/]".toRegex().matches("$it").not() }