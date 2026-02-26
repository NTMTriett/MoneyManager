package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: String): Flow<List<Category>>
    fun getTopLevelCategories(): Flow<List<Category>>
    fun getTopLevelCategoriesByType(type: String): Flow<List<Category>>
    fun getSubcategories(parentId: String): Flow<List<Category>>
    suspend fun addCategory(category: Category): Result<Category>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun getCategoryById(categoryId: String): Result<Category>
}