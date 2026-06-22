package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Category;
import model.Event;
import model.Reminder;
import persistence.CsvUtils;
import service.AppState;

public class EventFormDialog extends JDialog {

    private final AppState state;
    private final Runnable onSaved;
    private Event editingEvent = null; // null = Criação, preenchido = Edição

    // Declaração dos componentes visuais restaurada
    private final JTextField titleField = new JTextField(20);
    private final JTextField dateField = new JTextField(20);
    private final JTextField timeField = new JTextField(20);
    private final JTextField durationField = new JTextField(20);
    private final JTextField locationField = new JTextField(20);
    private final JTextArea descriptionArea = new JTextArea(3, 20);
    private final JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());

    private final JTextField leadField = new JTextField(8);
    private final DefaultListModel<Integer> leadModel = new DefaultListModel<>();
    private final JList<Integer> leadList = new JList<>(leadModel);

    // Construtor antigo para manter compatibilidade com a tela principal (Criação)
    public EventFormDialog(Frame owner, AppState state, LocalDate initialDate, Runnable onSaved) {
        this(owner, state, initialDate, null, onSaved);
    }

    // Novo construtor híbrido (Criação e Edição)
    public EventFormDialog(Frame owner, AppState state, LocalDate initialDate, Event editingEvent, Runnable onSaved) {
        super(owner, editingEvent == null ? "Novo Evento" : "Editar Evento", true);
        this.state = state;
        this.editingEvent = editingEvent;
        this.onSaved = onSaved;

        if (editingEvent != null) {
            titleField.setText(editingEvent.getTitle());
            dateField.setText(editingEvent.getDate().toString());
            timeField.setText(editingEvent.getTime().toString());
            durationField.setText(String.valueOf(editingEvent.getDuration()));
            locationField.setText(editingEvent.getLocation());
            descriptionArea.setText(editingEvent.getDescription());
            categoryCombo.setSelectedItem(editingEvent.getCategory());

            state.getReminderDao().getAll().stream()
                .filter(r -> r.getEventId().equals(editingEvent.getId()))
                .forEach(r -> leadModel.addElement(r.getLeadTimeMinutes()));
        } else {
            dateField.setText(initialDate.toString());
            timeField.setText("09:00");
            durationField.setText("60");
        }

        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(categoryLabel((Category) value));
                return this;
            }
        });

        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildContent() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        int row = 0;
        addRow(form, row++, "Titulo*:", titleField);
        addRow(form, row++, "Data* (AAAA-MM-DD):", dateField);
        addRow(form, row++, "Hora* (HH:mm):", timeField);
        addRow(form, row++, "Duracao* (min):", durationField);
        addRow(form, row++, "Local:", locationField);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        addRow(form, row++, "Descricao:", new JScrollPane(descriptionArea));

        addRow(form, row++, "Categoria*:", categoryCombo);
        addRow(form, row++, "Lembretes (min antes):", buildLeadPanel());

        JButton save = new JButton("Salvar");
        JButton cancel = new JButton("Cancelar");
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);

        JPanel buttons = new JPanel();
        buttons.add(save);
        buttons.add(cancel);

        JPanel content = new JPanel(new BorderLayout());
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }

    private JPanel buildLeadPanel() {
        JButton add = new JButton("Adicionar");
        add.addActionListener(e -> addLead());
        JButton remove = new JButton("Remover");
        remove.addActionListener(e -> removeLead());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        top.add(leadField);
        top.add(add);
        top.add(remove);

        leadList.setVisibleRowCount(3);
        JScrollPane listScroll = new JScrollPane(leadList);
        listScroll.setPreferredSize(new Dimension(160, 60));

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(top, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        return panel;
    }

    private void addLead() {
        Integer lead = parseLead(leadField.getText().trim());
        if (lead == null) {
            return; 
        }
        if (!leadModel.contains(lead)) {
            leadModel.addElement(lead);
        }
        leadField.setText("");
    }

    private void removeLead() {
        int i = leadList.getSelectedIndex();
        if (i >= 0) {
            leadModel.remove(i);
        }
    }

    private Integer parseLead(String text) {
        if (text.isEmpty()) {
            error("Informe a antecedencia do lembrete em minutos.");
            return null;
        }
        int v;
        try {
            v = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            error("Antecedencia do lembrete invalida. Informe minutos (numero inteiro).");
            return null;
        }
        if (v < 0) {
            error("A antecedencia do lembrete nao pode ser negativa.");
            return null;
        }
        return v;
    }

    private void addRow(JPanel form, int row, String label, Component field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = row;
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(4, 4, 4, 8);
        form.add(new JLabel(label), lc);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1;
        fc.gridy = row;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(4, 0, 4, 4);
        form.add(field, fc);
    }

    private void onSave() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            error("O titulo nao pode ser vazio.");
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (RuntimeException ex) {
            error("Data invalida. Use o formato AAAA-MM-DD.");
            return;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(timeField.getText().trim());
        } catch (RuntimeException ex) {
            error("Hora invalida. Use o formato HH:mm (24h).");
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationField.getText().trim());
        } catch (NumberFormatException ex) {
            error("Duracao invalida. Informe um numero inteiro de minutos.");
            return;
        }
        if (duration <= 0) {
            error("A duracao deve ser maior que zero.");
            return;
        }

        Set<Integer> leads = new LinkedHashSet<>();
        for (int i = 0; i < leadModel.size(); i++) {
            leads.add(leadModel.get(i));
        }
        String pending = leadField.getText().trim();
        if (!pending.isEmpty()) {
            Integer lead = parseLead(pending);
            if (lead == null) {
                return; 
            }
            leads.add(lead);
        }

        if (editingEvent != null) {
            editingEvent.setTitle(title);
            editingEvent.setDate(date);
            editingEvent.setTime(time);
            editingEvent.setDuration(duration);
            editingEvent.setLocation(locationField.getText().trim());
            editingEvent.setDescription(descriptionArea.getText().trim());
            editingEvent.setCategory((Category) categoryCombo.getSelectedItem());

            state.getEventDao().rewriteAll();

            state.getReminderDao().getAll().removeIf(r -> r.getEventId().equals(editingEvent.getId()));
            for (int lead : leads) {
                Reminder r = new Reminder();
                r.setReminderId(CsvUtils.newUuid());
                r.setEventId(editingEvent.getId());
                r.setLeadTimeMinutes(lead);
                state.getReminderDao().getAll().add(r);
            }
            
            state.getReminderDao().rewriteAll();

        } else {
            Event e = new Event();
            e.setId(CsvUtils.newUuid());
            e.setTitle(title);
            e.setDate(date);
            e.setTime(time);
            e.setDuration(duration);
            e.setLocation(locationField.getText().trim());
            e.setDescription(descriptionArea.getText().trim());
            e.setCategory((Category) categoryCombo.getSelectedItem());
            state.stampOwner(e); 
            e.setRecurrenceId(null); 

            state.getEventDao().append(e);

            for (int lead : leads) {
                Reminder r = new Reminder();
                r.setReminderId(CsvUtils.newUuid());
                r.setEventId(e.getId());
                r.setLeadTimeMinutes(lead);
                state.getReminderDao().append(r);
            }
        }

        if (onSaved != null) {
            onSaved.run();
        }
        dispose();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro de validacao", JOptionPane.ERROR_MESSAGE);
    }

    private static String categoryLabel(Category c) {
        if (c == null) {
            return "";
        }
        return switch (c) {
            case MEETING -> "Meeting";
            case BIRTHDAY -> "Birthday";
            case APPOINTMENT -> "Appointment";
        };
    }
}