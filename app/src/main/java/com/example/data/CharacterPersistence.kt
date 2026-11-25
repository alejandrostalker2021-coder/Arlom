package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.ArmorWeight
import com.example.model.CharacterProfile
import com.example.model.EquipSlot
import com.example.model.EquipmentItem
import com.example.model.HeroClass
import com.example.model.ItemRarity
import com.example.model.QuestProgress
import com.example.model.Race
import com.example.model.WeaponCategory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameTypeConverters {
    @TypeConverter
    fun fromEquipSlot(slot: EquipSlot): String = slot.name

    @TypeConverter
    fun toEquipSlot(value: String): EquipSlot =
        runCatching { EquipSlot.valueOf(value) }.getOrDefault(EquipSlot.WEAPON)

    @TypeConverter
    fun fromItemRarity(rarity: ItemRarity): String = rarity.name

    @TypeConverter
    fun toItemRarity(value: String): ItemRarity =
        runCatching { ItemRarity.valueOf(value) }.getOrDefault(ItemRarity.COMMON)

    @TypeConverter
    fun fromRace(race: Race): String = race.name

    @TypeConverter
    fun toRace(value: String): Race =
        runCatching { Race.valueOf(value) }.getOrDefault(Race.HUMAN)

    @TypeConverter
    fun fromHeroClass(heroClass: HeroClass): String = heroClass.name

    @TypeConverter
    fun toHeroClass(value: String): HeroClass =
        runCatching { HeroClass.valueOf(value) }.getOrDefault(HeroClass.PALADIN)
}

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val race: Race,
    val heroClass: HeroClass,
    val level: Int,
    val xp: Int,
    val gold: Int,
    val potions: Int,
    val posX: Float,
    val posY: Float,
    val questsJson: String,
    val clearedEnclavesCsv: String = "",
    val unlockedZoneTier: Int = 1,
    val updatedAtMs: Long
)

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["characterId"]),
        Index(value = ["characterId", "slot", "isEquipped"]),
        Index(value = ["characterId", "rarity"])
    ]
)
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val name: String,
    val slot: EquipSlot,
    val rarity: ItemRarity,
    val isEquipped: Boolean,
    val requiredLevel: Int,
    val attackBonus: Int,
    val armorBonus: Int,
    val hpBonus: Int,
    val manaBonus: Int,
    val critPctBonus: Float,
    val dodgePctBonus: Float,
    val speedPctBonus: Float,
    val lifestealPctBonus: Float,
    val setId: String?,
    val setName: String?,
    val goldValue: Int,
    val description: String,
    val armorWeightName: String? = null,
    val weaponCategoryName: String? = null,
    val acquiredAtMs: Long = System.currentTimeMillis()
)

data class CharacterWithInventory(
    @Embedded val character: CharacterEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "characterId"
    )
    val items: List<InventoryItemEntity>
)

