package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import model.Event;
import persistence.EventDao;

// Calendario mensal da esquerda (RF04). Mostra o mes corrente numa grade de
// 7 colunas, com navegacao entre meses, destaque nos dias que tem evento e
// avisa por callback qual dia foi clicado pras abas reagirem depois.
// Le os eventos da colecao em memoria do EventDao (RNF02) - nao toca disco.
public class CalendarPanel extends JPanel {

    // callback simples pra avisar a janela principal qual dia foi selecionado
    public interface DaySelectionListener {
        void onDaySelected(LocalDate date);
    }

    private static final String[] WEEKDAYS = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab"};
    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("pt", "BR"));
    private static final Color EVENT_BG = new Color(255, 235, 150);
    private static final Color SELECTED_BORDER = new Color(0, 120, 215);

    private final EventDao eventDao;
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private DaySelectionListener listener;

    private final JLabel monthLabel;
    private final JPanel gridPanel;

    public CalendarPanel(EventDao eventDao) {
        super(new BorderLayout());
        this.eventDao = eventDao;
        this.currentMonth = YearMonth.now();
        this.selectedDate = LocalDate.now();

        // cabecalho: < mes/ano >
        JButton prev = new JButton("<");
        JButton next = new JButton(">");
        prev.addActionListener(e -> changeMonth(-1));
        next.addActionListener(e -> changeMonth(1));
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));

        JPanel header = new JPanel(new BorderLayout());
        header.add(prev, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);

        // 0 linhas = quantas precisar; 7 colunas (uma por dia da semana)
        gridPanel = new JPanel(new GridLayout(0, 7, 2, 2));

        add(header, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);

        rebuild();
    }

    public void setDaySelectionListener(DaySelectionListener l) {
        this.listener = l;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    // chamado de fora (ex: depois de criar/excluir evento) pra repintar o
    // destaque dos dias sem precisar reabrir nada.
    public void refresh() {
        rebuild();
    }

    private void changeMonth(int delta) {
        currentMonth = currentMonth.plusMonths(delta);
        rebuild();
    }

    // remonta a grade inteira: recomputa quais dias tem evento e recria os botoes.
    private void rebuild() {
        monthLabel.setText(capitalize(currentMonth.format(MONTH_FMT)));
        Set<Integer> daysWithEvents = daysWithEvents();

        gridPanel.removeAll();

        // linha de cabecalho com os nomes dos dias da semana
        for (String wd : WEEKDAYS) {
            JLabel l = new JLabel(wd, SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            gridPanel.add(l);
        }

        // celulas vazias ate cair no dia da semana do dia 1 (semana comeca no domingo)
        LocalDate first = currentMonth.atDay(1);
        int lead = first.getDayOfWeek().getValue() % 7; // domingo=0, segunda=1, ...
        for (int i = 0; i < lead; i++) {
            gridPanel.add(new JLabel(""));
        }

        int len = currentMonth.lengthOfMonth();
        for (int day = 1; day <= len; day++) {
            LocalDate date = currentMonth.atDay(day);
            JButton b = new JButton(String.valueOf(day));
            b.setMargin(new Insets(2, 2, 2, 2));

            if (daysWithEvents.contains(day)) {
                b.setBackground(EVENT_BG); // RF04: destaca dia com evento
                b.setOpaque(true);
                b.setFont(b.getFont().deriveFont(Font.BOLD));
            }
            if (date.equals(selectedDate)) {
                b.setBorder(BorderFactory.createLineBorder(SELECTED_BORDER, 2));
            }
            b.addActionListener(e -> selectDay(date));
            gridPanel.add(b);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void selectDay(LocalDate date) {
        selectedDate = date;
        rebuild(); // atualiza o destaque da selecao
        if (listener != null) {
            listener.onDaySelected(date);
        }
    }

    // dias do mes corrente que tem pelo menos um evento (consulta em memoria)
    private Set<Integer> daysWithEvents() {
        Set<Integer> days = new HashSet<>();
        for (Event e : eventDao.getAll()) {
            LocalDate d = e.getDate();
            if (d != null && YearMonth.from(d).equals(currentMonth)) {
                days.add(d.getDayOfMonth());
            }
        }
        return days;
    }

    // deixa a primeira letra do mes maiuscula ("junho 2026" -> "Junho 2026")
    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
