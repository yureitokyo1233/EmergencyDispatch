package dispatch;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*; // <-- Needed for TableModelEvent and TableModelListener
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.awt.geom.Ellipse2D;

public class main {

    public static void triggerDispatch(
            DispatchSystem system,
            int section,
            int type,
            int severity,
            JTable incidentTable,
            DefaultTableModel tableModel,
            JPanel mapPanel,
            JButton returnBtn,
            JFrame frame
    ) {


        // 🆕 Use the new reportIncident with medicalCondition
        system.reportIncident(section, severity, type, "");

        // 🚑 Dispatch nearest unit
        Unit dispatched = system.dispatchNearest(section);
        if (dispatched != null) {
            JOptionPane.showMessageDialog(frame, "Dispatched Unit " + dispatched.id + " from Section " + dispatched.section);
            if (returnBtn != null) returnBtn.setEnabled(true);
            dispatched.targetSection = section;
        } else {
            JOptionPane.showMessageDialog(frame, "No available units!");
        }

        // 🔄 Refresh map
        if (mapPanel != null) mapPanel.repaint();

        // 🧾 Refresh incident report table
        if (tableModel != null) {
            tableModel.setRowCount(0);
            for (Incident inc : system.getSortedIncidents()) {
                String typeStr = inc.type == 1 ? "ALS" : inc.type == 2 ? "BLS" : "CCT";
                String sevStr = "S" + inc.severity;
                String arrivalStr = inc.arrivalTime != null ? inc.arrivalTime.toString() : "";
                tableModel.addRow(new Object[]{
                        inc.id,
                        inc.section,
                        sevStr,
                        typeStr,
                        arrivalStr,
                        inc.medicalCondition  // 🩺 Added this column
                });
            }
        }
    }

    public static void main(String[] args) {
        DispatchSystem system = new DispatchSystem();

        Image bgImage = new ImageIcon("mapa.jpg").getImage();

        JFrame frame = new JFrame("Emergency Dispatch");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 840);

        int[][] positions = {{250, 100}, {750, 150}, {150, 500}, {850, 350}, {600, 650}, {950, 580}};
        int circleSize = 40;
        final int[] hovered = {-1};
        final int[] clicked = {-1};

        for (Unit u : system.units.values()) {
            int idx = u.section - 1;
            u.setPosition(positions[idx][0] + 10, positions[idx][1] + 10);
        }

