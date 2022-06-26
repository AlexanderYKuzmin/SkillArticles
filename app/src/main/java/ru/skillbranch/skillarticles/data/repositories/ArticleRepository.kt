package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.*

interface IArticleRepository {
    fun loadArticleContent(articleId: String): LiveData<List<String>?>
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

    override fun loadArticleContent(articleId: String): LiveData<List<String>?> {
        return network.loadArticleContent(articleId) // delay 5s
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