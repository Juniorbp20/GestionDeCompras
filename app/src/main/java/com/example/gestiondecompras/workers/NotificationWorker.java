package com.example.gestiondecompras.workers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gestiondecompras.R;
import com.example.gestiondecompras.activities.ListaPedidosActivity;
import com.example.gestiondecompras.activities.MainActivity;
import com.example.gestiondecompras.activities.TarjetasActivity;
import com.example.gestiondecompras.database.AppDatabase;
import com.example.gestiondecompras.models.Pedido;
import com.example.gestiondecompras.models.Tarjeta;
import com.example.gestiondecompras.utils.RecordatorioUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationWorker extends Worker {

    public static final String CANAL_COBROS = "recordatorios_cobros";
    public static final String CANAL_ENTREGAS = "recordatorios_entregas";
    public static final String CANAL_TARJETAS = "recordatorios_tarjetas";
    public static final String CANAL_RESUMEN = "resumen_diario";
    public static final String CANAL_FINANZAS = "finanzas_presupuestos";
    private static final String PREF_AVISOS_PRESUPUESTO = "avisos_presupuesto_mes";
    private static final int BASE_PRESUPUESTO = 4 * 1_000_000;

    private static final String PREF_DEDUPE = "notif_dedupe";
    private static final String PREF_ULTIMO_RESUMEN = "ultimo_resumen_dia";

    private static final int ID_COBROS = 1001;
    private static final int ID_RESUMEN = 1002;
    private static final int ID_PRUEBA = 1003;
    // Rangos separados por un millón para que los IDs de entregas, cortes y pagos nunca coincidan.
    private static final int RANGO_IDS = 1_000_000;
    private static final int BASE_ENTREGA = RANGO_IDS;
    private static final int BASE_CORTE = 2 * RANGO_IDS;
    private static final int BASE_PAGO = 3 * RANGO_IDS;

    /** Canales de versiones anteriores que ya no se usan. */
    private static final String[] CANALES_OBSOLETOS = {"default_channel", "recordatorios_diarios"};

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        if (!prefs.getBoolean("notif_enabled", true)) {
            return Result.success();
        }
        if (!puedeNotificar(context)) {
            return Result.success();
        }

        Calendar ahora = Calendar.getInstance();
        if (RecordatorioUtils.esHoraSilencio(ahora.get(Calendar.HOUR_OF_DAY))) {
            return Result.success();
        }

        try {
            AppDatabase db = AppDatabase.getInstance(context);
            enviarResumenDiarioSiCorresponde(context, prefs, db, ahora);
            enviarRecordatorioCobros(context, prefs, db, ahora);
            enviarRecordatoriosEntregas(context, prefs, db, ahora);
            enviarRecordatoriosTarjetas(context, prefs, db, ahora);
            enviarAvisosPresupuesto(context, prefs, db, ahora);
            purgarDedupe(prefs, ahora);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void enviarResumenDiarioSiCorresponde(Context context, SharedPreferences prefs,
                                                  AppDatabase db, Calendar ahora) {
        if (!debeEnviarResumen(prefs, ahora)) {
            return;
        }

        // Si el resumen se programa por la mañana, el día en curso apenas tiene actividad:
        // en ese caso se resume el día anterior completo.
        int horaResumen = RecordatorioUtils.normalizarHoraResumen(prefs.getInt("reminder_hour", 9));
        boolean resumirAyer = RecordatorioUtils.resumirDiaAnterior(horaResumen);
        Calendar dia = (Calendar) ahora.clone();
        if (resumirAyer) {
            dia.add(Calendar.DAY_OF_YEAR, -1);
        }

        long inicioDia = inicioDelDia(dia);
        long finDia = finDelDia(dia);

        int pedidosDia = db.pedidoDao().countPedidosEnRango(inicioDia, finDia);
        double ventas = db.pedidoDao().totalVentasEnRango(inicioDia, finDia);
        double ganancia = db.pedidoDao().totalGananciaEnRango(inicioDia, finDia);
        double cobrado = db.abonoDao().getTotalRango(inicioDia, finDia);
        double porCobrar = db.pedidoDao().totalPorCobrarGlobal();
        int entregasDia = db.pedidoDao().countEntregasEnRango(inicioDia, finDia);
        int atrasados = db.pedidoDao().countAtrasados(inicioDelDia(ahora));

        String detalle = context.getString(R.string.notif_resumen_pedidos, pedidosDia) + "\n"
                + context.getString(R.string.notif_resumen_ventas, ventas) + "\n"
                + context.getString(R.string.notif_resumen_ganancia, ganancia) + "\n"
                + context.getString(R.string.notif_resumen_cobrado, cobrado) + "\n"
                + context.getString(R.string.notif_resumen_por_cobrar, porCobrar) + "\n"
                + context.getString(R.string.notif_resumen_entregas, entregasDia) + "\n"
                + context.getString(R.string.notif_resumen_atrasados, atrasados);

        NotificationCompat.Builder builder = base(context, CANAL_RESUMEN)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(context.getString(resumirAyer
                        ? R.string.notif_resumen_title_ayer : R.string.notif_resumen_title))
                .setContentText(context.getString(R.string.notif_resumen_pedidos, pedidosDia)
                        + " · " + context.getString(R.string.notif_resumen_por_cobrar, porCobrar))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detalle));

        notificar(context, ID_RESUMEN, builder);
        prefs.edit().putString(PREF_ULTIMO_RESUMEN, RecordatorioUtils.claveDia(ahora)).apply();
    }

    private void enviarRecordatorioCobros(Context context, SharedPreferences prefs,
                                          AppDatabase db, Calendar ahora) {
        int cantidad = db.pedidoDao().countPorCobrar();
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (cantidad <= 0) {
            manager.cancel(ID_COBROS);
            return;
        }

        // Un solo aviso de cobros al día: evita que suene en cada ejecución del trabajo.
        String clave = RecordatorioUtils.claveDedupe("cob", 0, "d", ahora);
        if (yaEnviada(prefs, clave)) {
            return;
        }

        double total = db.pedidoDao().totalPorCobrarGlobal();
        int atrasados = db.pedidoDao().countAtrasados(inicioDelDia(ahora));

        String texto = atrasados > 0
                ? context.getString(R.string.notif_cobros_text_atrasados, cantidad, total, atrasados)
                : context.getString(R.string.notif_cobros_text, cantidad, total);

        NotificationCompat.Builder builder = base(context, CANAL_COBROS)
                .setContentTitle(context.getString(R.string.notif_cobros_title))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(pendingIntentPedidos(context, "por_cobrar", 101))
                .addAction(R.drawable.ic_chat, context.getString(R.string.recordatorio_enviar_varios),
                        pendingIntentPedidos(context, "por_cobrar", 104))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOnlyAlertOnce(true);

        notificar(context, ID_COBROS, builder);
        marcarEnviada(prefs, clave);
    }

    private void enviarRecordatoriosEntregas(Context context, SharedPreferences prefs,
                                             AppDatabase db, Calendar ahora) {
        int diasAntes = Math.max(0, prefs.getInt("dias_entrega", 1));
        Calendar limite = (Calendar) ahora.clone();
        limite.add(Calendar.DAY_OF_YEAR, diasAntes);

        long desde = inicioDelDia(ahora);
        long hasta = finDelDia(limite);
        List<Pedido> entregas = db.pedidoDao().getEntregasPendientesEnRango(desde, hasta);

        for (Pedido pedido : entregas) {
            if (pedido.fechaEntregaEpoch == null) {
                continue;
            }
            Calendar fechaEntrega = Calendar.getInstance();
            fechaEntrega.setTimeInMillis(pedido.fechaEntregaEpoch);
            int dias = RecordatorioUtils.diasEntre(ahora, fechaEntrega);
            if (dias < 0) {
                continue;
            }

            if (dias == 0) {
                String clave = RecordatorioUtils.claveDedupe("ent", pedido.id, "d", fechaEntrega);
                if (!yaEnviada(prefs, clave)) {
                    mostrarEntrega(context, pedido, dias, clave, prefs);
                }
            } else if (diasAntes > 0 && dias == diasAntes) {
                String clave = RecordatorioUtils.claveDedupe("ent", pedido.id, "a", fechaEntrega);
                if (!yaEnviada(prefs, clave)) {
                    mostrarEntrega(context, pedido, dias, clave, prefs);
                }
            }
        }
    }

    private void mostrarEntrega(Context context, Pedido pedido, int dias, String clave,
                                SharedPreferences prefs) {
        String texto;
        if (dias == 0) {
            texto = context.getString(R.string.notif_entrega_hoy, pedido.getClienteNombre());
        } else if (dias == 1) {
            texto = context.getString(R.string.notif_entrega_manana, pedido.getClienteNombre());
        } else {
            texto = context.getString(R.string.notif_entrega_en_dias, pedido.getClienteNombre(), dias);
        }

        NotificationCompat.Builder builder = base(context, CANAL_ENTREGAS)
                .setContentTitle(context.getString(R.string.notif_entrega_title))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(pendingIntentPedidos(context, "pendiente", 102))
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        int id = BASE_ENTREGA + (int) (pedido.id % RANGO_IDS);
        notificar(context, id, builder);
        marcarEnviada(prefs, clave);
    }

    private void enviarRecordatoriosTarjetas(Context context, SharedPreferences prefs,
                                             AppDatabase db, Calendar ahora) {
        int diasCorte = Math.max(0, prefs.getInt("dias_corte", 3));
        int diasPago = Math.max(0, prefs.getInt("dias_pago", 3));

        List<Tarjeta> tarjetas = db.tarjetaDao().getAllTarjetas();
        for (Tarjeta tarjeta : tarjetas) {
            if (tarjeta.getDiaCorte() > 0) {
                procesarEventoTarjeta(context, prefs, tarjeta, tarjeta.getDiaCorte(),
                        diasCorte, true, ahora);
            }
            if (tarjeta.getDiaPago() > 0) {
                procesarEventoTarjeta(context, prefs, tarjeta, tarjeta.getDiaPago(),
                        diasPago, false, ahora);
            }
        }
    }

    private void procesarEventoTarjeta(Context context, SharedPreferences prefs, Tarjeta tarjeta,
                                       int diaEvento, int diasAntes, boolean esCorte,
                                       Calendar ahora) {
        Calendar fechaEvento = RecordatorioUtils.proximaFechaDelMes(diaEvento, ahora);
        int dias = RecordatorioUtils.diasEntre(ahora, fechaEvento);
        String tipo = esCorte ? "cor" : "pag";

        if (dias == 0) {
            String clave = RecordatorioUtils.claveDedupe(tipo, tarjeta.id, "d", fechaEvento);
            if (!yaEnviada(prefs, clave)) {
                mostrarEventoTarjeta(context, tarjeta, 0, esCorte);
                marcarEnviada(prefs, clave);
            }
        } else if (diasAntes > 0 && dias == diasAntes) {
            String clave = RecordatorioUtils.claveDedupe(tipo, tarjeta.id, "a", fechaEvento);
            if (!yaEnviada(prefs, clave)) {
                mostrarEventoTarjeta(context, tarjeta, dias, esCorte);
                marcarEnviada(prefs, clave);
            }
        }
    }

    private void mostrarEventoTarjeta(Context context, Tarjeta tarjeta, int dias, boolean esCorte) {
        String nombre = nombreTarjeta(tarjeta);
        String titulo = context.getString(esCorte ? R.string.notif_corte_title : R.string.notif_pago_title);
        String texto;
        if (dias == 0) {
            texto = context.getString(esCorte ? R.string.notif_corte_hoy : R.string.notif_pago_hoy, nombre);
        } else {
            texto = context.getString(esCorte ? R.string.notif_corte_en_dias : R.string.notif_pago_en_dias,
                    nombre, dias);
        }

        NotificationCompat.Builder builder = base(context, CANAL_TARJETAS)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(pendingIntentTarjetas(context, esCorte ? 201 : 202))
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        int base = esCorte ? BASE_CORTE : BASE_PAGO;
        int id = base + (int) (tarjeta.id % RANGO_IDS);
        notificar(context, id, builder);
    }

    private boolean debeEnviarResumen(SharedPreferences prefs, Calendar ahora) {
        String hoy = RecordatorioUtils.claveDia(ahora);
        if (hoy.equals(prefs.getString(PREF_ULTIMO_RESUMEN, ""))) {
            return false;
        }

        int hora = RecordatorioUtils.normalizarHoraResumen(prefs.getInt("reminder_hour", 9));
        int minuto = Math.max(0, Math.min(59, prefs.getInt("reminder_minute", 0)));

        Calendar objetivo = (Calendar) ahora.clone();
        objetivo.set(Calendar.HOUR_OF_DAY, hora);
        objetivo.set(Calendar.MINUTE, minuto);
        objetivo.set(Calendar.SECOND, 0);
        objetivo.set(Calendar.MILLISECOND, 0);

        return !ahora.before(objetivo);
    }

    private long inicioDelDia(Calendar fecha) {
        Calendar inicio = (Calendar) fecha.clone();
        inicio.set(Calendar.HOUR_OF_DAY, 0);
        inicio.set(Calendar.MINUTE, 0);
        inicio.set(Calendar.SECOND, 0);
        inicio.set(Calendar.MILLISECOND, 0);
        return inicio.getTimeInMillis();
    }

    private long finDelDia(Calendar fecha) {
        Calendar fin = (Calendar) fecha.clone();
        fin.set(Calendar.HOUR_OF_DAY, 23);
        fin.set(Calendar.MINUTE, 59);
        fin.set(Calendar.SECOND, 59);
        fin.set(Calendar.MILLISECOND, 999);
        return fin.getTimeInMillis();
    }

    private boolean yaEnviada(SharedPreferences prefs, String clave) {
        Set<String> claves = prefs.getStringSet(PREF_DEDUPE, Collections.emptySet());
        return claves != null && claves.contains(clave);
    }

    private void marcarEnviada(SharedPreferences prefs, String clave) {
        Set<String> claves = new HashSet<>(prefs.getStringSet(PREF_DEDUPE, Collections.emptySet()));
        claves.add(clave);
        prefs.edit().putStringSet(PREF_DEDUPE, claves).apply();
    }

    private void purgarDedupe(SharedPreferences prefs, Calendar ahora) {
        Set<String> claves = new HashSet<>(prefs.getStringSet(PREF_DEDUPE, Collections.emptySet()));
        if (claves.isEmpty()) {
            return;
        }

        Calendar limite = (Calendar) ahora.clone();
        limite.add(Calendar.DAY_OF_YEAR, -8);
        String fechaLimite = RecordatorioUtils.claveDia(limite);

        boolean cambio = false;
        java.util.Iterator<String> iterator = claves.iterator();
        while (iterator.hasNext()) {
            String clave = iterator.next();
            if (clave == null || clave.length() < 8) {
                iterator.remove();
                cambio = true;
                continue;
            }
            String fecha = clave.substring(clave.length() - 8);
            if (fecha.compareTo(fechaLimite) < 0) {
                iterator.remove();
                cambio = true;
            }
        }

        if (cambio) {
            prefs.edit().putStringSet(PREF_DEDUPE, claves).apply();
        }
    }

    private NotificationCompat.Builder base(Context context, String canal) {
        return new NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.drawable.ic_notifications)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntentMain(context))
                .setAutoCancel(true);
    }

    private PendingIntent pendingIntentMain(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent pendingIntentPedidos(Context context, String filtro, int requestCode) {
        Intent intent = new Intent(context, ListaPedidosActivity.class);
        intent.putExtra("filtro_estado", filtro);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent pendingIntentTarjetas(Context context, int requestCode) {
        Intent intent = new Intent(context, TarjetasActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressLint("MissingPermission")
    private void notificar(Context context, int id, NotificationCompat.Builder builder) {
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }

    private String nombreTarjeta(Tarjeta tarjeta) {
        if (tarjeta.getAlias() != null && !tarjeta.getAlias().isEmpty()) {
            return tarjeta.getAlias();
        }
        if (tarjeta.getBanco() != null && !tarjeta.getBanco().isEmpty()) {
            return tarjeta.getBanco();
        }
        return getApplicationContext().getString(R.string.tarjeta);
    }

    public static boolean puedeNotificar(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static boolean enviarNotificacionPrueba(Context context) {
        crearCanales(context);
        if (!puedeNotificar(context)) {
            return false;
        }

        String texto = context.getString(R.string.notif_prueba_text);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_COBROS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.notif_prueba_title))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(PendingIntent.getActivity(context, 103,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(ID_PRUEBA, builder.build());
        return true;
    }

    public static void crearCanales(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        for (String obsoleto : CANALES_OBSOLETOS) {
            manager.deleteNotificationChannel(obsoleto);
        }

        crearCanal(manager, CANAL_COBROS,
                context.getString(R.string.notif_canal_cobros_nombre),
                context.getString(R.string.notif_canal_cobros_desc),
                NotificationManager.IMPORTANCE_HIGH);
        crearCanal(manager, CANAL_ENTREGAS,
                context.getString(R.string.notif_canal_entregas_nombre),
                context.getString(R.string.notif_canal_entregas_desc),
                NotificationManager.IMPORTANCE_HIGH);
        crearCanal(manager, CANAL_TARJETAS,
                context.getString(R.string.notif_canal_tarjetas_nombre),
                context.getString(R.string.notif_canal_tarjetas_desc),
                NotificationManager.IMPORTANCE_HIGH);
        crearCanal(manager, CANAL_RESUMEN,
                context.getString(R.string.notif_canal_resumen_nombre),
                context.getString(R.string.notif_canal_resumen_desc),
                NotificationManager.IMPORTANCE_DEFAULT);
        crearCanal(manager, CANAL_FINANZAS,
                context.getString(R.string.notif_canal_finanzas_nombre),
                context.getString(R.string.notif_canal_finanzas_desc),
                NotificationManager.IMPORTANCE_DEFAULT);
    }

    /**
     * Avisa cuando un presupuesto llega al 80 % y cuando se supera. Cada aviso se envía una sola
     * vez por categoría y mes (el registro se reinicia al cambiar de mes).
     */
    private void enviarAvisosPresupuesto(Context context, SharedPreferences prefs, AppDatabase db, Calendar ahora) {
        String mes = String.format(Locale.US, "%04d%02d", ahora.get(Calendar.YEAR), ahora.get(Calendar.MONTH) + 1);
        Set<String> enviados = new HashSet<>(prefs.getStringSet(PREF_AVISOS_PRESUPUESTO, Collections.emptySet()));
        // Descarta avisos de meses anteriores
        enviados.removeIf(clave -> !clave.startsWith(mes + "_"));

        boolean cambio = false;
        for (com.example.gestiondecompras.servicios.FinanzasServicio.EstadoPresupuesto e :
                new com.example.gestiondecompras.servicios.FinanzasServicio(db).presupuestos(ahora.getTimeInMillis())) {
            if (e.estado == com.example.gestiondecompras.utils.finanzas.CalculosFinancieros.EstadoPresupuesto.NORMAL) {
                continue;
            }
            boolean excedido = e.estado == com.example.gestiondecompras.utils.finanzas.CalculosFinancieros.EstadoPresupuesto.EXCEDIDO;
            String clave = mes + "_" + e.categoria.id + (excedido ? "_x" : "_a");
            if (enviados.contains(clave)) {
                continue;
            }
            String moneda = "RD$ %,.2f";
            String titulo = context.getString(excedido ? R.string.notif_presupuesto_excedido_title
                    : R.string.notif_presupuesto_alerta_title);
            String texto = excedido
                    ? context.getString(R.string.notif_presupuesto_excedido_text, e.categoria.nombre,
                    String.format(Locale.getDefault(), moneda, e.gastado), String.format(Locale.getDefault(), moneda, e.limite))
                    : context.getString(R.string.notif_presupuesto_alerta_text,
                    String.valueOf(Math.round(e.porcentaje)), e.categoria.nombre);

            Intent intent = new Intent(context, com.example.gestiondecompras.activities.PresupuestosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(context, 105, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder builder = base(context, CANAL_FINANZAS)
                    .setContentTitle(titulo)
                    .setContentText(texto)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                    .setContentIntent(pi)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER);
            notificar(context, BASE_PRESUPUESTO + (int) (e.categoria.id % RANGO_IDS), builder);
            enviados.add(clave);
            cambio = true;
        }
        if (cambio || !enviados.equals(prefs.getStringSet(PREF_AVISOS_PRESUPUESTO, Collections.emptySet()))) {
            prefs.edit().putStringSet(PREF_AVISOS_PRESUPUESTO, enviados).apply();
        }
    }

    private static void crearCanal(NotificationManager manager, String id, String nombre,
                                   String descripcion, int importancia) {
        NotificationChannel canal = new NotificationChannel(id, nombre, importancia);
        canal.setDescription(descripcion);
        manager.createNotificationChannel(canal);
    }
}
