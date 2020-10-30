package social.tsu.android.ui.new_post

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.background_color_items.*
import kotlinx.android.synthetic.main.font_items.*
import kotlinx.android.synthetic.main.font_style_items.*
import kotlinx.android.synthetic.main.fragment_text_edit.*
import kotlinx.android.synthetic.main.gradient_items.*
import kotlinx.android.synthetic.main.text_color_items.*
import social.tsu.android.R
import social.tsu.android.network.DownloadHelper
import social.tsu.android.ui.new_post.adapter.ColorAdapter
import social.tsu.android.ui.new_post.adapter.ColorGradientAdapter
import social.tsu.android.ui.new_post.adapter.ColorTextAdapter
import social.tsu.android.ui.new_post.adapter.FontAdapter
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.dismissKeyboard
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class TextMediaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_text_edit, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        post_text_toolbar.inflateMenu(R.menu.post_top_save)
        post_text_toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_save) {
                Dexter.withContext(requireContext())
                    .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ).withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                            if (report.areAllPermissionsGranted()) {
                                dismissKeyboard()
                                canvas_text.isCursorVisible = false
                                val result = saveTextDrawable()
                                canvas_text.isCursorVisible = true
                                result?.let {
                                    DownloadHelper().getImageContentUri(
                                        requireContext(),
                                        result
                                    )?.let { uri ->
                                        proceedNext(uri)
                                    }
                                }
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: List<PermissionRequest?>?,
                            token: PermissionToken
                        ) {
                            token.continuePermissionRequest()
                        }
                    }).onSameThread().check()
            }
            return@setOnMenuItemClickListener true
        }


        post_text_toolbar.setNavigationOnClickListener {
            PostTypesFragment.showToolbar()
            dismissKeyboard()
            findNavController().popBackStack()
        }
        setUpFontStyleClickListeners()

        rvColor.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val adapter = ColorAdapter()
        rvColor.adapter = adapter
        adapter.onItemClick = { color ->
            canvas.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
        }

        rvTextColor.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val textAdapter = ColorTextAdapter()
        rvTextColor.adapter = textAdapter
        textAdapter.onItemClick = { color ->
            canvas_text.setTextColor(ContextCompat.getColor(requireContext(), color))
            canvas_text.setHintTextColor(ContextCompat.getColor(requireContext(), color))
        }

        rvFont.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val adapterFont = FontAdapter()
        rvFont.adapter = adapterFont
        adapterFont.onItemClick = { font ->
            updateFont(font)
        }

        rvGradient.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val gradientAdapter = ColorGradientAdapter()
        rvGradient.adapter = gradientAdapter
        gradientAdapter.onItemClick = { color ->
            canvas.setBackgroundResource(color)
        }

        panelClick.setOnClickListener {
            panelClick.visibility = View.GONE
            canvas_text.requestFocus()
            canvas_text.hint = ""
            val imm: InputMethodManager =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            canvas_text.isCursorVisible = true
        }

        panelTouch.setOnTouchListener { _, motionEvent ->
            dismissKeyboard()
            return@setOnTouchListener true
        }

        canvas_text.movementMethod = null
        canvas_text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                canvas_text?.text?.let {
                    for (span in it.getSpans(0, it.length, UnderlineSpan::class.java)) {
                        it.removeSpan(span)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val fittingLines: Int = canvas_text.height / canvas_text.lineHeight
                if (fittingLines > 0) {
                    canvas_text.maxLines = fittingLines
                }
            }
        })


        seekFont.progress = 40
        seekFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val fittingLines: Int = canvas_text.height / canvas_text.lineHeight
                if (fittingLines > 0) {
                    canvas_text.maxLines = fittingLines
                }
                canvas_text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (p1 + 8).toFloat())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
    }

    private fun setUpFontStyleClickListeners() {

        font_bold.setOnClickListener {
            updateFontStyle(canvas_text.typeface, Typeface.BOLD)
        }

        font_italic.setOnClickListener {
            updateFontStyle(canvas_text.typeface, Typeface.ITALIC)
        }

        font_regular.setOnClickListener {
            updateFontStyle(Typeface.create(canvas_text.typeface, Typeface.NORMAL), Typeface.NORMAL)
        }

    }

    private fun updateFont(@FontRes fontResId: Int) {
        ResourcesCompat.getFont(
            requireContext(), fontResId,
            object : ResourcesCompat.FontCallback() {
                override fun onFontRetrieved(typeface: Typeface) {
                    canvas_text.typeface = typeface
                }

                override fun onFontRetrievalFailed(reason: Int) {
                }
            }, Handler(

            )
        )
    }

    private fun updateFontStyle(typeface: Typeface, style: Int) {
        canvas_text.setTypeface(typeface, style)
    }


    private fun saveTextDrawable(): File? {
        var f: File? = null

        val bitmap = canvas.drawToBitmap()
        val fileName = "IMG_${System.currentTimeMillis()}.png"
        f = try {
            var docsFolder = File(Environment.getExternalStorageDirectory().toString() + "/Text")
            var isPresent = true
            if (!docsFolder.exists()) {
                isPresent = docsFolder.mkdir()
            }
            if (isPresent.not()) {
                docsFolder =
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.absolutePath.toString())
                if (!docsFolder.exists()) {
                    isPresent = docsFolder.mkdir()

                } else
                    isPresent = true
            }
            if (isPresent) {
                val file = File(docsFolder.absolutePath, fileName)
                file.createNewFile()
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)
                val bitmapData = bos.toByteArray()
                val fos = FileOutputStream(file)
                fos.write(bitmapData)
                fos.flush()
                fos.close()
                file
            } else
                null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        return f
    }

    private fun proceedNext(photoUri: Uri) {
        val fragment = requireParentFragment().requireParentFragment()
        if (fragment is PostTypesFragment) {
            fragment.next(photoUri = photoUri)
        }
    }
}