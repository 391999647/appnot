package com.noteapp

import com.noteapp.data.NoteRepository

/**
 * 全局单例仓库
 *
 * TODO: 替换为 DI 容器（如 Koin），移除全局可变单例，
 * 改为通过 Pager 构造函数注入 Repository 实例。
 */
object AppRepo {
    private var _repo: NoteRepository? = null

    val repo: NoteRepository
        get() {
            if (_repo == null) {
                _repo = NoteRepository()
                _repo!!.load()
            }
            return _repo!!
        }

    /**
     * 显式初始化仓库，确保在首次访问前完成数据加载。
     * 需在 AndroidContextHolder.init() 之后调用。
     */
    fun initialize() {
        repo // 触发 lazy 创建 + load()
    }
}
