package com.learning.kidsdrawingapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Log.v
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.createDeviceProtectedStorageContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.get
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
import java.util.jar.Manifest
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {
    lateinit var dv: DrawingView
    lateinit var brushselector: ImageButton
    private lateinit var ll_colors_picker: LinearLayout
    lateinit var ib_color_pickercurrent: ImageButton
    lateinit var ib_gallery: ImageButton
    lateinit var iv_background: ImageView
    lateinit var ib_undo: ImageButton
    lateinit var ib_redo: ImageButton
    lateinit var fl_container : FrameLayout
    lateinit var ib_save: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dv = findViewById<DrawingView>(R.id.Drawing_view)
        dv.setBrushSize(10f)
        ll_colors_picker = findViewById(R.id.ll_color_pickers)
        ib_color_pickercurrent = ll_colors_picker[7] as ImageButton
        ib_color_pickercurrent.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        brushselector = findViewById(R.id.ib_brush_size)
        brushselector.setOnClickListener {
            selectBrushSizeDialog()
        }
        ib_gallery = findViewById(R.id.ib_gallery_accessor)
        iv_background = findViewById(R.id.ib_background_image)
        ib_gallery.setOnClickListener{
            if(isReadStorageGranted()){
                // will add image from gallery.
                val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickImageIntent, GALLERY_RETRIEVE)
            }else{
                requestStoragePermissions()
            }
        }
        ib_undo = findViewById(R.id.ib_undo)
        ib_redo = findViewById(R.id.ib_redo)
        ib_undo.setOnClickListener{
            dv.undoOnClick()
        }
        ib_undo.setOnLongClickListener {
            dv.undoAllonClick()
        }
        ib_redo.setOnClickListener{
            dv.reduOnClick()
        }
        ib_save = findViewById(R.id.ib_save)
        fl_container = findViewById(R.id.fl_Drawing_view_container)
        ib_save.setOnClickListener{
            if(isReadStorageGranted()){
                BitmapAsyncTask(getBitmapFromView(fl_container), it).execute()
            }else{
                requestStoragePermissions()
            }
        }

    }
    private fun selectBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_size_dialog)
        brushDialog.setTitle("Select brush size: ")
        brushDialog.show()
        val verysmallBrush = brushDialog.findViewById<ImageButton>(R.id.ib_very_small_brush)
        verysmallBrush.setOnClickListener{
            dv.setBrushSize(5f)
            brushDialog.dismiss()
        }
        val smallBrush = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBrush.setOnClickListener{
            dv.setBrushSize(10f)
            brushDialog.dismiss()
        }
        val mediumBrush = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBrush.setOnClickListener{
            dv.setBrushSize(15f)
            brushDialog.dismiss()
        }
        val largeBrush = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBrush.setOnClickListener{
            dv.setBrushSize(20f)
            brushDialog.dismiss()
        }
    }
    fun pickColor(view: View){
        val ibColorPicker = view as ImageButton
        if( ibColorPicker != ib_color_pickercurrent){
            val tColor = ibColorPicker.tag.toString()
            dv.setColor(tColor)
            ibColorPicker.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
            ib_color_pickercurrent.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            ib_color_pickercurrent = ibColorPicker

        }
    }

    @SuppressLint("StaticFieldLeak")
    @Suppress("DEPRECATION")
    private inner class BitmapAsyncTask(val mBitmap: Bitmap, val view: View): AsyncTask<Any, Unit, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            try{
                val bytes = ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.PNG, 95, bytes)
                val file = File(externalCacheDir!!.absoluteFile.toString() +File.separator
                + "kidsDrawingApp_" + System.currentTimeMillis()/1000 + ".png")
                val fos = FileOutputStream(file)
                fos.write(bytes.toByteArray())
                fos.close()
                result = file.absolutePath
            }catch (e: Exception){
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(result!!.isNotEmpty()){
                val snb = Snackbar.make(view, "Successfully saved to $result", Snackbar.LENGTH_SHORT )
                snb.setAction("Share" , onShareClick(this@MainActivity, result))
                snb.show()
            }else{
                val snb = Snackbar.make(view, "Failed to save", Snackbar.LENGTH_SHORT )
                snb.setBackgroundTint(Color.RED)
                snb.setTextColor(Color.WHITE)
                snb.show()
            }
            stopProgressDialog()

        }
    }
    class onShareClick(val context: Context,val result: String): View.OnClickListener{
        override fun onClick(view: View){
            MediaScannerConnection.scanFile(
                    context,
                    arrayOf(result),
                    null
            ) { path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"
                startActivity(context,
                        Intent.createChooser(
                                shareIntent, "share"
                        ),null
                )
            }
        }
    }


    private lateinit var customDialog : Dialog
    fun showProgressDialog(){
        customDialog = Dialog(this)
        customDialog.setContentView(R.layout.progress_bar)
        customDialog.setCancelable(false)
        customDialog.show()
    }
    fun stopProgressDialog(){
        customDialog.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY_RETRIEVE){
                try{
                    if(data?.data != null){
                       iv_background.setImageURI(data.data)
                        Log.e("image path", data.data.toString())
                    }else{
                        Toast.makeText(this, "some thing went wrong, try again.", Toast.LENGTH_SHORT).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

    }

    fun requestStoragePermissions(){
        val readpm = android.Manifest.permission.READ_EXTERNAL_STORAGE
        val writepm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, arrayOf(readpm,
                writepm).toString())){
            Toast.makeText(this, "Need storage permission to add background image", Toast.LENGTH_LONG).show()

        }else{
            ActivityCompat.requestPermissions(this, arrayOf(readpm, writepm), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Now you can add background imgage.", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Opps you denied the permission.", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun getBitmapFromView(view: View): Bitmap{
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = view.background
        if(background != null){
            background.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap
    }
    fun isReadStorageGranted(): Boolean {
        val result= ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }
    companion object{
        const val STORAGE_PERMISSION_CODE = 1
        const val GALLERY_RETRIEVE = 2
    }
}