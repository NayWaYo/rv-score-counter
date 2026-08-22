package com.rvetiveau.rvscorecount.data.db

import androidx.room.TypeConverter
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.PartyState
import java.util.UUID

class Converters {
    @TypeConverter fun fromUUID(value: UUID?): String? = value?.toString()
    @TypeConverter fun toUUID(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter fun fromPartyState(value: PartyState?): String? = value?.name
    @TypeConverter fun toPartyState(value: String?): PartyState? =
        value?.let { PartyState.valueOf(it) }

    @TypeConverter fun fromCardSuit(value: CardSuit?): String? = value?.name
    @TypeConverter fun toCardSuit(value: String?): CardSuit? =
        value?.let { CardSuit.valueOf(it) }
}
