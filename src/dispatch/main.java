package dispatch;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.awt.geom.Ellipse2D;

public class main {
    public static void main(String[] args) {
        DispatchSystem system = new DispatchSystem();

        // Load background image
        Image bgImage = new ImageIcon("mapa.jpg").getImage();

        JFrame frame = new JFrame("Emergency Dispatch");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 840);

        int[][] positions = {{250,100},{750,150},{150,500},{850,350},{600,650},{950,580}};
        int circleSize = 40;
        final int[] hovered = { -1 };
        final int[] clicked = { -1 };

        // Map Panel
        JPanel mapPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw background image
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

                    // Show label only when hovered, with yellow text and black outline
                    if (i == hovered[0]) {
                        String label = "Section " + (i+1);
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
                    int idx = u.section - 1;
                    int ux = positions[idx][0] + 10;
                    int uy = positions[idx][1] + 10;
                    g.fillOval(ux, uy, 20, 20);
                }
            }
        };
        mapPanel.setPreferredSize(new Dimension(400, 350));

        // Incident Table Model
        String[] columns = {"ID", "Section", "Severity", "Type"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable incidentTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(incidentTable);

        // Tabbed Pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Map", mapPanel);
        tabs.addTab("Incident Reports", tableScroll);

        JButton returnBtn = new JButton("Return Ambulance");
        returnBtn.setEnabled(false);

        // Mouse listeners for hover and click
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
                        String[] types = {"ALS","BLS","CCT"};
                        int type = JOptionPane.showOptionDialog(frame, "Select Unit Type", "Unit Type", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, types, types[0]) + 1;
                        if (type < 1) {
                            clicked[0] = -1;
                            mapPanel.repaint();
                            return;
                        }

                        String[] statuses;
                        if (type == 1) statuses = new String[]{"S4","S3"};
                        else if (type == 2) statuses = new String[]{"S3","S2"};
                        else statuses = new String[]{"S2","S1"};

                        int statusIdx = JOptionPane.showOptionDialog(frame, "Select Status", "Status", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, statuses, statuses[0]);
                        if (statusIdx < 0) {
                            clicked[0] = -1;
                            mapPanel.repaint();
                            return;
                        }
                        int severity = 4 - statusIdx;

                        system.reportIncident(section, severity, type);

                        int confirm = JOptionPane.showConfirmDialog(frame, "Dispatch now?", "Dispatch", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            Unit dispatched = system.dispatchNearest(section);
                            if (dispatched != null) {
                                JOptionPane.showMessageDialog(frame, "Dispatched Unit " + dispatched.id + " from Section " + dispatched.section);
                                returnBtn.setEnabled(true);
                            } else {
                                JOptionPane.showMessageDialog(frame, "No available units!");
                            }
                            mapPanel.repaint();
                        }

                        // Update incident table
                        tableModel.setRowCount(0);
                        for (Incident inc : system.getSortedIncidents()) {
                            String typeStr = inc.type == 1 ? "ALS" : inc.type == 2 ? "BLS" : "CCT";
                            String sevStr = "S" + inc.severity;
                            tableModel.addRow(new Object[]{inc.id, inc.section, sevStr, typeStr});
                        }
                        clicked[0] = -1; // Reset after handling
                        mapPanel.repaint();
                        break;
                    }
                }
            }
        });

        returnBtn.addActionListener(e -> {
            boolean changed = false;
            for (Unit u : system.units.values()) {
                if (!u.available) {
                    u.available = true;
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

        frame.add(tabs, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setUndecorated(true);
    }
}
