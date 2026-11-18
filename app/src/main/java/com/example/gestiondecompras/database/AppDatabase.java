package com.example.gestiondecompras.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.gestiondecompras.daos.AbonoDao;
import com.example.gestiondecompras.daos.ClienteDao;
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
import com.example.gestiondecompras.models.finanzas.CategoriaFin;
import com.example.gestiondecompras.models.finanzas.Cuenta;
import com.example.gestiondecompras.models.finanzas.Movimiento;
import com.example.gestiondecompras.models.finanzas.Presupuesto;
import com.example.gestiondecompras.models.finanzas.Recurrente;
import com.example.gestiondecompras.daos.finanzas.CategoriaFinDao;
import com.example.gestiondecompras.daos.finanzas.CuentaDao;
import com.example.gestiondecompras.daos.finanzas.MovimientoDao;
import com.example.gestiondecompras.daos.finanzas.PresupuestoDao;
import com.example.gestiondecompras.daos.finanzas.RecurrenteDao;

@Database(
        entities = {
                Cliente.class,
                Tienda.class,
                Tarjeta.class,
                Pedido.class,
                Gasto.class,
                Abono.class,
                CategoriaFin.class,
                Cuenta.class,
                Movimiento.class,
                Presupuesto.class,
                Recurrente.class
        },
        version = 9,
        exportSchema = true
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

    /**
     * Finanzas completas: categorías personalizables, cuentas, movimientos (ingresos y gastos),
     * presupuestos y recurrentes. Los gastos existentes se copian como movimientos; la tabla
     * `gastos` se conserva como respaldo.
     */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            crearTablasFinanzas(db);
            sembrarFinanzas(db);
            db.execSQL("INSERT INTO `fin_movimientos` (`tipo`, `monto`, `fecha_epoch`, `descripcion`, `notas`, "
                    + "`categoria_id`, `cuenta_id`, `recurrente_id`) "
                    + "SELECT 'gasto', g.`monto`, g.`fechaEpoch`, g.`descripcion`, g.`notas`, "
                    + "COALESCE((SELECT c.`id` FROM `fin_categorias` c WHERE c.`tipo` = 'gasto' AND c.`nombre` = g.`categoria`), "
                    + "(SELECT c.`id` FROM `fin_categorias` c WHERE c.`tipo` = 'gasto' AND c.`nombre` = 'Otros')), "
                    + "(SELECT `id` FROM `fin_cuentas` ORDER BY `id` LIMIT 1), NULL FROM `gastos` g");
        }
    };

    /** Crea las tablas de Finanzas exactamente como las define Room (versión 7). */
    static void crearTablasFinanzas(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `fin_categorias` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`nombre` TEXT, `tipo` TEXT, `color_key` TEXT, `icono_key` TEXT, "
                + "`archivada` INTEGER NOT NULL, `orden` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `fin_cuentas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`nombre` TEXT, `tipo` TEXT, `saldo_inicial` REAL NOT NULL, `archivada` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `fin_movimientos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`tipo` TEXT, `monto` REAL NOT NULL, `fecha_epoch` INTEGER NOT NULL, `descripcion` TEXT, "
                + "`notas` TEXT, `categoria_id` INTEGER, `cuenta_id` INTEGER, `recurrente_id` INTEGER, "
                + "FOREIGN KEY(`categoria_id`) REFERENCES `fin_categorias`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , "
                + "FOREIGN KEY(`cuenta_id`) REFERENCES `fin_cuentas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fin_movimientos_categoria_id` ON `fin_movimientos` (`categoria_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fin_movimientos_cuenta_id` ON `fin_movimientos` (`cuenta_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fin_movimientos_fecha_epoch` ON `fin_movimientos` (`fecha_epoch`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `fin_presupuestos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`categoria_id` INTEGER NOT NULL, `monto_mensual` REAL NOT NULL, "
                + "FOREIGN KEY(`categoria_id`) REFERENCES `fin_categorias`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fin_presupuestos_categoria_id` ON `fin_presupuestos` (`categoria_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `fin_recurrentes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`tipo` TEXT, `monto` REAL NOT NULL, `descripcion` TEXT, `notas` TEXT, `categoria_id` INTEGER, "
                + "`cuenta_id` INTEGER, `frecuencia` TEXT, `proxima_fecha_epoch` INTEGER NOT NULL, "
                + "`dia_ancla` INTEGER NOT NULL, `activo` INTEGER NOT NULL, "
                + "FOREIGN KEY(`categoria_id`) REFERENCES `fin_categorias`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , "
                + "FOREIGN KEY(`cuenta_id`) REFERENCES `fin_cuentas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fin_recurrentes_categoria_id` ON `fin_recurrentes` (`categoria_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fin_recurrentes_cuenta_id` ON `fin_recurrentes` (`cuenta_id`)");
    }

    /** Categorías y cuenta iniciales de Finanzas (solo si aún no hay ninguna). */
    static void sembrarFinanzas(@NonNull SupportSQLiteDatabase db) {
        android.database.Cursor c = db.query("SELECT COUNT(*) FROM `fin_categorias`");
        int existentes = 0;
        if (c.moveToFirst()) {
            existentes = c.getInt(0);
        }
        c.close();
        if (existentes > 0) {
            return;
        }
        String[][] gastos = {
                {"Comida", "naranja", "comida"}, {"Transporte", "azul", "transporte"},
                {"Hogar", "marron", "hogar"}, {"Servicios", "dorado", "servicios"},
                {"Salud", "rojo", "salud"}, {"Entretenimiento", "morado", "ocio"},
                {"Compras", "verde_azulado", "compras"}, {"Otros", "gris", "billetera"}
        };
        String[][] ingresos = {
                {"Sueldo", "verde", "trabajo"}, {"Ventas", "azul", "tendencia"},
                {"Servicios", "cian", "servicios"}, {"Préstamos recibidos", "indigo", "banco"},
                {"Regalos", "rosa", "regalo"}, {"Otros ingresos", "gris", "billetera"}
        };
        insertarCategorias(db, gastos, "gasto");
        insertarCategorias(db, ingresos, "ingreso");
        db.execSQL("INSERT INTO `fin_cuentas` (`nombre`, `tipo`, `saldo_inicial`, `archivada`) "
                + "VALUES ('Efectivo', 'efectivo', 0, 0)");
    }

    private static void insertarCategorias(SupportSQLiteDatabase db, String[][] filas, String tipo) {
        for (int i = 0; i < filas.length; i++) {
            db.execSQL("INSERT INTO `fin_categorias` (`nombre`, `tipo`, `color_key`, `icono_key`, `archivada`, `orden`) "
                            + "VALUES (?, ?, ?, ?, 0, ?)",
                    new Object[]{filas[i][0], tipo, filas[i][1], filas[i][2], i});
        }
    }

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
            sembrarFinanzas(db);
        }
    };

    /** Pedidos: compra en dólares, número de rastreo y foto del recibo (columnas opcionales). */
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `monto_usd` REAL");
            db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `tasa_cambio` REAL");
            db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `rastreo` TEXT");
            db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `foto_recibo` TEXT");
        }
    };

    /** Pedidos: artículo o descripción de lo comprado (opcional). */
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `articulo` TEXT");
        }
    };

    public static AppDatabase getInstance(final android.content.Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "GestionCompras.db")
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                            // Solo las versiones 1-3 (anteriores a Room, sin migración posible) se
                            // recrean desde cero. Para cualquier versión nueva es obligatorio añadir
                            // su Migration: si falta, la app falla en pruebas en lugar de borrar en
                            // silencio los datos del usuario.
                            .fallbackToDestructiveMigrationFrom(1, 2, 3)
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
    public abstract AbonoDao abonoDao();
    public abstract CategoriaFinDao categoriaFinDao();
    public abstract CuentaDao cuentaDao();
    public abstract MovimientoDao movimientoDao();
    public abstract PresupuestoDao presupuestoDao();
    public abstract RecurrenteDao recurrenteDao();
}
