# 📅 Java Event Planner

## 1. Sobre o Projeto
O **Java Event Planner** é um aplicativo desktop *offline* focado no gerenciamento de calendário e agendamento de equipes. Desenvolvido nativamente em **Java Swing**, o sistema permite que múltiplos usuários organizem eventos, configurem lembretes e visualizem agendas em formato diário, semanal ou consolidado em uma grade de equipe (modo *Planner*).

Este projeto foi desenvolvido como requisito para a disciplina **SCC0504**, com foco em manipulação eficiente de memória, design de interface (Single-Threaded via EDT) e persistência híbrida em arquivos CSV.

## 2. Funcionalidades Principais
Para enriquecer a documentação do projeto, destacamos as principais features da aplicação:
- **Gestão de Eventos (CRUD):** Criação, edição e exclusão de eventos com interface modal e proteção de *Ownership* (apenas o dono edita seu evento).
- **Eventos Recorrentes:** Agendamento de séries de eventos (diários, semanais, mensais) com cálculo automático de ocorrências futuras de forma plana.
- **Prevenção de Double-Booking:** Sistema anti-colisão de horários que faz um *Dry Run* para impedir o agendamento de eventos concorrentes no mesmo horário.
- **Exclusão em Cascata:** Gerenciamento de persistência seguro que deleta lembretes órfãos e trata atualizações em lote para toda uma série de ocorrências.
- **Busca em Tempo Real:** Motor de pesquisa MVC que utiliza `JList` para filtrar os eventos conforme o usuário digita a palavra-chave.

## 2.1 Visões de Calendário

A aplicação oferece 3 visões principais:

- **Visão Diária**: Mostra todos os eventos de um dia específico, ordenados por hora
- **Visão Semanal**: Mostra eventos dos 7 próximos dias a partir do dia selecionado
- **Visão de Equipe (Planner)**: Grade 7x48 slots mostrando agendas de todos os membros da equipe em intervalos de 30 minutos

## 2.2 Controle de Acesso

- **5 Usuários Padrão**: Alice, Bob, Carol, David, Emma
- **Usuários Dinâmicos**: Novos usuários podem ser criados ao adicionar eventos
- **Proteção de Ownership**: Apenas o criador de um evento pode editá-lo ou deletá-lo
- **Autenticação por Login**: Obrigatório ao iniciar a aplicação

## 2.3 Lembretes e Alertas

- **Lembretes Personalizáveis**: Configure com antecedência (ex: 1440 minutos = 1 dia antes)
- **Alertas ao Login**: Mostra eventos com lembretes dentro das próximas 24 horas
- **Dupla Notificação**: 
  - Tipo A: Eventos que têm lembrete e atingiram a antecedência
  - Tipo B: Eventos que ocorrem nas próximas 24 horas (mesmo sem lembrete)

## 3. Como Rodar o Projeto

### Pré-requisitos
- Java Development Kit (JDK) 17 ou superior
- Sistema operacional: Windows, macOS ou Linux

### Passos para Execução

1. Clone o repositório:
   git clone https://github.com/Droid-2002/Java-Event-Planner.git
   cd Java-Event-Planner

2. Compile todos os arquivos Java:
   cd src/main/java
   javac -d Main.java

3. Execute a aplicação:
   cd ../../../
   java -cp Main

4. Na primeira execução, você será solicitado a fazer login
   - Escolha um usuário da lista padrão (ex: Alice, Bob, Carol, David, Emma)
   - Os arquivos events.csv e reminders.csv serão criados automaticamente

## 4. Estrutura do Projeto

A arquitetura de pacotes foi dividida seguindo o padrão de separação de responsabilidades (MVC):

* `/model/`: Contém as classes de domínio e objetos de dados (`Event.java`, `Reminder.java`, `Category.java`). Representa a estrutura de negócio.
* `/persistence/`: Gerencia a camada de acesso e I/O de disco (`EventDao.java`, `ReminderDao.java`, `CsvUtils.java`). É onde o modelo *Full Rewrite* e *Append* operam.
* `/service/`: Abriga lógicas de estado global e sessão (`AppState.java`), controlando qual usuário está autenticado.
* `/ui/`: Concentra todo o front-end e componentes gráficos em Java Swing (`MainWindow.java`, `EventFormDialog.java`, `SearchDialog.java`).
* `Main.java`: Ponto de entrada que inicializa a aplicação estritamente dentro da *Event Dispatch Thread* (EDT).

## 5. Decisões Arquiteturais (ADRs)

* **In-Memory Processing:** Para evitar leituras lentas a cada clique no calendário, o CSV é lido uma única vez no startup. Toda a navegação e filtragem roda na memória (`ArrayList`), resultando em uma interface ultra-responsiva.
* **Persistência Híbrida:** Novos eventos usam anexação simples no disco (*Append*). Modificações e exclusões disparam uma reescrita atômica do arquivo todo (*Full Rewrite*), prevenindo quebras no layout do arquivo plano.

## 6. Formato de Dados e Persistência

### events.csv
Colunas (RFC 4180 compliant):
- `id`: ID único do evento
- `title`: Título do evento
- `date`: Data do evento
- `time`: Horário do evento
- `category`: Categoria do evento
- `owner`: Dono do evento

### reminders.csv
Colunas (RFC 4180 compliant):
- `id`: ID único do lembrete
- `event_id`: ID do evento associado
- `date`: Data do lembrete
- `time`: Horário do lembrete
- `owner`: Dono do lembrete
  
### Eventos Recorrentes
- Eventos recorrentes compartilham o mesmo `recurrenceId` gerado automaticamente
- Ocorrências são calculadas de forma **plana** (cada ocorrência é um evento separado)
- Tipos: DIARIA, SEMANAL, MENSAL

## 7. Padrões de Design Utilizados

| Padrão | Implementação | Benefício |
|--------|---------------|-----------|
| **DAO** | EventDao, ReminderDao | Abstração de persistência |
| **Repository** | Coleção em memória (ArrayList) | Performance, responsividade |
| **Observer** | CalendarPanel → Views | Sincronização automática de telas |
| **Singleton** | AppState | Estado global consistente |
| **Factory** | EventRow.create() | Criação de componentes |
| **MVC** | Model + Persistence + UI | Separação de responsabilidades |

## 8. Como Usar a Aplicação

### Criar um Novo Evento
1. Clique no botão "Novo Evento" na janela principal
2. Preencha os campos obrigatórios (Título*, Data*, Hora*, Duração*, Categoria*)
3. (Opcional) Adicione local, descrição, lembretes
4. (Opcional) Configure recorrência (diária, semanal, mensal)
5. Clique em "Salvar"

### Editar um Evento
1. Abra o diálogo "Buscar e Gerenciar Eventos"
2. Digite o nome do evento
3. Clique em "Editar" (apenas disponível se você for o dono)
4. Modifique os campos e salve

### Deletar um Evento
1. Abra o diálogo "Buscar e Gerenciar Eventos"
2. Selecione o evento
3. Clique em "Deletar" (apenas disponível se você for o dono)

### Visualizar Agendas
- **Diária**: Selecione um dia no calendário
- **Semanal**: Selecione um dia para ver a semana inteira
- **Equipe**: Veja a grade de agendas de todos os membros
- 
## 9. Autores

Projeto desenvolvido por:

* **Mateus Vargas Saracuza** – 13674087
* **Rafael Mendonça Duarte** – 16817608