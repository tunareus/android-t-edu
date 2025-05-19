package com.example.myapplication.di

import com.example.myapplication.MyApplication
import com.example.myapplication.presentation.ui.activity.MainActivity
import com.example.myapplication.presentation.ui.fragment.DetailFragment
import com.example.myapplication.presentation.ui.fragment.ListFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    DataModule::class,
    DomainModule::class,
    ViewModelModule::class
])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: MyApplication): AppComponent
    }

    fun inject(application: MyApplication)
    fun inject(mainActivity: MainActivity)
    fun inject(listFragment: ListFragment)
    fun inject(detailFragment: DetailFragment)
}