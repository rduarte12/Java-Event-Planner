// Ponto de entrada da aplicacao Java Event Planner.
// Carrega os dados dos CSV em memoria e abre o login por selecao (RF01).
// A janela principal vem na proxima etapa.

import javax.swing.SwingUtilities;

import persistence.EventDao;
import persistence.ReminderDao;
import service.AppState;
import ui.LoginDialog;
import ui.MainWindow;

public class Main {

    // RNF01 / DT02: aplicacao single-threaded.
    // Nao criamos Thread, Runnable nem SwingWorker. Toda a UI roda na EDT
    // nativa do Swing, agendada aqui via SwingUtilities.invokeLater.
    public static void main(String[] args) {
        System.out.println("Java Event Planner - inicializando...");

        SwingUtilities.invokeLater(() -> {
            // DT01/RNF02: le os dois CSV uma unica vez no startup
            EventDao eventDao = new EventDao();
            ReminderDao reminderDao = new ReminderDao();
            eventDao.load();
            reminderDao.load();

            AppState state = new AppState(eventDao, reminderDao);

            // RF01: bloqueia ate o usuario escolher quem e (modal)
            LoginDialog login = new LoginDialog(state);
            login.setVisible(true);

            // so chega aqui depois do login confirmado (modal). abre a tela principal.
            if (state.isLoggedIn()) {
                System.out.println("Usuario ativo: " + state.getActiveUser());
                MainWindow main = new MainWindow(state);
                main.setVisible(true);
            }
        });
    }
}
