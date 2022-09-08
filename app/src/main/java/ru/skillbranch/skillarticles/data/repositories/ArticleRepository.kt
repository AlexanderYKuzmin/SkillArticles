package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.skillbranch.skillarticles.data.*
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownElement
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownParser
import java.lang.RuntimeException

interface IArticleRepository {
    fun loadArticleContent(articleId: String): LiveData<List<MarkdownElement>?>
    fun getArticle(articleId: String): LiveData<ArticleData?>
    fun loadArticlePersonalInfo(articleId: String): LiveData<ArticlePersonalInfo?>
    fun getAppSettings(): LiveData<AppSettings>
    fun updateSettings(appSettings: AppSettings)
    fun updateArticlePersonalInfo(info: ArticlePersonalInfo)
}

class ArticleRepository(
    private val local: LocalDataHolder = LocalDataHolder,
    private val network: NetworkDataHolder = NetworkDataHolder,
    private val prefs: PrefManager = PrefManager()
) : IArticleRepository{

    override fun loadArticleContent(articleId: String): LiveData<List<MarkdownElement>?> {
        Log.d("ArticleRepository", "start loading content")
        val cont = network.loadArticleContent(articleId)
        Log.d("ArticleRepository", "cont: ${cont.value}")
        val data = network.loadArticleContent(articleId)
            .map { str -> str?.let { MarkdownParser.parse(it) } }// delay 5s
        val mdElements = data.value
        mdElements?.forEachIndexed { index, markdownElement ->  Log.d("ArticleRepository", "index: $index, content: $markdownElement")}

        return data
    }

    override fun getArticle(articleId: String): LiveData<ArticleData?> {
        return local.findArticle(articleId) //2s delay from db
    }

    override fun loadArticlePersonalInfo(articleId: String): LiveData<ArticlePersonalInfo?> {
        return local.findArticlePersonalInfo(articleId)
    }

    override fun getAppSettings(): LiveData<AppSettings> = prefs.settings
    override fun updateSettings(appSettings: AppSettings) {
        //local.updateAppSettings(appSettings)
        prefs.isBigText = appSettings.isBigText
        prefs.isDarkMode = appSettings.isDarkMode
    }

    override fun updateArticlePersonalInfo(info: ArticlePersonalInfo) {
        local.updateArticlePersonalInfo(info)
    }
}