package com.example.researchcenter.shared.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.researchcenter.R
import com.google.android.material.button.MaterialButton

class ConfirmDialogFragment(
    private val title: String,
    private val message: String,
    private val confirmText: String = "Confirm",
    private val cancelText: String = "Cancel",
    private val isDestructive: Boolean = true,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit = {}
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvMessage).text = message

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirm)

        btnCancel.text = cancelText
        btnConfirm.text = confirmText

        if (!isDestructive) {
            btnConfirm.backgroundTintList = resources.getColorStateList(R.color.primary_green, null)
        }

        btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }

        btnConfirm.setOnClickListener {
            onConfirm()
            dismiss()
        }
    }
}
