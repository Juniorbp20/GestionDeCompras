package com.example.gestiondecompras.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.gestiondecompras.daos.AbonoDao;
import com.example.gestiondecompras.daos.ClienteDao;
import com.example.gestiondecompras.daos.GastoDao;
import com.example.gestiondecompras.daos.PedidoDao;
import com.example.gestiondecompras.daos.ReporteDao;
import com.example.gestiondecompras.daos.TarjetaDao;
import com.example.gestiondecompras.daos.TiendaDao;
import com.example.gestiondecompras.models.Abono;
import com.example.gestiondecompras.models.Cliente;
import com.example.gestiondecompras.models.Gasto;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.models.Tienda;

@Database(
        entities = {
                Cliente.class,
                Tienda.class,
                Tarjeta.class,
                Pedido.class,
                Gasto.class,
                Abono.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `abonos` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`pedido_id` INTEGER NOT NULL, " +
                    "`monto` REAL NOT NULL, " +
                    "`fecha_epoch` INTEGER NOT NULL, " +
                    "`metodo` TEXT, " +
                    "`notas` TEXT, " +
                    "FOREIGN KEY(`pedido_id`) REFERENCES `pedidos`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_abonos_pedido_id` ON `abonos` (`pedido_id`)");
            db.execSQL("INSERT INTO `abonos` (`pedido_id`, `monto`, `fecha_epoch`, `metodo`, `notas`) " +
                    "SELECT `id`, `total_general`, COALESCE(`fecha_registro_epoch`, 0), 'Efectivo', " +
                    "'Pago registrado antes de abonos' FROM `pedidos` WHERE `estado` = 'pagado'");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `tarjetas` ADD COLUMN `dia_pago` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Callback PREPOPULATE_CALLBACK = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            db.execSQL("INSERT INTO tiendas (nombre) VALUES ('Temu')");
            db.execSQL("INSERT INTO tiendas (nombre) VALUES ('Shein')");
            db.execSQL("INSERT INTO tiendas (nombre) VALUES ('Amazon')");
            db.execSQL("INSERT INTO tiendas (nombre) VALUES ('EBay')");
            db.execSQL("INSERT INTO tiendas (nombre) VALUES ('AliExpress')");
            db.execSQL("INSERT INTO tiendas (nombre) VALUES ('Alibaba')");
        }
    };

    public static AppDatabase getInstance(final android.content.Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "GestionCompras.db")
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                            .fallbackToDestructiveMigration()
                            .setJournalMode(JournalMode.TRUNCATE) // Asegura que todo esté en el archivo .db
                            .addCallback(PREPOPULATE_CALLBACK)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract ClienteDao clienteDao();
    public abstract TiendaDao tiendaDao();
    public abstract TarjetaDao tarjetaDao();
    public abstract PedidoDao pedidoDao();
    public abstract ReporteDao reporteDao();
    public abstract GastoDao gastoDao();
    public abstract AbonoDao abonoDao();
}
