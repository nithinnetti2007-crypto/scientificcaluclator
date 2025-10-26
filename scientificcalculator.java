

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ScientificCalculator extends JFrame {
    private final JTextField display;
    private double firstOperand = 0;
    private String operator = "";
    private boolean startNewNumber = true; // whether to start a new number on digit press
    private final JCheckBox degreesToggle;

    public ScientificCalculator() {
        super("Scientific Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(6, 6));

        display = new JTextField("0");
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setEditable(false);
        display.setFont(new Font("SansSerif", Font.BOLD, 28));
        display.setBackground(Color.WHITE);
        display.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(display, BorderLayout.NORTH);

        degreesToggle = new JCheckBox("Degrees");
        degreesToggle.setSelected(true);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(display, BorderLayout.CENTER);
        topPanel.add(degreesToggle, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel buttons = createButtonsPanel();
        add(buttons, BorderLayout.CENTER);

        // keyboard support (basic)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (Character.isDigit(c) || c == '.') {
                    pressDigit(String.valueOf(c));
                } else if (c == '+') pressOperator("+");
                else if (c == '-') pressOperator("-");
                else if (c == '*') pressOperator("×");
                else if (c == '/') pressOperator("÷");
                else if (c == '\n' || c == '=') calculateResult();
                else if (c == '\b') backspace();
            }
        });
        // make sure frame gets focus for key events
        setFocusable(true);

        setVisible(true);
    }

    private JPanel createButtonsPanel() {
        String[][] btns = {
            {"MC", "C", "⌫", "÷"},
            {"sin", "cos", "tan", "×"},
            {"ln", "log", "√", "-"},
            {"x^y", "x!", "1/x", "+"},
            {"7", "8", "9", "="},
            {"4", "5", "6", "±"},
            {"1", "2", "3", "."},
            {"0", "00", "Ans", " " } // 'Ans' returns last result; last cell empty spacer
        };

        JPanel panel = new JPanel(new GridLayout(btns.length, btns[0].length, 6, 6));
        for (String[] row : btns) {
            for (String label : row) {
                if (label.trim().isEmpty()) {
                    panel.add(new JLabel()); // spacer
                    continue;
                }
                JButton b = new JButton(label);
                b.setFont(new Font("SansSerif", Font.PLAIN, 18));
                b.addActionListener(e -> onButtonPress(label));
                panel.add(b);
            }
        }
        return panel;
    }

    private double lastAnswer = 0;

    private void onButtonPress(String label) {
        switch (label) {
            case "C" -> clearAll();
            case "MC" -> clearAll(); // simple behavior
            case "⌫" -> backspace();
            case "+" -> pressOperator("+");
            case "-" -> pressOperator("-");
            case "×" -> pressOperator("×");
            case "÷" -> pressOperator("÷");
            case "=" -> calculateResult();
            case "." -> pressDigit(".");
            case "±" -> toggleSign();
            case "00" -> pressDigit("00");
            case "Ans" -> setDisplay(removeTrailingZeros(lastAnswer));
            case "sin" -> applyUnary("sin");
            case "cos" -> applyUnary("cos");
            case "tan" -> applyUnary("tan");
            case "ln" -> applyUnary("ln");
            case "log" -> applyUnary("log10");
            case "√" -> applyUnary("sqrt");
            case "x!" -> applyUnary("fact");
            case "1/x" -> applyUnary("inv");
            case "x^y" -> pressOperator("^");
            default -> {
                // digits 0-9
                if (label.matches("\\d+") || label.equals(".")) pressDigit(label);
                else pressDigit(label);
            }
        }
        // request focus so keyboard events keep working
        requestFocusInWindow();
    }

    private void pressDigit(String d) {
        if (startNewNumber) {
            if (d.equals(".")) {
                setDisplay("0.");
                startNewNumber = false;
                return;
            } else {
                setDisplay(d);
                startNewNumber = false;
                return;
            }
        }
        String cur = display.getText();
        // avoid multiple dots
        if (d.equals(".") && cur.contains(".")) return;
        setDisplay(cur + d);
    }

    private void pressOperator(String op) {
        try {
            double cur = Double.parseDouble(display.getText());
            if (!operator.isEmpty() && !startNewNumber) {
                // chain calculation
                firstOperand = computeBinary(firstOperand, cur, operator);
                setDisplay(removeTrailingZeros(firstOperand));
            } else {
                firstOperand = cur;
            }
            operator = op;
            startNewNumber = true;
        } catch (NumberFormatException ex) {
            setDisplay("Error");
        }
    }

    private void calculateResult() {
        if (operator.isEmpty()) return;
        try {
            double secondOperand = Double.parseDouble(display.getText());
            double result = computeBinary(firstOperand, secondOperand, operator);
            setDisplay(removeTrailingZeros(result));
            lastAnswer = result;
            operator = "";
            startNewNumber = true;
        } catch (NumberFormatException ex) {
            setDisplay("Error");
        } catch (ArithmeticException ae) {
            setDisplay("Math Error");
        }
    }

    private double computeBinary(double a, double b, String op) {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "×" -> a * b;
            case "÷" -> {
                if (b == 0) throw new ArithmeticException("Divide by zero");
                yield a / b;
            }
            case "^" -> Math.pow(a, b);
            default -> b;
        };
    }

    private void applyUnary(String func) {
        try {
            double v = Double.parseDouble(display.getText());
            double res;
            switch (func) {
                case "sin" -> res = Math.sin(toRadiansIfNeeded(v));
                case "cos" -> res = Math.cos(toRadiansIfNeeded(v));
                case "tan" -> res = Math.tan(toRadiansIfNeeded(v));
                case "ln" -> {
                    if (v <= 0) { setDisplay("Math Error"); return; }
                    res = Math.log(v);
                }
                case "log10" -> {
                    if (v <= 0) { setDisplay("Math Error"); return; }
                    res = Math.log10(v);
                }
                case "sqrt" -> {
                    if (v < 0) { setDisplay("Math Error"); return; }
                    res = Math.sqrt(v);
                }
                case "inv" -> {
                    if (v == 0) { setDisplay("Math Error"); return; }
                    res = 1.0 / v;
                }
                case "fact" -> {
                    if (v < 0 || v != Math.floor(v) || v > 20) { // limit factorial input
                        setDisplay("Math Error");
                        return;
                    }
                    res = factorial((int) v);
                }
                default -> { res = v; }
            }
            setDisplay(removeTrailingZeros(res));
            lastAnswer = res;
            startNewNumber = true;
        } catch (NumberFormatException ex) {
            setDisplay("Error");
        }
    }

    private double toRadiansIfNeeded(double angle) {
        return degreesToggle.isSelected() ? Math.toRadians(angle) : angle;
    }

    private long factorial(int n) {
        long f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }

    private void toggleSign() {
        try {
            double v = Double.parseDouble(display.getText());
            v = -v;
            setDisplay(removeTrailingZeros(v));
        } catch (NumberFormatException ex) {
            setDisplay("Error");
        }
    }

    private void backspace() {
        String s = display.getText();
        if (s.length() <= 1) {
            setDisplay("0");
            startNewNumber = true;
            return;
        }
        setDisplay(s.substring(0, s.length() - 1));
    }

    private void clearAll() {
        setDisplay("0");
        firstOperand = 0;
        operator = "";
        startNewNumber = true;
        lastAnswer = 0;
    }

    private void setDisplay(String text) {
        display.setText(text);
    }

    private String removeTrailingZeros(double val) {
        // use BigDecimal to nicely strip trailing zeros
        BigDecimal bd = BigDecimal.valueOf(val).stripTrailingZeros();
        // For very large/small numbers, use plain string conversion
        String out = bd.toPlainString();
        return out;
    }

    public static void main(String[] args) {
        // Ensure GUI runs on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new ScientificCalculator());
    }
}
