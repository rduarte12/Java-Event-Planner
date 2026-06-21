package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import model.Event;
import persistence.EventDao;

// Busca modal (RF12). Fica por cima da tela principal e filtra os eventos em
// memoria em tempo real (a cada tecla) pelos campos titulo, descricao e local.
// Os resultados sao somente leitura: nao tem botao de editar nem deletar aqui.
public class SearchDialog extends JDialog {

    private final EventDao eventDao;
    private final JTextField queryField = new JTextField(28);
    private final JPanel results = new JPanel();

    public SearchDialog(Frame owner, EventDao eventDao) {
        super(owner, "Buscar eventos", true);
        this.eventDao = eventDao;

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        top.add(new JLabel("Palavra-chave:"), BorderLayout.WEST);
        top.add(queryField, BorderLayout.CENTER);

        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        results.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(results);
        scroll.setPreferredSize(new Dimension(560, 320));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // busca em tempo real: qualquer mudanca no texto refiltra
        queryField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JPanel content = new JPanel(new BorderLayout());
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        setContentPane(content);

        refresh(); // estado inicial (campo vazio)
        pack();
        setLocationRelativeTo(owner);
    }

    // refiltra os eventos conforme o texto e remonta a lista de resultados
    private void refresh() {
        results.removeAll();
        String q = queryField.getText().trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            addNotice("Digite uma palavra-chave para buscar...");
        } else {
            int achados = 0;
            for (Event e : eventDao.getAll()) {
                if (matches(e, q)) {
                    Component row = EventRow.create(e);
                    ((JPanel) row).setAlignmentX(Component.LEFT_ALIGNMENT);
                    results.add(row);
                    achados++;
                }
            }
            if (achados == 0) {
                addNotice("Nenhum evento encontrado.");
            }
        }
        results.revalidate();
        results.repaint();
    }

    // confere se a palavra aparece no titulo, descricao ou local
    private static boolean matches(Event e, String q) {
        return contains(e.getTitle(), q)
                || contains(e.getDescription(), q)
                || contains(e.getLocation(), q);
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(q);
    }

    private void addNotice(String text) {
        JLabel n = new JLabel(text);
        n.setForeground(Color.GRAY);
        n.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        results.add(n);
    }
}
