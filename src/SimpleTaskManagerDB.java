import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

public class SimpleTaskManagerDB extends JFrame {

    // Enum untuk merepresentasikan jenis tampilan tabel
    private enum ViewType {
        ACTIVE, OVERDUE, COMPLETED
    }

    private interface UIConstants {
        Color WARNA_OVERDUE = new Color(255, 200, 200); // Warna lebih menonjol
        Color WARNA_BARIS_ALT_TABEL = new Color(245, 248, 250);
        Color WARNA_PLACEHOLDER = Color.GRAY;
        Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 26);
        Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 18);
        Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);
        Font FONT_FIELD = new Font("Segoe UI", Font.PLAIN, 14);
        Font FONT_TABEL = new Font("Segoe UI", Font.PLAIN, 13);
        Font FONT_HEADER_TABEL = new Font("Segoe UI", Font.BOLD, 14);
    }

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private JTextField loginUser, regUser, regDisplayName, taskField, searchField;
    private JPasswordField loginPass, regPass;
    private DefaultTableModel taskModel;
    private JTable taskTable;
    private JButton addTaskBtn, searchBtn, resetSearchBtn, showActiveBtn, showCompletedBtn, showOverdueBtn;
    private DatePicker datePicker;
    private JLabel welcomeLabel, viewTitleLabel;
    private String currentUserDisplayName = "";
    private int currentUserId = -1;
    private ViewType currentView = ViewType.ACTIVE; // Menggunakan enum untuk status tampilan

    private final String DB_URL = "jdbc:mysql://localhost:3306/simple_task_manager?serverTimezone=Asia/Jakarta";
    private final String DB_USER = "root";
    private final String DB_PASS = "";

    public SimpleTaskManagerDB() {
        setTitle("Task Master - Manajer Tugas");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));

        JPanel loginWrapper = createWrapperPanel(createLoginPanel());
        JPanel registerWrapper = createWrapperPanel(createRegisterPanel());
        JPanel taskPanel = createTaskPanel();

        mainPanel.add(loginWrapper, "login");
        mainPanel.add(registerWrapper, "register");
        mainPanel.add(taskPanel, "task");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    private JPanel createWrapperPanel(JPanel content) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        wrapper.add(content);
        return wrapper;
    }

    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true), BorderFactory.createEmptyBorder(30, 30, 30, 30)));
        loginPanel.setPreferredSize(new Dimension(420, 320));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel loginTitle = new JLabel("Selamat Datang Kembali!");
        loginTitle.setFont(UIConstants.FONT_HEADER);
        loginTitle.setHorizontalAlignment(SwingConstants.CENTER);

        loginUser = new JTextField(20);
        loginPass = new JPasswordField(20);
        addPlaceholder(loginUser, "Masukkan username Anda");
        addPlaceholder(loginPass, "Masukkan password Anda");

        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> handleLogin());
        loginPass.addActionListener(e -> loginBtn.doClick());

        JButton goToRegisterBtn = new JButton("Buat Akun Baru");
        goToRegisterBtn.setContentAreaFilled(false);
        goToRegisterBtn.setBorderPainted(false);
        goToRegisterBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        goToRegisterBtn.addActionListener(e -> cardLayout.show(mainPanel, "register"));

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 0; loginPanel.add(loginTitle, gbc);
        gbc.gridy++; loginPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1; gbc.gridy++; loginPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; loginPanel.add(loginUser, gbc);
        gbc.gridx = 0; gbc.gridy++; loginPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; loginPanel.add(loginPass, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(loginBtn, gbc);
        gbc.gridy++; loginPanel.add(goToRegisterBtn, gbc);
        return loginPanel;
    }

    private JPanel createRegisterPanel() {
        JPanel registerPanel = new JPanel(new GridBagLayout());
        registerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true), BorderFactory.createEmptyBorder(30, 30, 30, 30)));
        registerPanel.setPreferredSize(new Dimension(450, 400));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel regTitle = new JLabel("Buat Akun");
        regTitle.setFont(UIConstants.FONT_HEADER);
        regTitle.setHorizontalAlignment(SwingConstants.CENTER);

        regUser = new JTextField(20);
        regPass = new JPasswordField(20);
        regDisplayName = new JTextField(20);
        addPlaceholder(regUser, "Pilih username");
        addPlaceholder(regPass, "Buat password yang kuat");
        addPlaceholder(regDisplayName, "Nama panggilan Anda");

        JButton registerBtn = new JButton("Daftar Sekarang");
        registerBtn.addActionListener(e -> handleRegistration());

        JButton backToLoginBtn = new JButton("Kembali ke Login");
        backToLoginBtn.setContentAreaFilled(false);
        backToLoginBtn.setBorderPainted(false);
        backToLoginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToLoginBtn.addActionListener(e -> cardLayout.show(mainPanel, "login"));

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 0; registerPanel.add(regTitle, gbc);
        gbc.gridy++; registerPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1; gbc.gridy++; registerPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; registerPanel.add(regUser, gbc);
        gbc.gridx = 0; gbc.gridy++; registerPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; registerPanel.add(regPass, gbc);
        gbc.gridx = 0; gbc.gridy++; registerPanel.add(new JLabel("Nama Panggilan:"), gbc);
        gbc.gridx = 1; registerPanel.add(regDisplayName, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        registerPanel.add(registerBtn, gbc);
        gbc.gridy++; registerPanel.add(backToLoginBtn, gbc);
        return registerPanel;
    }

    private JPanel createTaskPanel() {
        JPanel taskPanel = new JPanel(new BorderLayout(15, 15));
        taskPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        welcomeLabel = new JLabel("Selamat Datang!", SwingConstants.LEFT);
        welcomeLabel.setFont(UIConstants.FONT_HEADER);

        // --- PENAMBAHAN TOMBOL BARU ---
        showActiveBtn = new JButton("Tugas Aktif");
        showOverdueBtn = new JButton("Tugas Terlewat");
        showCompletedBtn = new JButton("Tugas Selesai");
        JPanel viewTogglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        viewTogglePanel.add(showActiveBtn);
        viewTogglePanel.add(showOverdueBtn);
        viewTogglePanel.add(showCompletedBtn);

        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(viewTogglePanel, BorderLayout.EAST);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        viewTitleLabel = new JLabel("Tugas Aktif");
        viewTitleLabel.setFont(UIConstants.FONT_SUBHEADER);
        viewTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JPanel actionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcInput = new GridBagConstraints();
        gbcInput.insets = new Insets(5, 5, 5, 5);
        gbcInput.fill = GridBagConstraints.HORIZONTAL;

        taskField = new JTextField();
        addPlaceholder(taskField, "Apa tugas baru Anda?");

        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        dateSettings.setAllowKeyboardEditing(false);
        datePicker = new DatePicker(dateSettings);
        datePicker.getComponentDateTextField().setEditable(false);

        JButton datePickerButton = datePicker.getComponentToggleCalendarButton();
        datePickerButton.setIcon(new CalendarIcon());
        datePickerButton.setText("");
        datePickerButton.setPreferredSize(new Dimension(30, 30));

        searchField = new JTextField();
        addPlaceholder(searchField, "Cari tugas...");
        addTaskBtn = new JButton("Tambah Tugas");
        searchBtn = new JButton();
        resetSearchBtn = new JButton("Tampilkan Semua");

        searchBtn.setIcon(new SearchIcon());
        searchBtn.setPreferredSize(new Dimension(30, 30));

        taskField.addActionListener(e -> addTaskBtn.doClick());
        searchField.addActionListener(e -> searchBtn.doClick());
        addTaskBtn.addActionListener(e -> handleAddTask());
        searchBtn.addActionListener(e -> loadTasks(searchField.getText().trim()));
        resetSearchBtn.addActionListener(e -> {
            searchField.setText("");
            addPlaceholder(searchField, "Cari tugas...");
            loadTasks("");
        });
        showActiveBtn.addActionListener(e -> switchView(ViewType.ACTIVE));
        showOverdueBtn.addActionListener(e -> switchView(ViewType.OVERDUE));
        showCompletedBtn.addActionListener(e -> switchView(ViewType.COMPLETED));

        gbcInput.gridx = 0; gbcInput.weightx = 0.5; actionPanel.add(taskField, gbcInput);
        gbcInput.gridx++; gbcInput.weightx = 0.2; actionPanel.add(datePicker, gbcInput);
        gbcInput.gridx++; gbcInput.weightx = 0.0; actionPanel.add(addTaskBtn, gbcInput);
        gbcInput.gridx++; gbcInput.weightx = 0.3; actionPanel.add(searchField, gbcInput);
        gbcInput.gridx++; gbcInput.weightx = 0.0; actionPanel.add(searchBtn, gbcInput);
        gbcInput.gridx++; gbcInput.weightx = 0.0; actionPanel.add(resetSearchBtn, gbcInput);

        inputPanel.add(viewTitleLabel, BorderLayout.NORTH);
        inputPanel.add(actionPanel, BorderLayout.CENTER);

        taskModel = new DefaultTableModel(new Object[]{"Selesai", "Tugas", "Tenggat", "ID"}, 0) {
            @Override public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : Object.class; }
            @Override public boolean isCellEditable(int row, int col) { return col != 3; } // ID tidak bisa diedit
        };
        taskTable = new JTable(taskModel);
        setupTable();

        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        taskPanel.add(headerPanel, BorderLayout.NORTH);
        taskPanel.add(inputPanel, BorderLayout.CENTER);
        taskPanel.add(scrollPane, BorderLayout.SOUTH);

        return taskPanel;
    }

    private void setupTable() {
        taskTable.setFont(UIConstants.FONT_TABEL);
        taskTable.setRowHeight(30);
        taskTable.getTableHeader().setFont(UIConstants.FONT_HEADER_TABEL);
        taskTable.getTableHeader().setOpaque(false);
        taskTable.getTableHeader().setBackground(new Color(240, 240, 240));
        taskTable.getColumnModel().getColumn(0).setMaxWidth(60);
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(450);
        taskTable.getColumnModel().getColumn(3).setMinWidth(0);
        taskTable.getColumnModel().getColumn(3).setMaxWidth(0);
        taskTable.setDefaultRenderer(Object.class, new OverdueTaskRenderer());
        taskModel.addTableModelListener(e -> {
            if (e.getColumn() == 0 && e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                if (row >= 0 && row < taskModel.getRowCount()) {
                    Boolean done = (Boolean) taskModel.getValueAt(row, 0);
                    Integer taskId = (Integer) taskModel.getValueAt(row, 3);
                    updateTaskStatus(taskId, done, row);
                }
            }
        });
    }

    // --- LOGIKA PERGANTIAN TAMPILAN DIPERBARUI ---
    private void switchView(ViewType viewType) {
        this.currentView = viewType;
        boolean isInputVisible = (viewType == ViewType.ACTIVE);

        showActiveBtn.setEnabled(viewType != ViewType.ACTIVE);
        showOverdueBtn.setEnabled(viewType != ViewType.OVERDUE);
        showCompletedBtn.setEnabled(viewType != ViewType.COMPLETED);

        addTaskBtn.setVisible(isInputVisible);
        taskField.setVisible(isInputVisible);
        datePicker.setVisible(isInputVisible);

        switch (viewType) {
            case ACTIVE:
                viewTitleLabel.setText("Tugas Aktif");
                break;
            case OVERDUE:
                viewTitleLabel.setText("Tugas Terlewat");
                break;
            case COMPLETED:
                viewTitleLabel.setText("Tugas Selesai");
                break;
        }
        loadTasks("");
    }

    // --- LOGIKA UPDATE STATUS DIPERBARUI ---
    private void updateTaskStatus(int taskId, boolean done, int tableRow) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String sql = "UPDATE tasks SET done = ? WHERE id = ?";
                try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setBoolean(1, done);
                    ps.setInt(2, taskId);
                    return ps.executeUpdate() > 0;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        // Jika berhasil, hapus baris dari tampilan saat ini
                        // agar tidak perlu memuat ulang seluruh tabel.
                        taskModel.removeRow(tableRow);
                    } else {
                        // Jika gagal, muat ulang untuk sinkronisasi
                        JOptionPane.showMessageDialog(SimpleTaskManagerDB.this, "Gagal memperbarui status tugas.", "Error", JOptionPane.ERROR_MESSAGE);
                        loadTasks(searchField.getText().trim());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    loadTasks(searchField.getText().trim());
                }
            }
        }.execute();
    }

    private void handleLogin() {
        String username = loginUser.getText().trim();
        String password = new String(loginPass.getPassword());
        if (username.isEmpty() || password.isEmpty() || username.equals("Masukkan username Anda")) {
            JOptionPane.showMessageDialog(this, "Username dan password harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return authenticate(username, password); }
            @Override protected void done() {
                try {
                    if (get()) {
                        welcomeLabel.setText("Halo, " + currentUserDisplayName + "!");
                        switchView(ViewType.ACTIVE);
                        cardLayout.show(mainPanel, "task");
                    } else {
                        JOptionPane.showMessageDialog(SimpleTaskManagerDB.this, "Login gagal! Username atau password salah.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SimpleTaskManagerDB.this, "Terjadi error saat login.", "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void handleRegistration() {
        String username = regUser.getText().trim();
        String password = new String(regPass.getPassword());
        String displayName = regDisplayName.getText().trim();
        if (username.isEmpty() || password.isEmpty() || username.equals("Pilih username")) {
            JOptionPane.showMessageDialog(this, "Username dan password harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (displayName.isEmpty() || displayName.equals("Nama panggilan Anda")) {
            displayName = username;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return registerUser(username, password, displayName); }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(SimpleTaskManagerDB.this, "Registrasi berhasil! Silakan login.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                        cardLayout.show(mainPanel, "login");
                    } else {
                        JOptionPane.showMessageDialog(SimpleTaskManagerDB.this, "Registrasi gagal. Username mungkin sudah ada.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void handleAddTask() {
        String task = taskField.getText().trim();
        LocalDate dueDate = datePicker.getDate();
        if (task.isEmpty() || task.equals("Apa tugas baru Anda?")) {
            JOptionPane.showMessageDialog(this, "Nama tugas tidak boleh kosong!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                addTask(task, dueDate);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    taskField.setText("");
                    datePicker.clear();
                    addPlaceholder(taskField, "Apa tugas baru Anda?");
                    loadTasks("");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SimpleTaskManagerDB.this, "Gagal menambah tugas.", "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    // --- LOGIKA PEMUATAN TUGAS DIPERBARUI ---
    private void loadTasks(String searchTerm) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        taskTable.setEnabled(false);
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> rows = new ArrayList<>();
                String sql;
                String baseSql = "SELECT id, done, task_title, due_date FROM tasks WHERE user_id=? AND task_title LIKE ?";
                
                switch (currentView) {
                    case ACTIVE:
                        // Tugas yang belum selesai DAN (tenggatnya hari ini/mendatang ATAU tidak punya tenggat)
                        sql = baseSql + " AND done = FALSE AND (due_date >= CURDATE() OR due_date IS NULL) ORDER BY due_date ASC";
                        break;
                    case OVERDUE:
                        // Tugas yang belum selesai DAN tenggatnya sudah lewat
                        sql = baseSql + " AND done = FALSE AND due_date < CURDATE() ORDER BY due_date ASC";
                        break;
                    case COMPLETED:
                        // Semua tugas yang sudah selesai
                        sql = baseSql + " AND done = TRUE ORDER BY due_date DESC";
                        break;
                    default:
                        return rows; // Seharusnya tidak terjadi
                }

                try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, currentUserId);
                    ps.setString(2, "%" + searchTerm + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Date dueDate = rs.getDate("due_date");
                            rows.add(new Object[]{rs.getBoolean("done"), rs.getString("task_title"), dueDate == null ? "" : dueDate.toString(), rs.getInt("id")});
                        }
                    }
                }
                return rows;
            }
            @Override protected void done() {
                try {
                    taskModel.setRowCount(0);
                    for (Object[] row : get()) {
                        taskModel.addRow(row);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SimpleTaskManagerDB.this, "Gagal memuat tugas dari database.", "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    taskTable.setEnabled(true);
                }
            }
        }.execute();
    }

    private boolean authenticate(String username, String password) {
        String sql = "SELECT id, display_name FROM users WHERE username=? AND password=SHA2(?,256)";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentUserId = rs.getInt("id");
                    currentUserDisplayName = rs.getString("display_name");
                    if (currentUserDisplayName == null || currentUserDisplayName.trim().isEmpty()) {
                        currentUserDisplayName = username;
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean registerUser(String username, String password, String displayName) {
        String sql = "INSERT INTO users(username, password, display_name) VALUES(?, SHA2(?,256), ?)";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, displayName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addTask(String task, LocalDate dueDate) throws SQLException {
        String sql = "INSERT INTO tasks(user_id, task_title, due_date) VALUES (?, ?, ?)";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setString(2, task);
            ps.setDate(3, dueDate != null ? Date.valueOf(dueDate) : null);
            ps.executeUpdate();
        }
    }

    public static void addPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(UIConstants.WARNA_PLACEHOLDER);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { if (field.getText().equals(placeholder)) { field.setText(""); field.setForeground(Color.BLACK); } }
            @Override public void focusLost(FocusEvent e) { if (field.getText().isEmpty()) { field.setText(placeholder); field.setForeground(UIConstants.WARNA_PLACEHOLDER); } }
        });
    }

    class OverdueTaskRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            c.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? UIConstants.WARNA_BARIS_ALT_TABEL : table.getBackground()));
            c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            
            if (table.getModel().getRowCount() > row) {
                boolean isDone = (Boolean) table.getValueAt(row, 0);
                String dueDateStr = (String) table.getValueAt(row, 2);
                if (!isDone && dueDateStr != null && !dueDateStr.isEmpty()) {
                    try {
                        LocalDate dueDate = LocalDate.parse(dueDateStr);
                        if (dueDate.isBefore(LocalDate.now()) && !isSelected) {
                            c.setBackground(UIConstants.WARNA_OVERDUE);
                        }
                    } catch (DateTimeParseException e) { /* Abaikan */ }
                }
            }
            return c;
        }
    }

    private static class SearchIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.GRAY);
            g2d.drawOval(x + 3, y + 3, 10, 10);
            g2d.drawLine(x + 12, y + 12, x + 16, y + 16);
            g2d.dispose();
        }
        @Override public int getIconWidth() { return 20; }
        @Override public int getIconHeight() { return 20; }
    }
    
    private static class CalendarIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.GRAY);
            g2d.drawRect(x + 3, y + 4, 14, 13);
            g2d.drawLine(x + 3, y + 8, x + 17, y + 8);
            g2d.fillRect(x + 5, y + 2, 2, 4);
            g2d.fillRect(x + 13, y + 2, 2, 4);
            g2d.dispose();
        }
        @Override public int getIconWidth() { return 20; }
        @Override public int getIconHeight() { return 20; }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (Exception ex) {
            System.err.println("Gagal menginisialisasi LaF");
        }
        SwingUtilities.invokeLater(() -> {
            new SimpleTaskManagerDB().setVisible(true);
        });
    }
}