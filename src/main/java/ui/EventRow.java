package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import model.Category;
import model.Event;

// Monta a "linha" visual de um evento, usada pelas Visoes Diaria e Semanal.
// Mostra titulo, horario, duracao, categoria, local e dono. Ja deixa um
// espaco reservado a esquerda pro marcador de serie (preenchido no Prompt 10).
final class EventRow {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private EventRow() {
        // so fabrica de componentes
    }

    static JComponent create(Event e) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));

        // espaco reservado pro icone/marcador de serie (Prompt 10).
        // por enquanto vai vazio, mas a largura ja fica guardada.
        JLabel marker = new JLabel(" ");
        marker.setPreferredSize(new Dimension(14, 1));
        row.add(marker, BorderLayout.WEST);

        row.add(new JLabel(text(e)), BorderLayout.CENTER);

        // evita que o BoxLayout estique a linha na vertical
        Dimension pref = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        return row;
    }

    // texto da linha em HTML so pra deixar o titulo em negrito e o resto cinza
    private static String text(Event e) {
        String time = e.getTime() != null ? e.getTime().format(TIME_FMT) : "--:--";
        StringBuilder details = new StringBuilder();
        details.append(time)
                .append(" · ").append(e.getDuration()).append(" min")
                .append(" · ").append(categoryLabel(e.getCategory()));
        if (e.getLocation() != null && !e.getLocation().isBlank()) {
            details.append(" · ").append(htmlEscape(e.getLocation()));
        }
        details.append(" · Dono: ").append(htmlEscape(nz(e.getOwner())));

        return "<html><b>" + htmlEscape(nz(e.getTitle())) + "</b> &nbsp;"
                + "<font color='gray'>" + details + "</font></html>";
    }

    private static String categoryLabel(Category c) {
        if (c == null) {
            return "-";
        }
        return switch (c) {
            case MEETING -> "Reuniao";
            case BIRTHDAY -> "Aniversario";
            case APPOINTMENT -> "Compromisso";
        };
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // escapa o basico pra nao quebrar o HTML do JLabel
    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
