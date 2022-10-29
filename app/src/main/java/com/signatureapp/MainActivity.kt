package com.signatureapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private lateinit var paint: DrawView
    private val multiplePermissionId = 1
    private val permissionsReqList: ArrayList<String> = arrayListOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val stokeList = arrayOf( 5, 10, 15, 20,30,35)
    private lateinit var sizeDialog:Dialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        paint = findViewById(R.id.draw_view)
        sizeDialog = Dialog(this)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val undoBtn = findViewById<Button>(R.id.undoBtn)

        val clearBtn = findViewById<Button>(R.id.clearBtn)

        val vto = paint.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                paint.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = paint.measuredWidth
                val height = paint.measuredHeight
                paint.init(height, width)
            }
        })

        undoBtn.setOnClickListener {
            if (paint.isTouch()) {
                paint.undo()
            }else{
                Toast.makeText(this@MainActivity, "Signature is required!!", Toast.LENGTH_LONG).show()
            }
        }
        clearBtn.setOnClickListener {
            if (paint.isTouch()) {
                paint.clear()
            }else{
                Toast.makeText(this@MainActivity, "Signature is required!!", Toast.LENGTH_LONG).show()

            }
        }
        saveBtn.setOnClickListener {
            if (paint.isTouch()) {
                if (checkMultipleRequestPermissions()) {
                    paint.save()?.let { it1 -> storeBitmap(it1) }
                }
            }else{
                Toast.makeText(this@MainActivity, "Signature is required!!", Toast.LENGTH_LONG).show()
            }
        }

        sizeDialog.setContentView(R.layout.stroke_dialog)
        sizeDialog.window!!.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        sizeDialog.setCancelable(false)
        val fanSlider = sizeDialog.findViewById<Slider>(R.id.fanSlider)
        val saveDialogBtn = sizeDialog.findViewById<Button>(R.id.save_btn)
        val cancelDialogBtn = sizeDialog.findViewById<Button>(R.id.cancel_btn)
        fanSlider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {

                }

                override fun onStopTrackingTouch(slider: Slider) {
                    paint.setStrokeWidth(stokeList[slider.value.roundToInt()])
                }
            }
        )
        saveDialogBtn.setOnClickListener {
            paint.setStrokeWidth(stokeList[fanSlider.value.roundToInt()])
            sizeDialog.dismiss()
        }
        cancelDialogBtn.setOnClickListener {
            paint.setStrokeWidth(stokeList[fanSlider.value.roundToInt()])
            sizeDialog.dismiss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.penColor -> {
                val color = ColorPickerDialog.Builder(this@MainActivity)
                    .setTitle("Color")
                    .setPreferenceName("MyColorPicker")
                    .setCancelable(false)
                    .setPositiveButton("Ok",
                        ColorEnvelopeListener { envelope, _ ->
                            paint.setColor(envelope.color)
                        })
                    .setNegativeButton(
                        getString(R.string.cancel)
                    ) { dialogInterface, _ -> dialogInterface.dismiss() }
                    .attachAlphaSlideBar(true) // the default value is true.
                    .attachBrightnessSlideBar(true) // the default value is true.
                    .setBottomSpace(12) // set a bottom space between the last sidebar and buttons.
                val colorPickerView: ColorPickerView = color.colorPickerView
                val bubbleFlag = BubbleFlag(this@MainActivity)
                bubbleFlag.flagMode = FlagMode.ALWAYS
                colorPickerView.flagView = bubbleFlag
                colorPickerView.setColorListener(ColorEnvelopeListener { _, _ -> })
                color.show()
                true
            }
            R.id.penSize -> {
                sizeDialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun storeBitmap(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this@MainActivity, "Saved to Photos", Toast.LENGTH_LONG).show()
            it.close()
        }

    }

    private fun checkMultipleRequestPermissions(): Boolean {
        val listPermissionsNeeded: MutableList<String> = ArrayList()

        for (p in permissionsReqList) {
            val result = ContextCompat.checkSelfPermission(this, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    paint.save()?.let { it1 -> storeBitmap(it1) }
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        settingActivityOpen(this)
                    } else {
                        showDialogOK(this) { _: DialogInterface?, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> checkMultipleRequestPermissions()
                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun settingActivityOpen(activity: Activity) {
        Toast.makeText(
            activity,
            "Go to settings and enable permissions",
            Toast.LENGTH_LONG
        )
            .show()
        val i = Intent()
        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        i.addCategory(Intent.CATEGORY_DEFAULT)
        val packageName = activity.packageName
        i.data = Uri.parse("package:$packageName")
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        activity.startActivity(i)
    }

    private fun showDialogOK(activity: Activity, okListener: DialogInterface.OnClickListener) {
        MaterialAlertDialogBuilder(activity)
            .setMessage("All Permissions are required for this app")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

}