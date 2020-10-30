package social.tsu.android.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_simple_webview.*
import social.tsu.android.R
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack

class SimpleWebViewFragment : Fragment() {
    private lateinit var webView: WebView

    val args by navArgs<SimpleWebViewFragmentArgs>()
    private var uploadMessage: ValueCallback<Array<Uri?>?>? = null
    val REQUEST_SELECT_FILE = 120

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_simple_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireActivity().isInternetAvailable().not())
            requireActivity().internetSnack()

        webView = view.findViewById<WebView>(R.id.webview)

        val settings = webView.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
        }
        pbWebView.show()
        viewProgress.show()
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress > 70)
                    viewProgress.hide()
                if (newProgress == 100)
                    pbWebView.hide()
            }


            override fun onShowFileChooser(
                mWebView: WebView?,
                filePathCallback: ValueCallback<Array<Uri?>?>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (uploadMessage != null) {
                    uploadMessage?.onReceiveValue(null)
                    uploadMessage = null
                }
                uploadMessage = filePathCallback
                val intent: Intent? = fileChooserParams.createIntent()

                try {
                    startActivityForResult(intent, REQUEST_SELECT_FILE)
                } catch (e: ActivityNotFoundException) {
                    uploadMessage = null
                    snack("Cannot Open File Chooser")
                    return false
                }
                return true
            }
        }

        val url = args.url

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(url)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onStop() {
        dismissKeyboard()
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_FILE) {
            if (uploadMessage == null) {
                return
            }
            uploadMessage?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                )
            )
            uploadMessage = null

        }
    }
}
