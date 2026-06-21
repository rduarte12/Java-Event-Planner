package ui;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import service.AppState;

// Janela principal (RF04/RF05). Divide a tela: calendario mensal a esquerda
// e um painel de abas a direita (Diaria / Semanal / Equipe). Por enquanto as
// abas sao placeholders - vao ser preenchidas nas proximas etapas.
public class MainWindow extends JFrame {

    private final AppState state;
    private final CalendarPanel calendarPanel;
    private final JTabbedPane tabs;

    public MainWindow(AppState state) {
        super("Java Event Planner - " + state.getActiveUser());
        this.state = state;

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // esquerda: calendario lendo a colecao em memoria
        calendarPanel = new CalendarPanel(state.getEventDao());

        // direita: tres abas navegaveis ainda vazias (RF05)
        tabs = new JTabbedPane();
        tabs.addTab("Visao Diaria", placeholder("Visao Diaria"));
        tabs.addTab("Visao Semanal", placeholder("Visao Semanal"));
        tabs.addTab("Visao de Equipe (Persons)", placeholder("Visao de Equipe (Persons)"));

        // quando um dia e clicado no calendario, as abas vao reagir (proxima etapa)
        calendarPanel.setDaySelectionListener(date -> {
            // TODO proxima etapa: filtrar e atualizar as abas conforme o dia selecionado
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, calendarPanel, tabs);
        split.setDividerLocation(340);
        setContentPane(split);

        setSize(900, 600);
        setMinimumSize(new java.awt.Dimension(700, 450));
        setLocationRelativeTo(null);
    }

    // painel temporario pra cada aba enquanto as views nao existem
    private JComponent placeholder(String name) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(name + " (em construcao)", SwingConstants.CENTER), BorderLayout.CENTER);
        return p;
    }
}
