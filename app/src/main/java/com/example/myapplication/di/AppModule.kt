package com.example.myapplication.di

import android.app.Application
import android.content.Context
import com.example.myapplication.MyApplication
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    @Singleton
    fun provideApplication(app: MyApplication): Application = app

    @Provides
    @Singleton
    fun provideContext(app: MyApplication): Context = app.applicationContext
}