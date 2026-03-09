package com.farm.seeker

import android.os.Bundle
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.farm.seeker.game.FarmFragment
import com.farm.seeker.game.FarmManager
import com.farm.seeker.friend.FriendFragment
import com.farm.seeker.friend.FriendManager
import com.farm.seeker.me.MeFragment
import com.farm.seeker.me.MeManager
import com.farm.seeker.solana.SolanaManager
import com.farm.seeker.network.ApiClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var activeFragment: Fragment? = null
    lateinit var solanaManager: SolanaManager
    lateinit var friendManager: FriendManager
    lateinit var meManager: MeManager
    lateinit var farmManager: FarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Init ApiClient
        ApiClient.init(this)
        
        solanaManager = SolanaManager(this)
        friendManager = FriendManager(this, solanaManager)
        meManager = MeManager(this, solanaManager)
        farmManager = FarmManager(this, solanaManager)
        
        // Enable edge-to-edge (Immersive Status Bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.sky_blue)
        
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val fragmentContainer = findViewById<android.view.View>(R.id.fragment_container)
        
        // Apply window insets to BottomNavigationView to avoid overlap with system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Add padding to bottom, but keep the height wrap_content + padding
            // Or update layout params to increase height
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        // Apply window insets to FragmentContainer to avoid overlap with status bar
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        if (savedInstanceState == null) {
            val farmFragment = FarmFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, farmFragment, "farm")
                .commit()
            activeFragment = farmFragment
        } else {
            // Restore active fragment reference from the one that is currently visible
            activeFragment = supportFragmentManager.fragments.firstOrNull { it.isVisible }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_farm -> {
                    switchFragment("farm")
                    true
                }
                R.id.navigation_friend -> {
                    switchFragment("friend")
                    true
                }
                R.id.navigation_me -> {
                    switchFragment("me")
                    true
                }
                else -> false
            }
        }
        
        // Auto update user profile on app launch
        lifecycleScope.launch(Dispatchers.IO) {
            if (solanaManager.isLoggedIn()) {
                val result = solanaManager.updateUserProfile()
                println("Auto update user profile: $result")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::solanaManager.isInitialized && solanaManager.isLoggedIn()) {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    solanaManager.fetchUserProfile()
                }
                if (result == "Success") {
                    // Update Active Fragment
                    val current = activeFragment
                    if (current is FarmFragment) {
                        current.updateLoginState()
                    } else if (current is MeFragment) {
                        current.refreshData()
                    }
                }
            }
        }
    }

    private fun switchFragment(tag: String): Fragment {
        val transaction = supportFragmentManager.beginTransaction()
        
        var targetFragment = supportFragmentManager.findFragmentByTag(tag)
        
        // If fragment doesn't exist, create and add it
        if (targetFragment == null) {
            targetFragment = when (tag) {
                "farm" -> FarmFragment()
                "friend" -> FriendFragment()
                "me" -> MeFragment()
                else -> throw IllegalArgumentException("Unknown tag")
            }
            transaction.add(R.id.fragment_container, targetFragment, tag)
        }
        
        // If target is already active and visible, do nothing
        if (targetFragment == activeFragment && targetFragment.isVisible) {
            return targetFragment
        }

        // Hide the currently active fragment
        activeFragment?.let {
            if (it != targetFragment && !it.isHidden) {
                transaction.hide(it)
            }
        }

        // Show the target fragment
        if (targetFragment.isHidden) {
            transaction.show(targetFragment)
        }

        activeFragment = targetFragment
        transaction.commit()
        
        return targetFragment
    }

    fun navigateToFarm(friendId: String, friendName: String, friendLevel: Int, friendAvatarIndex: Int) {
        // 1. Switch to Farm Fragment
        val farmFragment = switchFragment("farm") as? FarmFragment
        
        // 2. Update Bottom Navigation Selection
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_farm
        
        // 3. Call visitFarm on the fragment
        farmFragment?.visitFarm(friendId, friendName, friendLevel, friendAvatarIndex)
    }
}