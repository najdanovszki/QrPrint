package com.webtic.qrprint.ui.components

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.webtic.qrprint.R
import com.webtic.qrprint.models.QuantityDetails

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class QuantityInputForm @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0, defStyleRes: Int = 0) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    private lateinit var btnPlusSZ: ImageButton
    private lateinit var btnMinusSZ: ImageButton
    private lateinit var editSZ: EditText
    private lateinit var btnPlusMZ: ImageButton
    private lateinit var btnMinusMZ: ImageButton
    private lateinit var editMZ: EditText
    private lateinit var btnPlusLZ: ImageButton
    private lateinit var btnMinusLZ: ImageButton
    private lateinit var editLZ: EditText
    private lateinit var btnPlusSD: ImageButton
    private lateinit var btnMinusSD: ImageButton
    private lateinit var editSD: EditText
    private lateinit var btnPlusMD: ImageButton
    private lateinit var btnMinusMD: ImageButton
    private lateinit var editMD: EditText
    private lateinit var btnPlusLD: ImageButton
    private lateinit var btnMinusLD: ImageButton
    private lateinit var editLD: EditText
    private lateinit var btnPlusSR: ImageButton
    private lateinit var btnMinusSR: ImageButton
    private lateinit var editSR: EditText
    private lateinit var btnPlusMR: ImageButton
    private lateinit var btnMinusMR: ImageButton
    private lateinit var editMR: EditText
    private lateinit var btnPlusLR: ImageButton
    private lateinit var btnMinusLR: ImageButton
    private lateinit var editLR: EditText
    private lateinit var btnPlusSP: ImageButton
    private lateinit var btnMinusSP: ImageButton
    private lateinit var editSP: EditText
    private lateinit var btnPlusMP: ImageButton
    private lateinit var btnMinusMP: ImageButton
    private lateinit var editMP: EditText
    private lateinit var btnPlusLP: ImageButton
    private lateinit var btnMinusLP: ImageButton
    private lateinit var editLP: EditText
    private lateinit var btnSave: Button
    private var quantityDetails: QuantityDetails = QuantityDetails()
    private var saveButtonClickListener: ((Int) -> Unit)? = null
    init {
        val inflater = LayoutInflater.from(context).inflate(R.layout.layout_quantity_input_table, this, true)
        btnPlusSZ = inflater.findViewById<ImageButton>(R.id.btnPlusSZ)
        btnMinusSZ = inflater.findViewById<ImageButton>(R.id.btnMinusSZ)
        editSZ = inflater.findViewById<EditText>(R.id.editSZ)
        btnPlusMZ = inflater.findViewById<ImageButton>(R.id.btnPlusMZ)
        btnMinusMZ = inflater.findViewById<ImageButton>(R.id.btnMinusMZ)
        editMZ = inflater.findViewById<EditText>(R.id.editMZ)
        btnPlusLZ = inflater.findViewById<ImageButton>(R.id.btnPlusLZ)
        btnMinusLZ = inflater.findViewById<ImageButton>(R.id.btnMinusLZ)
        editLZ = inflater.findViewById<EditText>(R.id.editLZ)
        btnPlusSD = inflater.findViewById<ImageButton>(R.id.btnPlusSD)
        btnMinusSD = inflater.findViewById<ImageButton>(R.id.btnMinusSD)
        editSD = inflater.findViewById<EditText>(R.id.editSD)
        btnPlusMD = inflater.findViewById<ImageButton>(R.id.btnPlusMD)
        btnMinusMD = inflater.findViewById<ImageButton>(R.id.btnMinusMD)
        editMD = inflater.findViewById<EditText>(R.id.editMD)
        btnPlusLD = inflater.findViewById<ImageButton>(R.id.btnPlusLD)
        btnMinusLD = inflater.findViewById<ImageButton>(R.id.btnMinusLD)
        editLD = inflater.findViewById<EditText>(R.id.editLD)
        btnPlusSR = inflater.findViewById<ImageButton>(R.id.btnPlusSR)
        btnMinusSR = inflater.findViewById<ImageButton>(R.id.btnMinusSR)
        editSR = inflater.findViewById<EditText>(R.id.editSR)
        btnPlusMR = inflater.findViewById<ImageButton>(R.id.btnPlusMR)
        btnMinusMR = inflater.findViewById<ImageButton>(R.id.btnMinusMR)
        editMR = inflater.findViewById<EditText>(R.id.editMR)
        btnPlusLR = inflater.findViewById<ImageButton>(R.id.btnPlusLR)
        btnMinusLR = inflater.findViewById<ImageButton>(R.id.btnMinusLR)
        editLR = inflater.findViewById<EditText>(R.id.editLR)
        btnPlusSP = inflater.findViewById<ImageButton>(R.id.btnPlusSP)
        btnMinusSP = inflater.findViewById<ImageButton>(R.id.btnMinusSP)
        editSP = inflater.findViewById<EditText>(R.id.editSP)
        btnPlusMP = inflater.findViewById<ImageButton>(R.id.btnPlusMP)
        btnMinusMP = inflater.findViewById<ImageButton>(R.id.btnMinusMP)
        editMP = inflater.findViewById<EditText>(R.id.editMP)
        btnPlusLP = inflater.findViewById<ImageButton>(R.id.btnPlusLP)
        btnMinusLP = inflater.findViewById<ImageButton>(R.id.btnMinusLP)
        editLP = inflater.findViewById<EditText>(R.id.editLP)

        btnSave = inflater.findViewById<Button>(R.id.btnSave)
        btnPlusSZ.setOnClickListener{
            var oldValue = if(editSZ.text.isNotBlank()){editSZ.text.toString().toInt()} else{0}
            editSZ.setText("${oldValue + 1}")
        }
        btnMinusSZ.setOnClickListener{
            var oldValue = if(editSZ.text.isNotBlank()){editSZ.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editSZ.setText("${oldValue - 1}")
        }
        btnPlusMZ.setOnClickListener{
            var oldValue = if(editMZ.text.isNotBlank()){editMZ.text.toString().toInt()} else{0}
            editMZ.setText("${oldValue + 1}")
        }
        btnMinusMZ.setOnClickListener{
            var oldValue = if(editMZ.text.isNotBlank()){editMZ.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editMZ.setText("${oldValue - 1}")
        }
        btnPlusLZ.setOnClickListener{
            var oldValue = if(editLZ.text.isNotBlank()){editLZ.text.toString().toInt()} else{0}
            editLZ.setText("${oldValue + 1}")
        }
        btnMinusLZ.setOnClickListener{
            var oldValue = if(editLZ.text.isNotBlank()){editLZ.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editLZ.setText("${oldValue - 1}")
        }
        btnPlusSD.setOnClickListener{
            var oldValue = if(editSD.text.isNotBlank()){editSD.text.toString().toInt()} else{0}
            editSD.setText("${oldValue + 1}")
        }
        btnMinusSD.setOnClickListener{
            var oldValue = if(editSD.text.isNotBlank()){editSD.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editSD.setText("${oldValue - 1}")
        }
        btnPlusMD.setOnClickListener{
            var oldValue = if(editMD.text.isNotBlank()){editMD.text.toString().toInt()} else{0}
            editMD.setText("${oldValue + 1}")
        }
        btnMinusMD.setOnClickListener{
            var oldValue = if(editMD.text.isNotBlank()){editMD.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editMD.setText("${oldValue - 1}")
        }
        btnPlusLD.setOnClickListener{
            var oldValue = if(editLD.text.isNotBlank()){editLD.text.toString().toInt()} else{0}
            editLD.setText("${oldValue + 1}")
        }
        btnMinusLD.setOnClickListener{
            var oldValue = if(editLD.text.isNotBlank()){editLD.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editLD.setText("${oldValue - 1}")
        }
        btnPlusSR.setOnClickListener{
            var oldValue = if(editSR.text.isNotBlank()){editSR.text.toString().toInt()} else{0}
            editSR.setText("${oldValue + 1}")
        }
        btnMinusSR.setOnClickListener{
            var oldValue = if(editSR.text.isNotBlank()){editSR.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editSR.setText("${oldValue - 1}")
        }
        btnPlusMR.setOnClickListener{
            var oldValue = if(editMR.text.isNotBlank()){editMR.text.toString().toInt()} else{0}
            editMR.setText("${oldValue + 1}")
        }
        btnMinusMR.setOnClickListener{
            var oldValue = if(editMR.text.isNotBlank()){editMR.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editMR.setText("${oldValue - 1}")
        }
        btnPlusLR.setOnClickListener{
            var oldValue = if(editLR.text.isNotBlank()){editLR.text.toString().toInt()} else{0}
            editLR.setText("${oldValue + 1}")
        }
        btnMinusLR.setOnClickListener{
            var oldValue = if(editLR.text.isNotBlank()){editLR.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editLR.setText("${oldValue - 1}")
        }
        btnPlusSP.setOnClickListener{
            var oldValue = if(editSP.text.isNotBlank()){editSP.text.toString().toInt()} else{0}
            editSP.setText("${oldValue + 1}")
        }
        btnMinusSP.setOnClickListener{
            var oldValue = if(editSP.text.isNotBlank()){editSP.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editSP.setText("${oldValue - 1}")
        }
        btnPlusMP.setOnClickListener{
            var oldValue = if(editMP.text.isNotBlank()){editMP.text.toString().toInt()} else{0}
            editMP.setText("${oldValue + 1}")
        }
        btnMinusMP.setOnClickListener{
            var oldValue = if(editMP.text.isNotBlank()){editMP.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editMP.setText("${oldValue - 1}")
        }
        btnPlusLP.setOnClickListener{
            var oldValue = if(editLP.text.isNotBlank()){editLP.text.toString().toInt()} else{0}
            editLP.setText("${oldValue + 1}")
        }
        btnMinusLP.setOnClickListener{
            var oldValue = if(editLP.text.isNotBlank()){editLP.text.toString().toInt()} else{0}
            if(oldValue > 0)
                editLP.setText("${oldValue - 1}")
        }

        btnSave.setOnClickListener {
            saveButtonClickListener?.invoke(1)
        }

        editSZ.doAfterTextChanged {
            quantityDetails.SZ = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editMZ.doAfterTextChanged {
            quantityDetails.MZ = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editLZ.doAfterTextChanged {
            quantityDetails.LZ = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editSD.doAfterTextChanged {
            quantityDetails.SD = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editMD.doAfterTextChanged {
            quantityDetails.MD = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editLD.doAfterTextChanged {
            quantityDetails.LD = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editSR.doAfterTextChanged {
            quantityDetails.SR = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editMR.doAfterTextChanged {
            quantityDetails.MR = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editLR.doAfterTextChanged {
            quantityDetails.LR = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editSP.doAfterTextChanged {
            quantityDetails.SP = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editMP.doAfterTextChanged {
            quantityDetails.MP = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }
        editLP.doAfterTextChanged {
            quantityDetails.LP = if(it?.isEmpty() == true){ 0} else{it.toString().toInt()}
        }


    }

    fun setSaveButtonClickListener(listener: (Int) -> Unit){
        saveButtonClickListener = listener
    }

    fun setQuantityDetails(quantityDetails: QuantityDetails) {
        this.quantityDetails = quantityDetails
        editSZ.setText("${quantityDetails.SZ}")
        editMZ.setText("${quantityDetails.MZ}")
        editLZ.setText("${quantityDetails.LZ}")
        editSD.setText("${quantityDetails.SD}")
        editMD.setText("${quantityDetails.MD}")
        editLD.setText("${quantityDetails.LD}")
        editSR.setText("${quantityDetails.SR}")
        editMR.setText("${quantityDetails.MR}")
        editLR.setText("${quantityDetails.LR}")
        editSP.setText("${quantityDetails.SP}")
        editMP.setText("${quantityDetails.MP}")
        editLP.setText("${quantityDetails.LP}")
    }

    fun getQuantityDetails(): QuantityDetails {
        return this.quantityDetails
    }

}