@Dao
interface CharacterDao {
    @Transaction
    @Query("SELECT * FROM characters ORDER BY updatedAtMs DESC")
    fun observeAllWithInventory(): Flow<List<CharacterWithInventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCharacter(entity: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE characterId = :characterId ORDER BY isEquipped DESC, acquiredAtMs DESC")
    fun observeInventory(characterId: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE characterId = :characterId AND isEquipped = 1")
    fun observeEquippedItems(characterId: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE characterId = :characterId AND isEquipped = 0 AND rarity = :rarity ORDER BY requiredLevel DESC")
    fun observeBagByRarity(characterId: String, rarity: ItemRarity): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<InventoryItemEntity>)

    @Query("UPDATE inventory_items SET isEquipped = 0 WHERE characterId = :characterId AND slot = :slot AND isEquipped = 1")
    suspend fun unequipSlot(characterId: String, slot: EquipSlot)

    @Query("UPDATE inventory_items SET isEquipped = 1 WHERE id = :itemId AND characterId = :characterId")
    suspend fun markEquipped(characterId: String, itemId: String)

    @Transaction
    suspend fun equipItemAtomically(characterId: String, itemId: String, slot: EquipSlot) {
        unequipSlot(characterId, slot)
        markEquipped(characterId, itemId)
    }

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM inventory_items WHERE characterId = :characterId")
    suspend fun clearForCharacter(characterId: String)

    @Transaction
    suspend fun replaceForCharacter(characterId: String, items: List<InventoryItemEntity>) {
        clearForCharacter(characterId)
        if (items.isNotEmpty()) {
            upsertItems(items)
        }
    }
}

@Database(
    entities = [CharacterEntity::class, InventoryItemEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(GameTypeConverters::class)
abstract class GameDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getInstance(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "aethelgard_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

fun EquipmentItem.toEntity(characterId: String, isEquipped: Boolean): InventoryItemEntity =
    InventoryItemEntity(
        id = id,
        characterId = characterId,
        name = name,
        slot = slot,
        rarity = rarity,
        isEquipped = isEquipped,
        requiredLevel = requiredLevel,
        attackBonus = attackBonus,
        armorBonus = armorBonus,
        hpBonus = hpBonus,
        manaBonus = manaBonus,
        critPctBonus = critPctBonus,
        dodgePctBonus = dodgePctBonus,
        speedPctBonus = speedPctBonus,
        lifestealPctBonus = lifestealPctBonus,
        setId = setId,
        setName = setName,
        goldValue = goldValue,
        description = description,
        armorWeightName = armorWeight?.name,
        weaponCategoryName = weaponCategory?.name
    )

fun InventoryItemEntity.toDomain(): EquipmentItem =
    EquipmentItem(
        id = id,
        name = name,
        slot = slot,
        rarity = rarity,
        requiredLevel = requiredLevel,
        attackBonus = attackBonus,
        armorBonus = armorBonus,
        hpBonus = hpBonus,
        manaBonus = manaBonus,
        critPctBonus = critPctBonus,
        dodgePctBonus = dodgePctBonus,
        speedPctBonus = speedPctBonus,
        lifestealPctBonus = lifestealPctBonus,
        setId = setId,
        setName = setName,
        goldValue = goldValue,
        description = description,
        armorWeight = armorWeightName?.let { runCatching { ArmorWeight.valueOf(it) }.getOrNull() },
        weaponCategory = weaponCategoryName?.let { runCatching { WeaponCategory.valueOf(it) }.getOrNull() }
    )

class CharacterRepository(
    private val characterDao: CharacterDao,
    private val inventoryDao: InventoryDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val questsType = Types.newParameterizedType(List::class.java, QuestProgress::class.java)
    private val questsAdapter = moshi.adapter<List<QuestProgress>>(questsType)

    val allCharacters: Flow<List<CharacterProfile>> =
        characterDao.observeAllWithInventory().map { rows ->
            rows.map { row ->
                val quests = runCatching {
                    questsAdapter.fromJson(row.character.questsJson)
                }.getOrNull() ?: emptyList()

                val equippedMap = row.items
                    .filter { it.isEquipped }
                    .associate { it.slot to it.toDomain() }

                val bagItems = row.items
                    .filter { !it.isEquipped }
                    .map { it.toDomain() }

                val clearedEnclaves = row.character.clearedEnclavesCsv
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                CharacterProfile(
                    id = row.character.id,
                    name = row.character.name,
                    race = row.character.race,
                    heroClass = row.character.heroClass,
                    level = row.character.level,
                    xp = row.character.xp,
                    gold = row.character.gold,
                    potions = row.character.potions,
                    posX = row.character.posX,
                    posY = row.character.posY,
                    equipped = equippedMap,
                    inventory = bagItems,
                    quests = quests,
                    clearedEnclaves = clearedEnclaves,
                    unlockedZoneTier = row.character.unlockedZoneTier.coerceIn(1, 4),
                    updatedAtMs = row.character.updatedAtMs
                )
            }
        }

    fun observeInventoryItems(characterId: String): Flow<List<InventoryItemEntity>> =
        inventoryDao.observeInventory(characterId)

    fun observeBagByRarity(characterId: String, rarity: ItemRarity): Flow<List<EquipmentItem>> =
        inventoryDao.observeBagByRarity(characterId, rarity).map { list -> list.map { it.toDomain() } }

    suspend fun saveProfile(profile: CharacterProfile) {
        val now = System.currentTimeMillis()
        val entity = CharacterEntity(
            id = profile.id,
            name = profile.name,
            race = profile.race,
            heroClass = profile.heroClass,
            level = profile.level,
            xp = profile.xp,
            gold = profile.gold,
            potions = profile.potions,
            posX = profile.posX,
            posY = profile.posY,
            questsJson = questsAdapter.toJson(profile.quests),
            clearedEnclavesCsv = profile.clearedEnclaves.sorted().joinToString(","),
            unlockedZoneTier = profile.unlockedZoneTier.coerceIn(1, 4),
            updatedAtMs = now
        )
        characterDao.upsertCharacter(entity)

        val itemEntities = buildList {
            profile.equipped.values.forEach { add(it.toEntity(profile.id, isEquipped = true)) }
            profile.inventory.forEach { add(it.toEntity(profile.id, isEquipped = false)) }
        }
        inventoryDao.replaceForCharacter(profile.id, itemEntities)
    }

    suspend fun equipItem(characterId: String, item: EquipmentItem) {
        inventoryDao.equipItemAtomically(characterId, item.id, item.slot)
    }

    suspend fun unequipSlot(characterId: String, slot: EquipSlot) {
        inventoryDao.unequipSlot(characterId, slot)
    }

    suspend fun deleteItem(itemId: String) {
        inventoryDao.deleteItemById(itemId)
    }

    suspend fun deleteProfile(id: String) {
        characterDao.deleteById(id)
    }
}