        JPanel mapPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);

                for (int i = 0; i < 6; i++) {
                    int x = positions[i][0];
                    int y = positions[i][1];
                    if (i == hovered[0] || i == clicked[0]) {
                        g.setColor(Color.GRAY);
                        g.fillOval(x, y, circleSize, circleSize);
                    } else {
                        g.setColor(Color.LIGHT_GRAY);
                        g.fillOval(x, y, circleSize, circleSize);
                    }
                    g.setColor(Color.BLACK);
                    g.drawOval(x, y, circleSize, circleSize);

                    if (i == hovered[0]) {
                        String label = "Section " + (i + 1);
                        if (i == 0) label += " (With Ambulance)";
                        if (i == 5) label += " (With Ambulance)";
                        int labelX = x;
                        int labelY = y - 10;
                        g.setColor(Color.BLACK);
                        g.drawString(label, labelX - 1, labelY - 1);
                        g.drawString(label, labelX - 1, labelY + 1);
                        g.drawString(label, labelX + 1, labelY - 1);
                        g.drawString(label, labelX + 1, labelY + 1);
                        g.drawString(label, labelX, labelY - 1);
                        g.drawString(label, labelX, labelY + 1);
                        g.drawString(label, labelX - 1, labelY);
                        g.drawString(label, labelX + 1, labelY);
                        g.setColor(Color.YELLOW);
                        g.drawString(label, labelX, labelY);
                    }
                }
                for (Unit u : system.units.values()) {
                    g.setColor(u.available ? Color.BLUE : Color.RED);
                    g.fillOval(u.x, u.y, 20, 20);
                }
            }
        };
        mapPanel.setPreferredSize(new Dimension(1280, 800));
        mapPanel.setBackground(Color.BLACK);
        mapPanel.setOpaque(true);

        // Chat panel (not shown at startup)
        final float[] opacity = {1.0f};
        JPanel chatPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity[0]));
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chatPanel.setOpaque(false);
        chatPanel.setBackground(new Color(0, 0, 0, 0));
        chatPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(255, 255, 255, 220));
        JTextField chatInput = new JTextField();

        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        chatInput.addActionListener(e -> {
            String input = chatInput.getText().trim().toUpperCase();
            chatInput.setText("");

            chatArea.append("You: " + input + "\n");

            if (ChatHashmap.codes.containsKey(input)) {
                String service = ChatHashmap.codes.get(input);
                chatArea.append("System: " + service + "\n");

                try {
                    String zone = input.substring(0, 2);
                    int section = Integer.parseInt(zone) / 10;

                    int type = input.contains("ALS") ? 1 : input.contains("BLS") ? 2 : 3;
                    int severity = input.contains("S1") ? 1 :
                            input.contains("S2") ? 2 :
                                    input.contains("S3") ? 3 : 4;

                    triggerDispatch(system, section, type, severity, null, null, mapPanel, null, frame);
                } catch (Exception ex) {
                    chatArea.append("System: Failed to parse dispatch code.\n");
                }
            } else {
                chatArea.append("System: Unknown code. Please try again.\n");
            }
        });

        // Incident report table (not shown at startup
        String[] columns = {"ID", "Section", "Severity", "Type", "Arrival Time", "Medical Condition"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only "Medical Condition" editable
            }


        }; JTable incidentTable = new JTable(tableModel);
        incidentTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // Commit edit on focus loss
        JScrollPane tableScroll = new JScrollPane(incidentTable);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();

                // Only check when Medical Condition column is edited
                if (col == 5 && row >= 0) {
                    String newCondition = (String) tableModel.getValueAt(row, col);
                    if (newCondition == null || newCondition.isBlank()) return;

                    // Get details from that row
                    int section = (int) tableModel.getValueAt(row, 1);
                    String sevStr = (String) tableModel.getValueAt(row, 2);
                    String typeStr = (String) tableModel.getValueAt(row, 3);
                    int severity = Integer.parseInt(sevStr.substring(1));
                    int type = typeStr.equals("ALS") ? 1 : typeStr.equals("BLS") ? 2 : 3;

                    // Fetch incidents
                    List<Incident> all = system.getIncidents();

                    // Find similar ones
                    List<Incident> matches = new ArrayList<>();
                    for (Incident i : all) {
                        if (i.section == section &&
                                i.severity == severity &&
                                i.type == type &&
                                i.medicalCondition.equalsIgnoreCase(newCondition)) {
                            matches.add(i);
                        }
                    }

                    // Merge if multiple found
                    if (matches.size() > 1) {
                        Incident base = matches.get(0);
                        for (int i = 1; i < matches.size(); i++) {
                            all.remove(matches.get(i));
                        }
                        system.setIncidents(all);

                        // Refresh table
                        tableModel.setRowCount(0);
                        for (Incident inc : system.getSortedIncidents()) {
                            String typeS = inc.type == 1 ? "ALS" : inc.type == 2 ? "BLS" : "CCT";
                            String sevS = "S" + inc.severity;
                            String arrival = inc.arrivalTime != null ? inc.arrivalTime.toString() : "";
                            tableModel.addRow(new Object[]{
                                    inc.id, inc.section, sevS, typeS, arrival, inc.medicalCondition
                            });
                        }

                        JOptionPane.showMessageDialog(null,
                                "Merged similar incidents under '" + newCondition + "'");
                    }
                }
            }
        });
