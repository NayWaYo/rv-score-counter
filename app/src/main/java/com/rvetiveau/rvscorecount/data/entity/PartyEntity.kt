package com.rvetiveau.rvscorecount.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import java.util.UUID

@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val status: String,
    val currentDealerIndex: Int,
    val createdAt: Long,
    /** Belote end rule (schema v3), [END_RULE_TARGET_SCORE] or [END_RULE_ROUND_COUNT]; null for the Papayoo. */
    @ColumnInfo(defaultValue = "NULL") val endRuleType: String? = null,
    @ColumnInfo(defaultValue = "NULL") val endRuleValue: Int? = null
)

const val END_RULE_TARGET_SCORE = "TARGET_SCORE"
const val END_RULE_ROUND_COUNT = "ROUND_COUNT"

fun PartyEntity.toDomain() = Party(
    id = UUID.fromString(id),
    gameId = gameId,
    status = PartyState.valueOf(status),
    currentDealerIndex = currentDealerIndex,
    createdAt = createdAt,
    endRule = endRule()
)

private fun PartyEntity.endRule(): BeloteEndRule? {
    if (endRuleType == null && endRuleValue == null) return null
    checkNotNull(endRuleType) { "Party $id has an end rule value without a type" }
    checkNotNull(endRuleValue) { "Party $id has an end rule type without a value" }
    return when (endRuleType) {
        END_RULE_TARGET_SCORE -> BeloteEndRule.TargetScore(endRuleValue)
        END_RULE_ROUND_COUNT -> BeloteEndRule.RoundCount(endRuleValue)
        else -> error("Party $id has an unknown end rule type '$endRuleType'")
    }
}

fun Party.toEntity() = PartyEntity(
    id = id.toString(),
    gameId = gameId,
    status = status.name,
    currentDealerIndex = currentDealerIndex,
    createdAt = createdAt,
    endRuleType = when (endRule) {
        is BeloteEndRule.TargetScore -> END_RULE_TARGET_SCORE
        is BeloteEndRule.RoundCount -> END_RULE_ROUND_COUNT
        null -> null
    },
    endRuleValue = when (endRule) {
        is BeloteEndRule.TargetScore -> endRule.points
        is BeloteEndRule.RoundCount -> endRule.rounds
        null -> null
    }
)
