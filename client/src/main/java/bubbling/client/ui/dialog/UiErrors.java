package bubbling.client.ui.dialog;

import bubbling.common.dto.system.ExceptionResponse;
import javax.swing.*;
import java.awt.Component;

/** Common error shape; no new transport error codes are assigned by the UI. */
public final class UiErrors {
    private UiErrors() {
    }

    public static void show(Component owner, ExceptionResponse error) {
        JOptionPane.showMessageDialog(owner, error.message(), "안내", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void perform(Component owner, Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            show(owner, new ExceptionResponse(null, e.getMessage()));
        }
    }
}
