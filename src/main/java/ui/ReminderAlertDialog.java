package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import model.Event;

// Janela unica de alertas (RF15) mostrada logo apos o login e ANTES da tela
// principal. Lista, sem duplicatas, os eventos que o ReminderService juntou
// dos Tipos A (lead time) e B (iminencia). E modal: bloqueia ate o OK.
public class ReminderAlertDialog extends JDialog {

    public ReminderAlertDialog(Frame owner, List<Event> alerts) {
        super(owner, "Lembretes - proximas 24h", true);

        JLabel header = new JLabel("Voce tem " + alerts.size()
                + " compromisso(s) nas proximas 24 horas:");
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        for (Event e : alerts) {
            Component row = EventRow.create(e);
            ((JPanel) row).setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(row);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(520, 240));

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(ok);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(ok);

        JPanel content = new JPanel(new BorderLayout());
        content.add(header, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);

        pack();
        setLocationRelativeTo(owner);
    }
}
