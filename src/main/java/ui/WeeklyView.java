package ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import model.Event;
import persistence.EventDao;

// Visao Semanal (RF07): lista consolidada dos eventos do dia selecionado (D)
// ate D+6, agrupada por data e ordenada por data e horario. Filtragem em
// memoria (RNF02), atualizada de forma sincrona na EDT.
public class WeeklyView extends EventListView {

    private static final DateTimeFormatter RANGE_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DAY_FMT =
            DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale.of("pt", "BR"));

    private final EventDao eventDao;

    public WeeklyView(EventDao eventDao) {
        this.eventDao = eventDao;
        showWeek(LocalDate.now());
    }

    // mostra a janela de 7 dias a partir de 'start' (o dia clicado no calendario)
    public void showWeek(LocalDate start) {
        clear();
        LocalDate end = start.plusDays(6);
        addHeader("Semana: " + start.format(RANGE_FMT) + " a " + end.format(RANGE_FMT));

        List<Event> daSemana = eventDao.getAll().stream()
                .filter(e -> dentroDoIntervalo(e.getDate(), start, end))
                .sorted(Comparator
                        .comparing(Event::getDate)
                        .thenComparing(Event::getTime,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (daSemana.isEmpty()) {
            addNotice("Nenhum evento nos proximos 7 dias.");
        } else {
            // agrupa por data: insere um cabecalho cada vez que a data muda
            LocalDate atual = null;
            for (Event e : daSemana) {
                if (!e.getDate().equals(atual)) {
                    atual = e.getDate();
                    addHeader(capitalize(atual.format(DAY_FMT)));
                }
                addEvent(e);
            }
        }
        finish();
    }

    // true se a data cai no intervalo [start, end] (inclusivo nas duas pontas)
    private static boolean dentroDoIntervalo(LocalDate d, LocalDate start, LocalDate end) {
        return d != null && !d.isBefore(start) && !d.isAfter(end);
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
