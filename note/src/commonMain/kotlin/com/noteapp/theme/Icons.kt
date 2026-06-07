package com.noteapp.theme

import com.tencent.kuikly.core.base.attr.ImageUri

/**
 * 图标资源常量 - 引用 Kuikly common assets 资源
 */
object Icons {
    private fun icon(name: String): ImageUri = ImageUri.commonAssets("icons/$name.png")

    val MENU = icon("functional-more")
    val CLOSE = icon("functional-delete")
    val ADD = icon("functional-new-note")
    val SEARCH = icon("functional-search")
    val DELETE = icon("category-trash")
    val BACK = icon("functional-undo")
    val FORWARD = icon("functional-redo")
    val TAG = icon("category-tag")
    val EXPORT = icon("functional-export")
    val PREVIEW = icon("functional-settings")
    val RESTORE = icon("functional-refresh")
    val EMPTY_TRASH = icon("category-trash")
    val NOTE_EMPTY = icon("category-notebook")
    val WARNING = icon("functional-settings")
    val INFO = icon("functional-settings")
    val CHECK = icon("functional-save")
    val SELECT_ALL = icon("format-list-check")
    val SELECTED = icon("functional-star")
    val MORE = icon("functional-more")
    val CLEAR = icon("functional-delete")
    val HISTORY = icon("category-clock")
    val SAVE = icon("functional-save")
    val SHARE = icon("functional-share")
    val DRAG = icon("functional-more")
    val REFRESH = icon("functional-refresh")
    val PIN = icon("category-pin")
    val SEARCH_SCOPE = icon("functional-search")
}
