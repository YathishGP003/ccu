package a75f.io.renatus
import a75f.io.logic.Globals
import a75f.io.data.RenatusDatabaseBuilder
import a75f.io.data.message.DatabaseHelper
import a75f.io.data.message.MessageDatabaseHelper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    /*@Provides
    fun provideMessageDatabaseHelper() : a75f.io.data.message.DatabaseHelper =
        a75f.io.data.message.MessageDatabaseHelper(
            RenatusDatabaseBuilder.getInstance(
                Globals.getInstance().applicationContext
            )
        )*/

}