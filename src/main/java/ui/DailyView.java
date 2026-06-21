package ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import model.Event;
import persistence.EventDao;

// Visao Diaria (RF06): lista os eventos do dia selecionado no calendario,
// em ordem de horario. Filtragem 100% sobre o ArrayList em memoria (RNF02).
public class DailyView extends EventListView {

    private static final DateTimeFormatter DAY_FMT =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.of("pt", "BR"));

    private final EventDao eventDao;

    public DailyView(EventDao eventDao) {
        this.eventDao = eventDao;
        showDay(LocalDate.now());
    }

    // chamado pelo listener do calendario sempre que o dia muda.
    // tudo sincrono na EDT - resposta imediata (US03).
    public void showDay(LocalDate date) {
        clear();
        addHeader("Eventos de " + capitalize(date.format(DAY_FMT)));

        List<Event> doDia = eventDao.getAll().stream()
                .filter(e -> date.equals(e.getDate()))
                .sorted(Comparator.comparing(Event::getTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (doDia.isEmpty()) {
            addNotice("Nenhum evento neste dia.");
        } else {
            for (Event e : doDia) {
                addEvent(e);
            }
        }
        finish();
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
