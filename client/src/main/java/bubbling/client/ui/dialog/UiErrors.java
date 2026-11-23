package bubbling.client.ui.dialog;

import bubbling.common.dto.system.ExceptionResponse;
import java.awt.Component;

/** Common error shape; no new transport error codes are assigned by the UI. */
public final class UiErrors {
    private UiErrors() {
    }

    public static void show(Component owner, ExceptionResponse error) {
        NotificationCards.error(owner, error.message());
    }

    public static void perform(Component owner, Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            show(owner, new ExceptionResponse(null, e.getMessage()));
        }
    }
}
