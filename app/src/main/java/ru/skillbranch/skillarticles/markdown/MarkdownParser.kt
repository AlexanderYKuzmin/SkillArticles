package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {
    private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

    //group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"                                        //1
    private const val HEADER_GROUP = "(^#{1,6} .+?$)"                                                   //2
    private const val QUOTE_GROUP = "(^> .+?$)"                     //3
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"   //4
    private const val BOLD_GROUP =
        "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!\\_)_{2}[^_].*?[^_]?_{2}(?!_))" //5
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?[^~]?~{2}(?!~))" //6
    private const val RULE_GROUP = "(^[-_*]{3}$)" //7
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))" //8
    private const val LINK_GROUP = "((\\[[^\\[\\]]*?\\]\\(.+?\\))|(^\\[*?]\\(.*?)\\))"   //9 (10, 11)
    private const val ORDERED_LIST_ITEM_GROUP = "(^\\d+\\.\\s.+$)" //12
    private const val IMAGE_GROUP = "(^!\\[[^\\[\\]]*?\\]\\(.+?\\))" //13
    //private const val MULTILINE_GROUP = "(```[^\\s]((.+\\n)+)*.+```)" //14
    private const val MULTILINE_GROUP = "(```[^\\s](.+)```|```[^\\s]((.+\\n)+)*.+```)" //14

    //result regex
    const val MARKDOWN_GROUPS =
        "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP|$ITALIC_GROUP" +
                "|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP|$INLINE_GROUP|$LINK_GROUP" +
                "|$ORDERED_LIST_ITEM_GROUP|$IMAGE_GROUP|$MULTILINE_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    fun clear(string: String): String? {
        val dividerRemoved = string.replace("_{3}|\\*{3}|-{3}".toRegex(), " ")
        return dividerRemoved.replace((
                "#{1,6} " + // Headers
                "|(?<!`)\\[|\\](?!`)|(?<!`)\\(.+\\)(?!`)" + //Links
                "|(?<!.)([-+*] )" +
                "|((?<![ \\w])[-+](?![.]))|((?<![*_~])[*_~]{1,3}(?!\\s))|(?<!\\s)([*_~]{1,3}(?=[*_~\\s\\W]))" + //italic, bold, strike and others
                "|((?<!.)> )" + //Quotes
                "|((?<!.)\\d+\\. )" + // Ordered
                "|((?<= )`(?![ `])(?=.*`))|((?<=`.*)(?<!`)`(?=[ \\n]))|((?<=\\s)`{3}(?!\\s))|(?<!\\s)`{3}(?=\\s)" //Inline & Multiline
                ).toRegex(), "")
    }

    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            //if something is found then everything before - TEXT
            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }
            //found text
            var text: CharSequence

            //groups range for iterate by groups
            val groups = 1..15
            var group = -1
            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }
            when (group) {
                //NOT FOUND -> break

                -1 -> {
                    break@loop
                }


                //UNORDERED LIST
                1 -> {
                    //text without "*. "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    //find inner elements
                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)

                    //next find start from position "endIndex" (last regex character)
                    lastStartIndex = endIndex
                }

                //HEADER
                2 -> {
                    val reg = "^#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length

                    //text without "{#} "
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)

                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //QUOTE
                3 -> {
                    //text without "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    val subs = findElements(text)
                    val element = Element.Quote(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //ITALIC
                4 -> {
                    //text without "*{}*"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val subs = findElements(text)
                    val element = Element.Italic(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //BOLD
                5 -> {
                    //text without "**{}**"
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))
                    val subs = findElements(text)
                    val element = Element.Bold(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //STRIKE
                6 -> {
                    //text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))
                    val subs = findElements(text)
                    val element = Element.Strike(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //RULE
                7 -> {
                    //text without "***" insert empty character
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //INLINE CODE
                8 -> {
                    //text without "`{}`"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val element = Element.InlineCode(text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //LINK
                9 -> {
                    //full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    val (title: String, link: String) = "\\[(.*)]\\((.*)\\)".toRegex().find(text)!!.destructured
                    val element = Element.Link(link, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //ORDERED LIST
                12 -> {
                    val fullText = string.subSequence(startIndex, endIndex)
                    val (order: String, text: String) = "(^[1-9]+\\.)\\s(.+)".toRegex().find(fullText)!!.destructured

                    //find inner elements
                    val subs = findElements(text)
                    val element = Element.OrderedListItem(order, text, subs)
                    parents.add(element)

                    //next find start from position "endIndex" (last regex character)
                    lastStartIndex = endIndex
                }

                //IMAGE
                13 -> {
                    val fullText = string.subSequence(startIndex, endIndex)
                    val (altOrigin: String?, urlWithTitle: String) = "\\[(.*)]\\((.*)\\)".toRegex().find(fullText)!!.destructured

                    val url = urlWithTitle.substringBefore(" ")
                    val title = if (url.length < urlWithTitle.length) {
                        urlWithTitle.substringAfter(" ") .replace("\"", "")
                    } else ""
                    val alt = if (altOrigin.isEmpty()) null else altOrigin
                    val element = Element.Image(url, alt, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }

                //MULTILINE
                14 -> {
                    text = string.subSequence(startIndex.plus(3), endIndex.minus(3))
                    val element = Element.BlockCode(text = text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
            }
        }
        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }
        return parents
    }
}

data class MarkdownText(val elements: List<Element>)

sealed class Element() {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule(
        override val text: CharSequence = " ", // for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        val type: Type = Type.MIDDLE,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element() {
        enum class Type { START, END, MIDDLE, SINGLE }
    }

    data class Image(
        val url: String,
        val alt: String?,
        override val text: CharSequence = "",
        override val elements: List<Element> = emptyList()
    ) : Element()
}