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

    private static final String PREF_DEDUPE = "notif_dedupe";
    private static final String PREF_ULTIMO_RESUMEN = "ultimo_resumen_dia";

    private static final int ID_COBROS = 1001;
    private static final int ID_RESUMEN = 1002;
    private static final int ID_PRUEBA = 1003;
    private static final int BASE_ENTREGA = 2000;
    private static final int BASE_CORTE = 3000;
    private static final int BASE_PAGO = 4000;

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
            enviarRecordatorioCobros(context, db, ahora);
            enviarRecordatoriosEntregas(context, prefs, db, ahora);
            enviarRecordatoriosTarjetas(context, prefs, db, ahora);
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

        long inicioHoy = inicioDelDia(ahora);
        long finHoy = finDelDia(ahora);

        int pedidosHoy = db.pedidoDao().countPedidosEnRango(inicioHoy, finHoy);
        double ventas = db.pedidoDao().totalVentasEnRango(inicioHoy, finHoy);
        double ganancia = db.pedidoDao().totalGananciaEnRango(inicioHoy, finHoy);
        double cobrado = db.abonoDao().getTotalRango(inicioHoy, finHoy);
        double porCobrar = db.pedidoDao().totalPorCobrarGlobal();
        int entregasHoy = db.pedidoDao().countEntregasEnRango(inicioHoy, finHoy);
        int atrasados = db.pedidoDao().countAtrasados(inicioHoy);

        String detalle = context.getString(R.string.notif_resumen_pedidos, pedidosHoy) + "\n"
                + context.getString(R.string.notif_resumen_ventas, ventas) + "\n"
                + context.getString(R.string.notif_resumen_ganancia, ganancia) + "\n"
                + context.getString(R.string.notif_resumen_cobrado, cobrado) + "\n"
                + context.getString(R.string.notif_resumen_por_cobrar, porCobrar) + "\n"
                + context.getString(R.string.notif_resumen_entregas, entregasHoy) + "\n"
                + context.getString(R.string.notif_resumen_atrasados, atrasados);

        NotificationCompat.Builder builder = base(context, CANAL_RESUMEN)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(context.getString(R.string.notif_resumen_title))
                .setContentText(context.getString(R.string.notif_resumen_pedidos, pedidosHoy)
                        + " · " + context.getString(R.string.notif_resumen_por_cobrar, porCobrar))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detalle));

        notificar(context, ID_RESUMEN, builder);
        prefs.edit().putString(PREF_ULTIMO_RESUMEN, RecordatorioUtils.claveDia(ahora)).apply();
    }

    private void enviarRecordatorioCobros(Context context, AppDatabase db, Calendar ahora) {
        int cantidad = db.pedidoDao().countPorCobrar();
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (cantidad <= 0) {
            manager.cancel(ID_COBROS);
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
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        notificar(context, ID_COBROS, builder);
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

        int id = BASE_ENTREGA + (int) (pedido.id % 100000);
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
        int id = base + (int) (tarjeta.id % 100000);
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
        return "Tarjeta";
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
    }

    private static void crearCanal(NotificationManager manager, String id, String nombre,
                                   String descripcion, int importancia) {
        NotificationChannel canal = new NotificationChannel(id, nombre, importancia);
        canal.setDescription(descripcion);
        manager.createNotificationChannel(canal);
    }
}
