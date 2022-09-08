package ru.skillbranch.skillarticles.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.text.getSpans
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.databinding.ActivityRootBinding
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.hideKeyboard
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownBuilder
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownImageView
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan
import ru.skillbranch.skillarticles.ui.delegates.AttrValue
import ru.skillbranch.skillarticles.ui.delegates.viewBinding
import ru.skillbranch.skillarticles.viewmodels.*

class RootActivity : AppCompatActivity(), IArticleView {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var viewModelFactory: ViewModelProvider.Factory = ViewModelFactory(this, "0")

    private val viewModel: ArticleViewModel by viewModels { ViewModelFactory(this, "0") }
    private val vb: ActivityRootBinding by viewBinding(ActivityRootBinding::inflate)

    //private lateinit var vb: ActivityRootBinding
    private val vbBottombar
        get() = vb.bottombar.binding
    private val vbSubmenu
        get() = vb.submenu.binding

    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //vb = ActivityRootBinding.inflate(layoutInflater)


        //setContentView(vb.root)
        setupToolbar()
        setupBottombar()
        setupSubmenu()
        setCopyListener()
        //setupSearch()

        //vb.scroll.addView(MarkdownImageView(this, 14f, "http://developer.alexanderklimov.ru/android/images/android_cat.jpg", "Iceland", "It is iceland mountain"))

        //val vmFactory = ViewModelFactory("0")
        //viewModel = ViewModelProvider(this, vmFactory)[ArticleViewModel::class.java]
        viewModel.observeState(this, ::renderUi)
        viewModel.observeSubState(this, ArticleState::toBottombarData, ::renderBotombar)
        viewModel.observeSubState(this, ArticleState::toSubmenuData, ::renderSubmenu)

        viewModel.observeNotification(this) {
            renderNotification(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val menuItem = menu.findItem(R.id.action_search)
        searchView = (menuItem.actionView as SearchView)
        searchView.queryHint = getString(R.string.article_search_placeholder)

        //restore SearchView
        if (viewModel.currentState.isSearch) {
            menuItem.expandActionView()
            searchView.setQuery(viewModel.currentState.searchQuery, false)
            searchView.requestFocus()
        } else {
            searchView.clearFocus()
        }

        menuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState, outPersistentState)
    }

