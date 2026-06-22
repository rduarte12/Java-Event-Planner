package ui;

import java.awt.*;
import java.time.LocalTime;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import model.Event;

public class TeamScheduleCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        // Chama a superclasse para obter a estrutura básica do JLabel
        JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Estilização padrão de célula vazia
        cell.setText("");
        cell.setBackground(Color.WHITE);
        cell.setForeground(Color.BLACK);
        cell.setHorizontalAlignment(SwingConstants.CENTER);

        // Estilização da coluna de horários (Coluna 0)
        if (column == 0) {
            cell.setText((String) value);
            cell.setBackground(new Color(240, 240, 240)); // Tom cinza leve
            cell.setFont(cell.getFont().deriveFont(Font.BOLD));
            return cell;
        }

        // Se o valor contido na célula for um Evento, customizamos visualmente
        if (value instanceof Event) {
            Event event = (Event) value;

            // Validação de null
            if (event.getTime() == null || event.getTitle() == null || event.getCategory() == null) {
                return cell; // Retorna célula padrão se dados forem incompletos
            }

            // Só escreve o título do evento na célula correspondente ao horário de início
            // real
            if (event.getTime().equals(slotIndexToTime(row))) {
                cell.setText(event.getTitle());
            }

            // Define a cor de fundo dinamicamente baseada na categoria (Requisito de Cores)
            switch (event.getCategory().name()) {
                case "MEETING":
                    cell.setBackground(new Color(173, 216, 230)); // Light Blue
                    break;
                case "BIRTHDAY":
                    cell.setBackground(new Color(255, 182, 193)); // Light Red/Pink
                    break;
                case "APPOINTMENT":
                    cell.setBackground(new Color(255, 222, 173)); // Navajo White/Orange
                    break;
                default:
                    cell.setBackground(Color.LIGHT_GRAY);
            }

            // Se a célula estiver selecionada pelo mouse, destaca a borda ou escurece
            // levemente
            if (isSelected) {
                Color bgColor = cell.getBackground();
                if (bgColor != null) {
                    cell.setBackground(bgColor.darker());
                }
            }
        }

        return cell;
    }

    private LocalTime slotIndexToTime(int rowIndex) {
        int totalMinutes = rowIndex * 30;
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60);
    }
}