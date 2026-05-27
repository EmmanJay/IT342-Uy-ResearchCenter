package com.example.researchcenter.shared.data

import android.content.Context
import androidx.room.*
import com.example.researchcenter.shared.model.Repository

@Entity(tableName = "repositories")
data class RepositoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String?,
    val role: String?,
    val ownerId: Long,
    val ownerName: String?,
    val memberCount: Int,
    val materialCount: Int,
    val bookmarked: Boolean,
    val createdAt: String,
    val updatedAt: String?
) {
    fun toModel(): Repository = Repository(
        id = id, name = name, description = description,
        role = role, ownerId = ownerId, ownerName = ownerName,
        memberCount = memberCount,
        materialCount = materialCount, bookmarked = bookmarked,
        createdAt = createdAt, updatedAt = updatedAt
    )
    
    companion object {
        fun fromModel(repo: Repository): RepositoryEntity = RepositoryEntity(
            id = repo.id, name = repo.name, description = repo.description,
            role = repo.role, ownerId = repo.ownerId, ownerName = repo.ownerName,
            memberCount = repo.memberCount,
            materialCount = repo.materialCount, bookmarked = repo.bookmarked,
            createdAt = repo.createdAt, updatedAt = repo.updatedAt
        )
    }
}

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM repositories")
    fun getAll(): List<RepositoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(repos: List<RepositoryEntity>)

    @Query("DELETE FROM repositories")
    fun deleteAll()
}

@Database(entities = [RepositoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repositoryDao(): RepositoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "research_center_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
