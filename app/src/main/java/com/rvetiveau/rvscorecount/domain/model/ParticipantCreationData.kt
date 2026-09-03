package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** Pure domain input for Party creation; CreatePartyUseCase re-validates names even if the UI already did. */
data class ParticipantCreationData(val id: UUID, val name: String)
