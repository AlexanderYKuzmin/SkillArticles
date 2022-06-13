package ru.skillbranch.skillarticles.ui

import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toolbar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.get
import androidx.core.view.marginEnd
import androidx.core.view.marginLeft
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import kotlinx.android.synthetic.main.layout_search.*
import kotlinx.android.synthetic.main.layout_search.view.*
import kotlinx.android.synthetic.main.layout_submenu.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.Notify
import ru.skillbranch.skillarticles.viewmodels.ViewModelFactory

class RootActivity : AppCompatActivity() {
    private lateinit var viewModel: ArticleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
        setupToolbar()
        setupBottombar()
        setupSubmenu()
        setupSearch()

        val vmFactory = ViewModelFactory("0")
        viewModel = ViewModelProvider(this, vmFactory)[ArticleViewModel::class.java]
        viewModel.observeState(this){
            renderUi(it)
        }

        viewModel.observeNotification(this) {
            renderNotification(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu[0].isChecked = viewModel.currentState.isSearch

        menu[0].icon = getDrawable(getIconMenuItem(menu[0].isChecked))

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                item.isChecked = !item.isChecked
                item.icon = getDrawable(getIconMenuItem(item.isChecked))
                viewModel.handleSearchMode(item.isChecked)
            }
        }
        return true
    }

    private fun getIconMenuItem(isChecked: Boolean): Int {
        return if (isChecked) R.drawable.ic_baseline_search_24
        else R.drawable.ic_search_black_24dp
    }

    private fun renderNotification(notify: Notify) {
        val snackbar = Snackbar.make(coordinator_container, notify.message, Snackbar.LENGTH_LONG)
            .setAnchorView(bottombar)
            .setTextColor(getColor(R.color.color_accent_dark))

        when(notify) {
            is Notify.TextMessage ->  {}

            is Notify.ActionMessage -> {
                snackbar.setAction(notify.actionLabel) {
                    notify.actionHandler.invoke()
                }
            }

            is Notify.ErrorMessage -> {
                with(snackbar) {
                    setBackgroundTint(getColor(R.color.design_default_color_error))
                    setTextColor(getColor(android.R.color.white))
                    setActionTextColor(getColor(android.R.color.white))
                    setAction(notify.errLabel) {
                        notify.errHandler?.invoke()
                    }
                }
            }
        }

        snackbar.show()
    }

    private fun renderUi(data: ArticleState) {
        btn_settings.isChecked = data.isShowMenu
        if (data.isShowMenu) submenu.open() else submenu.close()

        btn_like.isChecked = data.isLike
        btn_bookmark.isChecked = data.isBookmark

        switch_mode.isChecked = data.isDarkMode
        delegate.localNightMode =
            if (data.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

        if (data.isBigText) {
            tv_text_content.textSize = 18f
            btn_text_up.isChecked = true
            btn_text_down.isChecked = false
        } else {
            tv_text_content.textSize = 14f
            btn_text_up.isChecked = false
            btn_text_down.isChecked = true
        }

        tv_text_content.text =
            if (data.isLoadingContent) "loading" else data.content.first() as String

        toolbar.title = data.title ?: "Skill Articles"
        toolbar.subtitle = data.category ?: "loading..."
        if (data.categoryIcon != null) toolbar.logo = getDrawable(data.categoryIcon as Int)

        Log.d("Search_Panel", "Before is open. data.isSearch = ${data.isSearch}")
        if (data.isSearch) {
            Log.d("Search_Panel", "search is open. data.isSearch = ${data.isSearch}")
            search_panel.open()
            et_search.setText(data.searchQuery)
        } else search_panel.close()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = if (toolbar.childCount > 2) toolbar.getChildAt(2) as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
        //logo?.background = getDrawable(R.drawable.ic_launcher_background)
        /*val lp = logo?.layoutParams as? Toolbar.LayoutParams
        lp?.let{
            it.width = dpToIntPx(40)
            it.height = dpToIntPx(40)
            it.marginEnd = dpToIntPx(16)
            logo.layoutParams = it
            println("Layout Params of logotype ${logo.layoutParams.width} :: ${logo.layoutParams.height}")
        }*/

        logo?.layoutParams?.width = dpToIntPx(40)
        logo?.layoutParams?.height = dpToIntPx(40)

        val title = toolbar.getChildAt(1) as TextView
        val subtitle = toolbar.getChildAt(0) as TextView

        title.setPadding(dpToIntPx(16), 0, 0,0)
        subtitle.setPadding(dpToIntPx(16), 0, 0,0)
    }

    private fun setupSubmenu() {
        btn_text_up.setOnClickListener { viewModel.handleUpText() }
        btn_text_down.setOnClickListener { viewModel.handleDownText() }
        switch_mode.setOnClickListener { viewModel.handleNightMode() }
    }

    private fun setupBottombar() {
        btn_like.setOnClickListener { viewModel.handleLike() }
        btn_bookmark.setOnClickListener { viewModel.handleBookmark() }
        btn_share.setOnClickListener { viewModel.handleShare() }
        btn_settings.setOnClickListener { viewModel.handleToggleMenu() }
    }

    private fun setupSearch() {
        et_search.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                viewModel.handleSearch(et_search.text.toString())
            }
        }
    }
}