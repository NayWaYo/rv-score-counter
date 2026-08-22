package com.rvetiveau.rvscorecount.di

import android.content.Context
import androidx.room.Room
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.RoundDao
import com.rvetiveau.rvscorecount.data.dao.RoundScoreDao
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "rv_score_counter.db")
            .build()

    @Provides
    @Singleton
    fun providePartyDao(db: AppDatabase): PartyDao = db.partyDao()

    @Provides
    @Singleton
    fun provideParticipantDao(db: AppDatabase): ParticipantDao = db.participantDao()

    @Provides
    @Singleton
    fun provideRoundDao(db: AppDatabase): RoundDao = db.roundDao()

    @Provides
    @Singleton
    fun provideRoundScoreDao(db: AppDatabase): RoundScoreDao = db.roundScoreDao()
}