    /*override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        viewModel.restoreState()
        super.onRestoreInstanceState(savedInstanceState)
    }*/

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                item.isChecked = !item.isChecked
                item.icon = getDrawable(getIconMenuItem(item.isChecked))
                //viewModel.handleSearchMode(item.isChecked)
            }
        }
        return true
    }*/

    /*private fun getIconMenuItem(isChecked: Boolean): Int {
        return if (isChecked) R.drawable.ic_baseline_search_24
        else R.drawable.ic_search_black_24dp
    }*/

    private fun renderNotification(notify: Notify) {
        val snackbar = Snackbar.make(vb.coordinatorContainer, notify.message, Snackbar.LENGTH_LONG)
            .setAnchorView(vb.bottombar)
            .setTextColor(getColor(R.color.color_accent_dark))

        when (notify) {
            is Notify.TextMessage -> {
            }

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

    override fun renderBotombar(data: BottombarData) {
        with(vbBottombar) {
            btnSettings.isChecked = data.isShowMenu
            btnLike.isChecked = data.isLike
            btnBookmark.isChecked = data.isBookmark
        }
        if (data.isSearch) showSearchbar(data.resultsCount, data.searchPosition)
        else hideSearchbar()
    }

    override fun renderSubmenu(data: SubmenuData) {
        Log.d("RootActivity", "renderSubmenu $data")
        with(vbSubmenu) {
            switchMode.isChecked = data.isDarkMode
            btnTextDown.isChecked = !data.isBigText
            btnTextUp.isChecked = data.isBigText
        }

        if (data.isShowMenu) vb.submenu.open() else vb.submenu.close()
    }

    override fun renderUi(data: ArticleState) {
        delegate.localNightMode =
            if (data.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

        /*with(vb.tvTextContent) {          // old version
            textSize = if (data.isBigText) 18f else 14f
            //movementMethod = ScrollingMovementMethod()
            movementMethod = LinkMovementMethod()
            val content = if (data.isLoadingContent) "loading" else data.content.first()
            if (text.toString() == content) return@with
            setText(content, TextView.BufferType.SPANNABLE)

            MarkdownBuilder(context)
                .markdownToSpan(data.content)
                .run {
                    setText(this, TextView.BufferType.SPANNABLE)
                }

        }*/

        with(vb.tvTextContent) {
            textSize = if (data.isBigText) 18f else 14f
            isLoading = data.content.isEmpty()
            setContent(data.content)
        }

        with(vb.toolbar) {
            title = data.title ?: "loading..."
            subtitle = data.category ?: "loading..."
            if (data.categoryIcon != null) logo = getDrawable(data.categoryIcon as Int)
        }

        if (data.isLoadingContent) return

        if (data.isSearch) {
            renderSearchResult(data.searchResults)
            renderSearchPosition(data.searchPosition, data.searchResults)
        } else clearSearchResult()
    }

    override fun setupToolbar() {
        setSupportActionBar(vb.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = if (vb.toolbar.childCount > 2) vb.toolbar.getChildAt(2) as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
        //logo?.background = getDrawable(R.drawable.ic_launcher_background)
        val lp = logo?.layoutParams as? Toolbar.LayoutParams
        lp?.let {
            it.width = dpToIntPx(40)
            it.height = dpToIntPx(40)
            it.marginEnd = dpToIntPx(16)
            logo.layoutParams = it
        }

        /*logo?.layoutParams?.width = dpToIntPx(40)
        logo?.layoutParams?.height = dpToIntPx(40)*/

        //val title = vb.toolbar.getChildAt(1) as TextView
        //val subtitle = vb.toolbar.getChildAt(0) as TextView

        //title.setPadding(dpToIntPx(16), 0, 0,0)
        //subtitle.setPadding(dpToIntPx(16), 0, 0,0)
    }

    override fun setupSubmenu() {
        with(vbSubmenu) {
            btnTextUp.setOnClickListener { viewModel.handleUpText() }
            btnTextDown.setOnClickListener { viewModel.handleDownText() }
            switchMode.setOnClickListener { viewModel.handleNightMode() }
        }
    }

    override fun setupBottombar() {
        with(vbBottombar) {
            btnLike.setOnClickListener { viewModel.handleLike() }
            btnBookmark.setOnClickListener { viewModel.handleBookmark() }
            btnShare.setOnClickListener { viewModel.handleShare() }
            btnSettings.setOnClickListener { viewModel.handleToggleMenu() }

            btnResultUp.setOnClickListener {
                if (!vb.tvTextContent.hasFocus()) vb.tvTextContent.requestFocus()
                hideKeyboard(it)
                searchView.clearFocus()
                viewModel.handleUpResult()
            }

            btnResultDown.setOnClickListener {
                if (!vb.tvTextContent.hasFocus()) vb.tvTextContent.requestFocus()
                hideKeyboard(it)
                viewModel.handleDownResult()
            }

            btnSearchClose.setOnClickListener {
                viewModel.handleSearchMode(false)
                invalidateOptionsMenu()
            }
        }
    }

    override fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
       /* val content = vb.tvTextContent.text as Spannable

        clearSearchResult()

            //Log.d("Root", "$content")
        Log.d("Root", "Length = ${content.length}")
        searchResult.forEach { (start, end) ->
            if (start >= content.length || end >= content.length) {
                Log.d("Root forEach", "start = $start, end = $end")
                return
            } else {
                content.setSpan(
                    SearchSpan(),
                    start,
                    end,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        }*/
        vb.tvTextContent.renderSearchResult(searchResult)
    }

    override fun renderSearchPosition(searchPosition: Int, searchResult: List<Pair<Int, Int>>) {
        /*val content = vb.tvTextContent.text as Spannable

        val spans = content.getSpans<SearchSpan>()

        content.getSpans<SearchFocusSpan>().forEach { content.removeSpan(it) }

        if (spans.isNotEmpty()) {
            val result = spans[searchPosition]
            //move to selection
            Selection.setSelection(content, content.getSpanStart(result))
            //set new search focus span
            content.setSpan(
                SearchFocusSpan(),
                content.getSpanStart(result),
                content.getSpanEnd(result),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val line: Int = vb.tvTextContent.layout.getLineForOffset(content.getSpanStart(result))
            Log.d("RootActivity", "Line number of text: $line, search position: ${content.getSpanStart(result)}")
            val y: Int = ((line + 1) * (vb.tvTextContent.lineHeight + vb.tvTextContent.lineSpacingExtra)).toInt()

            Log.d("RootActivity", "Y: $y")
            Log.d("RootActivity", "Scroll view height: ${vb.scroll.height}")
            vb.scroll.smoothScrollTo(0, y  - (vb.scroll.height - ((vb.tvTextContent.lineHeight + vb.tvTextContent.lineSpacingExtra) + dpToIntPx(56)).toInt()))
        }*/

        vb.tvTextContent.renderSearchPosition(searchResult.getOrNull(searchPosition))
    }

    override fun clearSearchResult() {
        /*val content = vb.tvTextContent.text as Spannable
        content.getSpans<SearchSpan>()
            .forEach { content.removeSpan(it) }*/
        vb.tvTextContent.clearSearchResult()
    }

    override fun showSearchbar(resultsCount: Int, searchPosition: Int) {
        with(vb.bottombar) {
            setSearchState(true)
            setSearchInfo(resultsCount, searchPosition)
        }

        vb.scroll.setMarginOptionally(bottom = dpToIntPx(56))
    }

    override fun hideSearchbar() {
        with(vb.bottombar) {
            setSearchState(false)
        }
        vb.scroll.setMarginOptionally(bottom = dpToIntPx(0))
    }

    override fun setCopyListener() {
        vb.tvTextContent.setCopyListener { copy ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied code", copy)
            clipboard.setPrimaryClip(clip)
            viewModel.handleCopyCode()
        }
    }

    /*companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val viewModelFactory: ViewModelFactory? = null
    }*/

    /*private fun setupSearch() {
        et_search.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                viewModel.handleSearch(et_search.text.toString())
            }
        }
    }*/
}