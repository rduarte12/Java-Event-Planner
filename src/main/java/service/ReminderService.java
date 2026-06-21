package service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.Event;
import model.Reminder;
import persistence.EventDao;
import persistence.ReminderDao;

// Calcula os alertas que aparecem logo apos o login (RF13/RF14/RF15).
// Tudo sincrono, rodando na EDT, em cima da colecao em memoria - sem thread.
public class ReminderService {

    private final EventDao eventDao;
    private final ReminderDao reminderDao;

    public ReminderService(EventDao eventDao, ReminderDao reminderDao) {
        this.eventDao = eventDao;
        this.reminderDao = reminderDao;
    }

    // versao usada na pratica: janela a partir do "agora"
    public List<Event> computeStartupAlerts() {
        return computeStartupAlerts(LocalDateTime.now());
    }

    // recebe o "agora" por parametro pra facilitar teste.
    // junta Tipo A (lead time) e Tipo B (iminencia) sem repetir evento.
    public List<Event> computeStartupAlerts(LocalDateTime now) {
        LocalDateTime windowEnd = now.plusHours(24);

        // dedup por id do evento, preservando ordem de insercao
        Map<String, Event> alertas = new LinkedHashMap<>();

        // Tipo B (RF14): qualquer evento cujo eventDateTime caia em [now, now+24h]
        for (Event e : eventDao.getAll()) {
            LocalDateTime dt = dateTimeOf(e);
            if (dt != null && inWindow(dt, now, windowEnd)) {
                alertas.put(e.getId(), e);
            }
        }

        // Tipo A (RF13): pra cada reminder, (eventDateTime - leadTime) em [now, now+24h]
        Map<String, Event> porId = indexById();
        for (Reminder r : reminderDao.getAll()) {
            Event e = porId.get(r.getEventId());
            if (e == null) {
                continue; // reminder orfao (evento nao existe mais)
            }
            LocalDateTime dt = dateTimeOf(e);
            if (dt == null) {
                continue;
            }
            LocalDateTime disparo = dt.minusMinutes(r.getLeadTimeMinutes());
            if (inWindow(disparo, now, windowEnd)) {
                alertas.put(e.getId(), e);
            }
        }

        // ordena por quando o evento acontece
        List<Event> resultado = new ArrayList<>(alertas.values());
        resultado.sort(Comparator.comparing(ReminderService::dateTimeOf,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return resultado;
    }

    // indexa os eventos por id pra achar o evento de um reminder rapido
    private Map<String, Event> indexById() {
        Map<String, Event> idx = new HashMap<>();
        for (Event e : eventDao.getAll()) {
            idx.put(e.getId(), e);
        }
        return idx;
    }

    // [start, end] inclusivo nas duas pontas
    private static boolean inWindow(LocalDateTime t, LocalDateTime start, LocalDateTime end) {
        return !t.isBefore(start) && !t.isAfter(end);
    }

    // combina date+time com seguranca (evento sem data/hora vira null)
    private static LocalDateTime dateTimeOf(Event e) {
        if (e.getDate() == null || e.getTime() == null) {
            return null;
        }
        return e.eventDateTime();
    }
}