// 🧩 Merge incidents when user edits the Medical Condition column
        tableModel.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.UPDATE) return;

            int row = e.getFirstRow();
            int column = e.getColumn();

            // Only run merge check if user edits the "Medical Condition" column (index 5)
            if (column == 5 && row >= 0) {
                String newCondition = (String) tableModel.getValueAt(row, column);
                if (newCondition == null || newCondition.isEmpty()) return;

                int section = (int) tableModel.getValueAt(row, 1);
                String sevStr = (String) tableModel.getValueAt(row, 2);
                int severity = Integer.parseInt(sevStr.substring(1)); // remove 'S'
                String typeStr = (String) tableModel.getValueAt(row, 3);
                int type = typeStr.equals("ALS") ? 1 : typeStr.equals("BLS") ? 2 : 3;

                // Fetch current incidents from the system
                java.util.List<Incident> allIncidents = system.getIncidents();
                Incident current = null;
                for (Incident inc : allIncidents) {
                    if (inc.id == (int) tableModel.getValueAt(row, 0)) {
                        current = inc;
                        inc.medicalCondition = newCondition;
                        break;
                    }
                }

                // Find other matching incidents
                java.util.List<Incident> toRemove = new java.util.ArrayList<>();
                for (Incident inc : allIncidents) {
                    if (inc != current &&
                            inc.section == section &&
                            inc.severity == severity &&
                            inc.type == type &&
                            inc.medicalCondition.equalsIgnoreCase(newCondition)) {

                        // ✅ Merge condition met
                        toRemove.add(inc);
                        System.out.println("Merged incident ID " + inc.id + " into ID " + current.id);
                    }
                }

                // Remove duplicates
                allIncidents.removeAll(toRemove);

                // Update the system
                system.setIncidents(allIncidents);

                // Refresh the table
                tableModel.setRowCount(0);
                for (Incident inc : system.getSortedIncidents()) {
                    String typeStr2 = inc.type == 1 ? "ALS" : inc.type == 2 ? "BLS" : "CCT";
                    String sevStr2 = "S" + inc.severity;
                    String arrivalStr = inc.arrivalTime != null ? inc.arrivalTime.toString() : "";
                    tableModel.addRow(new Object[]{
                            inc.id,
                            inc.section,
                            sevStr2,
                            typeStr2,
                            arrivalStr,
                            inc.medicalCondition
                    });
                }
            }
        });

        // Kebab menu
        JButton kebabBtn = new JButton("\u22EE"); // Unicode for vertical ellipsis
        kebabBtn.setFocusPainted(false);
        kebabBtn.setBorderPainted(false);
        kebabBtn.setContentAreaFilled(false);

        JPopupMenu kebabMenu = new JPopupMenu();
        JMenuItem chatItem = new JMenuItem("Chat");
        JMenuItem reportItem = new JMenuItem("Incident Report");
        kebabMenu.add(chatItem);
        kebabMenu.add(reportItem);

        // Set kebab menu and items to white
        kebabMenu.setBackground(Color.WHITE);
        kebabMenu.setOpaque(true);
        kebabMenu.setForeground(Color.BLACK);

        chatItem.setBackground(Color.WHITE);
        chatItem.setOpaque(true);
        chatItem.setForeground(Color.BLACK);

        reportItem.setBackground(Color.WHITE);
        reportItem.setOpaque(true);
        reportItem.setForeground(Color.BLACK);

        kebabBtn.addActionListener(e -> {
            kebabMenu.show(kebabBtn, kebabBtn.getWidth(), kebabBtn.getHeight());
        });

        // Show chat dialog
        chatItem.addActionListener(e -> {
            JDialog chatDialog = new JDialog(frame, "Chat", false);
            chatDialog.setContentPane(chatPanel);
            chatDialog.setSize(350, 250);
            chatDialog.setLocationRelativeTo(frame);
            chatDialog.setVisible(true);
        });

        // Show incident report dialog
        // Show incident report dialog
        reportItem.addActionListener(e -> {
            // Update table before showing
            tableModel.setRowCount(0);
            for (Incident inc : system.getSortedIncidents()) {
                String typeStr = inc.type == 1 ? "ALS" : inc.type == 2 ? "BLS" : "CCT";
                String sevStr = "S" + inc.severity;
                String arrivalStr = inc.arrivalTime != null ? inc.arrivalTime.toString() : "";

                // 🩺 Include the new medical condition column
                tableModel.addRow(new Object[]{
                        inc.id,
                        inc.section,
                        sevStr,
                        typeStr,
                        arrivalStr,
                        inc.medicalCondition
                });
            }

            JDialog reportDialog = new JDialog(frame, "Incident Report", false);
            reportDialog.setContentPane(tableScroll);
            reportDialog.setSize(700, 400); // slightly wider for new column
            reportDialog.setLocationRelativeTo(frame);
            reportDialog.setVisible(true);
        });


        // Place kebab button at top right
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(kebabBtn, BorderLayout.EAST);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(mapPanel, BorderLayout.CENTER);
        mainPanel.setBackground(Color.BLACK);

        final JButton returnBtn = new JButton("Return Ambulance");
        returnBtn.setEnabled(false);

        returnBtn.addActionListener(e -> {
            boolean changed = false;
            for (Unit u : system.units.values()) {
                if (!u.available) {
                    u.available = true;
                    int idx = u.section - 1;
                    u.setPosition(positions[idx][0] + 10, positions[idx][1] + 10);
                    changed = true;
                }
            }
            if (changed) {
                mapPanel.repaint();
                returnBtn.setEnabled(false);
            }
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(returnBtn);
        controlPanel.setBackground(Color.BLACK);

        mapPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                hovered[0] = -1;
                for (int i = 0; i < positions.length; i++) {
                    int x = positions[i][0];
                    int y = positions[i][1];
                    Ellipse2D circle = new Ellipse2D.Float(x, y, circleSize, circleSize);
                    if (circle.contains(p)) {
                        hovered[0] = i;
                        break;
                    }
                }
                mapPanel.repaint();
            }
        });

        mapPanel.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                hovered[0] = -1;
                mapPanel.repaint();
            }

            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                for (int i = 0; i < positions.length; i++) {
                    int x = positions[i][0];
                    int y = positions[i][1];
                    Ellipse2D circle = new Ellipse2D.Float(x, y, circleSize, circleSize);
                    if (circle.contains(p)) {
                        clicked[0] = i;
                        mapPanel.repaint();

                        int section = i + 1;
                        String[] types = {"ALS", "BLS", "CCT"};
                        int type = JOptionPane.showOptionDialog(frame, "Select Unit Type", "Unit Type",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, types, types[0]) + 1;
                        if (type < 1) {
                            clicked[0] = -1;
                            mapPanel.repaint();
                            return;
                        }

                        String[] statuses;
                        if (type == 1) statuses = new String[]{"S4", "S3"};
                        else if (type == 2) statuses = new String[]{"S3", "S2"};
                        else statuses = new String[]{"S2", "S1"};

                        int statusIdx = JOptionPane.showOptionDialog(frame, "Select Status", "Status",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, statuses, statuses[0]);
                        if (statusIdx < 0) {
                            clicked[0] = -1;
                            mapPanel.repaint();
                            return;
                        }
                        int severity = 4 - statusIdx;


// 🆕 Report incident with medical condition included
                        system.reportIncident(section, severity, type, "");

                        int confirm = JOptionPane.showConfirmDialog(frame, "Dispatch now?", "Dispatch",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            Unit dispatched = system.dispatchNearest(section);
                            if (dispatched != null) {
                                JOptionPane.showMessageDialog(frame,
                                        "Dispatched Unit " + dispatched.id + " from Section " + dispatched.section);
                                returnBtn.setEnabled(true);
                                dispatched.targetSection = section;
                            } else {
                                JOptionPane.showMessageDialog(frame, "No available units!");
                            }
                            mapPanel.repaint();
                        }

                        clicked[0] = -1;
                        mapPanel.repaint();
                        break;
                    }
                }
            }
        });

        new javax.swing.Timer(30, e -> {
            boolean moved = false;
            for (Unit u : system.units.values()) {
                if (u.targetSection > 0) {
                    int tx = positions[u.targetSection - 1][0] + 10;
                    int ty = positions[u.targetSection - 1][1] + 10;
                    int dx = tx - u.x, dy = ty - u.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > 2) {
                        u.x += (int) (dx / dist * 2);
                        u.y += (int) (dy / dist * 2);
                        moved = true;
                    } else {
                        u.x = tx;
                        u.y = ty;
                        u.available = false;
                        JOptionPane.showMessageDialog(frame, "Ambulance " + u.id + " has arrived at Section " + u.targetSection + "!");
                        u.targetSection = -1;
                    }
                }
            }
            if (moved) mapPanel.repaint();
        }).start();

        frame.getContentPane().setBackground(Color.BLACK);
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setUndecorated(true);
    }
}
