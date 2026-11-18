package com.example.gestiondecompras.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Migraciones de la base de datos con datos reales. */
@RunWith(AndroidJUnit4.class)
public class MigracionTest {

    private static final String BD = "migracion-test";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    @Test
    public void migracion6a7_copiaLosGastosComoMovimientosConSuCategoria() throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(BD, 6);
        db.execSQL("INSERT INTO gastos (categoria, descripcion, monto, fechaEpoch, notas) "
                + "VALUES ('Comida', 'Mercado', 150.5, 1000, 'semanal')");
        db.execSQL("INSERT INTO gastos (categoria, descripcion, monto, fechaEpoch, notas) "
                + "VALUES ('Categoría desconocida', 'Varios', 20, 2000, NULL)");
        db.close();

        // Valida que el esquema resultante coincide exactamente con el de Room (versión 7)
        db = helper.runMigrationsAndValidate(BD, 7, true, AppDatabase.MIGRATION_6_7);

        try (Cursor c = db.query("SELECT m.tipo, m.monto, m.descripcion, m.notas, c.nombre, cu.nombre "
                + "FROM fin_movimientos m "
                + "JOIN fin_categorias c ON c.id = m.categoria_id "
                + "JOIN fin_cuentas cu ON cu.id = m.cuenta_id ORDER BY m.fecha_epoch")) {
            assertEquals(2, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("gasto", c.getString(0));
            assertEquals(150.5, c.getDouble(1), 0.0);
            assertEquals("Mercado", c.getString(2));
            assertEquals("semanal", c.getString(3));
            assertEquals("Comida", c.getString(4));
            assertEquals("Efectivo", c.getString(5));
            assertTrue(c.moveToNext());
            assertEquals("Otros", c.getString(4));
        }

        try (Cursor c = db.query("SELECT COUNT(*) FROM fin_categorias WHERE tipo = 'ingreso'")) {
            assertTrue(c.moveToFirst());
            assertEquals(6, c.getInt(0));
        }
        // La tabla antigua se conserva como respaldo
        try (Cursor c = db.query("SELECT COUNT(*) FROM gastos")) {
            assertTrue(c.moveToFirst());
            assertEquals(2, c.getInt(0));
        }
    }

    @Test
    public void migracion7a9_conservaPedidosYAnadeColumnasNuevas() throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(BD, 7);
        db.execSQL("INSERT INTO clientes (id, nombre, apellido, telefono, email, direccion, activo) "
                + "VALUES (1, 'Ana', 'Lopez', '809', 'a@b.c', '', 1)");
        db.execSQL("INSERT INTO pedidos (cliente_id, cliente_nombre, tienda, monto_compra, ganancia, "
                + "total_general, fecha_registro_epoch, notas, estado) "
                + "VALUES (1, 'Ana Lopez', 'Amazon', 100, 20, 120, 1000, 'nota', 'pendiente')");
        db.close();

        db = helper.runMigrationsAndValidate(BD, 9, true,
                AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9);

        try (Cursor c = db.query("SELECT cliente_nombre, total_general, notas, rastreo, articulo FROM pedidos")) {
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("Ana Lopez", c.getString(0));
            assertEquals(120, c.getDouble(1), 0.0);
            assertEquals("nota", c.getString(2));
            assertTrue(c.isNull(3));
            assertTrue(c.isNull(4));
        }
        db.execSQL("UPDATE pedidos SET articulo = 'Audífonos', rastreo = 'TBA123'");
        try (Cursor c = db.query("SELECT articulo, rastreo FROM pedidos")) {
            assertTrue(c.moveToFirst());
            assertEquals("Audífonos", c.getString(0));
            assertEquals("TBA123", c.getString(1));
        }
        db.close();
    }
}
