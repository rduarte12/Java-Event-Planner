package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import model.Event;

// Painel base reutilizado pelas Visoes Diaria e Semanal: uma lista vertical
// rolavel onde a gente vai empilhando cabecalhos de secao e linhas de evento.
// So cuida da parte visual; quem decide o que listar sao as subclasses.
public class EventListView extends JPanel {

    private final JPanel content;

    public EventListView() {
        super(new BorderLayout());
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // limpa a lista antes de repopular (chamado a cada troca de dia)
    protected void clear() {
        content.removeAll();
    }

    // cabecalho de secao (ex: a data, na visao semanal)
    protected void addHeader(String text) {
        JLabel h = new JLabel(text);
        h.setFont(h.getFont().deriveFont(Font.BOLD, 13f));
        h.setBorder(BorderFactory.createEmptyBorder(8, 4, 2, 4));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(h);
    }

    protected void addEvent(Event e) {
        Component row = EventRow.create(e);
        ((JPanel) row).setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(row);
    }

    // mensagem cinza pra quando nao tem evento no periodo
    protected void addNotice(String text) {
        JLabel n = new JLabel(text);
        n.setForeground(Color.GRAY);
        n.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(n);
    }

    // empurra tudo pro topo e redesenha
    protected void finish() {
        content.add(Box.createVerticalGlue());
        content.revalidate();
        content.repaint();
    }
}
