package com.farm.seeker.game

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewAssetLoader
import com.farm.seeker.MainActivity
import com.farm.seeker.R
import com.farm.seeker.jsbridge.WebAppInterface
import com.farm.seeker.solana.SolanaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FarmFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var pendingFriendId: String? = null
    private var pendingFriendName: String = "Friend"
    private var pendingFriendLevel: Int = 10
    private var pendingFriendAvatarIndex: Int = 1
    private var isOwnerMode: Boolean = true
    private var isPageLoaded: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_farm, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webview)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)

        setupWebView()
        setupSwipeRefresh()

        // Add JS Interface
        // Note: Using requireActivity() as context if needed, or just requireContext()
        val solanaManager = (activity as? MainActivity)?.solanaManager
        val farmManager = (activity as? MainActivity)?.farmManager
        val taskManager = solanaManager?.let { TaskManager(it) }
        val shopManager = solanaManager?.let { ShopManager(it) }
        val inventoryManager = solanaManager?.let { InventoryManager(it) }

        webView.addJavascriptInterface(WebAppInterface(
            context = requireContext(),
            solanaManager = solanaManager,
            farmManager = farmManager,
            taskManager = taskManager,
            shopManager = shopManager,
            inventoryManager = inventoryManager,
            onSwipeRefreshEnabled = { enabled ->
                if (::swipeRefresh.isInitialized) {
                    swipeRefresh.isEnabled = enabled && isOwnerMode
                }
            },
            onReturnHome = {
                isOwnerMode = true
                if (::swipeRefresh.isInitialized) {
                    swipeRefresh.isEnabled = true
                }
            }
        ), "seeker")

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Load the game URL
        webView.setBackgroundColor(0x00000000) // Transparent to avoid black flash
        webView.loadUrl("https://appassets.androidplatform.net/assets/game.html")
    }
    
    override fun onResume() {
        super.onResume()
        if (isPageLoaded) {
            updateLoginState()
        }
    }
    
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isPageLoaded) {
            updateLoginState()
        }
    }
    
    fun updateLoginState() {
        if (!::webView.isInitialized) return
        
        activity?.runOnUiThread {
            webView.evaluateJavascript("javascript:fetchUserData()", null)
        }
    }

    fun visitFarm(friendId: String, friendName: String = "Friend", friendLevel: Int = 10, friendAvatarIndex: Int = 1) {
        isOwnerMode = false
        if (::swipeRefresh.isInitialized) {
            swipeRefresh.isEnabled = false
        }
        
        // Generate Avatar URL
        val friendAvatarUrl = getAvatarUrl(friendAvatarIndex)

        if (::webView.isInitialized) {
            activity?.runOnUiThread {
                webView.evaluateJavascript("javascript:visitFriend('$friendId', '$friendName', $friendLevel, '$friendAvatarUrl')", null)
            }
        } else {
            pendingFriendId = friendId
            pendingFriendName = friendName
            pendingFriendLevel = friendLevel
            pendingFriendAvatarIndex = friendAvatarIndex
        }
    }
    
    private fun getAvatarUrl(index: Int): String {
        return when {
            index in 1..9 -> "https://appassets.androidplatform.net/res/drawable/avatar_m_$index.png"
            index in 11..19 -> "https://appassets.androidplatform.net/res/drawable/avatar_f_${index - 10}.png"
            else -> "https://appassets.androidplatform.net/res/drawable/avatar_m_1.png"
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            if (!isOwnerMode) {
                swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            val solanaManager = (activity as? MainActivity)?.solanaManager
            if (solanaManager != null && solanaManager.isLoggedIn()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = solanaManager.updateUserProfile()
                    withContext(Dispatchers.Main) {
                        swipeRefresh.isRefreshing = false
                        if (result == "Success") {
                            webView.evaluateJavascript("javascript:fetchUserData()", null)
                        }
                    }
                }
            } else {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        
        // Initialize WebView component and enable JavaScript
        settings.javaScriptEnabled = true
        
        // Set DOM storage support
        settings.domStorageEnabled = true
        
        // Configure cache strategy
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Enable zooming
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false // Hide the ugly controls

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireContext()))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request!!.url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                return false // Allow WebView to load the URL
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Mark page as loaded
                isPageLoaded = true
                
                // Inject initial login state
                updateLoginState()

                // Handle pending friend visit
                pendingFriendId?.let {
                    visitFarm(it, pendingFriendName, pendingFriendLevel, pendingFriendAvatarIndex)
                    pendingFriendId = null
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                Log.d("GameJS", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
        }
    }
}