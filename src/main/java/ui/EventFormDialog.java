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
import javax.swing.JCheckBox;
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
    private Event editingEvent = null;

    private final JTextField titleField = new JTextField(20);
    private final JTextField dateField = new JTextField(20);
    private final JTextField timeField = new JTextField(20);
    private final JTextField durationField = new JTextField(20);
    private final JTextField locationField = new JTextField(20);
    private final JTextArea descriptionArea = new JTextArea(3, 20);
    private final JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());

    // Componentes de Recorrência
    private final JCheckBox repeatCheck = new JCheckBox("Repetir evento");
    private final JComboBox<String> freqCombo = new JComboBox<>(new String[]{"Diariamente", "Semanalmente", "Mensalmente"});
    private final JTextField occurrencesField = new JTextField("10", 5);
    private final JPanel recurrencePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

    private final JTextField leadField = new JTextField(8);
    private final DefaultListModel<Integer> leadModel = new DefaultListModel<>();
    private final JList<Integer> leadList = new JList<>(leadModel);

    public EventFormDialog(Frame owner, AppState state, LocalDate initialDate, Runnable onSaved) {
        this(owner, state, initialDate, null, onSaved);
    }

    public EventFormDialog(Frame owner, AppState state, LocalDate initialDate, Event editingEvent, Runnable onSaved) {
        super(owner, editingEvent == null ? "Novo Evento" : "Editar Evento", true);
        this.state = state;
        this.editingEvent = editingEvent;
        this.onSaved = onSaved;

        recurrencePanel.add(new JLabel("Frequência:"));
        recurrencePanel.add(freqCombo);
        recurrencePanel.add(new JLabel("Repetir por:"));
        recurrencePanel.add(occurrencesField);
        recurrencePanel.add(new JLabel("vezes (2-50)"));

        freqCombo.setEnabled(false);
        occurrencesField.setEnabled(false);

        repeatCheck.addActionListener(e -> {
            boolean isChecked = repeatCheck.isSelected();
            freqCombo.setEnabled(isChecked);
            occurrencesField.setEnabled(isChecked);
        });

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

            repeatCheck.setEnabled(false);
            freqCombo.setEnabled(false);
            occurrencesField.setEnabled(false);
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
        
        addRow(form, row++, "", repeatCheck);
        addRow(form, row++, "", recurrencePanel);
        
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
        if (lead == null) return; 
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
            if (lead == null) return; 
            leads.add(lead);
        }

        if (editingEvent != null) {
            boolean updateSeries = false;
            if (editingEvent.getRecurrenceId() != null) {
                String[] options = {"Somente esta", "Esta e as futuras", "Cancelar"};
                int choice = JOptionPane.showOptionDialog(this,
                        "Este evento faz parte de uma série recorrente.\nDeseja aplicar a alteração somente a esta ocorrência ou a todas as futuras?",
                        "Editar Série Recorrente",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                
                if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return;
                if (choice == 1) updateSeries = true;
            }

            // === PROTEÇÃO CONTRA CHOQUE DE HORÁRIO NA EDIÇÃO ===
            if (updateSeries) {
                String recId = editingEvent.getRecurrenceId();
                LocalDate baseDate = editingEvent.getDate();
                for (Event e : state.getEventDao().getAll()) {
                    if (recId.equals(e.getRecurrenceId()) && !e.getDate().isBefore(baseDate)) {
                        if (hasConflict(e.getDate(), time, duration, null, recId)) {
                            error("Conflito detectado na série! Você já tem compromisso no dia " + e.getDate());
                            return;
                        }
                    }
                }
                
                for (Event e : state.getEventDao().getAll()) {
                    if (recId.equals(e.getRecurrenceId()) && !e.getDate().isBefore(baseDate)) {
                        e.setTitle(title);
                        e.setTime(time);
                        e.setDuration(duration);
                        e.setLocation(locationField.getText().trim());
                        e.setDescription(descriptionArea.getText().trim());
                        e.setCategory((Category) categoryCombo.getSelectedItem());
                        
                        state.getReminderDao().getAll().removeIf(r -> r.getEventId().equals(e.getId()));
                        for (int lead : leads) {
                            Reminder r = new Reminder();
                            r.setReminderId(CsvUtils.newUuid());
                            r.setEventId(e.getId());
                            r.setLeadTimeMinutes(lead);
                            state.getReminderDao().getAll().add(r);
                        }
                    }
                }
            } else {
                if (hasConflict(date, time, duration, editingEvent.getId(), null)) {
                    error("Choque de horário! Você já possui um evento nesse momento.");
                    return;
                }
                
                editingEvent.setTitle(title);
                editingEvent.setDate(date);
                editingEvent.setTime(time);
                editingEvent.setDuration(duration);
                editingEvent.setLocation(locationField.getText().trim());
                editingEvent.setDescription(descriptionArea.getText().trim());
                editingEvent.setCategory((Category) categoryCombo.getSelectedItem());

                state.getReminderDao().getAll().removeIf(r -> r.getEventId().equals(editingEvent.getId()));
                for (int lead : leads) {
                    Reminder r = new Reminder();
                    r.setReminderId(CsvUtils.newUuid());
                    r.setEventId(editingEvent.getId());
                    r.setLeadTimeMinutes(lead);
                    state.getReminderDao().getAll().add(r);
                }
            }
            state.getEventDao().rewriteAll();
            state.getReminderDao().rewriteAll();

        } else {
            if (repeatCheck.isSelected()) {
                int occurrences;
                try {
                    occurrences = Integer.parseInt(occurrencesField.getText().trim());
                } catch (NumberFormatException ex) {
                    error("Número de ocorrências inválido. Informe um número inteiro.");
                    return;
                }
                if (occurrences < 2 || occurrences > 50) {
                    error("O número de ocorrências deve ser entre 2 e 50.");
                    return;
                }

                String freq = (String) freqCombo.getSelectedItem();
                
                // === PROTEÇÃO CONTRA CHOQUE DE HORÁRIO NA CRIAÇÃO DE SÉRIE ===
                for (int i = 0; i < occurrences; i++) {
                    LocalDate loopDate = date;
                    if ("Diariamente".equals(freq)) {
                        loopDate = date.plusDays(i);
                    } else if ("Semanalmente".equals(freq)) {
                        loopDate = date.plusWeeks(i);
                    } else if ("Mensalmente".equals(freq)) {
                        loopDate = date.plusMonths(i);
                    }
                    if (hasConflict(loopDate, time, duration, null, null)) {
                        error("A série bate de frente com outro evento no dia " + loopDate + ".");
                        return;
                    }
                }

                String recurrenceId = CsvUtils.newUuid();

                for (int i = 0; i < occurrences; i++) {
                    LocalDate loopDate = date;
                    if ("Diariamente".equals(freq)) loopDate = date.plusDays(i);
                    else if ("Semanalmente".equals(freq)) loopDate = date.plusWeeks(i);
                    else if ("Mensalmente".equals(freq)) loopDate = date.plusMonths(i);

                    Event e = new Event();
                    e.setId(CsvUtils.newUuid());
                    e.setTitle(title);
                    e.setDate(loopDate);
                    e.setTime(time);
                    e.setDuration(duration);
                    e.setLocation(locationField.getText().trim());
                    e.setDescription(descriptionArea.getText().trim());
                    e.setCategory((Category) categoryCombo.getSelectedItem());
                    state.stampOwner(e);
                    e.setRecurrenceId(recurrenceId);

                    state.getEventDao().getAll().add(e);

                    for (int lead : leads) {
                        Reminder r = new Reminder();
                        r.setReminderId(CsvUtils.newUuid());
                        r.setEventId(e.getId());
                        r.setLeadTimeMinutes(lead);
                        state.getReminderDao().getAll().add(r);
                    }
                }
                state.getEventDao().rewriteAll();
                state.getReminderDao().rewriteAll();

            } else {
                // === PROTEÇÃO CONTRA CHOQUE DE HORÁRIO NA CRIAÇÃO SIMPLES ===
                if (hasConflict(date, time, duration, null, null)) {
                    error("Choque de horário! Você já possui um evento nesse momento.");
                    return;
                }

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
        if (c == null) return "";
        return switch (c) {
            case MEETING -> "Meeting";
            case BIRTHDAY -> "Birthday";
            case APPOINTMENT -> "Appointment";
        };
    }

    // Verifica se o novo horário colide com algum evento existente do usuário ativo
    private boolean hasConflict(LocalDate checkDate, LocalTime checkStart, int durationMin, String excludeId, String excludeRecId) {
        LocalTime checkEnd = checkStart.plusMinutes(durationMin);
        String me = state.getActiveUser();
        
        for (Event e : state.getEventDao().getAll()) {
            if (!e.getOwner().equals(me)) continue; // Só avalia a agenda do próprio usuário
            if (excludeId != null && e.getId().equals(excludeId)) continue; // Ignora o evento que está sendo editado
            if (excludeRecId != null && e.getRecurrenceId() != null && excludeRecId.equals(e.getRecurrenceId())) continue; // Ignora os irmãos da própria série
            
            if (e.getDate().equals(checkDate)) {
                LocalTime eStart = e.getTime();
                LocalTime eEnd = eStart.plusMinutes(e.getDuration());
                
                // Cálculo de interseção: começa antes do outro terminar E termina depois do outro começar
                if (checkStart.isBefore(eEnd) && checkEnd.isAfter(eStart)) {
                    return true; 
                }
            }
        }
        return false;
    }
}