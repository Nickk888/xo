package corp.NickAstafyev.xo;


import javax.swing.*;
import java.awt.*;

public class MyField extends JFrame {
    public MyField() {
        setTitle("Test Window");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(102, 60, 400, 400);
        JButton[] jbs = new JButton[5];
        for (int i = 0; i < 5; i++) {
            jbs[i] = new JButton("#" + i);
        }
        setLayout(new BorderLayout()); // выбор компоновщика элементов
        add(jbs[0], BorderLayout.EAST); // добавление кнопки на форму
        add(jbs[1], BorderLayout.WEST);
        add(jbs[2], BorderLayout.SOUTH);
        add(jbs[3], BorderLayout.NORTH);
        add(jbs[4], BorderLayout.CENTER);
        setVisible(true);
    }


}
