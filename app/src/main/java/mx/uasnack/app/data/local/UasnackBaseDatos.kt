package mx.uasnack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.uasnack.app.data.local.catalogo.CatalogoDao
import mx.uasnack.app.data.local.catalogo.ProductoEntity
import mx.uasnack.app.data.local.catalogo.TiendaEntity

// Base de datos local de la app (Room). Se crea una sola vez en UasnackApp.
//
// Cada entidad nueva se agrega a la lista de "entities" y sube "version" en 1.
//
// Hoy todo lo que guarda es CACHÉ (se puede volver a pedir al servidor), por eso ante un cambio de
// versión se borra y se recrea (fallbackToDestructiveMigration). Cuando entre algo que NO sea caché,
// como la bandeja de pedidos sin enviar (#184), habrá que escribir migraciones de verdad.
@Database(
    entities = [TiendaEntity::class, ProductoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class UasnackBaseDatos : RoomDatabase() {

    abstract fun catalogoDao(): CatalogoDao

    companion object {
        fun crear(contexto: Context): UasnackBaseDatos =
            Room.databaseBuilder(contexto, UasnackBaseDatos::class.java, "uasnack.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
