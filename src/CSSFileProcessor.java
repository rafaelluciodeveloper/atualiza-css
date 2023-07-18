import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CSSFileProcessor extends JFrame {
    private JTextField rootFolderField;
    private JTable cssFilesTable;

    private static final String MAPPING_FILE_PROPERTY = "c:\\temp\\mapping.csv";

    private static final String ROOT_DIRECTORY_PROPERTY  = "c:\\temp\\";


    public CSSFileProcessor() {
        setTitle("CSS File Processor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        // Root folder input
        JPanel folderPanel = new JPanel(new FlowLayout());
        JLabel rootFolderLabel = new JLabel("Root Folder:");
        rootFolderField = new JTextField(20);
        rootFolderField.setText(ROOT_DIRECTORY_PROPERTY);
        JButton folderButton = new JButton(new ImageIcon(getClass().getResource("Folder.png")));
        folderButton.addActionListener(e -> openFolderChooser());
        folderPanel.add(rootFolderLabel);
        folderPanel.add(rootFolderField);
        folderPanel.add(folderButton);

        // CSS files table
        cssFilesTable = new JTable(new DefaultTableModel(new Object[]{"Selected", "File Path"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) {
                    return Boolean.class;
                }
                return super.getColumnClass(column);
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(cssFilesTable);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton searchButton = new JButton("Search CSS Files", new ImageIcon(getClass().getResource("Find.png")));
        searchButton.addActionListener(e -> searchCSSFiles());
        JButton replaceButton = new JButton("Replace Selected Files" , new ImageIcon(getClass().getResource("Sync.png")));
        replaceButton.addActionListener(e -> replaceSelectedFiles());
        buttonPanel.add(searchButton);
        buttonPanel.add(replaceButton);

        mainPanel.add(folderPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void searchCSSFiles() {
        String rootFolderPath = rootFolderField.getText();
        DefaultTableModel tableModel = (DefaultTableModel) cssFilesTable.getModel();
        tableModel.setRowCount(0);

        try {
            Files.walk(Paths.get(rootFolderPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".css") || path.toString().endsWith(".xhtml"))
                    .forEach(path -> {
                        tableModel.addRow(new Object[]{true, path.toString()});
                        cssFilesTable.getColumnModel().getColumn(0).setPreferredWidth(70);
                        cssFilesTable.getColumnModel().getColumn(1).setPreferredWidth(430);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void replaceSelectedFiles() {
        DefaultTableModel tableModel = (DefaultTableModel) cssFilesTable.getModel();
        int rowCount = tableModel.getRowCount();

        for (int i = 0; i < rowCount; i++) {
            boolean isSelected = (boolean) tableModel.getValueAt(i, 0);
            String filePath = (String) tableModel.getValueAt(i, 1);

            if (isSelected) {
                replaceCSSFile(filePath);
            }
        }

        JOptionPane.showMessageDialog(this, "Replacement completed!");
    }

    private void replaceCSSFile(String filePath) {
        String mappingFilePath = MAPPING_FILE_PROPERTY;
        Map<String, String> mapping = loadMappingFile(mappingFilePath);

        try {
            // Read the original CSS file
            StringBuilder cssBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    String modifiedLine = line;
                    for (Map.Entry<String, String> entry : mapping.entrySet()) {
                        String oldClass = entry.getKey();
                        String newClass = entry.getValue();
                        modifiedLine = modifiedLine.replace(oldClass, newClass);
                    }
                    cssBuilder.append(modifiedLine).append(System.lineSeparator());
                    if (!line.equals(modifiedLine)) {
                        writeLog(filePath, getLastFolderName(rootFolderField.getText()) ,lineNumber);
                    }
                    lineNumber++;
                }
            }

            String cssContent = cssBuilder.toString();

            // Write the modified CSS file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(cssContent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> loadMappingFile(String mappingFilePath) {
        Map<String, String> mapping = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(mappingFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String oldClass = parts[0].trim();
                    String newClass = parts[1].trim();
                    mapping.put(oldClass, newClass);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mapping;
    }

    private void writeLog(String filePath, String sufix ,int lineNumber) {
        String logFilePath = "c:\\temp\\log_" + sufix + ".txt";
        String logEntry = "File: " + filePath + ", Line modified: " + lineNumber + "\n";
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFilePath, true))) {
            logWriter.write(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFolderChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Root Folder");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();
            rootFolderField.setText(selectedFolder.getAbsolutePath());
        }
    }

    private String getLastFolderName(String path) {
        String[] parts = path.split("\\\\"); // Divide o caminho usando o caractere de barra invertida como separador
        if (parts.length > 0) {
            return parts[parts.length - 1]; // Retorna o último elemento do array, que será o nome da última pasta
        }
        return "";
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CSSFileProcessor().setVisible(true));
    }

    private void confirmExit() {
        int result = JOptionPane.showConfirmDialog(this, "Deseja Sair?", "Fechar", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            dispose(); // Fecha a janela
        }
    }
}
